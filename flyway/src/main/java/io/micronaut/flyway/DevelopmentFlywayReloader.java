/*
 * Copyright 2017-2026 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.flyway;

import io.micronaut.context.WatchableBeanContext;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.DevelopmentActive;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.context.reload.ResourceKind;
import io.micronaut.context.watch.ReloadingConfigurationWatcher;
import io.micronaut.context.watch.ResourceChange;
import io.micronaut.core.annotation.Internal;
import io.micronaut.dev.DevRuntime;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.scheduling.io.watch.FileChangeBatch;
import io.micronaut.scheduling.io.watch.FileWatcher;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.configuration.ClassicConfiguration;
import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.api.output.ValidateResult;
import org.flywaydb.core.api.pattern.ValidatePattern;
import org.flywaydb.core.internal.plugin.PluginRegister;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Restarts the application in development mode when a migration changes, so that Flyway, which runs as the application
 * starts, applies it. Nothing else runs Flyway, and nothing of this bean exists outside development mode.
 * <p>
 * It watches the locations of the enabled data sources: a {@code classpath:} location through the resources below every
 * resource root of the runtime, and a {@code filesystem:} location through the file watcher. A file changed there is
 * only a reason to ask: the application restarts when a start would apply something. A note kept beside the migrations,
 * a save of the same content or a migration that a restart applied already restarts nothing.
 * <p>
 * An applied migration that was edited or removed is one Flyway would refuse, so a start could only fail: the error is
 * logged and the application goes on with the schema it has, until the file is put back. A restart for another reason
 * fails on it, which the runtime reports, and the next change starts a generation again. Nothing here repairs the schema
 * history or cleans the schema; with {@code clean-schema} a start cleans first and so can apply the edit, and restarts.
 * <p>
 * The restart comes after the batch that reported the change, and only if the batch did not restart the application
 * itself, as it does for a class edited with the migration: that one generation applies the migration.
 *
 * @since 8.2.0
 */
@Internal
@Context
@DevelopmentActive
@Requires(classes = DevRuntime.class)
@Requires(beans = DevRuntime.class)
final class DevelopmentFlywayReloader {

    private static final Logger LOG = LoggerFactory.getLogger(DevelopmentFlywayReloader.class);
    private static final String CONFIGURATION_PREFIX = "flyway";

    private final WatchableBeanContext context;
    private final DevRuntime runtime;
    private final @Nullable FileWatcher fileWatcher;
    private final AtomicBoolean restarting = new AtomicBoolean();

    /**
     * @param context The context
     * @param runtime The development runtime, which restarts the application
     * @param fileWatcher The file watcher, for a location outside the resource roots
     */
    DevelopmentFlywayReloader(WatchableBeanContext context, DevRuntime runtime, @Nullable FileWatcher fileWatcher) {
        this.context = context;
        this.runtime = runtime;
        this.fileWatcher = fileWatcher;
        for (ResourceKind kind : ResourceKind.values()) {
            context.resources(kind).watch(this::resourcesChanged);
        }
        // what the configuration says applies when Flyway runs, as the application starts
        context.configuration(CONFIGURATION_PREFIX).watchReloading(change -> ReloadingConfigurationWatcher.Outcome.REQUIRES_RESTART);
    }

    /**
     * Watches the {@code filesystem:} locations, once the data sources created their {@link Flyway}.
     *
     * @param event The event
     */
    @EventListener
    void onStartup(StartupEvent event) {
        if (fileWatcher == null) {
            return;
        }
        for (Configuration configuration : enabledConfigurations()) {
            for (Location location : configuration.getLocations()) {
                if (location.isFileSystem()) {
                    Path directory = Path.of(location.getRootPath()).toAbsolutePath().normalize();
                    if (Files.isDirectory(directory)) {
                        fileWatcher.directory(directory).watch(batch -> filesChanged(batch, location, configuration));
                    } else {
                        LOG.debug("The Flyway location {} is no directory: it is not watched", location);
                    }
                }
            }
        }
    }

    private void resourcesChanged(ResourceChange change) {
        if (change.initial()) {
            return;
        }
        List<Path> files = new ArrayList<>(change.changed());
        files.addAll(change.removed());
        Set<Configuration> affected = new LinkedHashSet<>();
        for (Configuration configuration : enabledConfigurations()) {
            for (Location location : configuration.getLocations()) {
                if (location.isClassPath() && anyUnderClassPathLocation(location, change.roots(), files)) {
                    affected.add(configuration);
                }
            }
        }
        if (!affected.isEmpty()) {
            restartOnceTheBatchIsDone(affected);
        }
    }

