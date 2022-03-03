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
package io.micronaut.flyway.event;

import io.micronaut.context.event.ApplicationEvent;
import io.micronaut.flyway.FlywayConfigurationProperties;

/**
 * Fired when a Flyway migration has finished.
 *
 * @author Iván López
 * @since 1.0.0
 */
public class MigrationFinishedEvent extends ApplicationEvent {

    /**
     * @param config The Flyway configuration that corresponds to the
     * Flyway instance that has been migrated
     */
    public MigrationFinishedEvent(FlywayConfigurationProperties config) {
        super(config);
    }
}
