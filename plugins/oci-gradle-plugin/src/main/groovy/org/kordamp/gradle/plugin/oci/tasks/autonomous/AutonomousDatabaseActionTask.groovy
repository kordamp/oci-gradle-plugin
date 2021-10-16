/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019-2021 Andres Almiray.
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
package org.kordamp.gradle.plugin.oci.tasks.autonomous


import com.oracle.bmc.database.DatabaseClient
import com.oracle.bmc.database.model.AutonomousDatabase
import com.oracle.bmc.database.requests.GetAutonomousDatabaseRequest
import com.oracle.bmc.database.requests.ListAutonomousDatabasesRequest
import com.oracle.bmc.database.requests.RestartAutonomousDatabaseRequest
import com.oracle.bmc.database.requests.StartAutonomousDatabaseRequest
import com.oracle.bmc.database.requests.StopAutonomousDatabaseRequest
import groovy.transform.CompileStatic
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.options.Option
import org.gradle.api.tasks.options.OptionValues
import org.kordamp.gradle.plugin.oci.tasks.AbstractOCITask
import org.kordamp.gradle.plugin.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.plugin.oci.tasks.traits.CompartmentIdAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.OptionalDatabaseIdAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.OptionalDatabaseNameAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.WaitForCompletionAwareTrait
import org.kordamp.jipsy.annotations.TypeProviderFor

import static org.kordamp.gradle.PropertyUtils.resolveValue
import static org.kordamp.gradle.util.StringUtils.isBlank
import static org.kordamp.gradle.util.StringUtils.isNotBlank

/**
 * @author Andres Almiray
 * @since 0.5.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class AutonomousDatabaseActionTask extends AbstractOCITask implements CompartmentIdAwareTrait,
    OptionalDatabaseIdAwareTrait,
    OptionalDatabaseNameAwareTrait,
    WaitForCompletionAwareTrait {
    static final String TASK_DESCRIPTION = 'Performs a given action on an Database.'

    private static enum DatabaseAction {
        START(AutonomousDatabase.LifecycleState.Available),
        STOP(AutonomousDatabase.LifecycleState.Stopped),
        RESTART(AutonomousDatabase.LifecycleState.Available)

        private AutonomousDatabase.LifecycleState state

        DatabaseAction(AutonomousDatabase.LifecycleState state) {
            this.state = state
        }

        AutonomousDatabase.LifecycleState state() {
            this.state
        }
    }

    @Internal
    final Property<DatabaseAction> action = project.objects.property(DatabaseAction)

    @Option(option = 'action', description = 'The action to be executed (REQUIRED).')
    void setAction(DatabaseAction action) {
        getAction().set(action)
    }

    @Input
    Provider<DatabaseAction> getResolvedAction() {
        project.providers.provider {
            String value = resolveValue('OCI_DATABASE_ACTION', 'oci.database.action', this, project.name)
            isNotBlank(value) ? DatabaseAction.valueOf(value) : action.getOrElse(DatabaseAction.STOP)
        }
    }

    @OptionValues("action")
    List<DatabaseAction> getAvailableActions() {
        return new ArrayList<DatabaseAction>(Arrays.asList(DatabaseAction.values()))
    }

    @Override
    protected void doExecuteTask() {
        validateDatabaseId()

        if (isBlank(getResolvedDatabaseId().orNull) && isBlank(getResolvedDatabaseName().orNull)) {
            throw new IllegalStateException("Missing value for either 'databaseId' or 'databaseName' in $path")
        }
        if (!getResolvedAction()) {
            throw new IllegalStateException("Missing value for 'action' in $path")
        }

        DatabaseClient client = createDatabaseClient()

        if (isNotBlank(getResolvedDatabaseId().orNull)) {
            databaseAction(client, getResolvedDatabaseId().get(), getResolvedAction().get())
        } else {
            validateCompartmentId()

            client.listAutonomousDatabases(ListAutonomousDatabasesRequest.builder()
                .compartmentId(getResolvedCompartmentId().get())
                .displayName(getResolvedDatabaseName().get())
                .build())
                .items.each { database ->
                setDatabaseId(database.id)
                databaseAction(client, database.id, getResolvedAction().get())
            }
        }
    }

    private void databaseAction(DatabaseClient client, String databaseId, DatabaseAction action) {
        println("Sending ${getResolvedAction().get().name()} to Database with id ${console.yellow(databaseId)}")

        switch (action) {
            case DatabaseAction.START:
                client.startAutonomousDatabase(StartAutonomousDatabaseRequest.builder()
                    .autonomousDatabaseId(databaseId)
                    .build())
                break
            case DatabaseAction.STOP:
                client.stopAutonomousDatabase(StopAutonomousDatabaseRequest.builder()
                    .autonomousDatabaseId(databaseId)
                    .build())
                break
            case DatabaseAction.RESTART:
                client.restartAutonomousDatabase(RestartAutonomousDatabaseRequest.builder()
                    .autonomousDatabaseId(databaseId)
                    .build())
                break
        }

        if (getResolvedWaitForCompletion().get()) {
            println("Waiting for Database to be ${state(action.state().name())}")
            client.waiters
                .forAutonomousDatabase(GetAutonomousDatabaseRequest.builder()
                    .autonomousDatabaseId(databaseId).build(),
                    action.state())
                .execute()
        }
    }
}

