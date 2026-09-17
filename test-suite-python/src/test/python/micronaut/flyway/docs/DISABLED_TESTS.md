# Python Docs Disabled Test Inventory

This file tracks the Python documentation examples of Micronaut Flyway under `test-suite-python/src/test/python/micronaut/flyway/docs`
that are disabled, or that carry a workaround because the direct port of the Java example does not compile or does not behave
like the Java example yet (Python compiler gaps). It is the bug-fixing task list for the Python compiler
(`micronaut-inject-python` / `micronaut-context-python`); every row references a `TODO(python)` comment in the sources.

The Python examples are compiled by every build and their tests run with `./gradlew pythonCheck -Ppython-ci`
(the "Python CI" GitHub workflow).

## Reconciliation

- Last generated active `@Disabled` count: 2.
- Last generated command: `rg -n "@Disabled\(" test-suite-python/src/test/python`.
- Last full-suite command: `./gradlew :test-suite-python:test -Ppython-ci`.
- Last full-suite result: build successful, 4 tests executed (2 test classes), 2 skipped.

## Migration Rules

- The snippet classes live in `io.micronaut.flyway.docs` in every language: a Python source package cannot be the imported
  Java package `micronaut.flyway` itself (the compiler generates its `__init__.py` for the imported
  `FlywayConfigurationCustomizer` shim).
- A Python class cannot extend the Java class `BaseJavaMigration`: `V3__Migrate_books` implements the `JavaMigration`
  interface and returns its version and description explicitly (the guide carries a `[.lang-python]` note); Python has no
  nested classes, so it is a top-level class of the factory module.
- A Python test class is a `@MicronautTest` with `@Test` methods and plain `assert` statements; the tests verify that the
  migrations actually ran by querying `"flyway_schema_history"` through the `@Named("books") DataSource`.
- `@Requires(property=, pattern=)` is used where the Java example would use a value list.

## Active `@Disabled` Tests

| Test | Reason |
| --- | --- |
| `CustomFlywayTypesFactoryTest.the_named_java_migrations_are_applied_to_the_flyway_configuration` | The `list[JavaMigration]` return type of the `@Named("books")` factory method is bridged as `java.util.List<JavaMigration>`; an array-typed bean (`JavaMigration[]`) cannot be declared in Python, so `DefaultFlywayConfigurationCustomizer` (`findBean(JavaMigration[].class, Qualifiers.byName("books"))`) does not find the migrations. `BooksFlywayConfigurationCustomizerTest` proves the same factory works through a custom `FlywayConfigurationCustomizer` injecting the list bean. The guide carries a `[.lang-python]` note. |
| `CustomFlywayTypesFactoryTest.the_java_migration_runs` | Same cause: without the array bean the default customizer never applies `V3__Migrate_books`. |

## Workarounds in the Sources

None.

## `java.type` usages

| Target | Reason |
| --- | --- |
| `BooksFlywayConfigurationCustomizerTest` (`FlywayConfigurationCustomizer`, `BooksFlywayConfigurationCustomizer`) | only `java.type(...)` aliases can be used as the runtime type argument of `ApplicationContext.getBean(...)` and of an `isinstance` check (imported shim classes only work as type hints, generic bases and annotation members). |
