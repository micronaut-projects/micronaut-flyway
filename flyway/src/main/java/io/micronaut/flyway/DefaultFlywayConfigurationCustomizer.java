/*
 * Copyright 2017-2024 original authors
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
