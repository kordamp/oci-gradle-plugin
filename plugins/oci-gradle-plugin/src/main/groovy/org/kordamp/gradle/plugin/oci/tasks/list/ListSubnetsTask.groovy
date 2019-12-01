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

import com.oracle.bmc.core.VirtualNetworkClient
import com.oracle.bmc.core.model.Subnet
import com.oracle.bmc.core.requests.ListSubnetsRequest
import com.oracle.bmc.core.responses.ListSubnetsResponse
import groovy.transform.CompileStatic
import org.kordamp.gradle.plugin.oci.tasks.AbstractOCITask
import org.kordamp.gradle.plugin.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.plugin.oci.tasks.traits.CompartmentIdAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.VcnIdAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.VerboseAwareTrait
import org.kordamp.jipsy.TypeProviderFor

import static org.kordamp.gradle.plugin.oci.tasks.printers.SubnetPrinter.printSubnet

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class ListSubnetsTask extends AbstractOCITask implements CompartmentIdAwareTrait, VcnIdAwareTrait, VerboseAwareTrait {
    static final String TASK_DESCRIPTION = 'Lists Subnets available on a Vcn.'

    @Override
    protected void doExecuteTask() {
        validateCompartmentId()
        validateVcnId()

        VirtualNetworkClient client = createVirtualNetworkClient()
        ListSubnetsResponse response = client.listSubnets(ListSubnetsRequest.builder()
            .compartmentId(getResolvedCompartmentId().get())
            .vcnId(getResolvedVcnId().get())
            .build())

        println('Total Subnets: ' + console.cyan(response.items.size().toString()))
        println(' ')
        for (Subnet subnet : response.items) {
            println(subnet.displayName + (getResolvedVerbose().get() ? ':' : ''))
            if (getResolvedVerbose().get()) {
                printSubnet(this, subnet, 0)
            }
        }
    }
}
