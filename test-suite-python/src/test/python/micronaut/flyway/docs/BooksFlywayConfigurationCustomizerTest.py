from typing import Annotated

import java
from jakarta.inject import Inject, Named
from javax.sql import DataSource
from micronaut.context import ApplicationContext
from micronaut.context.annotation import Property
from micronaut.inject.qualifiers import Qualifiers
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.flywaydb.core import Flyway
from org.junit.jupiter.api import Test

# TODO(python): only java.type(...) aliases can be used as runtime type arguments of getBean / isinstance
FlywayConfigurationCustomizer = java.type("io.micronaut.flyway.FlywayConfigurationCustomizer")
BooksFlywayConfigurationCustomizer = java.type("micronaut.flyway.docs.BooksFlywayConfigurationCustomizer")


@Property(name="spec.name", value="BooksFlywayConfigurationCustomizerTest")
@Property(name="datasources.books.url", value="jdbc:h2:mem:booksCustomizerDb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@Property(name="datasources.books.username", value="sa")
@Property(name="datasources.books.password", value="")
@Property(name="datasources.books.driver-class-name", value="org.h2.Driver")
@Property(name="flyway.datasources.books.enabled", value="true")
@Property(name="flyway.datasources.books.locations", value="classpath:db/books")
@MicronautTest(startApplication=False)
class BooksFlywayConfigurationCustomizerTest:

    application_context: Annotated[ApplicationContext, Inject]
    flyway: Annotated[Flyway, Inject, Named("books")]
    data_source: Annotated[DataSource, Inject, Named("books")]

    @Test
    def the_customizer_replaces_the_default_one(self) -> None:
        customizer = self.application_context.getBean(FlywayConfigurationCustomizer, Qualifiers.byName("books"))

        assert isinstance(customizer, BooksFlywayConfigurationCustomizer)
        assert customizer.getName() == "books"
        assert len(self.flyway.getConfiguration().getJavaMigrations()) == 1

    @Test
    def the_java_migration_set_by_the_customizer_runs(self) -> None:
        connection = self.data_source.getConnection()
        try:
            statement = connection.createStatement()
            result_set = statement.executeQuery(
                "select \"version\", \"description\", \"success\" from \"flyway_schema_history\" where \"version\" = '3'")
            assert result_set.next()
            assert result_set.getString("description") == "Migrate books"
            assert result_set.getBoolean("success")
        finally:
            connection.close()
