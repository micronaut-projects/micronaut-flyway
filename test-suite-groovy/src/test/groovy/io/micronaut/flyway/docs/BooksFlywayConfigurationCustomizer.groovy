package io.micronaut.flyway.docs

import io.micronaut.context.annotation.Requires

// tag::imports[]
import io.micronaut.flyway.FlywayConfigurationCustomizer
import jakarta.inject.Named
import jakarta.inject.Singleton
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.flywaydb.core.api.migration.JavaMigration
// end::imports[]

@Requires(property = "spec.name", value = "BooksFlywayConfigurationCustomizerSpec")
// tag::clazz[]
@Singleton
class BooksFlywayConfigurationCustomizer implements FlywayConfigurationCustomizer { // <1>

    private static final String NAME = "books"
    private final JavaMigration[] javaMigrations

    BooksFlywayConfigurationCustomizer(@Named(NAME) JavaMigration[] javaMigrations) { // <2>
        this.javaMigrations = javaMigrations
    }

    @Override
    void customizeFluentConfiguration(FluentConfiguration fluentConfiguration) {
        if (javaMigrations != null) {
            fluentConfiguration.javaMigrations(javaMigrations) // <3>
        }
    }

    @Override
    String getName() {
        return NAME
    }
}
// end::clazz[]
