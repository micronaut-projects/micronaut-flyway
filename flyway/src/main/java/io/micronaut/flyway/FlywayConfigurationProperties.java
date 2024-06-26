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
import io.micronaut.core.naming.conventions.StringConvention;
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

    // NOTE - Some of the ignored properties (javaMigrations, callbacks, resolvers, resourceProvider, javaMigrationClassProvider)
    // are custom Flyway types and implementations are meant to be provided by the user through an implementation of
    // FlywayConfigurationCustomizer if needed.
    //
    // Most of the other ignored properties have overloaded methods in FluentConfiguration, making it non-deterministic as to which
    // of the methods would be selected for setting the builder property, thus explicit property setters are provided here instead
    // that pass the value through to the builder.
    //
    // allEnvironments and environmentProvisionMode were added to the builder in 10.10.0, are undocumented, and were breaking things,
    // so added to the ignore list without a pass-through setter
    @ConfigurationBuilder(prefixes = "", excludes = {"jdbcProperties", "configuration", "dryRunOutput",
        "ignoreMigrationPatterns", "locations", "encoding", "target", "javaMigrations", "dataSource",
        "baselineVersion", "callbacks", "resolvers", "resourceProvider", "javaMigrationClassProvider",
        "allEnvironments", "environmentProvisionMode"})
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
     * @see <a href="https://documentation.red-gate.com/fd/flyway-cli-and-api/configuration/parameters">Flyway parameters</a>.
     * Sets the extra flyway parameters to be passed to {@link FluentConfiguration#configuration(Map)}.
     * WARNING: This will override any existing configuration properties with the same names.
     *
     * @param properties The properties to be set
     */
    public void setProperties(@MapFormat(transformation = MapFormat.MapTransformation.FLAT, keyFormat = StringConvention.CAMEL_CASE) Map<String, String> properties) {
        this.properties = properties;
    }

    /**
     * @see <a href="https://documentation.red-gate.com/fd/flyway-cli-and-api/configuration/parameters">Flyway parameters</a>.
     * Gets the extra flyway parameters to be passed to {@link FluentConfiguration#configuration(Map)}.
     *
     * @return The extra custom properties
     */
    public Map<String, String> getProperties() {
        return properties;
    }

    //Pass-through properties for overloaded FluentConfiguration methods

    /**
     * Sets the dry run output filename.
     *
     * @see FluentConfiguration#dryRunOutput(String)
     *
     * @param dryRunOutputFileName The dry run output filename
     */
    public void setDryRunOutput(String dryRunOutputFileName) {
        fluentConfiguration.dryRunOutput(dryRunOutputFileName);
    }

    /**
     * Sets the migration patterns to ignore.
     *
     * @see FluentConfiguration#ignoreMigrationPatterns(String...)
     *
     * @param ignoreMigrationPatterns The migration patterns to ignore
     */
    public void setIgnoreMigrationPatterns(String... ignoreMigrationPatterns) {
        fluentConfiguration.ignoreMigrationPatterns(ignoreMigrationPatterns);
    }

    /**
     * Sets the locations to scan recursively for migrations.
     *
     * @see FluentConfiguration#locations(String...)
     *
     * @param locations The locations to scan for migrations
     */
    public void setLocations(String... locations) {
        fluentConfiguration.locations(locations);
    }

    /**
     * Sets the encoding of SQL migrations.
     *
     * @see FluentConfiguration#encoding(String)
     *
     * @param encoding The encoding of SQL migrations
     */
    public void setEncoding(String encoding) {
        fluentConfiguration.encoding(encoding);
    }

    /**
     * Sets the target version up to which Flyway should consider migrations.
     *
     * @see FluentConfiguration#target(String)
     *
     * @param target The target version
     */
    public void setTarget(String target) {
        fluentConfiguration.target(target);
    }

    /**
     * The version to tag an existing schema with when executing baseline. Passes through to {@link FluentConfiguration#baselineVersion(String)}
     * @param baselineVersion The version to tag an existing schema with when executing baseline.
     */
    public void setBaselineVersion(String baselineVersion) {
        fluentConfiguration.baselineVersion(baselineVersion);
    }

 }
