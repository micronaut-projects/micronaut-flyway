/*
 * Copyright 2017-2020 original authors
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
package io.micronaut.flyway.graalvm;

import io.micronaut.context.condition.Condition;
import io.micronaut.core.annotation.Internal;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.resource.LoadableResource;
import org.flywaydb.core.internal.resource.classpath.ClassPathResource;
import org.flywaydb.core.internal.scanner.classpath.ResourceAndClassScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * This class is used in order to prevent Flyway from doing classpath scanning which is both slow and won't work in
 * native mode.
 *
 * Forked from the Quarkus: https://github.com/quarkusio/quarkus/blob/7a5efed2a97d88656484b431b472210e2bb7d2f3/extensions/flyway/runtime/src/main/java/io/quarkus/flyway/runtime/QuarkusPathLocationScanner.java
 *
 * @author Iván López
 * @since 2.0.0
 */
@SuppressWarnings("rawtypes")
@Internal
final class MicronautPathLocationScanner implements ResourceAndClassScanner {

    private static final Logger LOG = LoggerFactory.getLogger(Condition.class);
    private static final String LOCATION_SEPARATOR = "/";
    private static List<String> applicationMigrationFiles;

    private final Collection<LoadableResource> scannedResources = new ArrayList<>();

    public MicronautPathLocationScanner(Collection<Location> locations) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        for (String migrationFile : applicationMigrationFiles) {
            if (canHandleMigrationFile(locations, migrationFile)) {
                LOG.debug("Loading %{}", migrationFile);
                scannedResources.add(new ClassPathResource(null, migrationFile, classLoader, UTF_8));
            }
        }
    }

    @Override
    public Collection<LoadableResource> scanForResources() {
        return scannedResources;
    }

    @Override
    public Collection<Class<?>> scanForClasses() {
        // Classes are not supported in native mode
        return Collections.emptyList();
    }

    public static void setApplicationMigrationFiles(List<String> applicationMigrationFiles) {
        MicronautPathLocationScanner.applicationMigrationFiles = applicationMigrationFiles;
    }

    private boolean canHandleMigrationFile(Collection<Location> locations, String migrationFile) {
        for (Location location : locations) {
            String locationPath = location.getPath();
            if (!locationPath.endsWith(LOCATION_SEPARATOR)) {
                locationPath += "/";
            }

            if (migrationFile.startsWith(locationPath)) {
                return true;
            }

            LOG.debug("Migration file '{}' will be ignored because it does not start with '{}'", migrationFile, locationPath);
        }

        return false;
    }
}
