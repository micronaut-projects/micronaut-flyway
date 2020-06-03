package io.micronaut.configuration.dbmigration.flyway

import groovy.sql.Sql
import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import org.h2.jdbc.JdbcSQLSyntaxErrorException
import spock.lang.Specification

import javax.sql.DataSource

class FlywayCleanSpec extends Specification {

    void 'test Flyway.clean is not run by default'() {
        given: 'an existing table in the database'
        Map db = [url: 'jdbc:h2:mem:flywayClean', user: 'sa', password: '', driver: 'org.h2.Driver']
        Sql sql = Sql.newInstance(db.url, db.user, db.password, db.driver)
        sql.execute('create table foo(id int not null primary key);')

        and:
        ApplicationContext applicationContext = ApplicationContext.run(
            [
                'spec.name'                                     : FlywayCleanSpec.simpleName,
                'datasources.default.url'                       : 'jdbc:h2:mem:flywayClean;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
                'datasources.default.username'                  : 'sa',
                'datasources.default.password'                  : '',
                'datasources.default.driverClassName'           : 'org.h2.Driver',

                'jpa.default.packages-to-scan'                  : ['example.micronaut'],
                'jpa.default.properties.hibernate.hbm2ddl.auto' : 'none',
                'jpa.default.properties.hibernate.show_sql'     : true,

                'flyway.datasources.default.locations'          : 'classpath:moremigrations',
                'flyway.datasources.default.baseline-on-migrate': true, // Avoid Flyway complains because the schema is non-empty
            ] as Map,
            Environment.TEST
        )

        when: 'running the migrations'
        applicationContext.getBean(DataSource)

        then: 'the previously existing foo table in the schema is still there'
        sql.rows('select count(*) from foo').get(0)[0] == 0
    }

    void 'test Flyway.clean is run when enabled'() {
        given: 'an existing table in the database'
        Map db = [url: 'jdbc:h2:mem:flywayClean2', user: 'sa', password: '', driver: 'org.h2.Driver']
        Sql sql = Sql.newInstance(db.url, db.user, db.password, db.driver)
        sql.execute('create table foo(id int not null primary key);')

        and:
        ApplicationContext applicationContext = ApplicationContext.run(
            [
                'spec.name'                                    : FlywayCleanSpec.simpleName,
                'datasources.default.url'                      : 'jdbc:h2:mem:flywayClean2;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
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

        and: 'running the migrations'
        applicationContext.getBean(DataSource)

        when: 'trying to access the "foo" table'
        sql.rows('select count(*) from foo').get(0)[0] == 0

        then: 'it has been deleted'
        def e = thrown(JdbcSQLSyntaxErrorException)
        e.message.contains('Table "FOO" not found')
    }
}
