/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019-2023 Andres Almiray.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kordamp.gradle.plugin.oci.tasks.delete


import com.oracle.bmc.database.DatabaseClient
import com.oracle.bmc.database.model.AutonomousDatabase
import com.oracle.bmc.database.model.AutonomousDatabaseSummary
import com.oracle.bmc.database.requests.DeleteAutonomousDatabaseRequest
import com.oracle.bmc.database.requests.GetAutonomousDatabaseRequest
import com.oracle.bmc.database.requests.ListAutonomousDatabasesRequest
import groovy.transform.CompileStatic
import org.kordamp.gradle.plugin.oci.tasks.AbstractOCITask
import org.kordamp.gradle.plugin.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.plugin.oci.tasks.traits.CompartmentIdAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.OptionalDatabaseIdAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.OptionalDatabaseNameAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.WaitForCompletionAwareTrait
import org.kordamp.jipsy.annotations.TypeProviderFor

import static org.kordamp.gradle.util.StringUtils.isBlank
import static org.kordamp.gradle.util.StringUtils.isNotBlank

/**
 * @author Andres Almiray
 * @since 0.5.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class DeleteAutonomousDatabaseTask extends AbstractOCITask implements CompartmentIdAwareTrait,
    OptionalDatabaseIdAwareTrait,
    OptionalDatabaseNameAwareTrait,
    WaitForCompletionAwareTrait {
    static final String TASK_DESCRIPTION = 'Deletes a Database.'

    @Override
    protected void doExecuteTask() {
        validateDatabaseId()

        if (isBlank(getResolvedDatabaseId().orNull) && isBlank(getResolvedDatabaseName().orNull)) {
            throw new IllegalStateException("Missing value for either 'databaseId' or 'databaseName' in $path")
        }

        DatabaseClient client = createDatabaseClient()

        // TODO: check if database exists
        // TODO: check is database is in a 'deletable' state

        if (isNotBlank(getResolvedDatabaseId().orNull)) {
            AutonomousDatabase database = client.getAutonomousDatabase(GetAutonomousDatabaseRequest.builder()
                .autonomousDatabaseId(getResolvedDatabaseId().get())
                .build())
                .autonomousDatabase

            if (database) {
                setDatabaseName(database.displayName)
                deleteDatabase(client, database)
            }
        } else {
            validateCompartmentId()

            client.listAutonomousDatabases(ListAutonomousDatabasesRequest.builder()
                .compartmentId(getResolvedCompartmentId().get())
                .displayName(getResolvedDatabaseName().get())
                .build())
                .items.each { database ->
                setDatabaseId(database.id)
                deleteDatabase(client, database)
            }
        }
    }

    private void deleteDatabase(DatabaseClient client, AutonomousDatabase database) {
        println("Deleting Database '${database.displayName}' with id ${database.id}")
        client.deleteAutonomousDatabase(DeleteAutonomousDatabaseRequest.builder()
            .autonomousDatabaseId(database.id)
            .build())

        if (getResolvedWaitForCompletion().get()) {
            println("Waiting for Database to be ${state('Terminated')}")
            client.waiters
                .forAutonomousDatabase(GetAutonomousDatabaseRequest.builder().autonomousDatabaseId(database.id).build(),
                    AutonomousDatabase.LifecycleState.Terminated)
                .execute()
        }
    }

    private void deleteDatabase(DatabaseClient client, AutonomousDatabaseSummary database) {
        println("Deleting Database '${database.displayName}' with id ${database.id}")
        client.deleteAutonomousDatabase(DeleteAutonomousDatabaseRequest.builder()
            .autonomousDatabaseId(database.id)
            .build())

        if (getResolvedWaitForCompletion().get()) {
            println("Waiting for Database to be ${state('Terminated')}")
            client.waiters
                .forAutonomousDatabase(GetAutonomousDatabaseRequest.builder().autonomousDatabaseId(database.id).build(),
                    AutonomousDatabase.LifecycleState.Terminated)
                .execute()
        }
    }
}
