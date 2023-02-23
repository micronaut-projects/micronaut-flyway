package io.micronaut.flyway.docs

import io.micronaut.context.ApplicationContext
import io.micronaut.flyway.AbstractFlywaySpec
import io.micronaut.flyway.FlywayConfigurationProperties
import io.micronaut.flyway.YamlAsciidocTagCleaner
import io.micronaut.runtime.server.EmbeddedServer
import org.yaml.snakeyaml.Yaml
import spock.lang.AutoCleanup
import spock.lang.Shared

import javax.sql.DataSource

import static io.micronaut.context.env.Environment.TEST

class FlywaySpec extends AbstractFlywaySpec implements YamlAsciidocTagCleaner {

    String yamlConfig = '''\
//tag::yamlconfig[]
datasources:
  default:
    url: 'jdbc:h2:mem:flywayDb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE'
    username: 'sa'
    password: ''
    driverClassName: 'org.h2.Driver'
jpa:
  default:
    packages-to-scan:
      - 'example.micronaut'
    properties:
      hibernate:
        hbm2ddl:
          auto: none
        show_sql: true
flyway:
  datasources:
    default:
      enabled: true
'''//end::yamlconfig[]

    @Shared
    Map<String, Object> flywayMap = [
        jpa        : [
            default: [
                'packages-to-scan': ['example.micronaut'],
                properties        : [
                    hibernate: [
                        hbm2ddl   : [
                            auto: 'none'
                        ],
                        'show_sql': true,
                    ]
                ]

            ]
        ],
        flyway: [
            datasources: [
                default: [
                    enabled: true
                ]
            ]
        ],
        datasources: [
            default: [
                url            : 'jdbc:h2:mem:flywayDb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
                username       : DS_USERNAME,
                password       : DS_PASSWORD,
                driverClassName: DS_DRIVER,
            ]
        ]
    ]

    @Shared
    Map<String, Object> config = flatten(flywayMap)

    @Shared
    @AutoCleanup
    EmbeddedServer embeddedServer = ApplicationContext.run(EmbeddedServer, config as Map, TEST)

    void 'test flyway changelogs are executed'() {
        when:
        embeddedServer.applicationContext.getBean(DataSource)

        then:
        noExceptionThrown()

        when:
        FlywayConfigurationProperties config = embeddedServer.applicationContext.getBean(FlywayConfigurationProperties)

        then:
        noExceptionThrown()
        !config.isAsync()

        when:
        Map m = new Yaml().load(cleanYamlAsciidocTag(yamlConfig))

        then:
        m == flywayMap

        and:
        newSql().rows('select count(*) from books')[0][0] == 2
    }
}
