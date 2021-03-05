/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019-2021 Andres Almiray.
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

import com.oracle.bmc.apigateway.GatewayClient
import com.oracle.bmc.apigateway.model.GatewaySummary
import com.oracle.bmc.apigateway.requests.ListGatewaysRequest
import groovy.transform.CompileStatic
import org.kordamp.gradle.plugin.oci.tasks.AbstractOCITask
import org.kordamp.gradle.plugin.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.plugin.oci.tasks.traits.CompartmentIdAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.VerboseAwareTrait
import org.kordamp.jipsy.TypeProviderFor

import static org.kordamp.gradle.plugin.oci.tasks.printers.GatewayPrinter.printGateway

/**
 * @author Andres Almiray
 * @since 0.5.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class ListGatewaysTask extends AbstractOCITask implements CompartmentIdAwareTrait, VerboseAwareTrait {
    static final String TASK_DESCRIPTION = 'Lists available gateways.'

    @Override
    protected void doExecuteTask() {
        validateCompartmentId()

        GatewayClient client = createGatewayClient()

        List<GatewaySummary> items = client.listGateways(ListGatewaysRequest.builder()
            .compartmentId(getResolvedCompartmentId().get())
            .build())
            .gatewayCollection
            .items

        println('Total gateways: ' + console.cyan(items.size().toString()))
        println(' ')
        for (GatewaySummary gateway : items) {
            println(gateway.displayName + (getResolvedVerbose().get() ? ':' : ''))
            if (getResolvedVerbose().get()) {
                printGateway(this, gateway, 0)
            }
        }
    }
}
