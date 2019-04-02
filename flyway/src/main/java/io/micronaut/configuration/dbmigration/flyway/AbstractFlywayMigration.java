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
public class AbstractFlywayMigration {

    protected final ApplicationContext applicationContext;
    protected final ApplicationEventPublisher eventPublisher;

    // TODO: Change this
    public AbstractFlywayMigration(ApplicationContext applicationContext, ApplicationEventPublisher eventPublisher) {
        this.applicationContext = applicationContext;
        this.eventPublisher = eventPublisher;
    }

    public void run(FlywayConfigurationProperties config, String name, @Nullable DataSource dataSource) {
        FluentConfiguration fluentConfiguration = config.getFluentConfiguration();
        if (dataSource != null) {
            fluentConfiguration.dataSource(dataSource);
        }
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

//    public void run(DataSource dataSource, String name) {
//        applicationContext.findBean(
//                FlywayConfigurationProperties.class,
//                Qualifiers.byName(name)
//        ).ifPresent(config -> {
//            FluentConfiguration fluentConfiguration = config.getFluentConfiguration();
//            fluentConfiguration.dataSource(dataSource);
//            Flyway flyway = fluentConfiguration.load();
//            this.applicationContext.registerSingleton(Flyway.class, flyway, Qualifiers.byName(name), false);
//            if (config.isEnabled()) {
//                if (config.isAsync()) {
//                    runAsync(config, flyway);
//                } else {
//                    runFlyway(config, flyway);
//                }
//            }
//        });
//    }

//    public void run(FlywayConfigurationProperties config) {
//        String name = config.getNameQualifier();
//
//        FluentConfiguration fluentConfiguration = config.getFluentConfiguration();
//        Flyway flyway = fluentConfiguration.load();
//        this.applicationContext.registerSingleton(Flyway.class, flyway, Qualifiers.byName(name), false);
//        if (config.isEnabled()) {
//            if (config.isAsync()) {
//                runAsync(config, flyway);
//            } else {
//                runFlyway(config, flyway);
//            }
//        }
//    }

//    public void run(FlywayConfigurationProperties config, DataSource dataSource) {
//        String name = config.getNameQualifier();
//
//        FluentConfiguration fluentConfiguration = config.getFluentConfiguration();
//        if (dataSource != null) {
//            fluentConfiguration.dataSource(dataSource);
//        } else {
//            fluentConfiguration;
//        }
//        Flyway flyway = fluentConfiguration.load();
//        this.applicationContext.registerSingleton(Flyway.class, flyway, Qualifiers.byName(name), false);
//        if (config.isEnabled()) {
//            if (config.isAsync()) {
//                runAsync(config, flyway);
//            } else {
//                runFlyway(config, flyway);
//            }
//        }
//    }

    private void runFlyway(FlywayConfigurationProperties config, Flyway flyway) {
        if (config.isCleanSchema()) {
            flyway.clean();
            eventPublisher.publishEvent(new SchemaCleanedEvent(config));
        }
        flyway.migrate();
        eventPublisher.publishEvent(new MigrationFinishedEvent(config));
    }

    @Async(TaskExecutors.IO)
    public void runAsync(FlywayConfigurationProperties config, Flyway flyway) {
        runFlyway(config, flyway);
    }
}
