package io.micronaut.configuration.dbmigration.flyway;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;

import javax.inject.Singleton;
import javax.sql.DataSource;
import java.util.Collection;

@Singleton
public class DataSourceEventListener extends AbstractFlywayMigration implements BeanCreatedEventListener<DataSource> {

    private final Collection<FlywayConfigurationProperties> flywayConfigurationProperties;

    public DataSourceEventListener(ApplicationContext applicationContext, ApplicationEventPublisher eventPublisher, Collection<FlywayConfigurationProperties> flywayConfigurationProperties) {
        super(applicationContext, eventPublisher);
        this.flywayConfigurationProperties = flywayConfigurationProperties;
    }

    //    public DataSourceEventListener(ApplicationContext applicationContext, ApplicationEventPublisher eventPublisher) {
//        super(applicationContext, eventPublisher);
//        this.applicationContext = applicationContext;
//        this.eventPublisher = eventPublisher;
//    }


//    public DataSourceEventListener(ApplicationContext applicationContext,
//                                   ApplicationEventPublisher eventPublisher) {
//        this.applicationContext = applicationContext;
//        this.eventPublisher = eventPublisher;
//    }

    @Override
    public DataSource onCreated(BeanCreatedEvent<DataSource> event) {
        System.out.println("event = [" + event + "]");

        DataSource dataSource = event.getBean();

        String name = event.getBeanIdentifier().getName();

        // find the find in the list
        flywayConfigurationProperties.stream()
                .filter(flywayConfig -> flywayConfig.getNameQualifier().equals(name))
                .findFirst()
                .ifPresent(flywayConfig -> run(flywayConfig, name, dataSource));

        // findBean and check the returned optional, manually log. ya no!
//        run();
//        run(dataSource, name);

//        Optional<FlywayConfigurationProperties> config = applicationContext.findBean(
//                FlywayConfigurationProperties.class,
//                Qualifiers.byName(name)
//        );
//
//        if (config.isPresent()) {
//            run(dataSource, name);
//        }

//        config.ifPresent(this::run);

        return dataSource;
    }


}
