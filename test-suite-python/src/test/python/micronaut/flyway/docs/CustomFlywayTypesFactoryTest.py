from typing import Annotated

from jakarta.inject import Inject, Named
from javax.sql import DataSource
from micronaut.context.annotation import Property
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.flywaydb.core import Flyway
from org.junit.jupiter.api import Disabled, Test


@Property(name="spec.name", value="CustomFlywayTypesFactoryTest")
@Property(name="datasources.books.url", value="jdbc:h2:mem:booksTypesDb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@Property(name="datasources.books.username", value="sa")
@Property(name="datasources.books.password", value="")
@Property(name="datasources.books.driver-class-name", value="org.h2.Driver")
@Property(name="flyway.datasources.books.enabled", value="true")
@Property(name="flyway.datasources.books.locations", value="classpath:db/books")
@MicronautTest(startApplication=False)
class CustomFlywayTypesFactoryTest:

    flyway: Annotated[Flyway, Inject, Named("books")]
    data_source: Annotated[DataSource, Inject, Named("books")]

    # TODO(python): a `list[JavaMigration]` return type is bridged as `java.util.List<JavaMigration>`, an array-typed
    # bean (`JavaMigration[]`) cannot be declared in Python, so the default FlywayConfigurationCustomizer
    # (`findBean(JavaMigration[].class, Qualifiers.byName("books"))`) does not find the migrations of the factory
    @Disabled("TODO(python): array-typed beans cannot be declared in Python (see DISABLED_TESTS.md)")
    @Test
    def the_named_java_migrations_are_applied_to_the_flyway_configuration(self) -> None:
        java_migrations = self.flyway.getConfiguration().getJavaMigrations()
        assert len(java_migrations) == 1
        assert java_migrations[0].getVersion().getVersion() == "3"

    @Disabled("TODO(python): array-typed beans cannot be declared in Python (see DISABLED_TESTS.md)")
    @Test
    def the_java_migration_runs(self) -> None:
        connection = self.data_source.getConnection()
        try:
            statement = connection.createStatement()
            result_set = statement.executeQuery(
                "select \"version\", \"description\", \"success\" from \"flyway_schema_history\" where \"version\" is not null order by \"installed_rank\"")
            assert result_set.next()
            assert result_set.getString("version") == "1"
            assert result_set.next()
            assert result_set.getString("version") == "3"
            assert result_set.getString("description") == "Migrate books"
            assert result_set.getBoolean("success")
            assert not result_set.next()
        finally:
            connection.close()
