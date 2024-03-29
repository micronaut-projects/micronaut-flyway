package io.micronaut.flyway.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import io.micronaut.context.annotation.Requires
import io.micronaut.core.type.Argument
import io.micronaut.flyway.AbstractFlywaySpec
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.runtime.server.EmbeddedServer
import org.slf4j.LoggerFactory
import spock.lang.Issue

import javax.sql.DataSource

class FlywayLoggingSpec extends AbstractFlywaySpec {

    @Issue("https://github.com/micronaut-projects/micronaut-flyway/issues/470")
    void 'test flyway debug logging for sqlscript'() {
        given:
        MemoryAppender appender = new MemoryAppender()
        Logger l = (Logger) LoggerFactory.getLogger('org.flywaydb.core.internal.sqlscript')
        l.setLevel(Level.DEBUG)
        l.addAppender(appender)
        appender.start()

        and:
        EmbeddedServer embeddedServer = runServer(
                'spec.name'                                    : 'FlywayLoggingSpec',
                'jpa.default.packages-to-scan'                 : 'example.micronaut',
                'jpa.default.properties.hibernate.hbm2ddl.auto': 'none',
                'jpa.default.properties.hibernate.show_sql'    : true,
                'flyway.datasources.default.locations'         : 'classpath:db/migration',
                'endpoints.flyway.sensitive'                   : false,
                'datasources.default.url'                      : 'jdbc:h2:mem:FlywayLoggingSpec;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
                'datasources.default.username'                 : DS_USERNAME,
                'datasources.default.password'                 : DS_PASSWORD,
                'datasources.default.driver-class-name'        : DS_DRIVER)
        HttpClient client = applicationContext.createBean(HttpClient, embeddedServer.URL)

        when:
        applicationContext.getBeansOfType(DataSource)
        client.toBlocking().exchange(HttpRequest.GET('/flyway'), Argument.of(List, Map))

        then:
        conditions.eventually {
            !appender.events.isEmpty()
        }

        cleanup:
        appender.stop()
    }

    @Requires(property = 'spec.name', value = 'FlywayLoggingSpec')
    class MemoryAppender extends AppenderBase<ILoggingEvent> {

        List<String> events = []

        @Override
        protected void append(ILoggingEvent e) {
            events << e.formattedMessage
        }
    }
}
