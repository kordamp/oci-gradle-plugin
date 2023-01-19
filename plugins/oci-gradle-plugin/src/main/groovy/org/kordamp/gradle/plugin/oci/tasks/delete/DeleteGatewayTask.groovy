/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019-2023 Andres Almiray.
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

import com.oracle.bmc.apigateway.GatewayClient
import com.oracle.bmc.apigateway.model.Gateway
import com.oracle.bmc.apigateway.model.GatewaySummary
import com.oracle.bmc.apigateway.requests.DeleteGatewayRequest
import com.oracle.bmc.apigateway.requests.GetGatewayRequest
import com.oracle.bmc.apigateway.requests.ListGatewaysRequest
import groovy.transform.CompileStatic
import org.kordamp.gradle.plugin.oci.tasks.AbstractOCITask
import org.kordamp.gradle.plugin.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.plugin.oci.tasks.traits.CompartmentIdAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.OptionalGatewayIdAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.OptionalGatewayNameAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.WaitForCompletionAwareTrait
import org.kordamp.jipsy.annotations.TypeProviderFor

import static org.kordamp.gradle.util.StringUtils.isBlank
import static org.kordamp.gradle.util.StringUtils.isNotBlank

/**
 * @author Andres Almiray
 * @since 0.5.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class DeleteGatewayTask extends AbstractOCITask implements CompartmentIdAwareTrait,
    OptionalGatewayIdAwareTrait,
    OptionalGatewayNameAwareTrait,
    WaitForCompletionAwareTrait {
    static final String TASK_DESCRIPTION = 'Deletes a Gateway.'

    @Override
    protected void doExecuteTask() {
        validateGatewayId()

        if (isBlank(getResolvedGatewayId().orNull) && isBlank(getResolvedGatewayName().orNull)) {
            throw new IllegalStateException("Missing value for either 'gatewayId' or 'gatewayName' in $path")
        }

        GatewayClient client = createGatewayClient()

        // TODO: check if gateway exists
        // TODO: check is gateway is in a 'deletable' state

        if (isNotBlank(getResolvedGatewayId().orNull)) {
            Gateway gateway = client.getGateway(GetGatewayRequest.builder()
                .gatewayId(getResolvedGatewayId().get())
                .build())
                .gateway

            if (gateway) {
                setGatewayName(gateway.displayName)
                deleteGateway(client, gateway)
            }
        } else {
            validateCompartmentId()

            client.listGateways(ListGatewaysRequest.builder()
                .compartmentId(getResolvedCompartmentId().get())
                .displayName(getResolvedGatewayName().get())
                .build())
                .gatewayCollection
                .items.each { gateway ->
                setGatewayId(gateway.id)
                deleteGateway(client, gateway)
            }
        }
    }

    private void deleteGateway(GatewayClient client, Gateway gateway) {
        println("Deleting Gateway '${gateway.displayName}' with id ${gateway.id}")

        client.deleteGateway(DeleteGatewayRequest.builder()
            .gatewayId(gateway.id)
            .build())

        if (getResolvedWaitForCompletion().get()) {
            println("Waiting for Gateway to be ${state('Terminated')}")
            client.waiters
                .forGateway(GetGatewayRequest.builder()
                    .gatewayId(gateway.id).build(),
                    Gateway.LifecycleState.Deleted)
                .execute()
        }
    }

    private void deleteGateway(GatewayClient client, GatewaySummary gateway) {
        println("Deleting Gateway '${gateway.displayName}' with id ${gateway.id}")

        client.deleteGateway(DeleteGatewayRequest.builder()
            .gatewayId(gateway.id)
            .build())

        if (getResolvedWaitForCompletion().get()) {
            println("Waiting for Gateway to be ${state('Terminated')}")
            client.waiters
                .forGateway(GetGatewayRequest.builder()
                    .gatewayId(gateway.id).build(),
                    Gateway.LifecycleState.Deleted)
                .execute()
        }
    }
}
