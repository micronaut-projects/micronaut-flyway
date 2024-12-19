plugins {
    id("io.micronaut.build.internal.flyway-base")
    `java-library`
}

description = "Test suite for Flyway + Manual DataSource Configuration"

dependencies {
    annotationProcessor(mn.micronaut.inject.java)
    annotationProcessor(mnData.micronaut.data.processor)
    implementation(mnData.micronaut.data.jdbc)
    implementation(mn.micronaut.http.server.netty)
    implementation(projects.micronautFlyway)
    implementation(mnSql.hikaricp)
    annotationProcessor(mnSerde.micronaut.serde.processor)
    implementation(mnSerde.micronaut.serde.jackson)
    runtimeOnly(mnLogging.logback.classic)
    runtimeOnly(mnSql.h2)

    testAnnotationProcessor(mn.micronaut.inject.java)
    testImplementation(mnTest.micronaut.test.junit5)
    testImplementation(mn.micronaut.http.client)
    testRuntimeOnly(mnTest.junit.jupiter.engine)
}
tasks.withType<Test> {
    useJUnitPlatform()
}