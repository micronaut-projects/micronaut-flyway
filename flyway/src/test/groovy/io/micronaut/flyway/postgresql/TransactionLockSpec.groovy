package io.micronaut.flyway.postgresql


import io.micronaut.flyway.AbstractFlywaySpec
import io.micronaut.flyway.FlywayConfigurationProperties
import org.flywaydb.core.Flyway
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.spock.Testcontainers
import spock.lang.Requires
import spock.lang.Shared
import spock.lang.Timeout

import javax.sql.DataSource
import java.util.concurrent.TimeUnit

@Testcontainers
@Requires({ DockerClientFactory.instance().isDockerAvailable() })
class TransactionLockSpec extends AbstractFlywaySpec {

    String yaml = """\
#tag::yaml[]
flyway:
  datasources:
    default:
      properties:
        flyway:
          postgresql:
            transactional:
              lock: false
#end::yaml[]
"""

    @Shared
    private PostgreSQLContainer postgresql = new PostgreSQLContainer("postgres:15-alpine")

    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void 'when create index concurrently exists, migration does not hang'() {
        given: 'a configuration with flyway.postgresql.transactional.lock = false'
        String config = yaml.replace("#tag::yaml[]", "").replace("#end::yaml[]", "")
        String value = config.substring(config.indexOf(": ") + ": ".length()).replace("\n", "")
        String key = config.substring(0, config.lastIndexOf(":")).replace("\n", "").replace("  ", "").replace(":", ".")
        run('spec.name'                               : TransactionLockSpec.simpleName,
            'datasources.default.url'                 : postgresql.getJdbcUrl(),
            'datasources.default.username'            : postgresql.getUsername(),
            'datasources.default.password'            : postgresql.getPassword(),
            'datasources.default.driverClassName'     : postgresql.getDriverClassName(),
            'flyway.enabled'                          : true,
            'flyway.datasources.default.locations'    : 'classpath:postgresql',
                (key): value
        )

        when: 'running the migrations'
        applicationContext.getBean(DataSource)
        applicationContext.getBean(FlywayConfigurationProperties)
        applicationContext.getBean(Flyway)

        then: 'no timeout happens'
        noExceptionThrown()
    }
}
