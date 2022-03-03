package io.micronaut.flyway

import groovy.sql.Sql
import io.micronaut.context.ApplicationContext
import io.micronaut.inject.qualifiers.Qualifiers
import spock.lang.AutoCleanup
import spock.lang.Shared

import javax.sql.DataSource
import java.sql.SQLException

import static io.micronaut.context.env.Environment.TEST

class FlywayMigratorSpec extends AbstractFlywaySpec {

    @Shared
    Map<String, Object> config = [
        'jpa.default.packages-to-scan'                 : ['example.micronaut'],
        'jpa.default.properties.hibernate.hbm2ddl.auto': 'none',
        'jpa.default.properties.hibernate.show_sql'    : true,

        'datasources.default.url'                      : 'jdbc:h2:mem:flywayDisabledForceMigrationsDb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
        'datasources.default.username'                 : DS_USERNAME,
        'datasources.default.password'                 : DS_PASSWORD,
        'datasources.default.driverClassName'          : DS_DRIVER,

        'flyway.enabled'                               : true,
        'flyway.datasources.default.enabled'           : false,
    ]

    @Shared
    @AutoCleanup
    ApplicationContext applicationContext = ApplicationContext.run(config as Map, TEST)

    void 'when migrations are disabled it is possible to run them using the FlywayMigrator'() {
        when:
        FlywayMigrator flywayMigrator = applicationContext.getBean(FlywayMigrator)
        DataSource dataSource = applicationContext.getBean(DataSource, Qualifiers.byName('default'))
        FlywayConfigurationProperties flywayConfigurationProperties = applicationContext.getBean(FlywayConfigurationProperties)
        Sql sql = newSql(config['datasources.default.url'] as String)

        then:
        noExceptionThrown()

        when:
        sql.rows('select count(*) from books')

        then: 'the migration was not already run'
        thrown(SQLException)

        when:
        flywayMigrator.run(flywayConfigurationProperties, dataSource)

        then:
        noExceptionThrown()

        and:
        conditions.eventually {
            sql.rows('select count(*) from books')[0][0] == 2
        }
    }
}
