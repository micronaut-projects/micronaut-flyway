package io.micronaut.flyway.docs

import groovy.sql.Sql
import io.micronaut.flyway.AbstractFlywaySpec
import io.micronaut.flyway.FlywayConfigurationProperties
import io.micronaut.flyway.YamlAsciidocTagCleaner
import org.yaml.snakeyaml.Yaml
import spock.lang.Shared

class GormMultipleDataSourcesSpec extends AbstractFlywaySpec implements YamlAsciidocTagCleaner {

    String gormConfig = '''\
spec.name: GormDocSpec
//tag::yamlconfig[]
dataSource:
  pooled: true
  jmxExport: true
  dbCreate: none
  url: 'jdbc:h2:mem:flywayGORMDb'
  driverClassName: org.h2.Driver
  username: sa
  password: ''

dataSources:
  books:
    pooled: true
    jmxExport: true
    dbCreate: none
    url: 'jdbc:h2:mem:flywayBooksDb'
    driverClassName: org.h2.Driver
    username: sa
    password: ''

flyway:
  datasources:
    default:
      enabled: true
    books:
      enabled: true
'''//end::yamlconfig[]

    @Shared
    Map<String, Object> flywayMap = [
        'spec.name': 'GormDocSpec',
        dataSource : [
            pooled         : true,
            jmxExport      : true,
            dbCreate       : 'none',
            url            : 'jdbc:h2:mem:flywayGORMDb',
            driverClassName: DS_DRIVER,
            username       : DS_USERNAME,
            password       : DS_PASSWORD
        ],
        dataSources : [
            books: [
                pooled         : true,
                jmxExport      : true,
                dbCreate       : 'none',
                url            : 'jdbc:h2:mem:flywayBooksDb',
                driverClassName: DS_DRIVER,
                username       : DS_USERNAME,
                password       : DS_PASSWORD
            ]
        ],
        flyway     : [
            datasources: [
                default: [
                    enabled: true
                ],
                books: [
                    enabled: true
                ]
            ]
        ]
    ]

    void 'test flyway migrations are executed with GORM with multiple datasources'() {
        given:
        run(flatten(flywayMap))

        when:
        Collection<FlywayConfigurationProperties> configurationProperties = applicationContext.getBeansOfType(FlywayConfigurationProperties)

        then:
        configurationProperties.find { it.nameQualifier == 'default' }
        configurationProperties.find { it.nameQualifier == 'books' }

        when:
        Map m = new Yaml().load(cleanYamlAsciidocTag(gormConfig))

        then:
        m == flywayMap

        when: 'connecting to the default datasource'
        Sql sql = newSql('jdbc:h2:mem:flywayGORMDb')

        then: 'the migrations have been executed'
        sql.rows('select count(*) from books')[0][0] == 2

        when: 'connecting to another datasource'
        Sql sql2 = newSql('jdbc:h2:mem:flywayBooksDb')

        then: 'the migrations have been executed'
        sql2.rows('select count(*) from books')[0][0] == 2
    }
}
