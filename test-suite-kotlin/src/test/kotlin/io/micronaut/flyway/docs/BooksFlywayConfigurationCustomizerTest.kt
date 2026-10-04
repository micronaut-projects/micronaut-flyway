package io.micronaut.flyway.docs

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.flyway.FlywayConfigurationCustomizer
import io.micronaut.inject.qualifiers.Qualifiers
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import jakarta.inject.Named
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import javax.sql.DataSource

@Property(name = "spec.name", value = "BooksFlywayConfigurationCustomizerTest")
@Property(name = "datasources.books.url", value = "jdbc:h2:mem:booksCustomizerDb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@Property(name = "datasources.books.username", value = "sa")
@Property(name = "datasources.books.password", value = "")
@Property(name = "datasources.books.driver-class-name", value = "org.h2.Driver")
@Property(name = "flyway.datasources.books.enabled", value = "true")
@Property(name = "flyway.datasources.books.locations", value = "classpath:db/books")
@MicronautTest(startApplication = false)
class BooksFlywayConfigurationCustomizerTest {

    @Inject
    lateinit var applicationContext: ApplicationContext

    @Inject
    @field:Named("books")
    lateinit var flyway: Flyway

    @Inject
    @field:Named("books")
    lateinit var dataSource: DataSource

    @Test
    fun theCustomizerReplacesTheDefaultOne() {
        val customizer = applicationContext.getBean(FlywayConfigurationCustomizer::class.java, Qualifiers.byName("books"))

        assertInstanceOf(BooksFlywayConfigurationCustomizer::class.java, customizer)
        assertEquals("books", customizer.name)
        assertEquals(1, flyway.configuration.javaMigrations.size)
    }

    @Test
    fun theJavaMigrationSetByTheCustomizerRuns() {
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery(
                    "select \"version\", \"description\", \"success\" from \"flyway_schema_history\" where \"version\" = '3'"
                ).use { resultSet ->
                    assertTrue(resultSet.next())
                    assertEquals("Migrate books", resultSet.getString("description"))
                    assertTrue(resultSet.getBoolean("success"))
                }
            }
        }
    }
}
