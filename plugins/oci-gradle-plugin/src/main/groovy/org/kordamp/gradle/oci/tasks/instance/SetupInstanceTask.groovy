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

import com.oracle.bmc.auth.AuthenticationDetailsProvider
import com.oracle.bmc.core.BlockstorageClient
import com.oracle.bmc.core.ComputeClient
import com.oracle.bmc.core.VirtualNetworkClient
import com.oracle.bmc.core.model.Image
import com.oracle.bmc.core.model.Instance
import com.oracle.bmc.core.model.Shape
import com.oracle.bmc.core.model.Subnet
import com.oracle.bmc.core.model.Vcn
import com.oracle.bmc.identity.IdentityClient
import com.oracle.bmc.identity.model.AvailabilityDomain
import com.oracle.bmc.identity.requests.ListAvailabilityDomainsRequest
import groovy.transform.CompileStatic
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskAction
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

    String getCreatedInstanceId() {
        return createdInstanceId.orNull
    }

    @TaskAction
    void executeTask() {
        validateCompartmentId()
        validateInstanceName()
        validateImage()
        validateShape()
        validatePublicKeyFile()

        AuthenticationDetailsProvider provider = resolveAuthenticationDetailsProvider()
        ComputeClient computeClient = new ComputeClient(provider)

        Image _image = validateImage(computeClient, getCompartmentId())
        Shape _shape = validateShape(computeClient, getCompartmentId())

        String networkCidrBlock = '10.0.0.0/16'
        String publicKeyFile = getPublicKeyFile().text
        String userDataFile = getUserDataFile()?.text
        String vcnDisplayName = getInstanceName() + '-vcn'
        String dnsLabel = getInstanceName()
        String internetGatewayDisplayName = getInstanceName() + '-internet-gateway'
        String kmsKeyId = ''

        IdentityClient identityClient = IdentityClient.builder().build(provider)
        VirtualNetworkClient vcnClient = new VirtualNetworkClient(provider)
        BlockstorageClient blockstorageClient = new BlockstorageClient(provider)

        Vcn vcn = maybeCreateVcn(this,
            vcnClient,
            getCompartmentId(),
            vcnDisplayName,
            dnsLabel,
            networkCidrBlock,
            true,
            isVerbose())

        maybeCreateInternetGateway(this,
            vcnClient,
            getCompartmentId(),
            internetGatewayDisplayName,
            vcn.id,
            true,
            isVerbose())

        Subnet subnet = null
        int subnetIndex = 0
        // create a Subnet per AvailabilityDomain
        for (AvailabilityDomain domain : identityClient.listAvailabilityDomains(ListAvailabilityDomainsRequest.builder()
            .compartmentId(getCompartmentId())
            .build()).items) {
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

            // save the first one
            if (subnet == null) subnet = s
            subnetIndex++
        }

        Instance instance = maybeCreateInstance(this,
            computeClient,
            blockstorageClient,
            getCompartmentId(),
            getInstanceName(),
            _image,
            _shape,
            subnet,
            publicKeyFile,
            userDataFile,
            kmsKeyId,
            isVerbose())
        createdInstanceId.set(instance.id)

        identityClient.close()
        computeClient.close()
        vcnClient.close()
        blockstorageClient.close()
    }
}
