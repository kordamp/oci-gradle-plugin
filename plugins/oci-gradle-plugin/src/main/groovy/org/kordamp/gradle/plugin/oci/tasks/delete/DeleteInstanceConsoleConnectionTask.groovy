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
package org.kordamp.gradle.plugin.oci.tasks.delete

import com.oracle.bmc.core.ComputeClient
import com.oracle.bmc.core.model.InstanceConsoleConnection
import com.oracle.bmc.core.requests.DeleteInstanceConsoleConnectionRequest
import com.oracle.bmc.core.requests.GetInstanceConsoleConnectionRequest
import groovy.transform.CompileStatic
import org.kordamp.gradle.plugin.oci.tasks.AbstractOCITask
import org.kordamp.gradle.plugin.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.plugin.oci.tasks.traits.InstanceConsoleConnectionIdAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.WaitForCompletionAwareTrait
import org.kordamp.jipsy.TypeProviderFor

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class DeleteInstanceConsoleConnectionTask extends AbstractOCITask implements InstanceConsoleConnectionIdAwareTrait,
    WaitForCompletionAwareTrait {
    static final String TASK_DESCRIPTION = 'Deletes an InstanceConsoleConnection.'

    @Override
    protected void doExecuteTask() {
        validateInstanceConsoleConnectionId()

        ComputeClient client = createComputeClient()

        // TODO: check if connection exists
        // TODO: check is connection is in a 'deletable' state

        client.deleteInstanceConsoleConnection(DeleteInstanceConsoleConnectionRequest.builder()
            .instanceConsoleConnectionId(getInstanceConsoleConnectionId().get())
            .build())

        if (isWaitForCompletion().get()) {
            println("Waiting for InstanceConsoleConnection to be ${state('Deleted')}")
            client.waiters
                .forInstanceConsoleConnection(GetInstanceConsoleConnectionRequest.builder()
                    .instanceConsoleConnectionId(getInstanceConsoleConnectionId().get()).build(),
                    InstanceConsoleConnection.LifecycleState.Deleted)
                .execute()
        }
    }
}
