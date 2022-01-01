/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019-2022 Andres Almiray.
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
package org.kordamp.gradle.plugin.oci.tasks.create

import com.oracle.bmc.database.DatabaseClient
import com.oracle.bmc.database.model.AutonomousDatabase
import com.oracle.bmc.database.model.AutonomousDatabaseSummary
import com.oracle.bmc.database.model.CreateAutonomousDatabaseDetails
import com.oracle.bmc.database.requests.CreateAutonomousDatabaseRequest
import com.oracle.bmc.database.requests.GetAutonomousDatabaseRequest
import com.oracle.bmc.database.requests.ListAutonomousDatabasesRequest
import groovy.transform.CompileStatic
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.kordamp.gradle.plugin.oci.tasks.AbstractOCITask
import org.kordamp.gradle.plugin.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.plugin.oci.tasks.traits.AdminPasswordAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.CompartmentIdAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.DatabaseNameAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.OptionalCpuCoreCountAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.OptionalDataStorageSizeAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.VerboseAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.WaitForCompletionAwareTrait
import org.kordamp.jipsy.annotations.TypeProviderFor

import static org.kordamp.gradle.plugin.oci.tasks.printers.AutonomousDatabasePrinter.printAutonomousDatabase

/**
 * @author Andres Almiray
 * @since 0.5.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class CreateAutonomousDatabaseTask extends AbstractOCITask implements CompartmentIdAwareTrait,
    DatabaseNameAwareTrait,
    AdminPasswordAwareTrait,
    OptionalCpuCoreCountAwareTrait,
    OptionalDataStorageSizeAwareTrait,
    WaitForCompletionAwareTrait,
    VerboseAwareTrait {
    static final String TASK_DESCRIPTION = 'Creates an Autonomous Database.'

    private final Property<String> createdAutonomousDatabaseId = project.objects.property(String)
    private static final Random RANDOM = new Random()

    @Internal
    Property<String> getCreatedAutonomousDatabaseId() {
        this.@createdAutonomousDatabaseId
    }

    @Override
    void doExecuteTask() {
        validateCompartmentId()
        validateDatabaseName()
        validateAdminPassword()

        DatabaseClient client = createDatabaseClient()

        AutonomousDatabase database = maybeCreateAutonomousDatabase(this,
            client,
            getResolvedCompartmentId().get(),
            getResolvedDatabaseName().get(),
            getResolvedAdminPassword().get(),
            getResolvedCpuCoreCount().getOrElse(1),
            getResolvedDataStorageSize().getOrElse(1),
            getResolvedWaitForCompletion().get(),
            getResolvedVerbose().get())
        createdAutonomousDatabaseId.set(database.id)
    }

    static AutonomousDatabase maybeCreateAutonomousDatabase(OCITask owner,
                                                            DatabaseClient client,
                                                            String compartmentId,
                                                            String databaseName,
                                                            String adminPassword,
                                                            int cpuCoreCount,
                                                            int dataStorageSize,
                                                            boolean waitForCompletion,
                                                            boolean verbose) {
        // 1. Check if it exists
        List<AutonomousDatabaseSummary> databases = client.listAutonomousDatabases(ListAutonomousDatabasesRequest.builder()
            .compartmentId(compartmentId)
            .displayName(databaseName)
            .build())
            .items

        if (!databases.empty) {
            AutonomousDatabase database = client.getAutonomousDatabase(GetAutonomousDatabaseRequest.builder()
                .autonomousDatabaseId(databases[0].id)
                .build())
                .autonomousDatabase
            println("Autonomous Database '${databaseName}' already exists. id = ${owner.console.yellow(database.id)}")
            if (verbose) printAutonomousDatabase(owner, database, 0)
            return database
        }

        // 2. Create
        println('Provisioning Autonomous Database. This may take a while.')
        AutonomousDatabase database = client.createAutonomousDatabase(CreateAutonomousDatabaseRequest.builder()
            .createAutonomousDatabaseDetails(CreateAutonomousDatabaseDetails.builder()
                .compartmentId(compartmentId)
                .displayName(databaseName)
                .adminPassword(adminPassword)
                .cpuCoreCount(cpuCoreCount)
                .dataStorageSizeInTBs(dataStorageSize)
                .dbName(databaseName + RANDOM.nextInt(500))
                .dbWorkload(CreateAutonomousDatabaseDetails.DbWorkload.Oltp)
                .licenseModel(CreateAutonomousDatabaseDetails.LicenseModel.LicenseIncluded)
                .build())
            .build())
            .autonomousDatabase

        if (waitForCompletion) {
            println("Waiting for Autonomous Database to be ${owner.state('Available')}")
            client.waiters
                .forAutonomousDatabase(GetAutonomousDatabaseRequest.builder()
                    .autonomousDatabaseId(database.id)
                    .build(),
                    AutonomousDatabase.LifecycleState.Available)
                .execute()
        }

        println("Autonomous Database '${databaseName}' has been provisioned. id = ${owner.console.yellow(database.id)}")
        if (verbose) printAutonomousDatabase(owner, database, 0)
        database
    }
}
