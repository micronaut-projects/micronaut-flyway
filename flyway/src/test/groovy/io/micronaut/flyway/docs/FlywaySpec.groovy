package io.micronaut.flyway.docs

import groovy.sql.Sql
import io.micronaut.flyway.FlywayConfigurationProperties
import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.flyway.YamlAsciidocTagCleaner
import io.micronaut.runtime.server.EmbeddedServer
import org.yaml.snakeyaml.Yaml
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

import javax.sql.DataSource

class FlywaySpec extends Specification implements YamlAsciidocTagCleaner {

    String yamlConfig = '''\
//tag::yamlconfig[]
datasources:
  default: # <3>
    url: 'jdbc:h2:mem:flywayDb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE'
    username: 'sa'
    password: ''
    driverClassName: 'org.h2.Driver'
jpa:
  default: # <3>
    packages-to-scan:
      - 'example.micronaut'
    properties:
      hibernate:
        hbm2ddl:
          auto: none # <1>
        show_sql: true
flyway:
  datasources: # <2>
    default: # <3>
      enabled: true # <4>
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
                username       : 'sa',
                password       : '',
                driverClassName: 'org.h2.Driver',
            ]
        ]
    ]

    @Shared
    Map<String, Object> config = [:] << flatten(flywayMap)

    @Shared
    @AutoCleanup
    EmbeddedServer embeddedServer = ApplicationContext.run(EmbeddedServer, config as Map<String, Object>, Environment.TEST)

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

        when:
        Map db = [url: 'jdbc:h2:mem:flywayDb', user: 'sa', password: '', driver: 'org.h2.Driver']
        Sql sql = Sql.newInstance(db.url, db.user, db.password, db.driver)

        then:
        sql.rows('select count(*) from books').get(0)[0] == 2
    }
}
