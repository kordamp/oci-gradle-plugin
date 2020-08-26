/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019-2020 Andres Almiray.
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
package org.kordamp.gradle.plugin.oci.tasks.instance

import com.oracle.bmc.core.BlockstorageClient
import com.oracle.bmc.core.ComputeClient
import com.oracle.bmc.core.VirtualNetworkClient
import com.oracle.bmc.core.model.Image
import com.oracle.bmc.core.model.Instance
import com.oracle.bmc.core.model.InternetGateway
import com.oracle.bmc.core.model.Shape
import com.oracle.bmc.core.model.Subnet
import com.oracle.bmc.core.model.Vcn
import com.oracle.bmc.core.requests.GetSubnetRequest
import com.oracle.bmc.core.requests.GetVcnRequest
import com.oracle.bmc.core.requests.ListSubnetsRequest
import com.oracle.bmc.identity.IdentityClient
import com.oracle.bmc.identity.model.AvailabilityDomain
import com.oracle.bmc.identity.requests.ListAvailabilityDomainsRequest
import groovy.transform.CompileStatic
import org.gradle.api.Transformer
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.internal.hash.HashUtil
import org.kordamp.gradle.plugin.oci.tasks.AbstractOCITask
import org.kordamp.gradle.plugin.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.plugin.oci.tasks.traits.CompartmentIdAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.ImageAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.InstanceNameAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.OptionalAvailabilityDomainAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.OptionalDnsLabelAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.OptionalSubnetIdAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.OptionalUserDataFileAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.PublicKeyFileAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.ShapeAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.VerboseAwareTrait
import org.kordamp.jipsy.TypeProviderFor

