package io.micronaut.flyway

import static io.micronaut.flyway.AbstractFlywaySpec.DS_DRIVER
import static io.micronaut.flyway.AbstractFlywaySpec.DS_PASSWORD
import static io.micronaut.flyway.AbstractFlywaySpec.DS_USERNAME

trait ConfigurationFixture {

    Map<String, Object> getConfiguration() {
        Map<String, Object> m = [:]
        if (specName) {
            m['spec.name'] = specName
        }
        m
    }

    Map<String, Object> getJpaConfiguration(List<String> packages = ['example.micronaut']) {
        [
                'jpa.default.packages-to-scan'                 : packages,
                'jpa.default.properties.hibernate.hbm2ddl.auto': 'none',
                'jpa.default.properties.hibernate.show_sql'    : true,
        ]
    }

    Map<String, Object> getDataSourceConfiguration(String database,
                                                   String dsName = 'default',
                                                   String driverClassName = DS_DRIVER,
                                                   String username = DS_USERNAME,
                                                   String password = DS_PASSWORD) {
        Map<String, Object> m = [:]
        m['datasources.' + dsName + '.url'] = driverClassName == DS_DRIVER ? h2JdbcUrl(database) : ''
        m['datasources.' + dsName + '.username'] = username
        m['datasources.' + dsName + '.password'] = password
        m['datasources.' + dsName + '.driverClassName'] = driverClassName
        m
    }

    String h2JdbcUrl(String dbName) {
        'jdbc:h2:mem:' + dbName + ';DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE'
    }

    String getSpecName() {
        null
    }
}
