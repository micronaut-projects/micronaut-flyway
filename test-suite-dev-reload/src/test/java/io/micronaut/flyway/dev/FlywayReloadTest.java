/*
 * Copyright 2017-2026 original authors
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
package io.micronaut.flyway.dev;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micronaut.context.ApplicationContext;
import io.micronaut.dev.tck.ReloadHarness;
import io.micronaut.dev.tck.ReloadTck;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs an application with Flyway through the development runtime: a migration added, edited or removed under a
 * location of an enabled data source restarts the application, whose start runs Flyway, unless Flyway would refuse it. The database is H2 in memory,
 * kept by {@code DB_CLOSE_DELAY} across the restarts, whether or not the pool is retained.
 */
class FlywayReloadTest {

    private static final String V1 = "db/migration/V1__owners.sql";
    private static final String V1_SQL = "CREATE TABLE OWNERS (ID INT PRIMARY KEY);\n";
    private static final String V2 = "db/migration/V2__owner_nick.sql";
    private static final String V2_SQL = "ALTER TABLE OWNERS ADD COLUMN NICK VARCHAR(30);\n";
    private static final Duration TIMEOUT = Duration.ofMinutes(1);
    // longer than a batch the runtime would handle for a restart that is not wanted
    private static final Duration NO_RESTART = Duration.ofSeconds(2);

    @TempDir
    Path project;

    @TempDir
    Path outside;

    private final String url = "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000";
    private final String otherUrl = "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000";

    @BeforeAll
    static void initializeH2() throws ClassNotFoundException {
        // H2 preallocates an exception as it initializes TraceObject, whose stack trace would otherwise hold the
        // frames of the first generation that opens a connection, and with them its classes
        Class.forName("org.h2.message.TraceObject", true, FlywayReloadTest.class.getClassLoader());
    }

    @Test
    void aNewMigrationAloneRestartsTheApplicationAndIsApplied() throws Exception {
        try (ReloadHarness harness = ReloadHarness.inDirectory(project)) {
            database(harness, "default", url);
            harness.resource(V1, V1_SQL);
            harness.start();
            assertFalse(columnExists(url, "OWNERS", "NICK"));

            harness.resource(V2, V2_SQL);
            harness.reload();
            awaitGeneration(harness, 2);

            assertTrue(columnExists(url, "OWNERS", "NICK"));
            assertEquals(List.of("1", "2"), appliedVersions(url));
            assertStaysAtGeneration(harness, 2);
        }
    }

    @Test
    void aFieldAddedBeforeItsMigrationFailsUntilTheMigrationAloneIsAdded() throws Exception {
        try (ReloadHarness harness = ReloadHarness.inDirectory(project)) {
            database(harness, "default", url);
            harness.resource(V1, V1_SQL);
            harness.source("example.Owners", owners("SELECT ID FROM OWNERS"));
            harness.start();

            harness.source("example.Owners", owners("SELECT ID, NICK FROM OWNERS"));
            harness.reload();
            assertEquals(2, harness.generation());
            assertThrows(SQLException.class, () -> query(url, "SELECT ID, NICK FROM OWNERS"));

            harness.resource(V2, V2_SQL);
            harness.reload();
            awaitGeneration(harness, 3);

            assertEquals(List.of(), query(url, "SELECT ID, NICK FROM OWNERS"));
        }
    }

    @Test
    void aMigrationAndAFieldInOneEditRestartOnce() throws Exception {
        try (ReloadHarness harness = ReloadHarness.inDirectory(project)) {
            database(harness, "default", url);
            harness.resource(V1, V1_SQL);
            harness.source("example.Owners", owners("SELECT ID FROM OWNERS"));
            harness.start();

            harness.source("example.Owners", owners("SELECT ID, NICK FROM OWNERS"));
            harness.resource(V2, V2_SQL);
            harness.reload();

            assertEquals(2, harness.generation());
            assertTrue(columnExists(url, "OWNERS", "NICK"));
            assertStaysAtGeneration(harness, 2);
        }
    }

