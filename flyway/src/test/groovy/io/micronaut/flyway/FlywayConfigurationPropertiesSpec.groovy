package io.micronaut.flyway

import groovy.sql.Sql
import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.runtime.server.EmbeddedServer
import org.flywaydb.core.Flyway
import spock.lang.Specification

import javax.sql.DataSource

class FlywayConfigurationPropertiesSpec extends Specification {

    void "test use the default database migrations locations"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(
            ['spec.name'                         : FlywayConfigurationPropertiesSpec.simpleName,
             'datasources.default.url'             : 'jdbc:h2:mem:flywayBooksDB1;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
             'datasources.default.username'        : 'sa',
             'datasources.default.password'        : '',
             'datasources.default.driverClassName' : 'org.h2.Driver',
             'flyway.datasources.default.enabled'  : true,
            ] as Map,
            Environment.TEST
        )

        when:
        applicationContext.getBean(DataSource)

        then:
        noExceptionThrown()

        when:
        applicationContext.getBean(FlywayConfigurationProperties)

        then:
        noExceptionThrown()

        when:
        applicationContext.getBean(Flyway)

        then:
        noExceptionThrown()

        when:
        Map db = [url: 'jdbc:h2:mem:flywayBooksDB1', user: 'sa', password: '', driver: 'org.h2.Driver']
        Sql sql = Sql.newInstance(db.url, db.user, db.password, db.driver)

        then:
        sql.rows('select count(*) from books').get(0)[0] == 2

        cleanup:
        applicationContext.close()
    }

    void "test define multiple locations from database migrations"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(
            ['spec.name'                         : FlywayConfigurationPropertiesSpec.simpleName,
             'datasources.default.url'             : 'jdbc:h2:mem:flywayBooksDB2;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
             'datasources.default.username'        : 'sa',
             'datasources.default.password'        : '',
             'datasources.default.driverClassName' : 'org.h2.Driver',
             'flyway.datasources.default.locations': 'classpath:db/migration,classpath:othermigrations',
            ] as Map,
            Environment.TEST
        )

        when:
        applicationContext.getBean(DataSource)

        then:
        noExceptionThrown()

        when:
        applicationContext.getBean(FlywayConfigurationProperties)

        then:
        noExceptionThrown()

        when:
        applicationContext.getBean(Flyway)

        then:
        noExceptionThrown()

        when:
        Map db = [url: 'jdbc:h2:mem:flywayBooksDB2', user: 'sa', password: '', driver: 'org.h2.Driver']
        Sql sql = Sql.newInstance(db.url, db.user, db.password, db.driver)

        then:
        sql.rows('select count(*) from books').get(0)[0] == 3

        cleanup:
        applicationContext.close()
    }

    void 'test define flyway database connection and not use Micronaut datasource'() {
        given:
        EmbeddedServer server = ApplicationContext.run(EmbeddedServer,
            ['spec.name'                         : FlywayConfigurationPropertiesSpec.simpleName,
             'flyway.datasources.books.locations': 'classpath:db/migration',
             'flyway.datasources.books.url'      : 'jdbc:h2:mem:flywayBooksDB3;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
             'flyway.datasources.books.user'     : 'sa',
             'flyway.datasources.books.password' : '',
            ] as Map,
            Environment.TEST
        )
        ApplicationContext applicationContext = server.applicationContext

        when:
        Map db = [url: 'jdbc:h2:mem:flywayBooksDB3', user: 'sa', password: '', driver: 'org.h2.Driver']
        Sql sql = Sql.newInstance(db.url, db.user, db.password, db.driver)

        then:
        sql.rows('select count(*) from books').get(0)[0] == 2

        cleanup:
        applicationContext.close()
    }

    void 'test define flyway database connection via JDBC only with the credentials on the URL and not use Micronaut datasource'() {
        given:
        EmbeddedServer server = ApplicationContext.run(EmbeddedServer,
           ['spec.name'                         : FlywayConfigurationPropertiesSpec.simpleName,
            'flyway.datasources.books.locations': 'classpath:db/migration',
            'flyway.datasources.books.url'      : 'jdbc:h2:mem:flywayBooksDB4;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;USER=sa;PASSWORD=',
           ] as Map,
           Environment.TEST
        )
        ApplicationContext applicationContext = server.applicationContext

        when:
        Map db = [url: 'jdbc:h2:mem:flywayBooksDB4', user: 'sa', password: '', driver: 'org.h2.Driver']
        Sql sql = Sql.newInstance(db.url, db.user, db.password, db.driver)

        then:
        sql.rows('select count(*) from books').get(0)[0] == 2

        cleanup:
        applicationContext.close()
    }
}
