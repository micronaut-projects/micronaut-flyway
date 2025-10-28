plugins {
    id("io.micronaut.application")
    id("io.micronaut.test-resources")
}
repositories {
    mavenCentral()
}
application {
    mainClass = "example.micronaut.Application"
}
dependencies {
    annotationProcessor(mnValidation.micronaut.validation.processor)
    implementation(mnValidation.micronaut.validation)
    annotationProcessor(mnSerde.micronaut.serde.processor)
    implementation(mnSerde.micronaut.serde.jackson)
    annotationProcessor(mnData.micronaut.data.processor)
    implementation(mnData.micronaut.data.jdbc)
    implementation(mnSql.micronaut.jdbc.hikari)
    implementation(mnSql.ojdbc11)
    runtimeOnly(libs.managed.flyway.oracle)
    implementation(projects.micronautFlyway)
    runtimeOnly(mnLogging.logback.classic)
    testImplementation(mnTest.micronaut.test.junit5)
    testImplementation(mn.micronaut.http.client)
    testRuntimeOnly(mnTest.junit.platform.launcher)
}
micronaut {
    version(libs.versions.micronaut.platform.get())
    runtime("netty")
    testRuntime("junit5")
}
