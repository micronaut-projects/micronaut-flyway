package io.micronaut.flyway

import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Requires
import io.micronaut.inject.qualifiers.Qualifiers
import jakarta.inject.Named
import jakarta.inject.Singleton
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.callback.Callback
import org.flywaydb.core.api.callback.Event
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.flywaydb.core.api.migration.JavaMigration

import javax.sql.DataSource

class FlywayConfigurationCustomizerSpec extends AbstractFlywaySpec {

    void 'flyway configuration can be customized with an implementation of FlywayConfigurationCustomizer'() {
        given:
        run('spec.name'                             : FlywayConfigurationCustomizerSpec.simpleName,
                'flyway.datasources.movies.enabled' : true,
                'datasources.movies.url'            : 'jdbc:h2:mem:flyway2Db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
                'datasources.movies.username'       : DS_USERNAME,
                'datasources.movies.password'       : DS_PASSWORD,
                'datasources.movies.driverClassName': DS_DRIVER,

                'flyway.datasources.books.enabled'  : true,
                'datasources.books.url'             : 'jdbc:h2:mem:flywayDb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
                'datasources.books.username'        : DS_USERNAME,
                'datasources.books.password'        : DS_PASSWORD,
                'datasources.books.driverClassName' : DS_DRIVER)

        when:
        applicationContext.getBean(DataSource, Qualifiers.byName('movies'))
        applicationContext.getBean(FlywayConfigurationProperties, Qualifiers.byName('movies'))
        Flyway moviesFlyway = applicationContext.getBean(Flyway, Qualifiers.byName('movies'))

        then:
        noExceptionThrown()
        moviesFlyway

        when:
        applicationContext.getBean(DataSource, Qualifiers.byName('books'))
        applicationContext.getBean(FlywayConfigurationProperties, Qualifiers.byName('books'))
        Flyway booksFlyway = applicationContext.getBean(Flyway, Qualifiers.byName('books'))

        then:
        noExceptionThrown()
        moviesFlyway.configuration.javaMigrations.size() == 1
        moviesFlyway.configuration.callbacks.size() == 1
        booksFlyway.configuration.javaMigrations.size() == 1
        booksFlyway.configuration.callbacks.size() == 0
    }
}

@Requires(property = "spec.name", value = "FlywayConfigurationCustomizerSpec")
//tag::typesfactorystart[]
@Factory
public class CustomFlywayTypesFactory {
//end::typesfactorystart[]

    //tag::migrations[]
    @Named("books")     //<1>
    @Singleton
    public JavaMigration[] booksMigrations() {
        return [ new V3__Migrate_books() ];      //<2>
    }
    //end::migrations[]

    @Named("movies")
    @Singleton
    JavaMigration[] moviesMigrations() {
        return [ new V3__Migrate_movies() ]
    }

    @Named("movies")
    @Singleton
    Callback[] moviesCallbacks() {
        return [ new Callback() {

            @Override
            boolean supports(Event event, org.flywaydb.core.api.callback.Context context) {
                return false
            }

            @Override
            boolean canHandleInTransaction(Event event, org.flywaydb.core.api.callback.Context context) {
                return false
            }

            @Override
            void handle(Event event, org.flywaydb.core.api.callback.Context context) {

            }

            @Override
            String getCallbackName() {
                return null
            }
        }]
    }

    //tag::javamigration[]

    static class V3__Migrate_books extends BaseJavaMigration    //<3>
    {

        @Override
        void migrate(Context context) throws Exception {
            //Execute migration
        }
    }
    //end::javamigration[]

    static class V3__Migrate_movies extends BaseJavaMigration
    {

        @Override
        void migrate(Context context) throws Exception {

        }
    }

//tag::typesfactoryend[]
}
//end::typesfactoryend[]

@Requires(property = "spec.name", value = "FlywayConfigurationCustomizerSpec")
//tag::customizer[]
@Singleton
public class BooksFlywayConfigurationCustomizer implements FlywayConfigurationCustomizer {  //<1>

    private final static String NAME = "books";
    private final JavaMigration[] javaMigrations;

    public BooksFlywayConfigurationCustomizer(@Named(NAME) JavaMigration[] javaMigrations) { //<2>
        this.javaMigrations = javaMigrations;
    }

    @Override
    public void customizeFluentConfiguration(FluentConfiguration fluentConfiguration) {
        if(javaMigrations != null) {
            fluentConfiguration.javaMigrations(javaMigrations); //<3>
        }
    }

    @Override
    public String getName() {
        return NAME;
    }
}
//end::customizer[]
