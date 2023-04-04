package example;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@MicronautTest
public class FlywayTest {

    @Test
    void testFlywayMigration(DataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement("select * from users")) {
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    Assertions.assertTrue(resultSet.next());
                }
            }
        }
    }
}
