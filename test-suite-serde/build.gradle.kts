plugins {
    `java-library`
    id("io.micronaut.build.internal.flyway-tests")
}

dependencies {
    testAnnotationProcessor(mn.micronaut.inject.java)
    testAnnotationProcessor(mnSerde.micronaut.serde.processor)
    testImplementation(mnSerde.micronaut.serde.jackson)
    testImplementation(mnTest.micronaut.test.junit5)
    testImplementation(mn.micronaut.http.server.netty)
    testImplementation(mn.micronaut.http.client)
    testImplementation(projects.micronautFlyway)
    testRuntimeOnly(mnSql.h2)
    testRuntimeOnly(mnSql.micronaut.jdbc.hikari)
    testRuntimeOnly(mnLogging.logback.classic)
}