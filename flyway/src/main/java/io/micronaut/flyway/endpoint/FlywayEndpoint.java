/*
 * Copyright 2017-2020 original authors
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
package io.micronaut.flyway.endpoint;

import io.micronaut.context.ApplicationContext;
import io.micronaut.flyway.FlywayConfigurationProperties;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.management.endpoint.annotation.Endpoint;
import io.micronaut.management.endpoint.annotation.Read;
import org.flywaydb.core.Flyway;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.Collection;

/**
 * Provides a flyway endpoint to get all the migrations applied.
 *
 * @author Iván López
 * @since 1.0.0
 */
@Endpoint(id = FlywayEndpoint.NAME)
public class FlywayEndpoint {

    /**
     * Endpoint name.
     */
    public static final String NAME = "flyway";

    private final ApplicationContext applicationContext;
    private final Collection<FlywayConfigurationProperties> flywayConfigurationProperties;

    /**
     * @param applicationContext            The {@link ApplicationContext}
     * @param flywayConfigurationProperties Collection of Flyway Configurations
     */
    public FlywayEndpoint(ApplicationContext applicationContext,
                          Collection<FlywayConfigurationProperties> flywayConfigurationProperties) {
        this.applicationContext = applicationContext;
        this.flywayConfigurationProperties = flywayConfigurationProperties;
    }

    /**
     * @return A list of Flyway migrations per active configuration
     */
    @Read
    public Publisher<FlywayReport> flywayMigrations() {

        return Flux.fromIterable(flywayConfigurationProperties)
                .filter(FlywayConfigurationProperties::isEnabled)
                .map(c -> new Pair<>(c,
                        applicationContext
                                .findBean(Flyway.class, Qualifiers.byName(c.getNameQualifier()))
                                .orElse(null)))
                .filter(pair -> pair.getSecond() != null)
                .map(pair -> new FlywayReport(
                        pair.getFirst().getNameQualifier(),
                        Arrays.asList(pair.getSecond().info().all()))
                );
    }

    /**
     * A pair of any types.
     *
     * @param <T1> The first type
     * @param <T2> The second type
     */
    static class Pair<T1, T2> {

        private final T1 first;
        private final T2 second;

        /**
         * @param first  The first parameter
         * @param second The second parameter
         */
        public Pair(T1 first, T2 second) {
            this.first = first;
            this.second = second;
        }

        /**
         * @return The first parameter
         */
        public T1 getFirst() {
            return first;
        }

        /**
         * @return The second parameter
         */
        public T2 getSecond() {
            return second;
        }
    }

}
