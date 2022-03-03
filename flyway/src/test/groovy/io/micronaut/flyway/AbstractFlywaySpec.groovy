package io.micronaut.flyway

import groovy.sql.Sql
import io.micronaut.context.ApplicationContext
import io.micronaut.runtime.server.EmbeddedServer
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static io.micronaut.context.env.Environment.TEST

abstract class AbstractFlywaySpec extends Specification {

    protected static final String DS_USERNAME = 'sa'
    protected static final String DS_PASSWORD = ''
    protected static final String DS_DRIVER = 'org.h2.Driver'
    protected static final String DS_URL = 'jdbc:h2:mem:flywayDb'

    protected final PollingConditions conditions = new PollingConditions(timeout: 5)
    protected ApplicationContext applicationContext
    protected EmbeddedServer embeddedServer

    protected void run(Map<String, Object> config) {
        applicationContext = ApplicationContext.run(config, TEST)
    }

    protected EmbeddedServer runServer(Map<String, Object> config) {
        embeddedServer = ApplicationContext.run(EmbeddedServer, config, TEST)
        applicationContext = embeddedServer.applicationContext
        embeddedServer
    }

    protected Sql newSql(String url = DS_URL) {
        Sql.newInstance(url, DS_USERNAME, DS_PASSWORD, DS_DRIVER)
    }

    void cleanup() {
        embeddedServer?.close()
        applicationContext?.close()
    }
}
