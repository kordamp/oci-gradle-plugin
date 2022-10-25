/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019-2022 Andres Almiray.
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
package org.kordamp.gradle.plugin.oci.tasks.create

import com.oracle.bmc.apigateway.GatewayClient
import com.oracle.bmc.apigateway.model.CreateGatewayDetails
import com.oracle.bmc.apigateway.model.Gateway
import com.oracle.bmc.apigateway.model.Gateway.EndpointType
import com.oracle.bmc.apigateway.model.GatewaySummary
import com.oracle.bmc.apigateway.requests.CreateGatewayRequest
import com.oracle.bmc.apigateway.requests.GetGatewayRequest
import com.oracle.bmc.apigateway.requests.ListGatewaysRequest
import groovy.transform.CompileStatic
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.options.Option
import org.gradle.api.tasks.options.OptionValues
import org.kordamp.gradle.plugin.oci.tasks.AbstractOCITask
import org.kordamp.gradle.plugin.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.plugin.oci.tasks.traits.CompartmentIdAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.GatewayNameAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.SubnetIdAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.VerboseAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.WaitForCompletionAwareTrait
import org.kordamp.jipsy.annotations.TypeProviderFor

import static org.kordamp.gradle.PropertyUtils.resolveValue
import static org.kordamp.gradle.plugin.oci.tasks.printers.GatewayPrinter.printGateway
import static org.kordamp.gradle.util.StringUtils.isNotBlank

/**
 * @author Andres Almiray
 * @since 0.5.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class CreateGatewayTask extends AbstractOCITask implements CompartmentIdAwareTrait,
    SubnetIdAwareTrait,
    GatewayNameAwareTrait,
    WaitForCompletionAwareTrait,
    VerboseAwareTrait {
    static final String TASK_DESCRIPTION = 'Creates a Gateway.'
    private final Property<String> createdGatewayId = project.objects.property(String)

    @Internal
    Property<String> getCreatedGatewayId() {
        this.@createdGatewayId
    }

    @Internal
    final Property<EndpointType> endpointType = project.objects.property(EndpointType)

    @Option(option = 'endpoint-type', description = 'The endpointType to use (OPTIONAL).')
    void setEndpointType(EndpointType endpointType) {
        getEndpointType().set(endpointType)
    }

    @Input
    Provider<EndpointType> getResolvedEndpointType() {
        project.providers.provider {
            String value = resolveValue('OCI_GATEWAY_ENDPOINT_TYPE', 'oci.gateway.endpoint.type', this, project.name)
            isNotBlank(value) ? EndpointType.valueOf(value) : endpointType.getOrElse(EndpointType.Public)
        }
    }

    @OptionValues("endpoint-type")
    List<EndpointType> getAvailableEndpointTypes() {
        return new ArrayList<EndpointType>(Arrays.asList(EndpointType.values()))
    }

    @Override
    void doExecuteTask() {
        validateCompartmentId()
        validateSubnetId()
        validateGatewayName()

        GatewayClient client = createGatewayClient()

        GatewaySummary gateway = maybeCreateGateway(this,
            client,
            getResolvedCompartmentId().get(),
            getResolvedSubnetId().get(),
            getResolvedGatewayName().get(),
            getResolvedEndpointType().get(),
            getResolvedWaitForCompletion().get(),
            getResolvedVerbose().get())
        createdGatewayId.set(gateway.id)
    }

    static GatewaySummary maybeCreateGateway(OCITask owner,
                                             GatewayClient client,
                                             String compartmentId,
                                             String subnetId,
                                             String gatewayName,
                                             EndpointType endpointType,
                                             boolean waitForCompletion,
                                             boolean verbose) {
        // 1. Check if it exists
        List<GatewaySummary> gateways = client.listGateways(ListGatewaysRequest.builder()
            .compartmentId(compartmentId)
            .build())
            .gatewayCollection
            .items

        GatewaySummary gatewaySummary = gateways.find { GatewaySummary ig -> ig.displayName == gatewayName }

        if (gatewaySummary) {
            println("Gateway '${gatewayName}' already exists. id = ${gatewaySummary.id}")
            if (verbose) printGateway(owner, gatewaySummary, 0)
            return gatewaySummary
        }

        if (!gateways.empty) {
            gatewaySummary = gateways[0]
            println("Gateway '${gatewaySummary.displayName}' exists. id = ${gatewaySummary.id}")
            if (verbose) printGateway(owner, gatewaySummary, 0)
            return gatewaySummary
        }

        Gateway gateway = client.createGateway(CreateGatewayRequest.builder()
            .createGatewayDetails(CreateGatewayDetails.builder()
                .compartmentId(compartmentId)
                .subnetId(subnetId)
                .displayName(gatewayName)
                .endpointType(endpointType)
                .build())
            .build())
            .gateway

        if (waitForCompletion) {
            println("Waiting for Gateway to be ${owner.state('Available')}")
            client.waiters.forGateway(GetGatewayRequest.builder()
                .gatewayId(gateway.id)
                .build(),
                Gateway.LifecycleState.Active)
                .execute()
        }

        gateways = client.listGateways(ListGatewaysRequest.builder()
            .compartmentId(compartmentId)
            .displayName(gatewayName)
            .build())
            .gatewayCollection
            .items

        println("Gateway '${gatewayName}' has been provisioned. id = ${owner.console.yellow(gateway.id)}")
        if (verbose) printGateway(owner, gateways[0], 0)
        gateways[0]
    }
}
