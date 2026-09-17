package io.micronaut.flyway.docs;

import io.micronaut.context.annotation.Requires;

// tag::imports[]
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.flywaydb.core.api.migration.JavaMigration;
// end::imports[]

@Requires(property = "spec.name", pattern = "CustomFlywayTypesFactoryTest|BooksFlywayConfigurationCustomizerTest")
// tag::clazz[]
@Factory
public class CustomFlywayTypesFactory {

    @Named("books") // <1>
    @Singleton
    public JavaMigration[] booksMigrations() {
        return new JavaMigration[] { new V3__Migrate_books() }; // <2>
    }

    static class V3__Migrate_books extends BaseJavaMigration { // <3>

        @Override
        public void migrate(Context context) throws Exception {
            // Execute migration
        }
    }
}
// end::clazz[]
