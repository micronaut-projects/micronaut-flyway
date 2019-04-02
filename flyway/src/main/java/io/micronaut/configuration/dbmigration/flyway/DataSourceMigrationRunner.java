package io.micronaut.configuration.dbmigration.flyway;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.core.naming.NameResolver;
import io.micronaut.inject.qualifiers.Qualifiers;

import javax.inject.Singleton;
import javax.sql.DataSource;
import java.util.Collection;

@Singleton
public class DataSourceMigrationRunner extends AbstractFlywayMigration implements BeanCreatedEventListener<DataSource> {

    public DataSourceMigrationRunner(ApplicationContext applicationContext,
                                     ApplicationEventPublisher eventPublisher) {
        super(applicationContext, eventPublisher);
    }

    @Override
    public DataSource onCreated(BeanCreatedEvent<DataSource> event) {
        System.out.println("event = [" + event + "]");

        DataSource dataSource = event.getBean();

        if (event.getBeanDefinition() instanceof NameResolver) {
            ((NameResolver) event.getBeanDefinition())
                    .resolveName().ifPresent(name -> {

                applicationContext.findBean(FlywayConfigurationProperties.class, Qualifiers.byName(name))
                .ifPresent(flywayConfig -> run(flywayConfig, name, dataSource));
            });
        }

        return dataSource;
    }


}
