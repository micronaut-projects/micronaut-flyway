package io.micronaut.configuration.dbmigration.flyway;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import io.micronaut.aop.Adapter;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Retention(RUNTIME)
@Target(ElementType.METHOD)
@Adapter(io.micronaut.context.event.BeanCreatedEventListener.class)
public @interface BeanCreatedEventListener {
}
