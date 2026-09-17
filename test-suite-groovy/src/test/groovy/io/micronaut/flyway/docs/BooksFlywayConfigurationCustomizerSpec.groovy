package io.micronaut.flyway.docs

import groovy.sql.Sql
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.flyway.FlywayConfigurationCustomizer
import io.micronaut.inject.qualifiers.Qualifiers
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import jakarta.inject.Named
import org.flywaydb.core.Flyway
import spock.lang.Specification

import javax.sql.DataSource

@Property(name = "spec.name", value = "BooksFlywayConfigurationCustomizerSpec")
@Property(name = "datasources.books.url", value = "jdbc:h2:mem:booksCustomizerDb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@Property(name = "datasources.books.username", value = "sa")
@Property(name = "datasources.books.password", value = "")
@Property(name = "datasources.books.driver-class-name", value = "org.h2.Driver")
@Property(name = "flyway.datasources.books.enabled", value = "true")
@Property(name = "flyway.datasources.books.locations", value = "classpath:db/books")
@MicronautTest(startApplication = false)
class BooksFlywayConfigurationCustomizerSpec extends Specification {

    @Inject
    ApplicationContext applicationContext

    @Inject
    @Named("books")
    Flyway flyway

    @Inject
    @Named("books")
    DataSource dataSource

    void "the customizer replaces the default one"() {
        when:
        def customizer = applicationContext.getBean(FlywayConfigurationCustomizer, Qualifiers.byName("books"))

        then:
        customizer instanceof BooksFlywayConfigurationCustomizer
        customizer.name == "books"
        flyway.configuration.javaMigrations.length == 1
    }

    void "the JavaMigration set by the customizer runs"() {
        when:
        def row = new Sql(dataSource).firstRow('select "version", "description", "success" from "flyway_schema_history" where "version" = \'3\'')

        then:
        row.description == "Migrate books"
        row.success
    }
}
