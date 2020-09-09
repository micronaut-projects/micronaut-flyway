package io.micronaut.flyway

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
                                                   String dataSourceName = 'default',
                                                   String driverClassName = 'org.h2.Driver',
                                                   String username = 'sa',
                                                   String password = '') {
        Map<String, Object> m = [:]
        m['datasources.' + dataSourceName + '.url'] = driverClassName == 'org.h2.Driver' ? h2JdbcUrl(database) : ''
        m['datasources.' + dataSourceName + '.username'] = username
        m['datasources.' + dataSourceName + '.password'] = password
        m['datasources.' + dataSourceName + '.driverClassName'] = driverClassName
        m
    }

    String h2JdbcUrl(String dbName) {
        'jdbc:h2:mem:' + dbName + ';DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE'
    }

    String getSpecName() {
        null
    }
}