    @Test
    void anAppliedMigrationEditedIsReportedAndTheApplicationKeepsRunning() throws Exception {
        try (ReloadHarness harness = ReloadHarness.inDirectory(project)) {
            database(harness, "default", url);
            harness.resource(V1, V1_SQL);
            harness.start();
            String checksum = checksums(url).get(0);
            ListAppender<ILoggingEvent> log = reloaderLog();

            harness.resource(V1, V1_SQL + "ALTER TABLE OWNERS ADD COLUMN EDITED INT;\n");
            harness.reload();
            assertStaysAtGeneration(harness, 1);

            assertTrue(log.list.stream().anyMatch(event -> event.getLevel() == Level.ERROR && event.getFormattedMessage().contains("checksum mismatch")),
                "the error is reported");
            assertTrue(harness.context().isRunning());
            assertEquals(List.of(checksum), checksums(url), "neither repaired nor applied again");
            assertFalse(columnExists(url, "OWNERS", "EDITED"));

            log.list.clear();
            harness.resource(V1, V1_SQL);
            harness.reload();

            assertStaysAtGeneration(harness, 1);
            assertTrue(log.list.isEmpty(), "put back, nothing is wrong");
            assertEquals(List.of(checksum), checksums(url));
        }
    }

    @Test
    void aRestartWithAnAppliedMigrationEditedFailsTheStartAndTheNextChangeRecovers() throws Exception {
        try (ReloadHarness harness = ReloadHarness.inDirectory(project)) {
            database(harness, "default", url);
            harness.resource(V1, V1_SQL);
            harness.source("example.Owners", owners("one"));
            harness.start();
            String checksum = checksums(url).get(0);

            harness.resource(V1, V1_SQL + "ALTER TABLE OWNERS ADD COLUMN EDITED INT;\n");
            harness.source("example.Owners", owners("two"));
            assertThrows(IllegalStateException.class, harness::reload);
            assertTrue(harness.runtime().isStartFailed());
            assertEquals(List.of(checksum), checksums(url), "neither repaired nor applied again");

            harness.resource(V1, V1_SQL);
            harness.reload();

            assertFalse(harness.runtime().isStartFailed());
            assertTrue(harness.context().isRunning());
            assertEquals(List.of(checksum), checksums(url));
        }
    }

    @Test
    void anAppliedMigrationEditedThenPutBackWithANewMigrationAppliesTheNewOne() throws Exception {
        try (ReloadHarness harness = ReloadHarness.inDirectory(project)) {
            database(harness, "default", url);
            harness.resource(V1, V1_SQL);
            harness.start();

            harness.resource(V1, V1_SQL + "ALTER TABLE OWNERS ADD COLUMN NICK VARCHAR(30);\n");
            harness.reload();
            assertStaysAtGeneration(harness, 1);

            // the fix a developer makes: the change moves to a migration of its own
            harness.resource(V1, V1_SQL);
            harness.resource(V2, V2_SQL);
            harness.reload();
            awaitGeneration(harness, 2);

            assertTrue(columnExists(url, "OWNERS", "NICK"));
            assertEquals(List.of("1", "2"), appliedVersions(url));
        }
    }

    @Test
    void aNewMigrationBesideAnEditedOneIsNotAppliedUntilTheEditIsPutBack() throws Exception {
        try (ReloadHarness harness = ReloadHarness.inDirectory(project)) {
            database(harness, "default", url);
            harness.resource(V1, V1_SQL);
            harness.start();

            harness.resource(V1, V1_SQL + "ALTER TABLE OWNERS ADD COLUMN EDITED INT;\n");
            harness.resource(V2, V2_SQL);
            harness.reload();
            assertStaysAtGeneration(harness, 1);
            assertFalse(columnExists(url, "OWNERS", "NICK"));

            harness.resource(V1, V1_SQL);
            harness.reload();
            awaitGeneration(harness, 2);
            assertTrue(columnExists(url, "OWNERS", "NICK"));
        }
    }

