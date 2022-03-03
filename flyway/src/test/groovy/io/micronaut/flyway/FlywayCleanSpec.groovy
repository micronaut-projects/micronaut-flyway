package io.micronaut.flyway

import groovy.sql.Sql
import org.h2.jdbc.JdbcSQLSyntaxErrorException

import javax.sql.DataSource

class FlywayCleanSpec extends AbstractFlywaySpec {

    void 'test Flyway.clean is not run by default'() {
        given: 'an existing table in the database'
        Sql sql = newSql('jdbc:h2:mem:flywayClean')
        sql.execute('create table foo(id int not null primary key)')

        and:
        run('spec.name'                                     : FlywayCleanSpec.simpleName,
            'datasources.default.url'                       : 'jdbc:h2:mem:flywayClean;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
            'datasources.default.username'                  : DS_USERNAME,
            'datasources.default.password'                  : DS_PASSWORD,
            'datasources.default.driverClassName'           : DS_DRIVER,

            'jpa.default.packages-to-scan'                  : ['example.micronaut'],
            'jpa.default.properties.hibernate.hbm2ddl.auto' : 'none',
            'jpa.default.properties.hibernate.show_sql'     : true,

            'flyway.datasources.default.locations'          : 'classpath:moremigrations',
            'flyway.datasources.default.baseline-on-migrate': true) // Avoid Flyway complains because the schema is non-empty

        when: 'running the migrations'
        applicationContext.getBean(DataSource)

        then: 'the previously existing foo table in the schema is still there'
        sql.rows('select count(*) from foo')[0][0] == 0
    }

    void 'test Flyway.clean is run when enabled'() {
        given: 'an existing table in the database'
        Sql sql = newSql('jdbc:h2:mem:flywayClean2')
        sql.execute('create table foo(id int not null primary key);')

        and:
        run('spec.name'                                    : FlywayCleanSpec.simpleName,
            'datasources.default.url'                      : 'jdbc:h2:mem:flywayClean2;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
            'datasources.default.username'                 : DS_USERNAME,
            'datasources.default.password'                 : DS_PASSWORD,
            'datasources.default.driverClassName'          : DS_DRIVER,

            'jpa.default.packages-to-scan'                 : ['example.micronaut'],
            'jpa.default.properties.hibernate.hbm2ddl.auto': 'none',
            'jpa.default.properties.hibernate.show_sql'    : true,

            'flyway.datasources.default.locations'         : 'classpath:moremigrations',
            'flyway.datasources.default.clean-schema'      : true)

        and: 'running the migrations'
        applicationContext.getBean(DataSource)

        when: 'trying to access the "foo" table'
        sql.rows('select count(*) from foo')[0][0] == 0

        then: 'it has been deleted'
        JdbcSQLSyntaxErrorException e = thrown()
        e.message.contains('Table "FOO" not found')
    }
}
