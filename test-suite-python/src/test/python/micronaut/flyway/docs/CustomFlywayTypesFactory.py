from micronaut.context.annotation import Requires

# tag::imports[]
from jakarta.inject import Named, Singleton
from micronaut.context.annotation import Factory
from org.flywaydb.core.api.migration import BaseJavaMigration, Context, JavaMigration
# end::imports[]


@Requires(property="spec.name", pattern="CustomFlywayTypesFactoryTest|BooksFlywayConfigurationCustomizerTest")
# tag::clazz[]
@Factory
class CustomFlywayTypesFactory:

    @Named("books")  # <1>
    @Singleton
    def books_migrations(self) -> list[JavaMigration]:
        return [V3__Migrate_books()]  # <2>


class V3__Migrate_books(BaseJavaMigration):  # <3>

    def migrate(self, context: Context) -> None:
        pass  # Execute migration
# end::clazz[]
