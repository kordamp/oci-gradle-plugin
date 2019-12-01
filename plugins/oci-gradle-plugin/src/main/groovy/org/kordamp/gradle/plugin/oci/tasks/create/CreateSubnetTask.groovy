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
package org.kordamp.gradle.plugin.oci.tasks.create

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
import org.gradle.api.tasks.Internal
import org.kordamp.gradle.plugin.oci.tasks.AbstractOCITask
import org.kordamp.gradle.plugin.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.plugin.oci.tasks.traits.AvailabilityDomainAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.CompartmentIdAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.OptionalDnsLabelAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.SubnetNameAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.VcnIdAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.VerboseAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.WaitForCompletionAwareTrait
import org.kordamp.jipsy.TypeProviderFor

import static org.kordamp.gradle.plugin.oci.tasks.printers.SubnetPrinter.printSubnet

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class CreateSubnetTask extends AbstractOCITask implements CompartmentIdAwareTrait,
    AvailabilityDomainAwareTrait,
    VcnIdAwareTrait,
    SubnetNameAwareTrait,
    OptionalDnsLabelAwareTrait,
    WaitForCompletionAwareTrait,
    VerboseAwareTrait {
    static final String TASK_DESCRIPTION = 'Creates a Subnet.'

    private final Property<String> createdSubnetId = project.objects.property(String)

    @Internal
    Property<String> getCreatedSubnetId() {
        this.@createdSubnetId
    }

    @Override
    void doExecuteTask() {
        validateCompartmentId()
        validateVcnId()
        validateAvailabilityDomain()
        validateSubnetName()
        validateDnsLabel(getVcnId().get())

        VirtualNetworkClient client = createVirtualNetworkClient()
        IdentityClient identityClient = createIdentityClient()

        AvailabilityDomain _availabilityDomain = validateAvailabilityDomain(identityClient, getCompartmentId().get())

        Subnet subnet = maybeCreateSubnet(this,
            client,
            getCompartmentId().get(),
            getVcnId().get(),
            getDnsLabel().get(),
            _availabilityDomain.name,
            getSubnetName().get(),
            '10.0.0.0/24',
            isWaitForCompletion().get(),
            isVerbose().get())
        createdSubnetId.set(subnet.id)
    }

    static Subnet maybeCreateSubnet(OCITask owner,
                                    VirtualNetworkClient client,
                                    String compartmentId,
                                    String vcnId,
                                    String dnsLabel,
                                    String availabilityDomain,
                                    String subnetName,
                                    String cidrBlock,
                                    boolean waitForCompletion,
                                    boolean verbose) {
        // 1. Check if it exists
        List<Subnet> subnets = client.listSubnets(ListSubnetsRequest.builder()
            .compartmentId(compartmentId)
            .vcnId(vcnId)
            .displayName(subnetName)
            .build())
            .items

        if (!subnets.empty) {
            Subnet subnet = subnets[0]
            println("Subnet '${subnetName}' already exists. id = ${owner.console.yellow(subnet.id)}")
            if (verbose) printSubnet(owner, subnet, 0)
            return subnets[0]
        }

        Subnet subnet = client.createSubnet(CreateSubnetRequest.builder()
            .createSubnetDetails(CreateSubnetDetails.builder()
                .compartmentId(compartmentId)
                .vcnId(vcnId)
                .availabilityDomain(availabilityDomain)
                .displayName(subnetName)
                .dnsLabel(dnsLabel)
                .cidrBlock(cidrBlock)
                .build())
            .build())
            .subnet

        if (waitForCompletion) {
            println("Waiting for Subnet to be ${owner.state('Available')}")
            client.waiters.forSubnet(GetSubnetRequest.builder()
                .subnetId(subnet.id)
                .build(),
                Subnet.LifecycleState.Available)
                .execute()
        }

        println("Subnet '${subnetName}' has been provisioned. id = ${owner.console.yellow(subnet.id)}")
        if (verbose) printSubnet(owner, subnet, 0)
        subnet
    }
}
