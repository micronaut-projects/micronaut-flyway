from micronaut.context.annotation import Requires

# tag::imports[]
from typing import Annotated

from jakarta.inject import Named, Singleton
from micronaut.flyway import FlywayConfigurationCustomizer
from org.flywaydb.core.api.configuration import FluentConfiguration
from org.flywaydb.core.api.migration import JavaMigration
# end::imports[]

NAME = "books"


@Requires(property="spec.name", value="BooksFlywayConfigurationCustomizerTest")
# tag::clazz[]
@Singleton
class BooksFlywayConfigurationCustomizer(FlywayConfigurationCustomizer):  # <1>

    def __init__(self, java_migrations: Annotated[list[JavaMigration], Named(NAME)]):  # <2>
        self.java_migrations = java_migrations

    def customizeFluentConfiguration(self, fluent_configuration: FluentConfiguration) -> None:
        if self.java_migrations is not None:
            fluent_configuration.javaMigrations(self.java_migrations)  # <3>

    def getName(self) -> str:
        return NAME
# end::clazz[]
