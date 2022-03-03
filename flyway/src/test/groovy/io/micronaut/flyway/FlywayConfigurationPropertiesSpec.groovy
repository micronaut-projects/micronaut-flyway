package io.micronaut.flyway

import groovy.sql.Sql
import org.flywaydb.core.Flyway

import javax.sql.DataSource

class FlywayConfigurationPropertiesSpec extends AbstractFlywaySpec {

    void 'test use the default database migrations locations'() {
        given:
        run('spec.name'                           : FlywayConfigurationPropertiesSpec.simpleName,
            'datasources.default.url'             : 'jdbc:h2:mem:flywayBooksDB1;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
            'datasources.default.username'        : DS_USERNAME,
            'datasources.default.password'        : DS_PASSWORD,
            'datasources.default.driverClassName' : DS_DRIVER,
            'flyway.datasources.default.enabled'  : true)

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

        and:
        newSql('jdbc:h2:mem:flywayBooksDB1').rows('select count(*) from books')[0][0] == 2
    }

    void 'test define multiple locations from database migrations'() {
        given:
        run('spec.name'                         : FlywayConfigurationPropertiesSpec.simpleName,
            'datasources.default.url'             : 'jdbc:h2:mem:flywayBooksDB2;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
            'datasources.default.username'        : DS_USERNAME,
            'datasources.default.password'        : DS_PASSWORD,
            'datasources.default.driverClassName' : DS_DRIVER,
            'flyway.datasources.default.locations': 'classpath:db/migration,classpath:othermigrations')

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

        and:
        newSql('jdbc:h2:mem:flywayBooksDB2').rows('select count(*) from books')[0][0] == 3
    }

    void 'test define flyway database connection and not use Micronaut datasource'() {
        given:
        runServer(
            'spec.name'                         : FlywayConfigurationPropertiesSpec.simpleName,
            'flyway.datasources.books.locations': 'classpath:db/migration',
            'flyway.datasources.books.url'      : 'jdbc:h2:mem:flywayBooksDB3;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
            'flyway.datasources.books.user'     : DS_USERNAME,
            'flyway.datasources.books.password' : DS_PASSWORD)

        when:
        Sql sql = newSql('jdbc:h2:mem:flywayBooksDB3')

        then:
        sql.rows('select count(*) from books')[0][0] == 2
    }

    void 'test define flyway database connection via JDBC only with the credentials on the URL and not use Micronaut datasource'() {
        given:
        run('spec.name'                         : FlywayConfigurationPropertiesSpec.simpleName,
            'flyway.datasources.books.locations': 'classpath:db/migration',
            'flyway.datasources.books.url'      : 'jdbc:h2:mem:flywayBooksDB4;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;USER=sa;PASSWORD=')

        when:
        Sql sql = newSql('jdbc:h2:mem:flywayBooksDB4')

        then:
        sql.rows('select count(*) from books')[0][0] == 2
    }
}
