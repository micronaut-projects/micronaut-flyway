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
package io.micronaut.flyway

import io.micronaut.context.ApplicationContext
import org.flywaydb.core.Flyway
import spock.lang.Specification
import spock.lang.Unroll

import javax.sql.DataSource

/**
 * The module's tests have no micronaut-dev on their classpath, as an application outside development mode has none.
 */
class DevelopmentFlywayReloaderSpec extends Specification {

    @Unroll
    void 'an application with #mode starts and migrates without micronaut-dev on its classpath'() {
        when:
        Class.forName('io.micronaut.dev.DevRuntime')

        then:
        thrown(ClassNotFoundException)

        when:
        ApplicationContext context = ApplicationContext.run(properties + [
            'datasources.default.url'              : "jdbc:h2:mem:reloader-${UUID.randomUUID()}".toString(),
            'datasources.default.driver-class-name': 'org.h2.Driver',
            'datasources.default.username'         : 'sa',
            'datasources.default.password'         : '',
            'flyway.datasources.default.enabled'   : true
        ])

        then:
        context.getBean(DataSource)
        context.containsBean(Flyway)
        !context.containsBean(DevelopmentFlywayReloader)

        cleanup:
        context?.close()

        where:
        mode                     | properties
        'no development mode' | [:]
        'development mode on' | ['micronaut.dev.enabled': true]
    }
}
