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
package org.kordamp.gradle.plugin.oci.tasks.autonomous

import com.oracle.bmc.database.DatabaseClient
import com.oracle.bmc.database.model.GenerateAutonomousDatabaseWalletDetails
import com.oracle.bmc.database.requests.GenerateAutonomousDatabaseWalletRequest
import com.oracle.bmc.database.requests.ListAutonomousDatabasesRequest
import com.oracle.bmc.database.responses.GenerateAutonomousDatabaseWalletResponse
import groovy.transform.CompileStatic
import org.apache.commons.io.FileUtils
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.OutputFile
import org.kordamp.gradle.plugin.oci.tasks.AbstractOCITask
import org.kordamp.gradle.plugin.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.plugin.oci.tasks.traits.CompartmentIdAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.OptionalDatabaseIdAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.OptionalDatabaseNameAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.WaitForCompletionAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.WalletPasswordAwareTrait
import org.kordamp.jipsy.annotations.TypeProviderFor

import static org.kordamp.gradle.util.StringUtils.isBlank
import static org.kordamp.gradle.util.StringUtils.isNotBlank

/**
 * @author Andres Almiray
 * @since 0.5.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class GenerateAutonomousDatabaseWalletTask extends AbstractOCITask implements CompartmentIdAwareTrait,
    OptionalDatabaseIdAwareTrait,
    OptionalDatabaseNameAwareTrait,
    WalletPasswordAwareTrait,
    WaitForCompletionAwareTrait {
    static final String TASK_DESCRIPTION = 'Generates and downloads a Wallet for a Database.'

    private final Provider<RegularFile> output

    GenerateAutonomousDatabaseWalletTask() {
        output = project.layout.buildDirectory.file('oci/autonomous/wallet.zip')
    }

    @OutputFile
    Provider<RegularFile> getOutput() {
        this.@output
    }

    @Override
    protected void doExecuteTask() {
        validateWalletPassword()
        validateDatabaseId()

        if (isBlank(getResolvedDatabaseId().orNull) && isBlank(getResolvedDatabaseName().orNull)) {
            throw new IllegalStateException("Missing value for either 'databaseId' or 'databaseName' in $path")
        }

        DatabaseClient client = createDatabaseClient()

        if (isNotBlank(getResolvedDatabaseId().orNull)) {
            generateAutonomousDatabaseWallet(client,
                getResolvedDatabaseId().get(),
                getResolvedWalletPassword().get(),
                output.get().asFile)
        } else {
            validateCompartmentId()

            client.listAutonomousDatabases(ListAutonomousDatabasesRequest.builder()
                .compartmentId(getResolvedCompartmentId().get())
                .displayName(getResolvedDatabaseName().get())
                .build())
                .items.each { database ->
                setDatabaseId(database.id)
                generateAutonomousDatabaseWallet(client,
                    database.id,
                    getResolvedWalletPassword().get(),
                    output.get().asFile)
            }
        }
    }

    static void generateAutonomousDatabaseWallet(DatabaseClient client,
                                                 String databaseId,
                                                 String password,
                                                 File output) {
        GenerateAutonomousDatabaseWalletResponse response = client.generateAutonomousDatabaseWallet(GenerateAutonomousDatabaseWalletRequest.builder()
            .autonomousDatabaseId(databaseId)
            .generateAutonomousDatabaseWalletDetails(GenerateAutonomousDatabaseWalletDetails.builder()
                .password(password)
                .build())
            .build())

        FileUtils.copyInputStreamToFile(response.inputStream, output)
        println("Wallet for database with id ${databaseId} has been saved to ${output.absolutePath}")
    }
}

