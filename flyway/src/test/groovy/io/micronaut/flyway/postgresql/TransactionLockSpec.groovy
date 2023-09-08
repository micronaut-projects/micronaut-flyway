package io.micronaut.flyway.postgresql


import io.micronaut.flyway.AbstractFlywaySpec
import io.micronaut.flyway.FlywayConfigurationProperties
import org.flywaydb.core.Flyway
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.spock.Testcontainers
import spock.lang.Shared
import spock.lang.Timeout

import javax.sql.DataSource
import java.util.concurrent.TimeUnit

@Testcontainers
class TransactionLockSpec extends AbstractFlywaySpec {

    @Shared
    private PostgreSQLContainer postgresql = new PostgreSQLContainer("postgres:15-alpine")

    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void 'when create index concurrently exists, migration does not hang'() {
        given: 'a configuration with flyway.postgresql.transactional.lock = false'
        run('spec.name'                               : TransactionLockSpec.simpleName,
            'datasources.default.url'                 : postgresql.getJdbcUrl(),
            'datasources.default.username'            : postgresql.getUsername(),
            'datasources.default.password'            : postgresql.getPassword(),
            'datasources.default.driverClassName'     : postgresql.getDriverClassName(),

            'flyway.enabled'                          : true,
            'flyway.datasources.default.locations'    : 'classpath:postgresql',

            'flyway.datasources.default.properties.flyway.postgresql.transactional.lock': 'false'
        )

        when: 'running the migrations'
        applicationContext.getBean(DataSource)
        applicationContext.getBean(FlywayConfigurationProperties)
        applicationContext.getBean(Flyway)

        then: 'no timeout happens'
        noExceptionThrown()
    }
}