    @Test
    void anAppliedMigrationEditedIsAppliedAgainOnlyWhenCleaningTheSchemaIsConfigured() throws Exception {
        try (ReloadHarness harness = ReloadHarness.inDirectory(project)) {
            database(harness, "default", url);
            harness.property("flyway.datasources.default.clean-schema", "true");
            harness.resource(V1, V1_SQL);
            harness.start();

            harness.resource(V1, V1_SQL + "ALTER TABLE OWNERS ADD COLUMN EDITED INT;\n");
            harness.reload();
            awaitGeneration(harness, 2);

            assertFalse(harness.runtime().isStartFailed());
            assertTrue(columnExists(url, "OWNERS", "EDITED"));
        }
    }

    @Test
    void oneDataSourceRefusedOfTwoSharingALocationHoldsTheRestartBack() throws Exception {
        try (ReloadHarness harness = ReloadHarness.inDirectory(project)) {
            database(harness, "default", url);
            database(harness, "other", otherUrl);
            harness.resource(V1, V1_SQL);
            harness.start();

            // the default data source refuses what the other applies
            execute(url, "UPDATE \"flyway_schema_history\" SET \"checksum\" = 1 WHERE \"version\" = '1'");
            harness.resource(V2, V2_SQL);
            harness.reload();

            assertStaysAtGeneration(harness, 1);
            assertFalse(columnExists(otherUrl, "OWNERS", "NICK"));
        }
    }

    @Test
    void anEditedMigrationWithCleaningOnlyAllowedByACustomizerIsStillRefused() throws Exception {
        try (ReloadHarness harness = ReloadHarness.inDirectory(project)) {
            database(harness, "default", url);
            harness.source("example.AllowClean", """
                package example;
                @jakarta.inject.Singleton
                @jakarta.inject.Named("default")
                public class AllowClean implements io.micronaut.flyway.FlywayConfigurationCustomizer {
                    @Override
                    public String getName() {
                        return "default";
                    }
                    @Override
                    public void customizeFluentConfiguration(org.flywaydb.core.api.configuration.FluentConfiguration configuration) {
                        configuration.cleanDisabled(false);
                    }
                }
                """);
            harness.resource(V1, V1_SQL);
            harness.start();

            harness.resource(V1, V1_SQL + "ALTER TABLE OWNERS ADD COLUMN EDITED INT;\n");
            harness.reload();

            assertStaysAtGeneration(harness, 1);
            assertFalse(columnExists(url, "OWNERS", "EDITED"));
        }
    }

    @Test
    void aRepeatableMigrationEditedRestartsAndIsAppliedAgain() throws Exception {
        try (ReloadHarness harness = ReloadHarness.inDirectory(project)) {
            database(harness, "default", url);
            harness.resource(V1, V1_SQL);
            harness.resource("db/migration/R__view.sql", "CREATE OR REPLACE VIEW OWNER_IDS AS SELECT ID FROM OWNERS;\n");
            harness.start();

            harness.resource("db/migration/R__view.sql", "CREATE OR REPLACE VIEW OWNER_IDS AS SELECT ID, 1 AS ONE FROM OWNERS;\n");
            harness.reload();
            awaitGeneration(harness, 2);

            assertTrue(columnExists(url, "OWNER_IDS", "ONE"));
        }
    }

    @Test
    void aSaveThatChangesNothingForFlywayDoesNotRestart() throws Exception {
        try (ReloadHarness harness = ReloadHarness.inDirectory(project)) {
            database(harness, "default", url);
            harness.resource(V1, V1_SQL);
            harness.start();

            // the same content, a file Flyway does not take for a migration, a resource outside the locations
            harness.resource(V1, V1_SQL);
            harness.resource("db/migration/notes.txt", "not a migration");
            harness.resource("db/migration/notes.sql", "SELECT 1;\n");
            harness.resource("elsewhere/V9__elsewhere.sql", "CREATE TABLE ELSEWHERE (ID INT);\n");
            harness.reload();

            assertStaysAtGeneration(harness, 1);
            assertEquals(List.of("1"), appliedVersions(url));
        }
    }

