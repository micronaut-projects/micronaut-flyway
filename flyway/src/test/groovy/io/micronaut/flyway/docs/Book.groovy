package io.micronaut.flyway.docs

//tag::bookImport[]
import grails.gorm.annotation.Entity
//end::bookImport[]
import io.micronaut.context.annotation.Requires
import io.micronaut.core.annotation.Introspected

@Introspected
//tag::annotation[]
@Entity
//end::annotation[]
@Requires(property = 'spec.name', value = 'GormDocSpec')
//tag::domain[]
class Book {
    String name
}
//end::domain[]
