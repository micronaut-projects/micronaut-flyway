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

@Requires(property = "spec.name", pattern = "CustomFlywayTypesFactoryTest|BooksFlywayConfigurationCustomizerTest")
// tag::clazz[]
@Factory
class CustomFlywayTypesFactory {

    @Named("books") // <1>
    @Singleton
    fun booksMigrations(): Array<JavaMigration> {
        return arrayOf(V3__Migrate_books()) // <2>
    }

    class V3__Migrate_books : BaseJavaMigration() { // <3>

        override fun migrate(context: Context) {
            // Execute migration
        }
    }
}
// end::clazz[]
