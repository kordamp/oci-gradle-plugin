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
package org.kordamp.gradle.plugin.oci.tasks.list

import com.oracle.bmc.core.ComputeClient
import com.oracle.bmc.core.model.Instance
import com.oracle.bmc.core.requests.ListInstancesRequest
import com.oracle.bmc.core.responses.ListInstancesResponse
import groovy.transform.CompileStatic
import org.kordamp.gradle.plugin.oci.tasks.AbstractOCITask
import org.kordamp.gradle.plugin.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.plugin.oci.tasks.traits.AvailabilityDomainAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.CompartmentIdAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.VerboseAwareTrait
import org.kordamp.jipsy.TypeProviderFor

import static org.kordamp.gradle.plugin.oci.tasks.printers.InstancePrinter.printInstance

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class ListInstancesTask extends AbstractOCITask implements CompartmentIdAwareTrait, AvailabilityDomainAwareTrait, VerboseAwareTrait {
    static final String TASK_DESCRIPTION = 'Lists available Instances.'

    @Override
    protected void doExecuteTask() {
        validateCompartmentId()

        ComputeClient client = createComputeClient()
        ListInstancesResponse response = client.listInstances(ListInstancesRequest.builder()
            .compartmentId(getCompartmentId().get())
            .availabilityDomain(getAvailabilityDomain().get())
            .build())

        println('Total Instances: ' + console.cyan(response.items.size().toString()))
        println(' ')
        for (Instance instance : response.items) {
            println(instance.displayName + (isVerbose().get() ? ':' : ''))
            if (isVerbose().get()) {
                printInstance(this, instance, 0)
            }
        }
    }
}
