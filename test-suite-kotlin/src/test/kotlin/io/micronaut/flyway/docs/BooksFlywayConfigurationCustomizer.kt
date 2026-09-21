package io.micronaut.flyway.docs

import io.micronaut.context.annotation.Requires

// tag::imports[]
import io.micronaut.flyway.FlywayConfigurationCustomizer
import jakarta.inject.Named
import jakarta.inject.Singleton
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.flywaydb.core.api.migration.JavaMigration
// end::imports[]

private const val NAME = "books"

@Requires(property = "spec.name", value = "BooksFlywayConfigurationCustomizerTest")
// tag::clazz[]
@Singleton
class BooksFlywayConfigurationCustomizer( // <1>
    @param:Named(NAME) private val javaMigrations: Array<JavaMigration>? // <2>
) : FlywayConfigurationCustomizer {

    override fun customizeFluentConfiguration(fluentConfiguration: FluentConfiguration) {
        if (javaMigrations != null) {
            fluentConfiguration.javaMigrations(*javaMigrations) // <3>
        }
    }

    override fun getName(): String = NAME
}
// end::clazz[]
