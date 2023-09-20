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
package io.micronaut.flyway;

import io.micronaut.context.annotation.ConfigurationBuilder;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.core.convert.format.MapFormat;
import io.micronaut.core.util.StringUtils;
import io.micronaut.core.util.Toggleable;
import org.flywaydb.core.api.configuration.FluentConfiguration;

import java.util.HashMap;
import java.util.Map;

/**
 * Create a Flyway Configuration for each sub-property of flyway.*.
 *
 * @author Iván López
 * @see org.flywaydb.core.api.configuration.FluentConfiguration
 * @since 1.0.0
 */
@Context
@EachProperty("flyway.datasources")
public class FlywayConfigurationProperties implements Toggleable {

    @SuppressWarnings("WeakerAccess")
    public static final boolean DEFAULT_ENABLED = true;

    @SuppressWarnings("WeakerAccess")
    public static final boolean DEFAULT_ASYNC = false;

    @SuppressWarnings("WeakerAccess")
    public static final boolean DEFAULT_CLEAN_SCHEMA = false;

    @ConfigurationBuilder(prefixes = "", excludes = {"jdbcProperties", "configuration"})
    FluentConfiguration fluentConfiguration = new FluentConfiguration();

    private final String nameQualifier;
    private boolean enabled = DEFAULT_ENABLED;
    private boolean async = DEFAULT_ASYNC;
    private boolean cleanSchema = DEFAULT_CLEAN_SCHEMA;
    private String url;
    private String user;
    private String password;
    private Map<String, String> properties = new HashMap<>();

    /**
     * @param name The name qualifier.
     */
    public FlywayConfigurationProperties(@Parameter String name) {
        this.nameQualifier = name;
    }

    /**
     * @return The qualifier associated with this flyway configuration
     */
    public String getNameQualifier() {
        return nameQualifier;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Set whether this flyway configuration is enabled. Default value ({@value #DEFAULT_ENABLED}).
     *
     * @param enabled true if it is enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Whether the flyway migrations should run asynchronously. Default value: {@value #DEFAULT_ASYNC}.
     * @return Whether the flyway migrations should run asynchronously
     */
    public boolean isAsync() {
        return async;
    }

    /**
     * Whether flyway migrations should run asynchronously.
     *
     * @param async true to run flyway migrations asynchronously
     */
    public void setAsync(boolean async) {
        this.async = async;
    }

    /**
     * Whether Flyway will clean the schema before running the migrations. Default value ({@value #DEFAULT_CLEAN_SCHEMA}).
     *
     * @return Whether clean the schema before running the migrations
     */
    public boolean isCleanSchema() {
        return cleanSchema;
    }

    /**
     * Set whether Flyway will clean the schema before running the migrations. Default value ({@value #DEFAULT_CLEAN_SCHEMA}).
     *
     * @param cleanSchema true to clean the schema before running the migrations.
     */
    public void setCleanSchema(boolean cleanSchema) {
        this.cleanSchema = cleanSchema;
    }

    /**
     * The JDBC url of the database to migrate.
     * @return JDBC url of the database to migrate
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param url The JDBC url of the database to migrate
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * The user of the database to migrate.
     * @return The user of the database to migrate
     */
    public String getUser() {
        return user;
    }

    /**
     * @param user The user of the database to migrate
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * @param username The username of the database to migrate
     */
    public void setUsername(String username) {
        this.user = username;
    }

    /**
     * The password of the database to migrate.
     * @return The password of the database to migrate
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password The password of the database to migrate
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Whether there is an alternative database configuration for the migration. By default, Micronaut framework will use the
     * {@link javax.sql.DataSource} defined for the application but if both {@code url} and {@code user} are defined,
     * then those will be used for Flyway.
     *
     * @return true if there is an alternative database configuration
     */
    public boolean hasAlternativeDatabaseConfiguration() {
        return StringUtils.hasText(this.getUrl());
    }

    /**
     * @return The flyway configuration builder
     */
    public FluentConfiguration getFluentConfiguration() {
        return fluentConfiguration;
    }


    /**
     * @see <a href="https://documentation.red-gate.com/fd/parameters-184127474.html">Flyway parameters</a>.
     * Sets the extra flyway parameters to be passed to {@link FluentConfiguration#configuration(Map)}.
     * WARNING: This may override any existing configurations
     *
     * @param properties The properties to be set
     */
    public void setProperties(@MapFormat(transformation = MapFormat.MapTransformation.FLAT) Map<String, String> properties) {
        this.properties = properties;
    }

    /**
     * @see <a href="https://documentation.red-gate.com/fd/parameters-184127474.html">Flyway parameters</a>.
     * Gets the extra flyway parameters to be passed to {@link FluentConfiguration#configuration(Map)}.
     * WARNING: This may override any existing configurations
     *
     * @return The extra custom properties
     */
    public Map<String, String> getProperties() {
        return properties;
    }
}
