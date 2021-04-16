package io.micronaut.flyway

import groovy.sql.Sql
import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import javax.sql.DataSource

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

    void 'when migrations are disable it is possible to run them using the FlywayMigrator'() {
        when:
        FlywayMigrator flywayMigrator = applicationContext.getBean(FlywayMigrator)
        DataSource dataSource = applicationContext.getBean(DataSource)
        FlywayConfigurationProperties flywayConfigurationProperties = applicationContext.getBean(FlywayConfigurationProperties)

        then:
        noExceptionThrown()

        when:
        flywayMigrator.run(flywayConfigurationProperties, dataSource)

        then:
        noExceptionThrown()

        when:
        PollingConditions conditions = new PollingConditions(timeout: 5)
        Sql sql = Sql.newInstance(config.get('datasources.default.url'),
                                  config.get('datasources.default.username'),
                                  config.get('datasources.default.password'),
                                  config.get('datasources.default.driverClassName'))

        then:
        conditions.eventually {
            sql.rows('select count(*) from books').get(0)[0] == 2
        }
    }

}
