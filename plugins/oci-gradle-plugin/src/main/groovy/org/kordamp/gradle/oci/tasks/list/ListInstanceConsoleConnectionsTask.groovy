/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019 Andres Almiray.
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
package org.kordamp.gradle.oci.tasks.list

import com.oracle.bmc.auth.AuthenticationDetailsProvider
import com.oracle.bmc.core.ComputeClient
import com.oracle.bmc.core.model.InstanceConsoleConnection
import com.oracle.bmc.core.requests.ListInstanceConsoleConnectionsRequest
import com.oracle.bmc.core.responses.ListInstanceConsoleConnectionsResponse
import groovy.transform.CompileStatic
import org.gradle.api.tasks.TaskAction
import org.kordamp.gradle.AnsiConsole
import org.kordamp.gradle.oci.tasks.AbstractOCITask
import org.kordamp.gradle.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.oci.tasks.traits.CompartmentAwareTrait
import org.kordamp.gradle.oci.tasks.traits.InstanceIdAwareTrait
import org.kordamp.jipsy.TypeProviderFor

import static org.kordamp.gradle.oci.tasks.printers.InstanceConsoleConnectionPrinter.printInstanceConsoleConnection

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class ListInstanceConsoleConnectionsTask extends AbstractOCITask implements CompartmentAwareTrait, InstanceIdAwareTrait {
    static final String DESCRIPTION = 'Lists available console connections on an instance.'

    @TaskAction
    void executeTask() {
        validateCompartmentId()
        validateInstanceId()

        AuthenticationDetailsProvider provider = resolveAuthenticationDetailsProvider()
        ComputeClient client = ComputeClient.builder().build(provider)
        ListInstanceConsoleConnectionsResponse response = client.listInstanceConsoleConnections(ListInstanceConsoleConnectionsRequest.builder()
            .compartmentId(compartmentId)
            .instanceId(instanceId)
            .build())
        client.close()

        AnsiConsole console = new AnsiConsole(project)
        println('Total InstanceConsoleConnections: ' + console.cyan(response.items.size().toString()))
        println(' ')
        int index = 0
        for (InstanceConsoleConnection connection : response.items) {
            println("Instance Console Connection ${index.toString().padLeft(2, '0')}:")
            printInstanceConsoleConnection(this, connection, 0)
            index++
        }
    }
}
