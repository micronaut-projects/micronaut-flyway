from micronaut.context.annotation import Requires

# tag::imports[]
from jakarta.inject import Named, Singleton
from micronaut.context.annotation import Factory
from org.flywaydb.core.api import MigrationVersion
from org.flywaydb.core.api.migration import Context, JavaMigration
# end::imports[]


@Requires(property="spec.name", pattern="CustomFlywayTypesFactoryTest|BooksFlywayConfigurationCustomizerTest")
# tag::clazz[]
@Factory
class CustomFlywayTypesFactory:

    @Named("books")  # <1>
    @Singleton
    def books_migrations(self) -> list[JavaMigration]:
        return [V3__Migrate_books()]  # <2>


class V3__Migrate_books(JavaMigration):  # <3>

    def getVersion(self) -> MigrationVersion:
        return MigrationVersion.fromVersion("3")

    def getDescription(self) -> str:
        return "Migrate books"

    def getChecksum(self) -> int | None:
        return None

    def canExecuteInTransaction(self) -> bool:
        return True

    def migrate(self, context: Context) -> None:
        pass  # Execute migration
# end::clazz[]
