package io.micronaut.flyway.serde;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
class EndpointSerdeTest {

    @Test
    void testFlywayEndpointWithSerde(@Client("/") HttpClient httpClient) {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpRequest<?>  request = HttpRequest.GET("/flyway");
        String json = assertDoesNotThrow(() -> client.retrieve(request));
        System.out.println(json);
        assertNotNull(json);
    }
}
