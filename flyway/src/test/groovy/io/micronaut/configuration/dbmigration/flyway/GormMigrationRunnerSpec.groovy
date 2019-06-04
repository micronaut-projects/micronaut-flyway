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

import grails.gorm.annotation.Entity
import groovy.sql.Sql
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Requires
import io.micronaut.context.env.Environment
import org.flywaydb.core.Flyway
import spock.lang.Specification

class GormMigrationRunnerSpec extends Specification {

    void "test migrations run when using GORM"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(
            [
             'spec.name'                           : GormMigrationRunnerSpec.simpleName,
             'dataSource.dbCreate'                 : 'none',
             'dataSource.url'                      : 'jdbc:h2:mem:flywayGormDB1', //;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=TRUE',
             'dataSource.driverClassName'          : 'org.h2.Driver',
             'dataSource.username'                 : 'sa',
             'dataSource.password'                 : '',
             'flyway.datasources.default.locations': 'classpath:databasemigrations',
            ] as Map,
            Environment.TEST
        )

        when:
        applicationContext.getBean(GormMigrationRunner)

        then:
        noExceptionThrown()

        when:
        applicationContext.getBean(Flyway)

        then:
        noExceptionThrown()

        when:
        Map db = [url: 'jdbc:h2:mem:flywayGormDB1', user: 'sa', password: '', driver: 'org.h2.Driver']
        Sql sql = Sql.newInstance(db.url, db.user, db.password, db.driver)

        then: 'the migrations have been executed'
        sql.rows('select count(*) from books').get(0)[0] == 2

        cleanup:
        applicationContext.close()
    }

    void "test migrations run when using multiple datasources in GORM"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(
            [
                'spec.name'                           : GormMigrationRunnerSpec.simpleName,
                'dataSource.dbCreate'                 : 'none',
                'dataSource.url'                      : 'jdbc:h2:mem:flywayGormDB2', //;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=TRUE',
                'dataSource.driverClassName'          : 'org.h2.Driver',
                'dataSource.username'                 : 'sa',
                'dataSource.password'                 : '',
                'dataSources.another.dbCreate'        : 'none',
                'dataSources.another.url'             : 'jdbc:h2:mem:flywayGormDB3', //;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=TRUE',
                'dataSources.another.driverClassName' : 'org.h2.Driver',
                'dataSources.another.username'        : 'sa',
                'dataSources.another.password'        : '',
                'flyway.datasources.default.locations': 'classpath:databasemigrations',
                'flyway.datasources.another.locations': 'classpath:databasemigrations',
            ] as Map,
            Environment.TEST
        )

        when: 'connecting to the default datasource'
        Map db = [url: 'jdbc:h2:mem:flywayGormDB2', user: 'sa', password: '', driver: 'org.h2.Driver']
        Sql sql = Sql.newInstance(db.url, db.user, db.password, db.driver)

        then: 'the migrations have been executed'
        sql.rows('select count(*) from books').get(0)[0] == 2

        when: 'connecting to another datasource'
        Map db2 = [url: 'jdbc:h2:mem:flywayGormDB3', user: 'sa', password: '', driver: 'org.h2.Driver']
        Sql sql2 = Sql.newInstance(db.url, db.user, db.password, db.driver)

        then: 'the migrations have been executed'
        sql2.rows('select count(*) from books').get(0)[0] == 2

        cleanup:
        applicationContext.close()
    }
}

@Entity
@Requires(property = 'spec.name', value = 'GormMigrationRunnerSpec')
class Book {
    String name

    static mapping = {
        name sqlType: 'varchar(255)'
    }

    static constraints = {
        name nullable: false, unique: true
    }
}
