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
package org.kordamp.gradle.plugin.oci.tasks.get

import com.oracle.bmc.apigateway.GatewayClient
import com.oracle.bmc.apigateway.model.GatewaySummary
import com.oracle.bmc.apigateway.requests.ListGatewaysRequest
import groovy.transform.CompileStatic
import org.kordamp.gradle.plugin.oci.tasks.AbstractOCITask
import org.kordamp.gradle.plugin.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.plugin.oci.tasks.traits.CompartmentIdAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.GatewayIdAwareTrait
import org.kordamp.jipsy.annotations.TypeProviderFor

import static org.kordamp.gradle.plugin.oci.tasks.printers.GatewayPrinter.printGateway

/**
 * @author Andres Almiray
 * @since 0.5.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class GetGatewayTask extends AbstractOCITask implements CompartmentIdAwareTrait,
    GatewayIdAwareTrait {
    static final String TASK_DESCRIPTION = 'Displays information for a specific gateway.'

    @Override
    protected void doExecuteTask() {
        validateCompartmentId()
        validateGatewayId()

        GatewayClient client = createGatewayClient()

        List<GatewaySummary> gateways = client.listGateways(ListGatewaysRequest.builder()
            .compartmentId(getResolvedCompartmentId().get())
            .build())
            .gatewayCollection
            .items

        GatewaySummary gatewaySummary = gateways.find { GatewaySummary ig -> ig.id == getResolvedGatewayId().get() }

        if (gatewaySummary) {
            println(gatewaySummary.displayName + ':')
            printGateway(this, gatewaySummary, 0)
        } else {
            println("Gateway with id ${getResolvedGatewayId().get()} was not found")
        }
    }
}
