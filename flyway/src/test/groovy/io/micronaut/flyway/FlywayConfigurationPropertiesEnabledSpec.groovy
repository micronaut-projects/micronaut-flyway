package io.micronaut.flyway

import io.micronaut.context.exceptions.NoSuchBeanException
import io.micronaut.inject.qualifiers.Qualifiers
import org.flywaydb.core.Flyway

import javax.sql.DataSource

class FlywayConfigurationPropertiesEnabledSpec extends AbstractFlywaySpec {

    void 'if no flyway configuration then no FlywayConfigurationProperties and Flyway beans are created'() {
        given:
        run('spec.name': FlywayConfigurationPropertiesEnabledSpec.simpleName)

        when:
        applicationContext.getBean(FlywayConfigurationProperties)

        then:
        NoSuchBeanException e = thrown()
        e.message.contains('No bean of type [' + FlywayConfigurationProperties.name + '] exists.')

        when:
        applicationContext.getBean(Flyway)

        then:
        e = thrown(NoSuchBeanException)
        e.message.contains('No bean of type [' + Flyway.name + '] exists.')
    }

    void 'if flyway configuration then FlywayConfigurationProperties and Flyway beans are created'() {
        given:
        run('spec.name'                         : FlywayConfigurationPropertiesEnabledSpec.simpleName,
            'flyway.datasources.movies.enabled' : true,
            'datasources.movies.url'            : 'jdbc:h2:mem:flyway2Db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
            'datasources.movies.username'       : DS_USERNAME,
            'datasources.movies.password'       : DS_PASSWORD,
            'datasources.movies.driverClassName': DS_DRIVER,

            'flyway.datasources.books.enabled'  : true,
            'datasources.books.url'             : 'jdbc:h2:mem:flywayDb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
            'datasources.books.username'        : DS_USERNAME,
            'datasources.books.password'        : DS_PASSWORD,
            'datasources.books.driverClassName' : DS_DRIVER)

        when:
        applicationContext.getBean(DataSource, Qualifiers.byName('movies'))
        applicationContext.getBean(FlywayConfigurationProperties, Qualifiers.byName('movies'))
        applicationContext.getBean(Flyway, Qualifiers.byName('movies'))

        then:
        noExceptionThrown()

        when:
        applicationContext.getBean(DataSource, Qualifiers.byName('books'))
        applicationContext.getBean(FlywayConfigurationProperties, Qualifiers.byName('books'))
        applicationContext.getBean(Flyway, Qualifiers.byName('books'))

        then:
        noExceptionThrown()
    }

    void 'if flyway is disabled, then FlywayConfigurationProperties bean is created but Flyway bean is not'() {
        given:
        run('spec.name'                         : FlywayConfigurationPropertiesEnabledSpec.simpleName,
            'flyway.datasources.movies.enabled' : false,
            'datasources.movies.url'            : 'jdbc:h2:mem:flyway2Db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
            'datasources.movies.username'       : DS_USERNAME,
            'datasources.movies.password'       : DS_PASSWORD,
            'datasources.movies.driverClassName': DS_DRIVER)

        when:
        applicationContext.getBean(FlywayConfigurationProperties)

        then:
        noExceptionThrown()

        when:
        applicationContext.getBean(Flyway)

        then:
        NoSuchBeanException e = thrown()
        e.message.contains('No bean of type [' + Flyway.name + '] exists')
    }
}
