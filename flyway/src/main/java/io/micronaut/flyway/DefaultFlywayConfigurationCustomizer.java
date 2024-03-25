package io.micronaut.flyway;

import io.micronaut.context.ApplicationContext;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.type.Argument;
import io.micronaut.inject.qualifiers.Qualifiers;
import org.flywaydb.core.api.ClassProvider;
import org.flywaydb.core.api.ResourceProvider;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.migration.JavaMigration;
import org.flywaydb.core.api.resolver.MigrationResolver;

/**
 * Default implementation of {@link FlywayConfigurationCustomizer}. Finds and configures all
 * {@link jakarta.inject.Named} instances of the following Flyway types:
 *
 * <ul>
 *   <li>{@link JavaMigration}[]</li>
 *   <li>{@link Callback}[]</li>
 *   <li>{@link MigrationResolver}[]</li>
 *   <li>{@link ResourceProvider}</li>
 *   <li>{@link ClassProvider}</li>
 * <ul/>
 *
 * @author Jeremy Grelle
 * @since 7.2.0
 */
public class DefaultFlywayConfigurationCustomizer implements FlywayConfigurationCustomizer {

    private final ApplicationContext applicationContext;
    private final String name;

    DefaultFlywayConfigurationCustomizer(ApplicationContext applicationContext, String name) {
        this.applicationContext = applicationContext;
        this.name = name;
    }

    @Override
    public void customizeFluentConfiguration(FluentConfiguration fluentConfiguration) {
        applicationContext.findBean(JavaMigration[].class, Qualifiers.byName(name))
            .ifPresent(fluentConfiguration::javaMigrations);

        applicationContext.findBean(Callback[].class, Qualifiers.byName(name))
            .ifPresent(fluentConfiguration::callbacks);

        applicationContext.findBean(MigrationResolver[].class, Qualifiers.byName(name))
            .ifPresent(fluentConfiguration::resolvers);

        applicationContext.findBean(ResourceProvider.class, Qualifiers.byName(name))
            .ifPresent(fluentConfiguration::resourceProvider);

        applicationContext.findBean(Argument.of(ClassProvider.class, Argument.of(JavaMigration.class)), Qualifiers.byName(name))
            .ifPresent(fluentConfiguration::javaMigrationClassProvider);
    }

    @Override
    public @NonNull String getName() {
        return this.name;
    }
}
