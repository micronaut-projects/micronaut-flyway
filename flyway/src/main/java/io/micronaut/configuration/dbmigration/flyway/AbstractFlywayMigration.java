/*
 * Copyright 2017-2018 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.micronaut.configuration.dbmigration.flyway;

import io.micronaut.configuration.dbmigration.flyway.event.MigrationFinishedEvent;
import io.micronaut.configuration.dbmigration.flyway.event.SchemaCleanedEvent;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.Async;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;

import javax.inject.Singleton;
import javax.sql.DataSource;

/**
 * Parent class that runs Flyway database migrations.
 *
 * @author James Kleeh
 * @author Iván López
 * @since 1.0.0
 */
@Singleton
class AbstractFlywayMigration {

    final ApplicationContext applicationContext;
    final ApplicationEventPublisher eventPublisher;

    /**
     * @param applicationContext The application context
     * @param eventPublisher     The event publisher
     */
    AbstractFlywayMigration(ApplicationContext applicationContext, ApplicationEventPublisher eventPublisher) {
        this.applicationContext = applicationContext;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Configure the Flyway runner for a specific configuration, a qualifier and a dataSource.
     *
     * @param config     The {@link FlywayConfigurationProperties}
     * @param name       The qualifier
     * @param dataSource The {@link DataSource}
     */
    void run(FlywayConfigurationProperties config, String name, DataSource dataSource) {
        FluentConfiguration fluentConfiguration = config.getFluentConfiguration();
        fluentConfiguration.dataSource(dataSource);

        Flyway flyway = fluentConfiguration.load();
        this.applicationContext.registerSingleton(Flyway.class, flyway, Qualifiers.byName(name), false);
        if (config.isEnabled()) {
            if (config.isAsync()) {
                runAsync(config, flyway);
            } else {
                runFlyway(config, flyway);
            }
        }
    }

    private void runFlyway(FlywayConfigurationProperties config, Flyway flyway) {
        if (config.isCleanSchema()) {
            flyway.clean();
            eventPublisher.publishEvent(new SchemaCleanedEvent(config));
        }
        flyway.migrate();
        eventPublisher.publishEvent(new MigrationFinishedEvent(config));
    }

    /**
     * Run a migration asynchronously.
     *
     * @param config The {@link FlywayConfigurationProperties}
     * @param flyway The {@link Flyway} already configured to run the migrations
     */
    @Async(TaskExecutors.IO)
    void runAsync(FlywayConfigurationProperties config, Flyway flyway) {
        runFlyway(config, flyway);
    }
}
