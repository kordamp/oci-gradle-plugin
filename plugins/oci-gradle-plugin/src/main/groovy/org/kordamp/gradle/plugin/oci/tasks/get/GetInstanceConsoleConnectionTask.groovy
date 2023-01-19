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
package org.kordamp.gradle.plugin.oci.tasks.get

import com.oracle.bmc.core.ComputeClient
import com.oracle.bmc.core.model.InstanceConsoleConnection
import com.oracle.bmc.core.requests.GetInstanceConsoleConnectionRequest
import groovy.transform.CompileStatic
import org.kordamp.gradle.plugin.oci.tasks.AbstractOCITask
import org.kordamp.gradle.plugin.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.plugin.oci.tasks.traits.InstanceConsoleConnectionIdAwareTrait
import org.kordamp.jipsy.annotations.TypeProviderFor

import static org.kordamp.gradle.plugin.oci.tasks.printers.InstanceConsoleConnectionPrinter.printInstanceConsoleConnection

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class GetInstanceConsoleConnectionTask extends AbstractOCITask implements InstanceConsoleConnectionIdAwareTrait {
    static final String TASK_DESCRIPTION = 'Displays information for a specific InstanceConsoleConnection.'

    @Override
    protected void doExecuteTask() {
        validateInstanceConsoleConnectionId()

        ComputeClient client = createComputeClient()

        InstanceConsoleConnection connection = client.getInstanceConsoleConnection(GetInstanceConsoleConnectionRequest.builder()
            .instanceConsoleConnectionId(getResolvedInstanceConsoleConnectionId().get())
            .build())
            .instanceConsoleConnection

        if (connection) {
            printInstanceConsoleConnection(this, connection, 0)
        } else {
            println("InstanceConsoleConnection with id ${getResolvedInstanceConsoleConnectionId().get()} was not found")
        }
    }
}
