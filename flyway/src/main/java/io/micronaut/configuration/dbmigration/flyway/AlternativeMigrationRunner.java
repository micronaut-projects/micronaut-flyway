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
import org.flywaydb.core.internal.jdbc.DriverDataSource;
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
@Singleton
public class AlternativeMigrationRunner extends AbstractFlywayMigration implements BeanCreatedEventListener<FlywayConfigurationProperties> {

    static final Logger LOG = LoggerFactory.getLogger(Condition.class);

    public AlternativeMigrationRunner(ApplicationContext applicationContext,
                                      ApplicationEventPublisher eventPublisher) {
        super(applicationContext, eventPublisher);
    }

    @Override
    public FlywayConfigurationProperties onCreated(BeanCreatedEvent<FlywayConfigurationProperties> event) {
        FlywayConfigurationProperties config = event.getBean();
        String name = config.getNameQualifier();

        if (config.isEnabled()) {
            if (config.hasAlternativeDatabaseConfiguration()) {
                DataSource dataSource = new DriverDataSource(Thread.currentThread().getContextClassLoader(), null, config.getUrl(), config.getUser(), config.getPassword());
                run(config, name, dataSource);
            } else {
                if (!applicationContext.containsBean(DataSource.class, Qualifiers.byName(config.getNameQualifier()))) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("* Flyway bean not created for identifier [" + name + "] because no data source was found with a named qualifier of the same name.");
                    }
                }
            }
        }

        return config;
    }
}
