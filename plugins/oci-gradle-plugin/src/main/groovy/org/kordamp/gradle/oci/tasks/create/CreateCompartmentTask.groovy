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

import com.oracle.bmc.identity.IdentityClient
import com.oracle.bmc.identity.model.Compartment
import com.oracle.bmc.identity.model.CreateCompartmentDetails
import com.oracle.bmc.identity.requests.CreateCompartmentRequest
import com.oracle.bmc.identity.requests.GetCompartmentRequest
import com.oracle.bmc.identity.requests.ListCompartmentsRequest
import groovy.transform.CompileStatic
import org.gradle.api.provider.Property
import org.kordamp.gradle.oci.tasks.AbstractOCITask
import org.kordamp.gradle.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.oci.tasks.traits.CompartmentDescriptionAwareTrait
import org.kordamp.gradle.oci.tasks.traits.CompartmentIdAwareTrait
import org.kordamp.gradle.oci.tasks.traits.CompartmentNameAwareTrait
import org.kordamp.gradle.oci.tasks.traits.VerboseAwareTrait
import org.kordamp.gradle.oci.tasks.traits.WaitForCompletionAwareTrait
import org.kordamp.jipsy.TypeProviderFor

import static org.kordamp.gradle.oci.tasks.printers.CompartmentPrinter.printCompartment

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class CreateCompartmentTask extends AbstractOCITask implements CompartmentIdAwareTrait,
    CompartmentNameAwareTrait,
    CompartmentDescriptionAwareTrait,
    WaitForCompletionAwareTrait,
    VerboseAwareTrait {
    static final String TASK_DESCRIPTION = 'Creates a Compartment.'

    private final Property<String> createdCompartmentId = project.objects.property(String)

    String getCreatedCompartmentId() {
        return createdCompartmentId.orNull
    }

    @Override
    void doExecuteTask() {
        validateCompartmentId()
        validateCompartmentName()
        validateCompartmentDescription()

        IdentityClient client = createIdentityClient()

        Compartment compartment = maybeCreateCompartment(this,
            client,
            getCompartmentId(),
            getCompartmentName(),
            getCompartmentDescription(),
            isWaitForCompletion(),
            isVerbose())
        createdCompartmentId.set(compartment.id)
    }

    static Compartment maybeCreateCompartment(OCITask owner,
                                              IdentityClient client,
                                              String parentCompartmentId,
                                              String compartmentName,
                                              String compartmentDescription,
                                              boolean waitForCompletion,
                                              boolean verbose) {
        // 1. Check if it exists
        List<Compartment> compartments = client.listCompartments(ListCompartmentsRequest.builder()
            .compartmentId(parentCompartmentId)
            .build()).items
        Compartment compartment = compartments.find { Compartment c -> c.name == compartmentName }

        if (compartment) {
            println("Compartment '${compartmentName}' already exists. id = ${owner.console.yellow(compartment.id)}")
            if (verbose) printCompartment(owner, compartment, 0)
            return compartment
        }
        // 2. Create
        println('Provisioning Compartment. This may take a while.')
        compartment = client.createCompartment(CreateCompartmentRequest.builder()
            .createCompartmentDetails(CreateCompartmentDetails.builder()
                .compartmentId(parentCompartmentId)
                .name(compartmentName)
                .description(compartmentDescription)
                .build())
            .build())
            .compartment

        if (waitForCompletion) {
            println("Waiting for Compartment to be ${owner.state('Active')}")
            client.waiters
                .forCompartment(GetCompartmentRequest.builder()
                    .compartmentId(compartment.id)
                    .build(),
                    Compartment.LifecycleState.Active)
                .execute()
        }

        println("Compartment '${compartmentName}' has been provisioned. id = ${owner.console.yellow(compartment.id)}")
        if (verbose) printCompartment(owner, compartment, 0)
        compartment
    }
}
