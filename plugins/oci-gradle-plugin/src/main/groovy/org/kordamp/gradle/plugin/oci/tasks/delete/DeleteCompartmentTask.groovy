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
package org.kordamp.gradle.plugin.oci.tasks.delete

import com.oracle.bmc.identity.IdentityClient
import com.oracle.bmc.identity.model.Compartment
import com.oracle.bmc.identity.requests.DeleteCompartmentRequest
import com.oracle.bmc.identity.requests.GetCompartmentRequest
import groovy.transform.CompileStatic
import org.kordamp.gradle.plugin.oci.tasks.AbstractOCITask
import org.kordamp.gradle.plugin.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.plugin.oci.tasks.traits.CompartmentIdAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.WaitForCompletionAwareTrait
import org.kordamp.jipsy.TypeProviderFor

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class DeleteCompartmentTask extends AbstractOCITask implements CompartmentIdAwareTrait,
    WaitForCompletionAwareTrait {
    static final String TASK_DESCRIPTION = 'Deletes a Compartment.'

    @Override
    protected void doExecuteTask() {
        validateCompartmentId()

        IdentityClient client = createIdentityClient()

        // TODO: check if compartment exists
        // TODO: check is compartment is in a 'deletable' state
        // TODO: check if compartment is empty

        String resolvedCompartmenId = getResolvedCompartmentId().get()
        Compartment compartment = client.getCompartment(GetCompartmentRequest.builder()
            .compartmentId(resolvedCompartmenId)
            .build())
            .compartment

        println("Deleting Compartment ${compartment.name} with id ${resolvedCompartmenId}")

        client.deleteCompartment(DeleteCompartmentRequest.builder()
            .compartmentId(resolvedCompartmenId)
            .build())

        if (getResolvedWaitForCompletion().get()) {
            println("Waiting for Compartment to be ${state('Deleted')}")
            client.waiters
                .forCompartment(GetCompartmentRequest.builder()
                    .compartmentId(resolvedCompartmenId)
                    .build(),
                    Compartment.LifecycleState.Deleted)
                .execute()
        }
    }
}
