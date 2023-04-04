package io.micronaut.flyway


import spock.lang.Specification

class StaticResourceProviderSpec extends Specification {
    void "test static resource provider"() {
        given:
        def resourceProvider = StaticResourceProvider.create(Thread.currentThread().getContextClassLoader())

        when:
        def resources = resourceProvider.getResources("", ['sql'] as String[])

        then:
        resources.size() == 2
        resources.find { it.filename == 'V1__create-books-schema.sql' }
        resources.find { it.filename == 'V1__create-books-schema.sql' }
            .read().text.contains("create table books")
    }
}
