package io.micronaut.flyway


import spock.lang.Specification

class StaticResourceProviderSpec extends Specification {
    private String previousImageSingletonsEnabled

    def setup() {
        previousImageSingletonsEnabled = System.getProperty('micronaut.graalvm.imagesingletons.enabled')
    }

    void cleanup() {
        if (previousImageSingletonsEnabled == null) {
            System.clearProperty('micronaut.graalvm.imagesingletons.enabled')
        } else {
            System.setProperty('micronaut.graalvm.imagesingletons.enabled', previousImageSingletonsEnabled)
        }
    }

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

    void "image singletons lookup can be disabled"() {
        given:
        System.setProperty('micronaut.graalvm.imagesingletons.enabled', 'false')

        expect:
        StaticResourceProvider.get() == null
    }
}
