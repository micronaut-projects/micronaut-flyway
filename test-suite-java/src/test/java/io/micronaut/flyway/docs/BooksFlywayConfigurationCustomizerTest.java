package io.micronaut.flyway.docs;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.flyway.FlywayConfigurationCustomizer;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    ApplicationContext applicationContext;

    @Inject
    @Named("books")
    Flyway flyway;

    @Inject
    @Named("books")
    DataSource dataSource;

    @Test
    void theCustomizerReplacesTheDefaultOne() {
        FlywayConfigurationCustomizer customizer = applicationContext.getBean(FlywayConfigurationCustomizer.class, Qualifiers.byName("books"));

        assertInstanceOf(BooksFlywayConfigurationCustomizer.class, customizer);
        assertEquals("books", customizer.getName());
        assertEquals(1, flyway.getConfiguration().getJavaMigrations().length);
    }

    @Test
    void theJavaMigrationSetByTheCustomizerRuns() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                 "select \"version\", \"description\", \"success\" from \"flyway_schema_history\" where \"version\" = '3'")) {
            assertTrue(resultSet.next());
            assertEquals("Migrate books", resultSet.getString("description"));
            assertTrue(resultSet.getBoolean("success"));
        }
    }
}
