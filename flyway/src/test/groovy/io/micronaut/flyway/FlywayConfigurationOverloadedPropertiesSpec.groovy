package io.micronaut.flyway

import io.micronaut.context.exceptions.BeanInstantiationException
import io.micronaut.inject.qualifiers.Qualifiers
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.Configuration
import org.flywaydb.core.api.pattern.ValidatePattern

import javax.sql.DataSource

class FlywayConfigurationOverloadedPropertiesSpec extends AbstractFlywaySpec {

    void 'overloaded properties in FluentConfiguration can be set'() {
        given:
        run('spec.name': FlywayConfigurationPropertiesEnabledSpec.simpleName,
                'flyway.datasources.movies.enabled': true,
                'datasources.movies.url': 'jdbc:h2:mem:flyway2Db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
                'datasources.movies.username': DS_USERNAME,
                'datasources.movies.password': DS_PASSWORD,
                'datasources.movies.driverClassName': DS_DRIVER,
                'flyway.datasources.movies.ignoreMigrationPatterns': '*:*',
                'flyway.datasources.movies.locations': 'classpath:db/migration,classpath:othermigrations',
                'flyway.datasources.movies.encoding': 'utf-8',
                'flyway.datasources.movies.target': '1',
                'flyway.datasources.movies.baseLineVersion': '1'
        )

        when:
        applicationContext.getBean(DataSource, Qualifiers.byName('movies'))
        applicationContext.getBean(FlywayConfigurationProperties, Qualifiers.byName('movies'))
        Configuration configuration = applicationContext.getBean(Flyway, Qualifiers.byName('movies')).getConfiguration()

        then:
        noExceptionThrown()
        configuration
        configuration.ignoreMigrationPatterns == [ ValidatePattern.fromPattern('*:*') ]
        configuration.locations.toString() == '[classpath:db/migration, classpath:othermigrations]'
        configuration.encoding.toString() == 'UTF-8'
        configuration.target.toString() == '1'
        configuration.baselineVersion.toString() == '1'
    }

    void 'overloaded team edition properties throw an exception'() {
        when:
        run([ 'spec.name': FlywayConfigurationPropertiesEnabledSpec.simpleName,
              'flyway.datasources.movies.enabled': true,
              'datasources.movies.url': 'jdbc:h2:mem:flyway2Db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
              'datasources.movies.username': DS_USERNAME,
              'datasources.movies.password': DS_PASSWORD,
              'datasources.movies.driverClassName': DS_DRIVER,
              (flywayProperty): value
        ])

        then:
        BeanInstantiationException ex = thrown()
        ex.message.contains("is not supported by OSS")

        where:
        flywayProperty | value
        'flyway.datasources.movies.dryRunOutput' | 'foo'
    }
}
