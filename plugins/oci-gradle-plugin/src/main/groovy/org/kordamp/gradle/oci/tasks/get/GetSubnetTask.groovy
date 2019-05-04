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
package org.kordamp.gradle.oci.tasks.get

import com.oracle.bmc.core.VirtualNetworkClient
import com.oracle.bmc.core.model.Subnet
import com.oracle.bmc.core.requests.GetSubnetRequest
import groovy.transform.CompileStatic
import org.kordamp.gradle.oci.tasks.AbstractOCITask
import org.kordamp.gradle.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.oci.tasks.traits.SubnetIdAwareTrait
import org.kordamp.jipsy.TypeProviderFor

import static org.kordamp.gradle.oci.tasks.printers.SubnetPrinter.printSubnet

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class GetSubnetTask extends AbstractOCITask implements SubnetIdAwareTrait {
    static final String TASK_DESCRIPTION = 'Displays information for an specific Subnet.'

    @Override
    protected void doExecuteTask() {
        validateSubnetId()

        VirtualNetworkClient client = createVirtualNetworkClient()

        Subnet subnet = client.getSubnet(GetSubnetRequest.builder()
            .subnetId(getSubnetId())
            .build())
            .subnet

        if (subnet) {
            println(subnet.displayName + ':')
            printSubnet(this, subnet, 0)
        } else {
            println("Subnet with id ${getSubnetId()} was not found")
        }
    }
}