    @Test
    void aMigrationRemovedIsReportedOnlyWhenFlywayWouldRefuseIt() throws Exception {
        try (ReloadHarness harness = ReloadHarness.inDirectory(project)) {
            database(harness, "default", url);
            harness.resource(V1, V1_SQL);
            harness.resource(V2, V2_SQL);
            harness.start();
            assertEquals(List.of("1", "2"), appliedVersions(url));

            // a migration never applied is gone, and nothing would notice; nor would Flyway of the highest applied one,
            // which it takes for a future version
            harness.resource("db/migration/V4__note.sql", "ALTER TABLE OWNERS ADD COLUMN NOTE VARCHAR(30);\n");
            harness.reload();
            awaitGeneration(harness, 2);
            ListAppender<ILoggingEvent> log = reloaderLog();
            remove(harness, "db/migration/V4__note.sql");
            assertStaysAtGeneration(harness, 2);
            assertTrue(log.list.isEmpty());

            remove(harness, V1);
            assertStaysAtGeneration(harness, 2);
            assertTrue(log.list.stream().anyMatch(event -> event.getLevel() == Level.ERROR), "the error is reported");
            assertTrue(harness.context().isRunning());

            log.list.clear();
            harness.resource(V1, V1_SQL);
            harness.reload();
            assertStaysAtGeneration(harness, 2);
            assertTrue(log.list.isEmpty());
        }
    }

    @Test
    void aWildcardLocationSelectsWhatItMatches() throws Exception {
        try (ReloadHarness harness = ReloadHarness.inDirectory(project)) {
            database(harness, "default", url);
            harness.property("flyway.datasources.default.locations", "classpath:db/*/migration");
            harness.resource("db/owners/migration/V1__owners.sql", V1_SQL);
            harness.start();

            harness.resource("db/owners/other/V2__other.sql", "CREATE TABLE OTHER_ONE (ID INT);\n");
            harness.reload();
            assertStaysAtGeneration(harness, 1);

            harness.resource("db/vets/migration/V2__vets.sql", "CREATE TABLE VETS (ID INT);\n");
            harness.reload();
            awaitGeneration(harness, 2);

            assertEquals(List.of("1", "2"), appliedVersions(url));
        }
    }

    @Test
    void aFileSystemLocationOutsideTheResourceRootsIsWatched() throws Exception {
        Path migrations = Files.createDirectories(outside.resolve("migrations"));
        Files.writeString(migrations.resolve("V1__owners.sql"), V1_SQL);
        try (ReloadHarness harness = ReloadHarness.inDirectory(project)) {
            database(harness, "default", url);
            harness.property("flyway.datasources.default.locations", "filesystem:" + migrations.toRealPath());
            harness.start();
            assertEquals(List.of("1"), appliedVersions(url));

            Files.writeString(migrations.resolve("notes.txt"), "not a migration");
            assertStaysAtGeneration(harness, 1);

            Files.writeString(migrations.resolve("V2__owner_nick.sql"), V2_SQL);
            awaitGeneration(harness, 2);

            assertTrue(columnExists(url, "OWNERS", "NICK"));

            // the next generation watches it too
            Files.writeString(migrations.resolve("V3__owner_note.sql"), "ALTER TABLE OWNERS ADD COLUMN NOTE VARCHAR(30);\n");
            awaitGeneration(harness, 3);

            assertTrue(columnExists(url, "OWNERS", "NOTE"));
        }
    }

    @Test
    void aFileSystemLocationUnderAResourceRootRestartsOnce() throws Exception {
        try (ReloadHarness harness = ReloadHarness.inDirectory(project)) {
            database(harness, "default", url);
            harness.resource("migrations/V1__owners.sql", V1_SQL);
            harness.property("flyway.datasources.default.locations", "filesystem:" + project.toRealPath().resolve("src/main/resources/migrations"));
            harness.start();

            Files.writeString(project.resolve("src/main/resources/migrations/V2__owner_nick.sql"), V2_SQL);
            awaitGeneration(harness, 2);

            assertTrue(columnExists(url, "OWNERS", "NICK"));
            assertStaysAtGeneration(harness, 2);
        }
    }

