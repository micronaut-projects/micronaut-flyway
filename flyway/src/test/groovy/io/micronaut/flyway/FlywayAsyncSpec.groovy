package io.micronaut.flyway

import groovy.sql.Sql
import io.micronaut.context.ApplicationContext
import spock.lang.AutoCleanup
import spock.lang.Shared

import javax.sql.DataSource

import static io.micronaut.context.env.Environment.TEST

class FlywayAsyncSpec extends AbstractFlywaySpec {

    @Shared
    Map<String, Object> config = [
        'datasources.default.url'                      : 'jdbc:h2:mem:flywayDb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
        'datasources.default.username'                 : DS_USERNAME,
        'datasources.default.password'                 : DS_PASSWORD,
        'datasources.default.driverClassName'          : DS_DRIVER,

        'jpa.default.packages-to-scan'                 : ['example.micronaut'],
        'jpa.default.properties.hibernate.hbm2ddl.auto': 'none',
        'jpa.default.properties.hibernate.show_sql'    : true,

        'flyway.datasources.default.async'             : true
    ]

    @Shared
    @AutoCleanup
    ApplicationContext applicationContext = ApplicationContext.run(config as Map, TEST)

    void 'test Flyway migrations are executed asynchronously'() {
        when:
        applicationContext.getBean(DataSource)

        then:
        noExceptionThrown()

        when:
        FlywayConfigurationProperties config = applicationContext.getBean(FlywayConfigurationProperties)

        then:
        noExceptionThrown()
        config.isAsync()

        when:
        Sql sql = newSql()

        then:
        conditions.eventually {
            sql.rows('select count(*) from books')[0][0] == 2
        }
    }
}
