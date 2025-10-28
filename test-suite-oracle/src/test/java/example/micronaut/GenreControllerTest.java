/*
 * Copyright 2017-2024 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package example.micronaut;

import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.http.uri.UriBuilder;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@MicronautTest
class GenreControllerTest {
    private static final String PATH_GENRES = "/genres";
    private static final String PATH_GENRES_LIST = "/genres/list";
    private static final Function<Long, String> FUNCTION_GENRES_ID = id ->
        UriBuilder.of(PATH_GENRES).path("" + id).build().toString();

    @Test
    void testFindNonExistingGenreReturns404(@Client("/") HttpClient httpClient) {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpClientResponseException thrown = assertThrows(HttpClientResponseException.class, () ->
            client.exchange(HttpRequest.GET(FUNCTION_GENRES_ID.apply(99L))));
        assertNotNull(thrown.getResponse());
        assertEquals(HttpStatus.NOT_FOUND, thrown.getStatus());
    }

    @Test
    void testGenreCrudOperations(@Client("/") HttpClient httpClient) {
        BlockingHttpClient client = httpClient.toBlocking();

        List<Long> genreIds = new ArrayList<>();

        HttpRequest<?> request = HttpRequest.POST(PATH_GENRES, Collections.singletonMap("name", "DevOps"));
        HttpResponse<?> response = client.exchange(request);
        genreIds.add(entityId(response));

        assertEquals(HttpStatus.CREATED, response.getStatus());

        request = HttpRequest.POST(PATH_GENRES, Collections.singletonMap("name", "Microservices"));
        response = client.exchange(request);

        assertEquals(HttpStatus.CREATED, response.getStatus());

        Long id = entityId(response);
        genreIds.add(id);
        request = HttpRequest.GET(FUNCTION_GENRES_ID.apply(id));

        Genre genre = client.retrieve(request, Genre.class);

        assertEquals("Microservices", genre.getName());

        request = HttpRequest.PUT(PATH_GENRES, new GenreUpdateCommand(id, "Micro-services"));
        response = client.exchange(request);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatus());

        request = HttpRequest.GET(FUNCTION_GENRES_ID.apply(id));
        genre = client.retrieve(request, Genre.class);
        assertEquals("Micro-services", genre.getName());

        request = HttpRequest.GET(PATH_GENRES_LIST);
        List<Genre> genres = client.retrieve(request, Argument.of(List.class, Genre.class));

        assertEquals(2, genres.size());

        request = HttpRequest.POST("/genres/ex", Collections.singletonMap("name", "Microservices"));
        response = client.exchange(request);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatus());

        request = HttpRequest.GET(PATH_GENRES_LIST);
        genres = client.retrieve(request, Argument.of(List.class, Genre.class));

        assertEquals(2, genres.size());

        request = HttpRequest.GET(UriBuilder.of(PATH_GENRES_LIST)
            .queryParam("size", "1")
            .build());
        genres = client.retrieve(request, Argument.of(List.class, Genre.class));

        assertEquals(1, genres.size());
        assertEquals("DevOps", genres.get(0).getName());

        request = HttpRequest.GET(UriBuilder.of(PATH_GENRES_LIST)
            .queryParam("size", "1")
            .queryParam("sort", "name,desc")
            .build());
        genres = client.retrieve(request, Argument.of(List.class, Genre.class));

        assertEquals(1, genres.size());
        assertEquals("Micro-services", genres.get(0).getName());

        request = HttpRequest.GET(UriBuilder.of(PATH_GENRES_LIST)
            .queryParam("size", "1")
            .queryParam("page", "2")
            .build());
        genres = client.retrieve(request, Argument.of(List.class, Genre.class));

        assertEquals(0, genres.size());

        // cleanup:
        for (Long genreId : genreIds) {
            request = HttpRequest.DELETE(FUNCTION_GENRES_ID.apply(genreId));
            response = client.exchange(request);
            assertEquals(HttpStatus.NO_CONTENT, response.getStatus());
        }
    }

    private static Long entityId(HttpResponse<?> response) {
        String path = "/genres/";
        String value = response.header(HttpHeaders.LOCATION);
        if (value == null) {
            return null;
        }
        int index = value.indexOf(path);
        if (index != -1) {
            return Long.valueOf(value.substring(index + path.length()));
        }
        return null;
    }
}
