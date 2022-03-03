package io.micronaut.flyway.docs

import io.micronaut.flyway.AbstractFlywaySpec
import io.micronaut.flyway.FlywayConfigurationProperties
import io.micronaut.flyway.GormMigrationRunner
import io.micronaut.flyway.YamlAsciidocTagCleaner
import org.flywaydb.core.Flyway
import org.yaml.snakeyaml.Yaml
import spock.lang.Shared

class GormSpec extends AbstractFlywaySpec implements YamlAsciidocTagCleaner {

    String gormConfig = '''\
spec.name: GormDocSpec
//tag::yamlconfig[]
dataSource: # <1>
  pooled: true
  jmxExport: true
  dbCreate: none # <2>
  url: 'jdbc:h2:mem:GORMDb'
  driverClassName: org.h2.Driver
  username: sa
  password: ''

flyway:
  datasources: # <3>
    default: # <4>
      enabled: true # <5>
'''//end::yamlconfig[]

    @Shared
    Map<String, Object> flywayMap = [
        'spec.name': 'GormDocSpec',
        dataSource : [
            pooled         : true,
            jmxExport      : true,
            dbCreate       : 'none',
            url            : 'jdbc:h2:mem:GORMDb',
            driverClassName: DS_DRIVER,
            username       : DS_USERNAME,
            password       : DS_PASSWORD
        ],
        flyway     : [
            datasources: [
                default: [
                    enabled: true
                ]
            ]
        ]
    ]

    void 'test flyway migrations are executed with GORM'() {
        given:
        run(flatten(flywayMap))

        when:
        applicationContext.getBean(GormMigrationRunner)

        then:
        noExceptionThrown()

        when:
        applicationContext.getBean(Flyway)

        then:
        noExceptionThrown()

        when:
        FlywayConfigurationProperties config = applicationContext.getBean(FlywayConfigurationProperties)

        then:
        noExceptionThrown()
        !config.isAsync()

        when:
        Map m = new Yaml().load(cleanYamlAsciidocTag(gormConfig))

        then:
        m == flywayMap

        and:
        newSql('jdbc:h2:mem:GORMDb').rows('select count(*) from books')[0][0] == 2
    }
}
