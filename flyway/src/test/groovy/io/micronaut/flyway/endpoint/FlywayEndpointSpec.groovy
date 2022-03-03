package io.micronaut.flyway.endpoint

import io.micronaut.core.type.Argument
import io.micronaut.flyway.AbstractFlywaySpec
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.client.HttpClient
import io.micronaut.runtime.server.EmbeddedServer

import javax.sql.DataSource

import static io.micronaut.http.HttpStatus.OK

class FlywayEndpointSpec extends AbstractFlywaySpec {

    void 'test flyway endpoint bean is available'() {
        given:
        run([:])

        expect:
        applicationContext.containsBean(FlywayEndpoint)
    }

    void 'test the flyway endpoint bean can be disabled'() {
        given:
        run('endpoints.flyway.enabled': false)

        expect:
        !applicationContext.containsBean(FlywayEndpoint)
    }

    void 'test the flyway endpoint bean is not available with all endpoints disabled'() {
        given:
        run('endpoints.all.enabled': false)

        expect:
        !applicationContext.containsBean(FlywayEndpoint)
    }

    void 'test the flyway endpoint bean is available will all disabled but having it enabled'() {
        given:
        run('endpoints.all.enabled': false, 'endpoints.flyway.enabled': true)

        expect:
        applicationContext.containsBean(FlywayEndpoint)
    }

    void 'test flyway endpoint'() {
        given:
        EmbeddedServer embeddedServer = runServer(
            'jpa.default.packages-to-scan'                 : 'example.micronaut',
            'jpa.default.properties.hibernate.hbm2ddl.auto': 'none',
            'jpa.default.properties.hibernate.show_sql'    : true,
            'flyway.datasources.default.locations'         : 'classpath:db/migration',
            'endpoints.flyway.sensitive'                   : false,
            'datasources.default.url'                      : 'jdbc:h2:mem:flywayDb1;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
            'datasources.default.username'                 : DS_USERNAME,
            'datasources.default.password'                 : DS_PASSWORD,
            'datasources.default.driver-class-name'        : DS_DRIVER)
        HttpClient client = applicationContext.createBean(HttpClient, embeddedServer.URL)

        when:
        //datasource must be loaded to show the migrations
        applicationContext.getBeansOfType(DataSource)
        HttpResponse<List<Map>> response = client.toBlocking()
            .exchange(HttpRequest.GET('/flyway'), Argument.of(List, Map))

        then:
        response.status() == OK
        List<FlywayReport> result = response.body()
        result.size() == 1
        result[0].name == 'default'
        result[0].migrations.size() == 2
        result[0].migrations[0].script == 'V1__create-books-schema.sql'
        result[0].migrations[1].script == 'V2__insert-data-books.sql'
    }

    void 'test flyway endpoint with multiple datasources'() {
        given:
        EmbeddedServer embeddedServer = runServer(
            'jpa.default.packages-to-scan'                 : 'example.micronaut',
            'jpa.default.properties.hibernate.hbm2ddl.auto': 'none',
            'jpa.default.properties.hibernate.show_sql'    : true,
            'flyway.datasources.default.locations'         : 'classpath:db/migration',
            'flyway.datasources.other.locations'           : 'classpath:db/moremigrations',
            'endpoints.flyway.sensitive'                   : false,
            'datasources.default.url'                      : 'jdbc:h2:mem:flywayDb2;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
            'datasources.default.username'                 : DS_USERNAME,
            'datasources.default.password'                 : DS_PASSWORD,
            'datasources.default.driver-class-name'        : DS_DRIVER,
            'datasources.other.url'                        : 'jdbc:h2:mem:flywayDb3;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
            'datasources.other.username'                   : DS_USERNAME,
            'datasources.other.password'                   : DS_PASSWORD,
            'datasources.other.driver-class-name'          : DS_DRIVER)
        HttpClient client = applicationContext.createBean(HttpClient, embeddedServer.URL)

        when:
        //datasource must be loaded to show the migrations
        applicationContext.getBeansOfType(DataSource)
        HttpResponse<List> response = client.toBlocking()
            .exchange(HttpRequest.GET('/flyway'), Argument.of(List, Map))

        then:
        response.status() == OK
        List<FlywayReport> result = response.body()
        result.sort { it.name }
        result[0].name == 'default'
        result[0].migrations.size() == 2
        result[0].migrations[0].script == 'V1__create-books-schema.sql'
        result[0].migrations[1].script == 'V2__insert-data-books.sql'
        result[1].name == 'other'
        result[1].migrations.size() == 1
        result[1].migrations[0].script == 'V1__create-books-schema.sql'
    }

    void 'test flyway endpoint without migrations'() {
        given:
        EmbeddedServer embeddedServer = runServer(
            'jpa.default.packages-to-scan'                 : 'example.micronaut',
            'jpa.default.properties.hibernate.hbm2ddl.auto': 'none',
            'jpa.default.properties.hibernate.show_sql'    : true,
            'endpoints.flyway.sensitive'                   : false,
            'datasources.default.url'                      : 'jdbc:h2:mem:flywayDb4;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
            'datasources.default.username'                 : DS_USERNAME,
            'datasources.default.password'                 : DS_PASSWORD,
            'datasources.default.driver-class-name'        : DS_DRIVER)
        HttpClient client = applicationContext.createBean(HttpClient, embeddedServer.URL)

        when:
        HttpResponse<List> response = client.toBlocking()
            .exchange(HttpRequest.GET('/flyway'), Argument.of(List, Map))

        then:
        response.status() == OK
        List<FlywayReport> result = response.body()
        result.size() == 0
    }
}
