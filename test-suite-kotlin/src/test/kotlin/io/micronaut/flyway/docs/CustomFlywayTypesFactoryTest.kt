package io.micronaut.flyway.docs

import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import jakarta.inject.Named
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import javax.sql.DataSource

@Property(name = "spec.name", value = "CustomFlywayTypesFactoryTest")
@Property(name = "datasources.books.url", value = "jdbc:h2:mem:booksTypesDb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@Property(name = "datasources.books.username", value = "sa")
@Property(name = "datasources.books.password", value = "")
@Property(name = "datasources.books.driver-class-name", value = "org.h2.Driver")
@Property(name = "flyway.datasources.books.enabled", value = "true")
@Property(name = "flyway.datasources.books.locations", value = "classpath:db/books")
@MicronautTest(startApplication = false)
class CustomFlywayTypesFactoryTest {

    @Inject
    @field:Named("books")
    lateinit var flyway: Flyway

    @Inject
    @field:Named("books")
    lateinit var dataSource: DataSource

    @Test
    fun theNamedJavaMigrationsAreAppliedToTheFlywayConfiguration() {
        assertEquals(1, flyway.configuration.javaMigrations.size)
        assertEquals("3", flyway.configuration.javaMigrations[0].version.version)
    }

    @Test
    fun theJavaMigrationRuns() {
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery(
                    "select \"version\", \"description\", \"success\" from \"flyway_schema_history\" where \"version\" is not null order by \"installed_rank\""
                ).use { resultSet ->
                    assertTrue(resultSet.next())
                    assertEquals("1", resultSet.getString("version"))
                    assertTrue(resultSet.next())
                    assertEquals("3", resultSet.getString("version"))
                    assertEquals("Migrate books", resultSet.getString("description"))
                    assertTrue(resultSet.getBoolean("success"))
                    assertFalse(resultSet.next())
                }
            }
        }
    }
}