    @Test
    void eachDataSourceFollowsItsOwnLocationsAndADisabledOneNone() throws Exception {
        String disabledUrl = "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000";
        try (ReloadHarness harness = ReloadHarness.inDirectory(project)) {
            database(harness, "default", url);
            database(harness, "other", otherUrl);
            database(harness, "disabled", disabledUrl);
            harness.property("flyway.datasources.other.locations", "classpath:db/other");
            harness.property("flyway.datasources.disabled.locations", "classpath:db/disabled");
            harness.property("flyway.datasources.disabled.enabled", "false");
            harness.resource(V1, V1_SQL);
            harness.resource("db/other/V1__vets.sql", "CREATE TABLE VETS (ID INT);\n");
            harness.resource("db/disabled/V1__disabled.sql", "CREATE TABLE DISABLED (ID INT);\n");
            harness.start();

            harness.resource("db/disabled/V2__more.sql", "CREATE TABLE MORE_DISABLED (ID INT);\n");
            harness.reload();
            assertStaysAtGeneration(harness, 1);

            harness.resource("db/other/V2__vet_name.sql", "ALTER TABLE VETS ADD COLUMN NAME VARCHAR(30);\n");
            harness.reload();
            awaitGeneration(harness, 2);

            assertTrue(columnExists(otherUrl, "VETS", "NAME"));
            assertEquals(List.of("1"), appliedVersions(url));
            assertFalse(tableExists(disabledUrl, "DISABLED"), "a disabled data source is not migrated");
        }
    }

    @Test
    void aMigrationUnderAProjectsOwnResourceRootIsSeenAsPyronautLaysItOut() throws Exception {
        // a Python project keeps its configuration, and db/migration, under config/ and more resources beside it
        Path config = project.resolve("config");
        Files.createDirectories(config.resolve("db/migration"));
        Files.writeString(config.resolve("db/migration/V1__owners.sql"), V1_SQL);
        Path views = Files.createDirectories(project.resolve("views"));
        try (ReloadHarness harness = ReloadHarness.inDirectory(project)) {
            database(harness, "default", url);
            harness.manifest("micronaut.dev.resources.config", "src/main/resources,config");
            harness.manifest("micronaut.dev.resources.other", "views");
            harness.start();
            assertEquals(List.of("1"), appliedVersions(url));

            Path v2 = config.resolve("db/migration/V2__owner_nick.sql");
            Files.writeString(v2, V2_SQL);
            harness.runtime().changed(List.of(v2), List.of());
            awaitGeneration(harness, 2);
            assertTrue(columnExists(url, "OWNERS", "NICK"));

            // the location names the path below a root of any kind
            Path other = Files.createDirectories(views.resolve("db/migration")).resolve("V3__owner_note.sql");
            Files.writeString(other, "ALTER TABLE OWNERS ADD COLUMN NOTE VARCHAR(30);\n");
            harness.runtime().changed(List.of(other), List.of());
            awaitGeneration(harness, 3);
            assertTrue(columnExists(url, "OWNERS", "NOTE"));
        }
    }

    @Test
    void aChangedLocationInTheConfigurationRestartsSoThatItIsMigrated() throws Exception {
        try (ReloadHarness harness = ReloadHarness.inDirectory(project)) {
            database(harness, "default", url);
            harness.resource(V1, V1_SQL);
            harness.resource("db/more/V2__owner_nick.sql", V2_SQL);
            harness.start();
            assertEquals(List.of("1"), appliedVersions(url));

            Path properties = project.resolve("src/main/resources/application.properties");
            Files.writeString(properties, Files.readString(properties) + "\nflyway.datasources.default.locations=classpath:db/migration,classpath:db/more\n");
            harness.runtime().changed(List.of(properties), List.of());
            harness.runtime().awaitGeneration(2, TIMEOUT);

            assertEquals(List.of("1", "2"), appliedVersions(url));
        }
    }

