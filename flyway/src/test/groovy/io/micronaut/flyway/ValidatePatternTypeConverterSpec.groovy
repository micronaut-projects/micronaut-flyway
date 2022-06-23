/*
 * Copyright 2017-2022 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.flyway

import org.flywaydb.core.api.ErrorCode
import org.flywaydb.core.api.FlywayException
import org.flywaydb.core.api.MigrationState
import org.flywaydb.core.api.pattern.ValidatePattern
import org.flywaydb.core.internal.license.FlywayTeamsUpgradeRequiredException
import spock.lang.Specification

/**
 * Created by Nenad Vico on 22/06/2022.
 */
class ValidatePatternTypeConverterSpec extends Specification {


    void "test empty string conversion"() {
        given:
        ValidatePatternTypeConverter validatePatternTypeConverter = new ValidatePatternTypeConverter()

        expect:
        !validatePatternTypeConverter.convert("", ValidatePattern.class).isPresent()
    }

    void "test wrong string conversion"() {
        given:
        ValidatePatternTypeConverter validatePatternTypeConverter = new ValidatePatternTypeConverter()

        when:
        validatePatternTypeConverter.convert("junk", ValidatePattern.class)

        then:
        def e = thrown(FlywayException)
        e.errorCode == ErrorCode.ERROR
        e.message == 'Invalid pattern \'junk\'. Pattern must be of the form <migration_type>:<migration_state> See https://rd.gt/37m4hXD for full details'
    }

    void "test future string conversion"() {
        given:
        ValidatePatternTypeConverter validatePatternTypeConverter = new ValidatePatternTypeConverter()

        expect:
        validatePatternTypeConverter.convert("*:future", ValidatePattern.class).get()
                .matchesMigration(false, MigrationState.FUTURE_SUCCESS)
    }

    void "test repeatable string conversion"() {
        given:
        ValidatePatternTypeConverter validatePatternTypeConverter = new ValidatePatternTypeConverter()

        when:
        validatePatternTypeConverter.convert("repeatable:*", ValidatePattern.class)

        then:
        def e = thrown(FlywayTeamsUpgradeRequiredException)
        e.errorCode == ErrorCode.ERROR
        e.message == 'Flyway Teams Edition upgrade required: ignoreMigrationPattern with type \'repeatable\' is not supported by Flyway Community Edition\n' +
                'Try Flyway Teams Edition for free: https://rd.gt/2VzHpkY'
    }

    void "test versioned string conversion"() {
        given:
        ValidatePatternTypeConverter validatePatternTypeConverter = new ValidatePatternTypeConverter()

        when:
        validatePatternTypeConverter.convert("versioned:missing", ValidatePattern.class)

        then:
        def e = thrown(FlywayTeamsUpgradeRequiredException)
        e.errorCode == ErrorCode.ERROR
        e.message == 'Flyway Teams Edition upgrade required: ignoreMigrationPattern with type \'versioned\' is not supported by Flyway Community Edition\n' +
                'Try Flyway Teams Edition for free: https://rd.gt/2VzHpkY'
    }

    void "test missing string conversion"() {
        given:
        ValidatePatternTypeConverter validatePatternTypeConverter = new ValidatePatternTypeConverter()

        expect:
        validatePatternTypeConverter.convert("*:missing", ValidatePattern.class).get()
                .matchesMigration(false, MigrationState.MISSING_SUCCESS)
    }

}
