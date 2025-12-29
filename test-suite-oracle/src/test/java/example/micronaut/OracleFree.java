package example.micronaut;

import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

/**
 * @see <a href="https://testcontainers.com/modules/oracle-free/">Oracle Free TestContainers</a>
 */
public class OracleFree {
    private static final String IMAGE_NAME = "gvenzl/oracle-free:23.4-slim-faststart";
    private static OracleContainer container;

    public static Map<String, String> getProperties() {
        if (container == null) {
            container = new OracleContainer(DockerImageName.parse(IMAGE_NAME));
            container.start();
            do {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            } while(!container.isRunning());
            return getProperties(container);
        } else {
            return getProperties(container);
        }
    }

    private static Map<String, String> getProperties(OracleContainer container) {
        return Map.of(
            "datasources.default.url", container.getJdbcUrl(),
            "datasources.default.username", container.getUsername(),
            "datasources.default.password", container.getPassword(),
            "datasources.default.db-type", "oracle",
            "datasources.default.dialect", "ORACLE",
            "datasources.default.driver-class-name", "oracle.jdbc.OracleDriver"
        );
    }
}
