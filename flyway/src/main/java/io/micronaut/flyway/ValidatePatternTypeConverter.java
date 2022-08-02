/*
 * Copyright 2017-2022 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.flyway;

import io.micronaut.core.convert.ConversionContext;
import io.micronaut.core.convert.TypeConverter;
import jakarta.inject.Singleton;
import org.flywaydb.core.api.pattern.ValidatePattern;

import java.util.Optional;

/**
 * Converts String's to Flyway ValidatePattern required for reading ignore-migration-patterns parameter.
 *
 * @author Nenad Vico
 * @see org.flywaydb.core.api.pattern.ValidatePattern
 * @since 5.4.1
 */
@Singleton
public class ValidatePatternTypeConverter implements TypeConverter<String, ValidatePattern> {

    @Override
    public Optional<ValidatePattern> convert(String pattern,
                                             Class<ValidatePattern> targetType,
                                             ConversionContext context) {
        if (pattern == null || pattern.trim().length() == 0) {
            return Optional.empty();
        }
        return Optional.of(ValidatePattern.fromPattern(pattern));
    }
}