import static org.kordamp.gradle.plugin.oci.tasks.create.CreateInstanceTask.maybeCreateInstance
import static org.kordamp.gradle.plugin.oci.tasks.create.CreateInternetGatewayTask.maybeCreateInternetGateway
import static org.kordamp.gradle.plugin.oci.tasks.create.CreateSubnetTask.maybeCreateSubnet
import static org.kordamp.gradle.plugin.oci.tasks.create.CreateVcnTask.maybeCreateVcn
import static org.kordamp.gradle.plugin.oci.tasks.get.GetInstancePublicIpTask.getInstancePublicIp
import static org.kordamp.gradle.util.StringUtils.isNotBlank

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
    OptionalUserDataFileAwareTrait,
    OptionalDnsLabelAwareTrait,
    OptionalAvailabilityDomainAwareTrait,
    OptionalSubnetIdAwareTrait,
    VerboseAwareTrait {
    static final String TASK_DESCRIPTION = 'Setups an Instance with Vcn, InternetGateway, Subnets, InstanceConsoleConnection, and Volume.'

    private final Property<String> createdInstanceId = project.objects.property(String)
    private final Provider<RegularFile> output

    SetupInstanceTask() {
        output = getResolvedInstanceName().map(new Transformer<RegularFile, String>() {
            @Override
            RegularFile transform(String s) {
                return project.layout.buildDirectory.file("oci/instance/${s}.properties").get()
            }
        })
    }

    @Internal
    Property<String> getCreatedInstanceId() {
        this.@createdInstanceId
    }

    @OutputFile
    Provider<RegularFile> getOutput() {
        this.@output
    }

    String outputProperty(String key) {
        Properties props = new Properties()
        props.load(output.get().asFile.newInputStream())
        props.get(key)
    }

    @Override
    protected void doExecuteTask() {
        validateCompartmentId()
        validateImage()
        validateShape()
        validatePublicKeyFile()

        output.get().asFile.parentFile.mkdirs()

        Properties props = new Properties()
        props.put('compartment.id', getResolvedCompartmentId().get())

        ComputeClient computeClient = createComputeClient()
        IdentityClient identityClient = createIdentityClient()
        VirtualNetworkClient vcnClient = createVirtualNetworkClient()
        BlockstorageClient blockstorageClient = createBlockstorageClient()

        Image _image = validateImage(computeClient, getResolvedCompartmentId().get())
        Shape _shape = validateShape(computeClient, getResolvedCompartmentId().get())

        File publicKeyFile = getResolvedPublicKeyFile().get().asFile
        File userDataFile = getResolvedUserDataFile()?.get()?.asFile
        String internetGatewayDisplayName = getResolvedInstanceName().get() + '-internet-gateway'
        String kmsKeyId = ''

        AvailabilityDomain availabilityDomain = null
        Subnet subnet = null
        Vcn vcn = null

        if (isNotBlank(getResolvedAvailabilityDomain().orNull)) {
            for (AvailabilityDomain ad : identityClient.listAvailabilityDomains(ListAvailabilityDomainsRequest.builder()
                .compartmentId(getResolvedCompartmentId().get())
                .build()).items) {
                if (ad.id == getResolvedAvailabilityDomain().get()) {
                    availabilityDomain = ad
                    break
                }
            }
        } else if (isNotBlank(getResolvedSubnetId().orNull)) {
            try {
                subnet = vcnClient.getSubnet(GetSubnetRequest.builder()
                    .subnetId(getResolvedSubnetId().get())
                    .build())
                    .subnet

                vcn = vcnClient.getVcn(GetVcnRequest.builder()
                    .vcnId(subnet.vcnId)
                    .build())
                    .vcn
            } catch (Exception ignored) {
                // ignored
            }
        }

        if (!subnet) {
            String networkCidrBlock = '10.0.0.0/16'
            String vcnDisplayName = getResolvedInstanceName().get() + '-vcn'
            String dnsLabel = normalizeDnsLabel(isNotBlank(getResolvedDnsLabel().orNull) ? getResolvedDnsLabel().get() : getResolvedInstanceName().get())
            vcn = maybeCreateVcn(this,
                vcnClient,
                getResolvedCompartmentId().get(),
                vcnDisplayName,
                dnsLabel,
                networkCidrBlock,
                true,
                getResolvedVerbose().get())
        }

        props.put('vcn.id', vcn.id)
        props.put('vcn.name', vcn.displayName)
        props.put('vcn.security-list.id', vcn.defaultSecurityListId)
        props.put('vcn.route-table.id', vcn.defaultRouteTableId)

        InternetGateway internetGateway = maybeCreateInternetGateway(this,
            vcnClient,
            getResolvedCompartmentId().get(),
            vcn.id,
            internetGatewayDisplayName,
            true,
            getResolvedVerbose().get())
        props.put('internet-gateway.id', internetGateway.id)

        if (!subnet) {
            if (availabilityDomain) {
                for (Subnet s : vcnClient.listSubnets(ListSubnetsRequest.builder()
                    .compartmentId(getResolvedCompartmentId().get())
                    .vcnId(vcn.id)
                    .build()).items) {
                    if (s.availabilityDomain == availabilityDomain.name) {
                        subnet = s
                        props.put('vcn.subnets', '1')
                        props.put('subnet.0.id'.toString(), s.id)
                        props.put('subnet.0.name'.toString(), s.displayName)
                        break
                    }
                }
            } else {
                int subnetIndex = 0
                // create a Subnet per AvailabilityDomain
                List<AvailabilityDomain> availabilityDomains = identityClient.listAvailabilityDomains(ListAvailabilityDomainsRequest.builder()
                    .compartmentId(getResolvedCompartmentId().get())
                    .build()).items
                props.put('vcn.subnets', availabilityDomains.size().toString())
                for (AvailabilityDomain domain : availabilityDomains) {
                    String subnetDnsLabel = 'sub' + HashUtil.sha1(vcn.id.bytes).asHexString()[0..8] + (subnetIndex.toString().padLeft(3, '0'))

                    Subnet s = maybeCreateSubnet(this,
                        vcnClient,
                        getResolvedCompartmentId().get(),
                        vcn.id,
                        subnetDnsLabel,
                        domain.name,
                        'Subnet ' + domain.name,
                        "10.0.${subnetIndex}.0/24".toString(),
                        true,
                        getResolvedVerbose().get())
                    props.put("subnet.${subnetIndex}.id".toString(), s.id)
                    props.put("subnet.${subnetIndex}.name".toString(), s.displayName)

                    // save the first one
                    if (subnet == null) subnet = s
                    if (availabilityDomain == null) availabilityDomain = domain
                    subnetIndex++
                }
            }
        }

        Instance instance = maybeCreateInstance(this,
            computeClient,
            vcnClient,
            blockstorageClient,
            identityClient,
            getResolvedCompartmentId().get(),
            getResolvedInstanceName().get(),
            _image,
            _shape,
            availabilityDomain,
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
            getResolvedCompartmentId().get(),
            instance.id)

        props.put('instance.public-ips', publicIps.size().toString())
        int publicIpIndex = 0
        for (String publicIp : publicIps) {
            props.put("instance.public-ip.${publicIpIndex++}".toString(), publicIp)
        }

        props.store(new FileWriter(getOutput().get().asFile), '')
        println("Result stored at ${console.yellow(getOutput().get().asFile.absolutePath)}")
    }

    private String normalizeDnsLabel(String dnsLabel) {
        String label = dnsLabel?.replace('.', '')?.replace('-', '')
        if (label?.length() > 15) label = HashUtil.sha1(dnsLabel.bytes).asHexString()[0..14]
        label
    }
}
