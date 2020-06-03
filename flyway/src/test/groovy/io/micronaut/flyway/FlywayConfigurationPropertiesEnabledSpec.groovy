package io.micronaut.flyway

import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.context.exceptions.NoSuchBeanException
import io.micronaut.inject.qualifiers.Qualifiers
import org.flywaydb.core.Flyway
import spock.lang.Specification

import javax.sql.DataSource

class FlywayConfigurationPropertiesEnabledSpec extends Specification {

    void 'if no flyway configuration then no FlywayConfigurationProperties and Flyway beans are created'() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(
            ['spec.name': FlywayConfigurationPropertiesEnabledSpec.simpleName] as Map,
            Environment.TEST
        )

        when:
        applicationContext.getBean(FlywayConfigurationProperties)

        then:
        def e = thrown(NoSuchBeanException)
        e.message.contains('No bean of type [' + FlywayConfigurationProperties.name + '] exists.')

        when:
        applicationContext.getBean(Flyway)

        then:
        e = thrown(NoSuchBeanException)
        e.message.contains('No bean of type [' + Flyway.name + '] exists.')

        cleanup:
        applicationContext.close()
    }

    void 'if flyway configuration then FlywayConfigurationProperties and Flyway beans are created'() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(
            ['spec.name'                         : FlywayConfigurationPropertiesEnabledSpec.simpleName,
             'flyway.datasources.movies.enabled' : true,
             'datasources.movies.url'            : 'jdbc:h2:mem:flyway2Db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
             'datasources.movies.username'       : 'sa',
             'datasources.movies.password'       : '',
             'datasources.movies.driverClassName': 'org.h2.Driver',

             'flyway.datasources.books.enabled'  : true,
             'datasources.books.url'             : 'jdbc:h2:mem:flywayDb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
             'datasources.books.username'        : 'sa',
             'datasources.books.password'        : '',
             'datasources.books.driverClassName' : 'org.h2.Driver',

            ] as Map
            , Environment.TEST
        )

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

        cleanup:
        applicationContext.close()
    }

    void 'if flyway is disabled, then FlywayConfigurationProperties bean is created but Flyway bean is not'() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(
            ['spec.name'                         : FlywayConfigurationPropertiesEnabledSpec.simpleName,
             'flyway.datasources.movies.enabled' : false,
             'datasources.movies.url'            : 'jdbc:h2:mem:flyway2Db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
             'datasources.movies.username'       : 'sa',
             'datasources.movies.password'       : '',
             'datasources.movies.driverClassName': 'org.h2.Driver',
            ] as Map
            , Environment.TEST
        )

        when:
        applicationContext.getBean(FlywayConfigurationProperties)

        then:
        noExceptionThrown()

        when:
        applicationContext.getBean(Flyway)

        then:
        def e = thrown(NoSuchBeanException)
        e.message.contains('No bean of type [' + Flyway.name + '] exists')

        cleanup:
        applicationContext.close()
    }
}
