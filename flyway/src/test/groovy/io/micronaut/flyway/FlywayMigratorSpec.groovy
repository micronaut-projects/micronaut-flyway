package io.micronaut.flyway

import groovy.sql.Sql
import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.inject.qualifiers.Qualifiers
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import javax.sql.DataSource
import java.sql.SQLException

class FlywayMigratorSpec extends Specification {

    @Shared
    Map<String, Object> config = [
        'jpa.default.packages-to-scan'                 : ['example.micronaut'],
        'jpa.default.properties.hibernate.hbm2ddl.auto': 'none',
        'jpa.default.properties.hibernate.show_sql'    : true,

        'datasources.default.url'                      : 'jdbc:h2:mem:flywayDisabledForceMigrationsDb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
        'datasources.default.username'                 : 'sa',
        'datasources.default.password'                 : '',
        'datasources.default.driverClassName'          : 'org.h2.Driver',

        'flyway.enabled'                               : true,
        'flyway.datasources.default.enabled'           : false,
    ]

    @Shared
    @AutoCleanup
    ApplicationContext applicationContext = ApplicationContext.run(config as Map<String, Object>, Environment.TEST)

    void 'when migrations are disabled it is possible to run them using the FlywayMigrator'() {
        when:
        FlywayMigrator flywayMigrator = applicationContext.getBean(FlywayMigrator)
        DataSource dataSource = applicationContext.getBean(DataSource, Qualifiers.byName("default"))
        FlywayConfigurationProperties flywayConfigurationProperties = applicationContext.getBean(FlywayConfigurationProperties)
        PollingConditions conditions = new PollingConditions(timeout: 5)
        Sql sql = Sql.newInstance(config.get('datasources.default.url'),
                                  config.get('datasources.default.username'),
                                  config.get('datasources.default.password'),
                                  config.get('datasources.default.driverClassName'))

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
        conditions.eventually {
            sql.rows('select count(*) from books').get(0)[0] == 2
        }
    }

}
