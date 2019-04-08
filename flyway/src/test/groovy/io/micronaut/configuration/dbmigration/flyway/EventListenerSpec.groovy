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

package io.micronaut.configuration.dbmigration.flyway

import io.micronaut.configuration.dbmigration.flyway.event.MigrationFinishedEvent
import io.micronaut.configuration.dbmigration.flyway.event.SchemaCleanedEvent
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Requires
import io.micronaut.context.env.Environment
import io.micronaut.runtime.event.annotation.EventListener
import org.flywaydb.core.Flyway
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import javax.inject.Singleton
import javax.sql.DataSource

class EventListenerSpec extends Specification {

    void 'test SchemaCleaned and MigrationFinished events are fired when Flyway cleans and migrate the schema'() {
        given: 'a configuration to clean the schema and run the migrations'
        ApplicationContext applicationContext = ApplicationContext.run(
            [
                'spec.name'                                    : EventListenerSpec.simpleName,
                'datasources.default.url'                      : 'jdbc:h2:mem:flywayEvent;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
                'datasources.default.username'                 : 'sa',
                'datasources.default.password'                 : '',
                'datasources.default.driverClassName'          : 'org.h2.Driver',

                'jpa.default.packages-to-scan'                 : ['example.micronaut'],
                'jpa.default.properties.hibernate.hbm2ddl.auto': 'none',
                'jpa.default.properties.hibernate.show_sql'    : true,

                'flyway.datasources.default.locations'         : 'classpath:moremigrations',
                'flyway.datasources.default.clean-schema'      : true,
            ] as Map,
            Environment.TEST
        )

        when: 'running the migrations'
        applicationContext.getBean(DataSource)
        applicationContext.getBean(FlywayConfigurationProperties)
        applicationContext.getBean(Flyway)

        then: 'the events are fired'
        new PollingConditions().eventually {
            applicationContext.getBean(TestEventListener).migrationFinishedEvents.size() == 1
            applicationContext.getBean(TestEventListener).schemaCleanedEvents.size() == 1
        }

    }

    @Singleton
    @Requires(property = 'spec.name', value = 'EventListenerSpec')
    static class TestEventListener {
        List<MigrationFinishedEvent> migrationFinishedEvents = []
        List<SchemaCleanedEvent> schemaCleanedEvents = []

        @EventListener
        void onStartupMigrate(MigrationFinishedEvent event) {
            migrationFinishedEvents.add(event)
        }

        @EventListener
        void onStartupCleaned(SchemaCleanedEvent event) {
            schemaCleanedEvents.add(event)
        }
    }
}
