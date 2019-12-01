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

import com.oracle.bmc.core.VirtualNetworkClient
import com.oracle.bmc.core.model.Subnet
import com.oracle.bmc.core.requests.DeleteSubnetRequest
import com.oracle.bmc.core.requests.GetSubnetRequest
import com.oracle.bmc.core.requests.ListSubnetsRequest
import groovy.transform.CompileStatic
import org.kordamp.gradle.plugin.oci.tasks.AbstractOCITask
import org.kordamp.gradle.plugin.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.plugin.oci.tasks.traits.CompartmentIdAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.OptionalSubnetIdAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.OptionalSubnetNameAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.VcnIdAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.WaitForCompletionAwareTrait
import org.kordamp.jipsy.TypeProviderFor

import static org.kordamp.gradle.StringUtils.isBlank
import static org.kordamp.gradle.StringUtils.isNotBlank

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class DeleteSubnetTask extends AbstractOCITask implements CompartmentIdAwareTrait,
    VcnIdAwareTrait,
    OptionalSubnetIdAwareTrait,
    OptionalSubnetNameAwareTrait,
    WaitForCompletionAwareTrait {
    static final String TASK_DESCRIPTION = 'Deletes a Subnet.'

    @Override
    protected void doExecuteTask() {
        validateSubnetId()

        if (isBlank(getSubnetId().orNull) && isBlank(getSubnetName().orNull)) {
            throw new IllegalStateException("Missing value for either 'subnetId' or 'subnetName' in $path")
        }

        VirtualNetworkClient client = createVirtualNetworkClient()

        // TODO: check if subnet exists
        // TODO: check is subnet is in a 'deletable' state

        if (isNotBlank(getSubnetId().orNull)) {
            Subnet subnet = client.getSubnet(GetSubnetRequest.builder()
                .subnetId(getSubnetId().get())
                .build())
                .subnet

            if (subnet) {
                setSubnetName(subnet.displayName)
                deleteSubnet(client, subnet)
            }
        } else {
            validateCompartmentId()
            validateVcnId()

            client.listSubnets(ListSubnetsRequest.builder()
                .compartmentId(getCompartmentId().get())
                .vcnId(getVcnId().get())
                .displayName(getSubnetName().get())
                .build())
                .items.each { subnet ->
                setSubnetId(subnet.id)
                deleteSubnet(client, subnet)
            }
        }
    }

    private void deleteSubnet(VirtualNetworkClient client, Subnet subnet) {
        println("Deleting Subnet '${subnet.displayName}' with id ${subnet.id}")

        client.deleteSubnet(DeleteSubnetRequest.builder()
            .subnetId(subnet.id)
            .build())

        if (isWaitForCompletion().get()) {
            println("Waiting for Subnet to be ${state('Terminated')}")
            client.waiters
                .forSubnet(GetSubnetRequest.builder().subnetId(subnet.id).build(),
                    Subnet.LifecycleState.Terminated)
                .execute()
        }
    }
}
