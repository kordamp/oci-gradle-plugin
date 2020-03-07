/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019-2020 Andres Almiray.
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
package org.kordamp.gradle.plugin.oci.tasks.list

import com.oracle.bmc.core.ComputeClient
import com.oracle.bmc.core.model.InstanceConsoleConnection
import com.oracle.bmc.core.requests.ListInstanceConsoleConnectionsRequest
import com.oracle.bmc.core.responses.ListInstanceConsoleConnectionsResponse
import groovy.transform.CompileStatic
import org.kordamp.gradle.plugin.oci.tasks.AbstractOCITask
import org.kordamp.gradle.plugin.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.plugin.oci.tasks.traits.CompartmentIdAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.InstanceIdAwareTrait
import org.kordamp.jipsy.TypeProviderFor

import static org.kordamp.gradle.plugin.oci.tasks.printers.InstanceConsoleConnectionPrinter.printInstanceConsoleConnection

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class ListInstanceConsoleConnectionsTask extends AbstractOCITask implements CompartmentIdAwareTrait, InstanceIdAwareTrait {
    static final String TASK_DESCRIPTION = 'Lists available InstanceConsoleConnection on an Instance.'

    @Override
    protected void doExecuteTask() {
        validateCompartmentId()
        validateInstanceId()

        ComputeClient client = createComputeClient()
        ListInstanceConsoleConnectionsResponse response = client.listInstanceConsoleConnections(ListInstanceConsoleConnectionsRequest.builder()
            .compartmentId(getResolvedCompartmentId().get())
            .instanceId(getResolvedInstanceId().get())
            .build())

        println('Total InstanceConsoleConnections: ' + console.cyan(response.items.size().toString()))
        println(' ')
        int index = 0
        for (InstanceConsoleConnection connection : response.items) {
            println("InstanceConsoleConnection ${index.toString().padLeft(2, '0')}:")
            printInstanceConsoleConnection(this, connection, 0)
            index++
        }
    }
}
