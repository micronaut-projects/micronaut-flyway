package io.micronaut.flyway.docs;

import io.micronaut.context.annotation.Requires;

// tag::imports[]
import io.micronaut.flyway.FlywayConfigurationCustomizer;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.migration.JavaMigration;
// end::imports[]

@Requires(property = "spec.name", value = "BooksFlywayConfigurationCustomizerTest")
// tag::clazz[]
@Singleton
public class BooksFlywayConfigurationCustomizer implements FlywayConfigurationCustomizer { // <1>

    private static final String NAME = "books";
    private final JavaMigration[] javaMigrations;

    public BooksFlywayConfigurationCustomizer(@Named(NAME) JavaMigration[] javaMigrations) { // <2>
        this.javaMigrations = javaMigrations;
    }

    @Override
    public void customizeFluentConfiguration(FluentConfiguration fluentConfiguration) {
        if (javaMigrations != null) {
            fluentConfiguration.javaMigrations(javaMigrations); // <3>
        }
    }

    @Override
    public String getName() {
        return NAME;
    }
}
// end::clazz[]
