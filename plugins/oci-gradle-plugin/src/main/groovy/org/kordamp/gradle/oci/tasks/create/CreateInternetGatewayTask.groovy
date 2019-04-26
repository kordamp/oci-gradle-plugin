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
package org.kordamp.gradle.oci.tasks.create

import com.oracle.bmc.auth.AuthenticationDetailsProvider
import com.oracle.bmc.core.VirtualNetworkClient
import com.oracle.bmc.core.model.CreateInternetGatewayDetails
import com.oracle.bmc.core.model.InternetGateway
import com.oracle.bmc.core.model.RouteRule
import com.oracle.bmc.core.model.RouteTable
import com.oracle.bmc.core.model.UpdateRouteTableDetails
import com.oracle.bmc.core.model.Vcn
import com.oracle.bmc.core.requests.CreateInternetGatewayRequest
import com.oracle.bmc.core.requests.GetInternetGatewayRequest
import com.oracle.bmc.core.requests.GetRouteTableRequest
import com.oracle.bmc.core.requests.GetVcnRequest
import com.oracle.bmc.core.requests.ListInternetGatewaysRequest
import com.oracle.bmc.core.requests.UpdateRouteTableRequest
import groovy.transform.CompileStatic
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskAction
import org.kordamp.gradle.oci.tasks.AbstractOCITask
import org.kordamp.gradle.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.oci.tasks.traits.CompartmentIdAwareTrait
import org.kordamp.gradle.oci.tasks.traits.InternateGatewayNameAwareTrait
import org.kordamp.gradle.oci.tasks.traits.VcnIdAwareTrait
import org.kordamp.gradle.oci.tasks.traits.VerboseAwareTrait
import org.kordamp.gradle.oci.tasks.traits.WaitForCompletionAwareTrait
import org.kordamp.jipsy.TypeProviderFor

import static org.kordamp.gradle.oci.tasks.printers.InternetGatewayPrinter.printInternetGateway

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class CreateInternetGatewayTask extends AbstractOCITask implements CompartmentIdAwareTrait,
    VcnIdAwareTrait,
    InternateGatewayNameAwareTrait,
    WaitForCompletionAwareTrait,
    VerboseAwareTrait {
    static final String TASK_DESCRIPTION = 'Creates a InternetGateway.'

    private final Property<String> createdInternetGatewayId = project.objects.property(String)

    String getCreatedInternetGatewayId() {
        return createdInternetGatewayId.orNull
    }

    @TaskAction
    void executeTask() {
        validateCompartmentId()
        validateVcnId()
        validateInternetGatewayName()

        AuthenticationDetailsProvider provider = resolveAuthenticationDetailsProvider()
        VirtualNetworkClient client = new VirtualNetworkClient(provider)

        InternetGateway internetGateway = maybeCreateInternetGateway(this,
            client,
            getCompartmentId(),
            getVcnId(),
            getInternetGatewayName(),
            isWaitForCompletion(),
            isVerbose())
        createdInternetGatewayId.set(internetGateway.id)

        client.close()
    }

    static InternetGateway maybeCreateInternetGateway(OCITask owner,
                                                      VirtualNetworkClient client,
                                                      String compartmentId,
                                                      String vcnId,
                                                      String internetGatewayName,
                                                      boolean waitForCompletion,
                                                      boolean verbose) {
        InternetGateway internetGateway = doMaybeCreateInternetGateway(owner,
            client,
            compartmentId,
            vcnId,
            internetGatewayName,
            waitForCompletion,
            verbose)

        maybeAddInternetGatewayToVcn(owner, client, vcnId, internetGateway)

        internetGateway
    }

    static InternetGateway doMaybeCreateInternetGateway(OCITask owner,
                                                        VirtualNetworkClient client,
                                                        String compartmentId,
                                                        String vcnId,
                                                        String internetGatewayName,
                                                        boolean waitForCompletion,
                                                        boolean verbose) {
        // 1. Check if it exists
        List<InternetGateway> internetGateways = client.listInternetGateways(ListInternetGatewaysRequest.builder()
            .compartmentId(compartmentId)
            .vcnId(vcnId)
            .build())
            .items

        InternetGateway internetGateway = internetGateways.find { InternetGateway ig -> ig.displayName == internetGatewayName }

        if (internetGateway) {
            println("InternetGateway '${internetGatewayName}' already exists. id = ${internetGateway.id}")
            if (verbose) printInternetGateway(owner, internetGateway, 0)
            return internetGateway
        }

        if (!internetGateways.empty) {
            internetGateway = internetGateways[0]
            println("InternetGateway '${internetGateway.displayName}' exists. id = ${internetGateway.id}")
            if (verbose) printInternetGateway(owner, internetGateway, 0)
            return internetGateway
        }

        internetGateway = client.createInternetGateway(CreateInternetGatewayRequest.builder()
            .createInternetGatewayDetails(CreateInternetGatewayDetails.builder()
                .compartmentId(compartmentId)
                .vcnId(vcnId)
                .displayName(internetGatewayName)
                .isEnabled(true)
                .build())
            .build())
            .internetGateway

        if (waitForCompletion) {
            println("Waiting for InternetGateway to be ${owner.console.green('Available')}")
            client.waiters.forInternetGateway(GetInternetGatewayRequest.builder()
                .igId(internetGateway.id)
                .build(),
                InternetGateway.LifecycleState.Available)
                .execute()
        }

        println("InternetGateway '${internetGatewayName}' has been provisioned. id = ${owner.console.yellow(internetGateway.id)}")
        if (verbose) printInternetGateway(owner, internetGateway, 0)
        internetGateway
    }

    static void maybeAddInternetGatewayToVcn(OCITask owner,
                                             VirtualNetworkClient client,
                                             String vcnId,
                                             InternetGateway internetGateway) {
        // 1. fetch the VCN
        Vcn vcn = client.getVcn(GetVcnRequest.builder()
            .vcnId(vcnId)
            .build())
            .vcn

        // 2. fetch RouteTable
        RouteTable routeTable = client.getRouteTable(GetRouteTableRequest.builder()
            .rtId(vcn.defaultRouteTableId)
            .build())
            .routeTable

        // 3. is the IG already in the RT?
        if (!routeTable.routeRules.find { RouteRule rr -> rr.networkEntityId == internetGateway.id }) {
            List<RouteRule> routeRules = []
            routeRules << RouteRule.builder()
                .destination('0.0.0.0/0')
                .destinationType(RouteRule.DestinationType.CidrBlock)
                .networkEntityId(internetGateway.id)
                .build()

            println("Adding InternetGateway '${internetGateway.displayName}' to RouteTable. vcnId = ${owner.console.yellow(vcnId)}")
            client.updateRouteTable(UpdateRouteTableRequest.builder()
                .updateRouteTableDetails(UpdateRouteTableDetails.builder()
                    .routeRules(routeRules).build())
                .rtId(vcn.defaultRouteTableId)
                .build())
        }
    }
}
