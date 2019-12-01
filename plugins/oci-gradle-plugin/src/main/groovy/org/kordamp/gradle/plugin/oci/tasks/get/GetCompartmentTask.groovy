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
package org.kordamp.gradle.plugin.oci.tasks.get

import com.oracle.bmc.identity.IdentityClient
import com.oracle.bmc.identity.model.Compartment
import com.oracle.bmc.identity.requests.GetCompartmentRequest
import groovy.transform.CompileStatic
import org.kordamp.gradle.plugin.oci.tasks.AbstractOCITask
import org.kordamp.gradle.plugin.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.plugin.oci.tasks.traits.CompartmentIdAwareTrait
import org.kordamp.jipsy.TypeProviderFor

import static org.kordamp.gradle.plugin.oci.tasks.printers.CompartmentPrinter.printCompartment

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class GetCompartmentTask extends AbstractOCITask implements CompartmentIdAwareTrait {
    static final String TASK_DESCRIPTION = 'Displays information for an specific Compartment.'

    @Override
    protected void doExecuteTask() {
        validateCompartmentId()

        IdentityClient client = createIdentityClient()
        Compartment compartment = client.getCompartment(GetCompartmentRequest.builder()
            .compartmentId(getResolvedCompartmentId().get())
            .build())
            .compartment

        println(compartment.name + ':')
        printCompartment(this, compartment, 0)
    }
}
