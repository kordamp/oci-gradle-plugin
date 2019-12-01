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
import com.oracle.bmc.core.model.InternetGateway
import com.oracle.bmc.core.requests.ListInternetGatewaysRequest
import com.oracle.bmc.core.responses.ListInternetGatewaysResponse
import groovy.transform.CompileStatic
import org.kordamp.gradle.plugin.oci.tasks.AbstractOCITask
import org.kordamp.gradle.plugin.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.plugin.oci.tasks.traits.CompartmentIdAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.VcnIdAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.VerboseAwareTrait
import org.kordamp.jipsy.TypeProviderFor

import static org.kordamp.gradle.plugin.oci.tasks.printers.InternetGatewayPrinter.printInternetGateway

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class ListInternetGatewaysTask extends AbstractOCITask implements CompartmentIdAwareTrait, VcnIdAwareTrait, VerboseAwareTrait {
    static final String TASK_DESCRIPTION = 'Lists InternetGateways available on a Vcn.'

    @Override
    protected void doExecuteTask() {
        validateCompartmentId()
        validateVcnId()

        VirtualNetworkClient client = createVirtualNetworkClient()
        ListInternetGatewaysResponse response = client.listInternetGateways(ListInternetGatewaysRequest.builder()
            .compartmentId(getCompartmentId().get())
            .vcnId(getVcnId().get())
            .build())

        println('Total InternetGateways: ' + console.cyan(response.items.size().toString()))
        println(' ')
        for (InternetGateway internetGateway : response.items) {
            println(internetGateway.displayName + (isVerbose().get() ? ':' : ''))
            if (isVerbose().get()) {
                printInternetGateway(this, internetGateway, 0)
            }
        }
    }
}