    @Test
    void eachMigrationRestartsAndTheRetiredGenerationsAreCollected() throws Exception {
        try (ReloadHarness harness = ReloadHarness.inDirectory(project)) {
            database(harness, "default", url);
            harness.resource(V1, V1_SQL);
            harness.start();

            harness.resource(V2, V2_SQL);
            harness.reload();
            awaitGeneration(harness, 2);

            harness.resource("db/migration/V3__owner_note.sql", "ALTER TABLE OWNERS ADD COLUMN NOTE VARCHAR(30);\n");
            harness.reload();
            awaitGeneration(harness, 3);

            assertEquals(List.of("1", "2", "3"), appliedVersions(url));
            ReloadTck.assertRetiredGenerationsCollected(harness);
        }
    }

    @Test
    void withoutARuntimeTheReloaderIsNotABean() throws Exception {
        try (ApplicationContext context = ApplicationContext.run(Map.of(
            "micronaut.dev.enabled", "true",
            "datasources.default.url", url,
            "datasources.default.driver-class-name", "org.h2.Driver",
            "datasources.default.username", "sa",
            "datasources.default.password", "",
            "flyway.datasources.default.enabled", "false"
        ))) {
            assertTrue(context.getBeanDefinitions(Object.class).stream()
                .noneMatch(definition -> definition.getBeanType().getSimpleName().equals("DevelopmentFlywayReloader")));
        }
    }

    private static void database(ReloadHarness harness, String name, String url) {
        harness.property("datasources." + name + ".url", url);
        harness.property("datasources." + name + ".driver-class-name", "org.h2.Driver");
        harness.property("datasources." + name + ".username", "sa");
        harness.property("datasources." + name + ".password", "");
        harness.property("flyway.datasources." + name + ".enabled", "true");
    }

    private static ListAppender<ILoggingEvent> reloaderLog() {
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        ((Logger) LoggerFactory.getLogger("io.micronaut.flyway.DevelopmentFlywayReloader")).addAppender(appender);
        return appender;
    }

    private static void remove(ReloadHarness harness, String resource) throws IOException {
        Path file = harness.projectDirectory().resolve("src/main/resources").resolve(resource);
        Files.delete(file);
        harness.runtime().changed(List.of(), List.of(file));
    }

    private static String owners(String query) {
        return """
            package example;
            @jakarta.inject.Singleton
            public class Owners {
                public String query() { return "%s"; }
            }
            """.formatted(query);
    }

    private static void awaitGeneration(ReloadHarness harness, int generation) throws TimeoutException {
        harness.runtime().awaitGeneration(generation, TIMEOUT);
    }

    private static void assertStaysAtGeneration(ReloadHarness harness, int generation) throws InterruptedException {
        Thread.sleep(NO_RESTART.toMillis());
        harness.runtime().whenReady().join();
        assertEquals(generation, harness.generation());
        assertFalse(harness.runtime().isStartFailed());
    }

    private static boolean columnExists(String url, String table, String column) throws SQLException {
        return !query(url, "SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = '" + table + "' AND COLUMN_NAME = '" + column + "'").isEmpty();
    }

    private static boolean tableExists(String url, String table) throws SQLException {
        return !query(url, "SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = '" + table + "'").isEmpty();
    }

    private static List<String> appliedVersions(String url) throws SQLException {
        return query(url, "SELECT \"version\" FROM \"flyway_schema_history\" WHERE \"success\" AND \"version\" IS NOT NULL ORDER BY \"installed_rank\"");
    }

    private static List<String> checksums(String url) throws SQLException {
        return query(url, "SELECT \"checksum\" FROM \"flyway_schema_history\" WHERE \"version\" IS NOT NULL ORDER BY \"installed_rank\"");
    }

    private static void execute(String url, String sql) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private static List<String> query(String url, String sql) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(sql)) {
            List<String> values = new ArrayList<>();
            while (rows.next()) {
                values.add(rows.getString(1));
            }
            return values;
        }
    }
}
