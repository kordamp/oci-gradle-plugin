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
package org.kordamp.gradle.oci.tasks.instance

import com.oracle.bmc.core.BlockstorageClient
import com.oracle.bmc.core.ComputeClient
import com.oracle.bmc.core.VirtualNetworkClient
import com.oracle.bmc.core.model.Image
import com.oracle.bmc.core.model.Instance
import com.oracle.bmc.core.model.InternetGateway
import com.oracle.bmc.core.model.Shape
import com.oracle.bmc.core.model.Subnet
import com.oracle.bmc.core.model.Vcn
import com.oracle.bmc.identity.IdentityClient
import com.oracle.bmc.identity.model.AvailabilityDomain
import com.oracle.bmc.identity.requests.ListAvailabilityDomainsRequest
import groovy.transform.CompileStatic
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.internal.hash.HashUtil
import org.kordamp.gradle.oci.tasks.AbstractOCITask
import org.kordamp.gradle.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.oci.tasks.traits.CompartmentIdAwareTrait
import org.kordamp.gradle.oci.tasks.traits.ImageAwareTrait
import org.kordamp.gradle.oci.tasks.traits.InstanceNameAwareTrait
import org.kordamp.gradle.oci.tasks.traits.PublicKeyFileAwareTrait
import org.kordamp.gradle.oci.tasks.traits.ShapeAwareTrait
import org.kordamp.gradle.oci.tasks.traits.UserDataFileAwareTrait
import org.kordamp.gradle.oci.tasks.traits.VerboseAwareTrait
import org.kordamp.jipsy.TypeProviderFor

import static org.kordamp.gradle.oci.tasks.create.CreateInstanceTask.maybeCreateInstance
import static org.kordamp.gradle.oci.tasks.create.CreateInternetGatewayTask.maybeCreateInternetGateway
import static org.kordamp.gradle.oci.tasks.create.CreateSubnetTask.maybeCreateSubnet
import static org.kordamp.gradle.oci.tasks.create.CreateVcnTask.maybeCreateVcn
import static org.kordamp.gradle.oci.tasks.get.GetInstancePublicIpTask.getInstancePublicIp

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class SetupInstanceTask extends AbstractOCITask implements CompartmentIdAwareTrait,
        InstanceNameAwareTrait,
        ImageAwareTrait,
        ShapeAwareTrait,
        PublicKeyFileAwareTrait,
        UserDataFileAwareTrait,
        VerboseAwareTrait {
    static final String TASK_DESCRIPTION = 'Setups an Instance with Vcn, InternetGateway, Subnets, InstanceConsoleConnection, and Volume.'

    private final Property<String> createdInstanceId = project.objects.property(String)
    private final RegularFileProperty result = project.objects.fileProperty()

    @Internal
    String getCreatedInstanceId() {
        return createdInstanceId.orNull
    }

    @OutputFile
    RegularFile getResult() {
        if (!result.present) {
            result.set(project.file("${project.buildDir}/oci/${getInstanceName()}.properties"))
        }
        this.result.get()
    }

    @Override
    protected void doExecuteTask() {
        validateCompartmentId()
        validateImage()
        validateShape()
        validatePublicKeyFile()

        result.get().asFile.parentFile.mkdirs()

        Properties props = new Properties()
        props.put('compartment.id', getCompartmentId())

        ComputeClient computeClient = createComputeClient()

        Image _image = validateImage(computeClient, getCompartmentId())
        Shape _shape = validateShape(computeClient, getCompartmentId())

        String networkCidrBlock = '10.0.0.0/16'
        File publicKeyFile = getPublicKeyFile()?.asFile
        File userDataFile = getUserDataFile()?.asFile
        String vcnDisplayName = getInstanceName() + '-vcn'
        String dnsLabel = getInstanceName()
        String internetGatewayDisplayName = getInstanceName() + '-internet-gateway'
        String kmsKeyId = ''

        IdentityClient identityClient = createIdentityClient()
        VirtualNetworkClient vcnClient = createVirtualNetworkClient()
        BlockstorageClient blockstorageClient = createBlockstorageClient()

        Vcn vcn = maybeCreateVcn(this,
                vcnClient,
                getCompartmentId(),
                vcnDisplayName,
                dnsLabel,
                networkCidrBlock,
                true,
                isVerbose())
        props.put('vcn.id', vcn.id)
        props.put('vcn.name', vcn.displayName)
        props.put('vcn.security-list.id', vcn.defaultSecurityListId)
        props.put('vcn.route-table.id', vcn.defaultRouteTableId)

        InternetGateway internetGateway = maybeCreateInternetGateway(this,
                vcnClient,
                getCompartmentId(),
                vcn.id,
                internetGatewayDisplayName,
                true,
                isVerbose())
        props.put('internet-gateway.id', internetGateway.id)

        Subnet subnet = null
        int subnetIndex = 0
        // create a Subnet per AvailabilityDomain
        List<AvailabilityDomain> availabilityDomains = identityClient.listAvailabilityDomains(ListAvailabilityDomainsRequest.builder()
                .compartmentId(getCompartmentId())
                .build()).items
        props.put('vcn.subnets', availabilityDomains.size().toString())
        for (AvailabilityDomain domain : availabilityDomains) {
            String subnetDnsLabel = 'sub' + HashUtil.sha1(vcn.id.bytes).asHexString()[0..8] + (subnetIndex.toString().padLeft(3, '0'))

            Subnet s = maybeCreateSubnet(this,
                    vcnClient,
                    getCompartmentId(),
                    vcn.id,
                    subnetDnsLabel,
                    domain.name,
                    'Subnet ' + domain.name,
                    "10.0.${subnetIndex}.0/24".toString(),
                    true,
                    isVerbose())
            props.put("subnet.${subnetIndex}.id".toString(), s.id)
            props.put("subnet.${subnetIndex}.name".toString(), s.displayName)

            // save the first one
            if (subnet == null) subnet = s
            subnetIndex++
        }

        Instance instance = maybeCreateInstance(this,
                computeClient,
                vcnClient,
                blockstorageClient,
                getCompartmentId(),
                getInstanceName(),
                _image,
                _shape,
                subnet,
                publicKeyFile,
                userDataFile,
                kmsKeyId,
                true)
        createdInstanceId.set(instance.id)
        props.put('instance.id', instance.id)
        props.put('instance.name', instance.displayName)

        Set<String> publicIps = getInstancePublicIp(this,
                computeClient,
                vcnClient,
                getCompartmentId(),
                instance.id)

        props.put('instance.public-ips', publicIps.size().toString())
        int publicIpIndex = 0
        for (String publicIp : publicIps) {
            props.put("instance.public-ip.${publicIpIndex++}".toString(), publicIp)
        }

        props.store(new FileWriter(getResult().asFile), '')
        println("Result stored at ${console.yellow(getResult().asFile.absolutePath)}")
    }
}
