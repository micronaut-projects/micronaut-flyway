/*
 * Copyright 2017-2024 original authors
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
package io.micronaut.flyway.endpoint;

import io.micronaut.core.annotation.Internal;
import io.micronaut.serde.annotation.SerdeImport;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.internal.info.MigrationInfoImpl;
import org.flywaydb.core.internal.resolver.ResolvedMigrationImpl;
import org.flywaydb.core.internal.resolver.sql.SqlMigrationExecutor;
import org.flywaydb.core.internal.schemahistory.BaseAppliedMigration;

/**
 * Serde imports for the flyway endpoint.
 */
@Internal
@SerdeImport(ResolvedMigrationImpl.class)
@SerdeImport(BaseAppliedMigration.class)
@SerdeImport(SqlMigrationExecutor.class)
@SerdeImport(MigrationVersion.class)
@SerdeImport(MigrationInfoImpl.class)
class FlywayEndpointSerdeImports {
}
