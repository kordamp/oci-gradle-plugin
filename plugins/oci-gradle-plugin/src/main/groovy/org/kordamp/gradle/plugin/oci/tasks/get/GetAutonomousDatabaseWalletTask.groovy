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
package org.kordamp.gradle.plugin.oci.tasks.get

import com.oracle.bmc.database.DatabaseClient
import com.oracle.bmc.database.model.AutonomousDatabaseWallet
import com.oracle.bmc.database.requests.GetAutonomousDatabaseWalletRequest
import groovy.transform.CompileStatic
import org.kordamp.gradle.plugin.oci.tasks.AbstractOCITask
import org.kordamp.gradle.plugin.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.plugin.oci.tasks.traits.DatabaseIdAwareTrait
import org.kordamp.jipsy.TypeProviderFor

import static org.kordamp.gradle.plugin.oci.tasks.printers.AutonomousDatabaseWalletPrinter.printAutonomousDatabaseWallet

/**
 * @author Andres Almiray
 * @since 0.5.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class GetAutonomousDatabaseWalletTask extends AbstractOCITask implements DatabaseIdAwareTrait {
    static final String TASK_DESCRIPTION = 'Retrieves a Wallet.'

    @Override
    protected void doExecuteTask() {
        validateDatabaseId()

        DatabaseClient client = createDatabaseClient()

        AutonomousDatabaseWallet wallet = client.getAutonomousDatabaseWallet(GetAutonomousDatabaseWalletRequest.builder()
            .autonomousDatabaseId(getResolvedDatabaseId().get())
            .build())
            .autonomousDatabaseWallet

        if (wallet) {
            printAutonomousDatabaseWallet(this, wallet, 0)
        } else {
            println("Wallet for database with id ${getResolvedDatabaseId().get()} was not found")
        }
    }
}
