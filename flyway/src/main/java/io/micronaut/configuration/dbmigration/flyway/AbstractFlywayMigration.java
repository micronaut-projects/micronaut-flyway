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

import javax.annotation.Nullable;
import javax.inject.Singleton;
import javax.sql.DataSource;

@Singleton
class AbstractFlywayMigration {

    final ApplicationContext applicationContext;
    final ApplicationEventPublisher eventPublisher;

    AbstractFlywayMigration(ApplicationContext applicationContext, ApplicationEventPublisher eventPublisher) {
        this.applicationContext = applicationContext;
        this.eventPublisher = eventPublisher;
    }

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

    @Async(TaskExecutors.IO)
    void runAsync(FlywayConfigurationProperties config, Flyway flyway) {
        runFlyway(config, flyway);
    }
}
