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
package io.micronaut.flyway;

import io.micronaut.core.naming.Named;
import org.flywaydb.core.api.configuration.FluentConfiguration;

/**
 * Interface for customizing Flyway configuration. Allows for injection of custom implementations
 * of Flyway-specific types, and for setting general configuration properties that might not yet
 * be explicitly supported in {@link FlywayConfigurationProperties}.
 *
 * @author Jeremy Grelle
 * @since 7.2.0
 */
public interface FlywayConfigurationCustomizer extends Named {

    /**
     * A callback for customizing Flyway configuration by setting properties on the
     * {@link FluentConfiguration} builder prior to execution of migrations.
     *
     * @param fluentConfiguration The configuration to be customized
     */
    void customizeFluentConfiguration(FluentConfiguration fluentConfiguration);
}
