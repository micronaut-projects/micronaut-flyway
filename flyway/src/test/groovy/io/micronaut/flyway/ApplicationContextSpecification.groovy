package io.micronaut.flyway

import io.micronaut.context.ApplicationContext
import spock.lang.AutoCleanup
import spock.lang.Shared

abstract class ApplicationContextSpecification extends AbstractFlywaySpec implements ConfigurationFixture {

    @Shared
    @AutoCleanup
    ApplicationContext applicationContext = ApplicationContext.run(configuration)
}
