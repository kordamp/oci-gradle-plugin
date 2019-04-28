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
package org.kordamp.gradle.oci.tasks.create

import com.oracle.bmc.core.ComputeClient
import com.oracle.bmc.core.model.CreateInstanceConsoleConnectionDetails
import com.oracle.bmc.core.model.InstanceConsoleConnection
import com.oracle.bmc.core.requests.CreateInstanceConsoleConnectionRequest
import com.oracle.bmc.core.requests.GetInstanceConsoleConnectionRequest
import groovy.transform.CompileStatic
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskAction
import org.kordamp.gradle.oci.tasks.AbstractOCITask
import org.kordamp.gradle.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.oci.tasks.traits.CompartmentIdAwareTrait
import org.kordamp.gradle.oci.tasks.traits.InstanceIdAwareTrait
import org.kordamp.gradle.oci.tasks.traits.PublicKeyFileAwareTrait
import org.kordamp.gradle.oci.tasks.traits.VerboseAwareTrait
import org.kordamp.gradle.oci.tasks.traits.WaitForCompletionAwareTrait
import org.kordamp.jipsy.TypeProviderFor

import static org.kordamp.gradle.oci.tasks.printers.InstanceConsoleConnectionPrinter.printInstanceConsoleConnection

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class CreateInstanceConsoleConnectionTask extends AbstractOCITask implements CompartmentIdAwareTrait,
    InstanceIdAwareTrait,
    PublicKeyFileAwareTrait,
    WaitForCompletionAwareTrait,
    VerboseAwareTrait {
    static final String TASK_DESCRIPTION = 'Creates an InstanceConsoleConnection.'

    private final Property<String> createdConnectionId = project.objects.property(String)

    String getCreatedInstanceConsoleConnectionId() {
        return createdConnectionId.orNull
    }

    @TaskAction
    void executeTask() {
        validateCompartmentId()
        validateInstanceId()
        validatePublicKeyFile()

        ComputeClient client = createComputeClient()

        InstanceConsoleConnection connection = createInstanceConsoleConnection(this,
            client,
            getInstanceId(),
            getPublicKeyFile().text,
            isWaitForCompletion(),
            isVerbose())
        createdConnectionId.set(connection.id)

        client.close()
    }

    static InstanceConsoleConnection createInstanceConsoleConnection(OCITask owner,
                                                                     ComputeClient client,
                                                                     String instanceId,
                                                                     String publicKeyFile,
                                                                     boolean waitForCompletion,
                                                                     boolean verbose) {
        println('Provisioning InstanceConsoleConnection. This may take a while.')
        CreateInstanceConsoleConnectionDetails details = CreateInstanceConsoleConnectionDetails.builder()
            .publicKey(publicKeyFile)
            .instanceId(instanceId)
            .build()

        InstanceConsoleConnection connection = client.createInstanceConsoleConnection(CreateInstanceConsoleConnectionRequest.builder()
            .createInstanceConsoleConnectionDetails(details)
            .build())
            .instanceConsoleConnection

        if (waitForCompletion) {
            println("Waiting for InstanceConsoleConnection to be ${owner.state('Active')}")
            client.waiters.forInstanceConsoleConnection(GetInstanceConsoleConnectionRequest.builder()
                .instanceConsoleConnectionId(connection.id)
                .build(),
                InstanceConsoleConnection.LifecycleState.Active)
                .execute()
        }

        println("InstanceConsoleConnection has been provisioned. id = ${owner.console.yellow(connection.id)}")
        if (verbose) printInstanceConsoleConnection(owner, connection, 0)
        connection
    }
}
