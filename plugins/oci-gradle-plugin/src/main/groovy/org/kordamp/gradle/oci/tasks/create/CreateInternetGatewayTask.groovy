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
import com.oracle.bmc.core.requests.CreateInternetGatewayRequest
import com.oracle.bmc.core.requests.GetInternetGatewayRequest
import com.oracle.bmc.core.requests.ListInternetGatewaysRequest
import groovy.transform.CompileStatic
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.kordamp.gradle.oci.tasks.AbstractOCITask
import org.kordamp.gradle.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.oci.tasks.traits.CompartmentIdAwareTrait
import org.kordamp.gradle.oci.tasks.traits.VcnIdAwareTrait
import org.kordamp.gradle.oci.tasks.traits.WaitForCompletionAwareTrait
import org.kordamp.jipsy.TypeProviderFor

import static org.kordamp.gradle.StringUtils.isBlank
import static org.kordamp.gradle.oci.tasks.printers.InternetGatewayPrinter.printInternetGateway

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class CreateInternetGatewayTask extends AbstractOCITask implements CompartmentIdAwareTrait,
    VcnIdAwareTrait,
    WaitForCompletionAwareTrait {
    static final String TASK_DESCRIPTION = 'Creates a InternetGateway.'

    private final Property<String> internetGatewayName = project.objects.property(String)
    private final Property<String> createdInternetGatewayId = project.objects.property(String)

    @Input
    @Option(option = 'internetGateway-name', description = 'The name of the InternetGateway to be created.')
    void setInternetGatewayName(String internetGatewayName) {
        this.internetGatewayName.set(internetGatewayName)
    }

    String getInternetGatewayName() {
        return internetGatewayName.orNull
    }

    String getCreatedInternetGatewayId() {
        return createdInternetGatewayId.orNull
    }

    @TaskAction
    void executeTask() {
        validateCompartmentId()
        validateVcnId()

        if (isBlank(getInternetGatewayName())) {
            setInternetGatewayName('internet-gateway-' + UUID.randomUUID().toString())
            project.logger.warn("Missing value for 'internetGatewayName' in $path. Value set to ${internetGatewayName}")
        }

        AuthenticationDetailsProvider provider = resolveAuthenticationDetailsProvider()
        VirtualNetworkClient client = new VirtualNetworkClient(provider)

        InternetGateway internetGateway = maybeCreateInternetGateway(this,
            client,
            getCompartmentId(),
            getVcnId(),
            getInternetGatewayName(),
            isWaitForCompletion())
        createdInternetGatewayId.set(internetGateway.id)
        // TODO: add it to the vcn routing table

        client.close()
    }

    static InternetGateway maybeCreateInternetGateway(OCITask owner,
                                                      VirtualNetworkClient client,
                                                      String compartmentId,
                                                      String vcnId,
                                                      String internetGatewayName,
                                                      boolean waitForCompletion) {
        // 1. Check if it exists
        List<InternetGateway> internetGateways = client.listInternetGateways(ListInternetGatewaysRequest.builder()
            .compartmentId(compartmentId)
            .vcnId(vcnId)
            .displayName(internetGatewayName)
            .build())
            .items

        if (!internetGateways.empty) {
            InternetGateway internetGateway = internetGateways[0]
            println("InternetGateway '${internetGatewayName}' already exists. id = ${internetGateway.id}")
            printInternetGateway(owner, internetGateway, 0)
            return internetGateways[0]
        }

        InternetGateway internetGateway = client.createInternetGateway(CreateInternetGatewayRequest.builder()
            .createInternetGatewayDetails(CreateInternetGatewayDetails.builder()
                .compartmentId(compartmentId)
                .vcnId(vcnId)
                .displayName(internetGatewayName)
                .isEnabled(true)
                .build())
            .build())
            .internetGateway

        if (waitForCompletion) {
            println("Waiting for InternetGateway to be Available")
            client.waiters.forInternetGateway(GetInternetGatewayRequest.builder()
                .igId(internetGateway.id)
                .build(),
                InternetGateway.LifecycleState.Available)
                .execute()
        }

        println("InternetGateway '${internetGatewayName}' has been provisioned. id = ${internetGateway.id}")
        printInternetGateway(owner, internetGateway, 0)
        internetGateway
    }
}