    private void filesChanged(FileChangeBatch batch, Location location, Configuration configuration) {
        for (Path file : batch.paths()) {
            if (matches(location, location.getRootPath() + '/' + resourceName(batch.root().relativize(file)))) {
                restartOnceTheBatchIsDone(Set.of(configuration));
                return;
            }
        }
    }

    private static boolean anyUnderClassPathLocation(Location location, List<Path> roots, List<Path> files) {
        for (Path file : files) {
            for (Path root : roots) {
                if (file.startsWith(root) && !file.equals(root) && matches(location, resourceName(root.relativize(file)))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean matches(Location location, String path) {
        String root = location.getRootPath();
        return (root.isEmpty() || path.startsWith(root + '/')) && location.matchesPath(path);
    }

    private static String resourceName(Path relative) {
        StringBuilder name = new StringBuilder();
        for (Path segment : relative) {
            if (!name.isEmpty()) {
                name.append('/');
            }
            name.append(segment);
        }
        return name.toString();
    }

    private List<Configuration> enabledConfigurations() {
        List<Configuration> configurations = new ArrayList<>();
        for (FlywayConfigurationProperties properties : context.getBeansOfType(FlywayConfigurationProperties.class)) {
            if (properties.isEnabled()) {
                // no data source of the name, no Flyway: a restart would apply nothing
                context.findBean(Flyway.class, Qualifiers.byName(properties.getNameQualifier()))
                    .ifPresent(flyway -> configurations.add(flyway.getConfiguration()));
            }
        }
        return configurations;
    }

    /**
     * A resource watch is told in the middle of a batch, on the runtime's thread, which a restart waits for, and before
     * the batch knows whether a class changed. So the decision is left to a thread of its own, when the batch is done.
     */
    private void restartOnceTheBatchIsDone(Set<Configuration> affected) {
        Thread.ofPlatform().daemon().name("micronaut-flyway-reload").start(() -> {
            try {
                while (runtime.isReloading()) {
                    runtime.whenReady().join();
                }
                // a batch that restarted the application, whichever one it was, ran Flyway with what the change left
                if (context.equals(runtime.context().orElse(null)) && !runtime.isStartFailed()
                    && affected.stream().anyMatch(DevelopmentFlywayReloader::startWouldApply) && restarting.compareAndSet(false, true)) {
                    LOG.info("Restarting the application for the Flyway migrations that changed");
                    runtime.restart();
                }
            } catch (RuntimeException e) {
                // the runtime reports a generation that fails to start itself
                LOG.debug("The restart for the Flyway migrations that changed did not complete: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * Whether a start would apply something, which logs what Flyway refuses instead. It asks a {@link Flyway} of its own
     * with plugins of its own, since the plugin that scans the class path keeps what it found, and configurations copied
     * from one another share it. Pending migrations are no failure, as they are none to a start.
     */
    private static boolean startWouldApply(Configuration configuration) {
        try {
            ClassicConfiguration fresh = new ClassicConfiguration(configuration);
            fresh.setPluginRegister(new PluginRegister());
            List<ValidatePattern> ignored = new ArrayList<>(List.of(configuration.getIgnoreMigrationPatterns()));
            ignored.add(ValidatePattern.fromPattern("*:pending"));
            fresh.setIgnoreMigrationPatterns(ignored.toArray(ValidatePattern[]::new));
            Flyway flyway = new Flyway(fresh);
            ValidateResult validation = flyway.validateWithResult();
            if (!validation.validationSuccessful) {
                if (!configuration.isCleanDisabled()) {
                    // clean-schema: a start cleans the schema first, and applies the migrations as they are now
                    return true;
                }
                if (configuration.isValidateOnMigrate()) {
                    LOG.error("Flyway refuses the migrations as they are now, so the application keeps running and is not restarted. Put the applied migration back, and make the change in a new one: {}",
                        validation.getAllErrorMessages());
                    return false;
                }
            }
            return flyway.info().pending().length > 0;
        } catch (FlywayException e) {
            LOG.debug("Flyway cannot tell what changed, which a start reports: {}", e.getMessage(), e);
            return true;
        }
    }
}
