package io.micronaut.flyway.docs

import io.micronaut.context.annotation.Requires

// tag::imports[]
import io.micronaut.context.annotation.Factory
import jakarta.inject.Named
import jakarta.inject.Singleton
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.flywaydb.core.api.migration.JavaMigration
// end::imports[]

@Requires(property = "spec.name", pattern = "CustomFlywayTypesFactorySpec|BooksFlywayConfigurationCustomizerSpec")
// tag::clazz[]
@Factory
class CustomFlywayTypesFactory {

    @Named("books") // <1>
    @Singleton
    JavaMigration[] booksMigrations() {
        return [new V3__Migrate_books()] // <2>
    }

    static class V3__Migrate_books extends BaseJavaMigration { // <3>

        @Override
        void migrate(Context context) throws Exception {
            // Execute migration
        }
    }
}
// end::clazz[]
