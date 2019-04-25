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
import com.oracle.bmc.core.model.CreateSubnetDetails
import com.oracle.bmc.core.model.Subnet
import com.oracle.bmc.core.requests.CreateSubnetRequest
import com.oracle.bmc.core.requests.GetSubnetRequest
import com.oracle.bmc.core.requests.ListSubnetsRequest
import com.oracle.bmc.identity.IdentityClient
import com.oracle.bmc.identity.model.AvailabilityDomain
import groovy.transform.CompileStatic
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.kordamp.gradle.oci.tasks.AbstractOCITask
import org.kordamp.gradle.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.oci.tasks.traits.AvailabilityDomainAwareTrait
import org.kordamp.gradle.oci.tasks.traits.CompartmentIdAwareTrait
import org.kordamp.gradle.oci.tasks.traits.VcnIdAwareTrait
import org.kordamp.gradle.oci.tasks.traits.WaitForCompletionAwareTrait
import org.kordamp.jipsy.TypeProviderFor

import static org.kordamp.gradle.StringUtils.isBlank
import static org.kordamp.gradle.oci.tasks.printers.SubnetPrinter.printSubnet

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class CreateSubnetTask extends AbstractOCITask implements CompartmentIdAwareTrait,
    AvailabilityDomainAwareTrait,
    VcnIdAwareTrait,
    WaitForCompletionAwareTrait {
    static final String TASK_DESCRIPTION = 'Creates a Subnet.'

    private final Property<String> subnetName = project.objects.property(String)
    private final Property<String> createdSubnetId = project.objects.property(String)

    @Input
    @Option(option = 'subnet-name', description = 'The name of the Subnet to be created.')
    void setSubnetName(String subnetName) {
        this.subnetName.set(subnetName)
    }

    String getSubnetName() {
        return subnetName.orNull
    }

    String getCreatedSubnetId() {
        return createdSubnetId.orNull
    }

    @TaskAction
    void executeTask() {
        validateCompartmentId()
        validateVcnId()
        validateAvailabilityDomain()

        if (isBlank(getSubnetName())) {
            setSubnetName('subnet-' + UUID.randomUUID().toString())
            project.logger.warn("Missing value for 'subnetName' in $path. Value set to ${subnetName}")
        }

        AuthenticationDetailsProvider provider = resolveAuthenticationDetailsProvider()
        VirtualNetworkClient client = new VirtualNetworkClient(provider)
        IdentityClient identityClient = IdentityClient.builder().build(provider)

        AvailabilityDomain _availabilityDomain = validateAvailabilityDomain(identityClient, compartmentId)

        Subnet subnet = maybeCreateSubnet(this,
            client,
            getCompartmentId(),
            getVcnId(),
            _availabilityDomain.name,
            getSubnetName(),
            '10.0.0.0/24',
            isWaitForCompletion())
        createdSubnetId.set(subnet.id)

        client.close()
    }

    static Subnet maybeCreateSubnet(OCITask owner,
                                    VirtualNetworkClient client,
                                    String compartmentId,
                                    String vcnId,
                                    String availabilityDomain,
                                    String subnetName,
                                    String cidrBlock,
                                    boolean waitForCompletion) {

        // 1. Check if it exists
        List<Subnet> subnets = client.listSubnets(ListSubnetsRequest.builder()
            .compartmentId(compartmentId)
            .vcnId(vcnId)
            .displayName(subnetName)
            .build())
            .items

        if (!subnets.empty) {
            Subnet subnet = subnets[0]
            println("Subnet '${subnetName}' already exists. id = ${subnet.id}")
            printSubnet(owner, subnet, 0)
            return subnets[0]
        }

        Subnet subnet = client.createSubnet(CreateSubnetRequest.builder()
            .createSubnetDetails(CreateSubnetDetails.builder()
                .availabilityDomain(availabilityDomain)
                .compartmentId(compartmentId)
                .displayName(subnetName)
                .cidrBlock(cidrBlock)
                .vcnId(vcnId)
                .build())
            .build())
            .subnet

        if (waitForCompletion) {
            println("Waiting for Subnet to be Available")
            client.waiters.forSubnet(GetSubnetRequest.builder()
                .subnetId(subnet.id)
                .build(),
                Subnet.LifecycleState.Available)
                .execute()
        }

        println("Subnet '${subnetName}' has been provisioned. id = ${subnet.id}")
        printSubnet(owner, subnet, 0)
        subnet
    }
}
