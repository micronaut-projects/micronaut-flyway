package io.micronaut.flyway.docs

import groovy.sql.Sql
import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import jakarta.inject.Named
import org.flywaydb.core.Flyway
import spock.lang.Specification

import javax.sql.DataSource

@Property(name = "spec.name", value = "CustomFlywayTypesFactorySpec")
@Property(name = "datasources.books.url", value = "jdbc:h2:mem:booksTypesDb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@Property(name = "datasources.books.username", value = "sa")
@Property(name = "datasources.books.password", value = "")
@Property(name = "datasources.books.driver-class-name", value = "org.h2.Driver")
@Property(name = "flyway.datasources.books.enabled", value = "true")
@Property(name = "flyway.datasources.books.locations", value = "classpath:db/books")
@MicronautTest(startApplication = false)
class CustomFlywayTypesFactorySpec extends Specification {

    @Inject
    @Named("books")
    Flyway flyway

    @Inject
    @Named("books")
    DataSource dataSource

    void "the named JavaMigrations are applied to the Flyway configuration"() {
        expect:
        flyway.configuration.javaMigrations.length == 1
        flyway.configuration.javaMigrations[0].version.version == "3"
    }

    void "the JavaMigration runs"() {
        when:
        def rows = new Sql(dataSource).rows('select "version", "description", "success" from "flyway_schema_history" where "version" is not null order by "installed_rank"')

        then:
        rows*.version == ["1", "3"]
        rows[1].description == "Migrate books"
        rows[1].success
    }
}
