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
import com.oracle.bmc.database.model.AutonomousDatabase
import com.oracle.bmc.database.requests.GetAutonomousDatabaseRequest
import groovy.transform.CompileStatic
import org.kordamp.gradle.plugin.oci.tasks.AbstractOCITask
import org.kordamp.gradle.plugin.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.plugin.oci.tasks.traits.DatabaseIdAwareTrait
import org.kordamp.jipsy.TypeProviderFor

import static org.kordamp.gradle.plugin.oci.tasks.printers.AutonomousDatabasePrinter.printAutonomousDatabase

/**
 * @author Andres Almiray
 * @since 0.5.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class GetAutonomousDatabaseTask extends AbstractOCITask implements DatabaseIdAwareTrait {
    static final String TASK_DESCRIPTION = 'Displays information for a specific Database.'

    @Override
    protected void doExecuteTask() {
        validateDatabaseId()

        DatabaseClient client = createDatabaseClient()

        AutonomousDatabase database = client.getAutonomousDatabase(GetAutonomousDatabaseRequest.builder()
            .autonomousDatabaseId(getResolvedDatabaseId().get())
            .build())
            .autonomousDatabase

        if (database) {
            println(database.displayName + ':')
            printAutonomousDatabase(this, database, 0)
        } else {
            println("Database with id ${getResolvedDatabaseId().get()} was not found")
        }
    }
}
