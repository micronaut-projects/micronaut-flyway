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

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.condition.Condition;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.inject.qualifiers.Qualifiers;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import javax.sql.DataSource;
import java.util.Optional;

/**
 * Factory used to create {@link Flyway} beans with the configuration defined in {@link FlywayConfigurationProperties}.
 *
 * @author Iván López
 * @since 1.0.0
 */
//@Factory
@Singleton
public class FlywayFactory extends AbstractFlywayMigration implements BeanCreatedEventListener<FlywayConfigurationProperties> {

    static final Logger LOG = LoggerFactory.getLogger(Condition.class);

    public FlywayFactory(ApplicationContext applicationContext, ApplicationEventPublisher eventPublisher) {
        super(applicationContext, eventPublisher);
    }

    @Override
    public FlywayConfigurationProperties onCreated(BeanCreatedEvent<FlywayConfigurationProperties> event) {
        FlywayConfigurationProperties config = event.getBean();

        if (config.isEnabled()) {
            if (config.hasAlternativeDatabaseConfiguration()) {
//                applicationContext.registerSingleton(DataSource.class,
//                        new DriverDataSource(Thread.currentThread().getContextClassLoader(), null,
//                                config.getUrl(), config.getUser(), config.getPassword()),
//                        Qualifiers.byName(config.getNameQualifier()),
//                        false);

                config.getFluentConfiguration().dataSource(config.getUrl(), config.getUser(), config.getPassword());
                run(config, config.getNameQualifier(), null);

//                run(, config.getNameQualifier());
                // here!
            } else {
                // check that datasource exists with the name
                //findBean().orElse
                Optional<DataSource> dataSource = applicationContext.findBean(DataSource.class, Qualifiers.byName(config.getNameQualifier()));
                if (dataSource.isPresent()) {
//                    config.getFluentConfiguration().dataSource(dataSource.get());
//                    run(config);
////                    run(config, dataSource.get(), config.getNameQualifier());
                } else {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("xxxx");
                    }
                }

            }
        }

        return config;

    }

//    public FlywayFactory(ApplicationEventPublisher eventPublisher, ApplicationContext applicationContext) {
//        this.eventPublisher = eventPublisher;
//        this.applicationContext = applicationContext;
//    }


//    @BeanCreatedEventListener
//    public FlywayConfigurationProperties onCreated2(BeanCreatedEvent<FlywayConfigurationProperties> event) {
//        FlywayConfigurationProperties config = event.getBean();
//
//        if (config.isEnabled()) {
//            if (config.hasAlternativeDatabaseConfiguration()) {
//                applicationContext.registerSingleton(DataSource.class,
//                        new DriverDataSource(Thread.currentThread().getContextClassLoader(), null,
//                                config.getUrl(), config.getUser(), config.getPassword()),
//                        Qualifiers.byName(config.getNameQualifier()),
//                        false);
//            }
//        }
//
//        return config;
//    }
//
//    @BeanCreatedEventListener
//    public DataSource onCreated(BeanCreatedEvent<DataSource> event) {
//        System.out.println("event = [" + event + "]");
//
//        //check if config is enabled
//        DataSource dataSource = event.getBean();
//
//        // Get the name from datasource
//        String name = "default";
//        FlywayConfigurationProperties config = applicationContext.getBean(
//                FlywayConfigurationProperties.class,
//                Qualifiers.byName(name)
//        );
//
//
//        FluentConfiguration fluentConfiguration = config.fluentConfiguration;
//        fluentConfiguration.dataSource(dataSource);
//        Flyway flyway = fluentConfiguration.load();
//        this.applicationContext.registerSingleton(Flyway.class, flyway, Qualifiers.byName(name), false);
//        if (config.isEnabled()) {
//            if (config.isAsync()) {
//
//                runAsync(config, flyway);
//            } else {
//                run(config, flyway);
//            }
//        }
//
//        return dataSource;
//    }
//
//    @Async(TaskExecutors.IO)
//    public void runAsync(FlywayConfigurationProperties config, Flyway flyway) {
//        run(config, flyway);
//    }
//
//    //        @Requires(condition = FlywayCondition.class)
////    @EachBean(FlywayConfigurationProperties.class)
////    private Flyway createFlyway(FlywayConfigurationProperties config) {
////        FluentConfiguration fluentConfiguration = config.fluentConfiguration;
////        if (config.hasAlternativeDatabaseConfiguration()) {
////            fluentConfiguration.dataSource(config.getUrl(), config.getUser(), config.getPassword());
////        } else {
////            fluentConfiguration.dataSource(config.getDataSource());
////        }
////
////        return fluentConfiguration.load();
////    }
//
//
////    /**
////     * Creates a {@link Flyway} bean per datasource if the configuration is correct.
////     *
////     * @param config The Flyway configuration
////     * @return The Flyway bean configured
////     */
////    @Context
////    @Requires(condition = FlywayAlternativeDatabaseCondition.class)
////    @EachBean(FlywayConfigurationProperties.class)
////    public Flyway flyway(@Parameter FlywayConfigurationProperties config
////                         ) {
////
////        System.out.println("config = [" + config + "]");
////
////        FluentConfiguration fluentConfiguration = config.getFluentConfiguration();
////        if (config.hasAlternativeDatabaseConfiguration()) {
////            fluentConfiguration.dataSource(config.getUrl(), config.getUser(), config.getPassword());
////        }
//////        else {
//////            fluentConfiguration.dataSource(dataSource);
//////        }
////
////        Flyway flyway = fluentConfiguration.load();
////        if (config.isCleanSchema()) {
////            flyway.clean();
////            eventPublisher.publishEvent(new SchemaCleanedEvent(config));
////        }
////        flyway.migrate();
////        eventPublisher.publishEvent(new MigrationFinishedEvent(config));
////
////        return flyway;
////    }
////
////    public void run(FlywayConfigurationProperties flywayConfigurationProperties, Flyway flyway) {
////        flywayConfigurationProperties.stream()
////                .filter(c -> c.isAsync() == async)
////                .map(c ->
////                        new Pair<>(c, applicationContext.findBean(Flyway.class, Qualifiers.byName(c.getNameQualifier()))))
////                .filter(pair -> pair.getSecond().isPresent())
////                .forEach(pair -> {
////                    FlywayConfigurationProperties config = pair.getFirst();
////                    Flyway flyway = pair.getSecond().get();
////                    if (config.isCleanSchema()) {
////                        flyway.clean();
////                        eventPublisher.publishEvent(new SchemaCleanedEvent(config));
////                    }
////                    flyway.migrate();
////                    eventPublisher.publishEvent(new MigrationFinishedEvent(config));
////                });
////    }
//
//    public void run(FlywayConfigurationProperties config, Flyway flyway) {
//        if (config.isCleanSchema()) {
//            flyway.clean();
//            eventPublisher.publishEvent(new SchemaCleanedEvent(config));
//        }
//        flyway.migrate();
//        eventPublisher.publishEvent(new MigrationFinishedEvent(config));
//    }
}
