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

import com.google.common.base.Strings
import com.oracle.bmc.auth.AuthenticationDetailsProvider
import com.oracle.bmc.core.BlockstorageClient
import com.oracle.bmc.core.ComputeClient
import com.oracle.bmc.core.VirtualNetworkClient
import com.oracle.bmc.core.model.BootVolume
import com.oracle.bmc.core.model.BootVolumeSourceDetails
import com.oracle.bmc.core.model.BootVolumeSourceFromBootVolumeDetails
import com.oracle.bmc.core.model.CreateBootVolumeDetails
import com.oracle.bmc.core.model.CreateInternetGatewayDetails
import com.oracle.bmc.core.model.CreateSubnetDetails
import com.oracle.bmc.core.model.CreateVcnDetails
import com.oracle.bmc.core.model.CreateVnicDetails
import com.oracle.bmc.core.model.Image
import com.oracle.bmc.core.model.Instance
import com.oracle.bmc.core.model.InstanceSourceViaImageDetails
import com.oracle.bmc.core.model.InternetGateway
import com.oracle.bmc.core.model.LaunchInstanceAgentConfigDetails
import com.oracle.bmc.core.model.LaunchInstanceDetails
import com.oracle.bmc.core.model.RouteRule
import com.oracle.bmc.core.model.Shape
import com.oracle.bmc.core.model.Subnet
import com.oracle.bmc.core.model.UpdateRouteTableDetails
import com.oracle.bmc.core.model.Vcn
import com.oracle.bmc.core.requests.CreateBootVolumeRequest
import com.oracle.bmc.core.requests.CreateInternetGatewayRequest
import com.oracle.bmc.core.requests.CreateSubnetRequest
import com.oracle.bmc.core.requests.CreateVcnRequest
import com.oracle.bmc.core.requests.GetBootVolumeRequest
import com.oracle.bmc.core.requests.GetInstanceRequest
import com.oracle.bmc.core.requests.GetSubnetRequest
import com.oracle.bmc.core.requests.LaunchInstanceRequest
import com.oracle.bmc.core.requests.ListBootVolumesRequest
import com.oracle.bmc.core.requests.ListImagesRequest
import com.oracle.bmc.core.requests.ListInstancesRequest
import com.oracle.bmc.core.requests.ListInternetGatewaysRequest
import com.oracle.bmc.core.requests.ListShapesRequest
import com.oracle.bmc.core.requests.ListSubnetsRequest
import com.oracle.bmc.core.requests.ListVcnsRequest
import com.oracle.bmc.core.requests.UpdateRouteTableRequest
import com.oracle.bmc.core.responses.ListImagesResponse
import com.oracle.bmc.core.responses.ListShapesResponse
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
import org.kordamp.gradle.oci.tasks.traits.CompartmentAwareTrait
import org.kordamp.gradle.oci.tasks.traits.PublicKeyFileAwareTrait
import org.kordamp.jipsy.TypeProviderFor

import static org.kordamp.gradle.StringUtils.isBlank

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class CreateInstanceTask extends AbstractOCITask implements CompartmentAwareTrait, AvailabilityDomainAwareTrait, PublicKeyFileAwareTrait {
    static final String DESCRIPTION = 'Creates an instance with VCN, Gateway, and Volume.'

    private final Property<String> instanceName = project.objects.property(String)
    private final Property<String> image = project.objects.property(String)
    private final Property<String> shape = project.objects.property(String)

    @Input
    @Option(option = 'instance-name', description = 'The name of the instance to be created (REQUIRED).')
    void setInstanceName(String instanceName) {
        this.instanceName.set(instanceName)
    }

    @Input
    @Option(option = 'image', description = 'The image to be use (REQUIRED).')
    void setImage(String image) {
        this.image.set(image)
    }

    @Input
    @Option(option = 'shape', description = 'The shape to use (REQUIRED).')
    void setShape(String shape) {
        this.shape.set(shape)
    }

    String getInstanceName() {
        instanceName.orNull
    }

    String getImage() {
        image.orNull
    }

    String getShape() {
        shape.orNull
    }

    @TaskAction
    void executeTask() {
        validateCompartmentId()
        validateAvailabilityDomain()

        if (isBlank(getInstanceName())) {
            setInstanceName(UUID.randomUUID().toString())
            project.logger.warn("Missing value of 'instanceName' in $path. Value set to ${instanceName}")
        }
        if (isBlank(getImage())) {
            throw new IllegalStateException("Missing value for 'image' in $path")
        }
        if (isBlank(getShape())) {
            throw new IllegalStateException("Missing value for 'shape' in $path")
        }

        AuthenticationDetailsProvider provider = resolveAuthenticationDetailsProvider()
        ComputeClient computeClient = new ComputeClient(provider)
        IdentityClient identityClient = IdentityClient.builder().build(provider)

        Image _image = validateImage(computeClient)
        if (!_image) {
            throw new IllegalStateException("Invalid image ${image}")
        }

        Shape _shape = validateShape(computeClient)
        if (!_shape) {
            throw new IllegalStateException("Invalid shape ${shape}")
        }

        AvailabilityDomain _availabilityDomain = validateAvailabilityDomain(identityClient, compartmentId)

        String networkCidrBlock = '10.0.0.0/16'
        String publicPublicKey = getPublicKeyFile().text
        String subnetDisplayName = getInstanceName() + '-subnet'
        String vcnDisplayName = getInstanceName() + '-vcn'
        String internetGatewayDisplayName = getInstanceName() + '-internet-gateway'
        String bootVolumeDisplayName = getInstanceName() + '-bootVolume'
        String kmsKeyId = ''

        VirtualNetworkClient vcnClient = new VirtualNetworkClient(provider)
        BlockstorageClient blockstorageClient = new BlockstorageClient(provider)

        println('Provisioning VCN. This may take a while.')
        Vcn vcn = maybeCreateVcn(vcnClient, compartmentId, vcnDisplayName, networkCidrBlock)
        println("VCN is provisioned with id ${vcn.id}")

        // TODO: flag for connecting to intranet
        // InternetGateway internetGateway = maybeCreateInternetGateway(vcnClient, compartmentId, internetGatewayDisplayName, vcn.id)

        // addInternetGatewayToRouteTable(vcnClient, vcn.defaultRouteTableId, internetGateway)

        Subnet subnet = maybeCreateSubnet(vcnClient,
            compartmentId,
            _availabilityDomain,
            subnetDisplayName,
            networkCidrBlock,
            vcn.id)

        Instance instance = maybeCreateInstance(computeClient,
            _availabilityDomain,
            _image,
            _shape,
            subnet,
            publicPublicKey,
            kmsKeyId)

        if (instance.lifecycleState != Instance.LifecycleState.Running) {
            println('Provisioning instance. This may take a while.')
            instance = waitForInstanceProvisioningToComplete(computeClient, instance.id)
            println("Instance is provisioned with id ${instance.id}")
        }

        printMonitoringStatus(instance)

        BootVolume bootVolume = createBootVolume(blockstorageClient,
            _availabilityDomain,
            instance.imageId,
            bootVolumeDisplayName,
            kmsKeyId)

        println('Provisioning bootVolume. This may take a while.')
        bootVolume = waitForBootVolumeToBeReady(blockstorageClient, bootVolume.id)
        println("BootVolume is provisioned with id ${bootVolume.id}")
    }

    private Image validateImage(ComputeClient client) {
        ListImagesResponse response = client.listImages(ListImagesRequest.builder()
            .compartmentId(compartmentId)
            .build())
        response.items.find { Image img -> img.displayName == getImage() }
    }

    private Shape validateShape(ComputeClient client) {
        ListShapesResponse response = client.listShapes(ListShapesRequest.builder()
            .compartmentId(compartmentId)
            .build())
        response.items.find { Shape sh -> sh.shape == getShape() }
    }

    private Vcn maybeCreateVcn(VirtualNetworkClient vcnClient,
                               String compartmentId,
                               String vcnName,
                               String cidrBlock) {
        List<Vcn> vcns = vcnClient.listVcns(ListVcnsRequest.builder()
            .compartmentId(compartmentId)
            .displayName(vcnName)
            .build())
            .items

        if (!vcns.empty) {
            return vcns[0]
        }

        vcnClient.createVcn(CreateVcnRequest.builder()
            .createVcnDetails(CreateVcnDetails.builder()
                .cidrBlock(cidrBlock)
                .compartmentId(compartmentId)
                .displayName(vcnName)
                .build())
            .build())
            .vcn
    }

    private InternetGateway maybeCreateInternetGateway(VirtualNetworkClient vcnClient,
                                                       String compartmentId,
                                                       String internetGatewayName,
                                                       String vcnId) {
        List<InternetGateway> internetGateways = vcnClient.listInternetGateways(ListInternetGatewaysRequest.builder()
            .compartmentId(compartmentId)
            .vcnId(vcnId)
            .displayName(internetGatewayName)
            .build())
            .items

        if (!internetGateways.empty) {
            return internetGateways[0]
        }

        vcnClient.createInternetGateway(CreateInternetGatewayRequest.builder()
            .createInternetGatewayDetails(CreateInternetGatewayDetails.builder()
                .compartmentId(compartmentId)
                .displayName(internetGatewayName)
                .isEnabled(true)
                .vcnId(vcnId)
                .build())
            .build())
            .internetGateway
    }

    private void addInternetGatewayToRouteTable(VirtualNetworkClient vcnClient,
                                                String routeTableId,
                                                InternetGateway internetGateway) {
        List<RouteRule> routeRules = []
        RouteRule internetAccessRoute = RouteRule.builder()
            .cidrBlock('0.0.0.0/0')
            .destination(internetGateway.id)
            .networkEntityId(internetGateway.id)
            .build()
        routeRules << internetAccessRoute

        vcnClient.updateRouteTable(UpdateRouteTableRequest.builder()
            .updateRouteTableDetails(UpdateRouteTableDetails.builder().routeRules(routeRules).build())
            .rtId(routeTableId)
            .build())
    }

    private Subnet maybeCreateSubnet(VirtualNetworkClient vcnClient,
                                     String compartmentId,
                                     AvailabilityDomain availabilityDomain,
                                     String subnetName,
                                     String cidrBlock,
                                     String vcnId) {
        List<Subnet> subnets = vcnClient.listSubnets(ListSubnetsRequest.builder()
            .compartmentId(compartmentId)
            .vcnId(vcnId)
            .displayName(subnetName)
            .build())
            .items

        if (!subnets.empty) {
            return subnets[0]
        }

        Subnet subnet = vcnClient.createSubnet(CreateSubnetRequest.builder()
            .createSubnetDetails(CreateSubnetDetails.builder()
                .availabilityDomain(availabilityDomain.name)
                .compartmentId(compartmentId)
                .displayName(subnetName)
                .cidrBlock(cidrBlock)
                .vcnId(vcnId)
                .build())
            .build())
            .subnet

        // wait
        vcnClient.waiters.forSubnet(GetSubnetRequest.builder()
            .subnetId(subnet.id)
            .build(),
            Subnet.LifecycleState.Available)
            .execute()

        subnet
    }

    private Instance maybeCreateInstance(ComputeClient computeClient,
                                         AvailabilityDomain availabilityDomain,
                                         Image image,
                                         Shape shape,
                                         Subnet subnet,
                                         String publicPublicKey,
                                         String kmsKeyId) {
        List<Instance> instances = computeClient.listInstances(ListInstancesRequest.builder()
            .compartmentId(compartmentId)
            .availabilityDomain(availabilityDomain.name)
            .displayName(instanceName.get())
            .build())
            .items

        if (!instances.empty) {
            return instances[0]
        }

        Map<String, String> metadata = new HashMap<>('public_authorized_keys': publicPublicKey)

        InstanceSourceViaImageDetails details =
            (Strings.isNullOrEmpty(kmsKeyId))
                ? InstanceSourceViaImageDetails.builder().imageId(image.id).build()
                : InstanceSourceViaImageDetails.builder()
                .imageId(image.id)
                .kmsKeyId(kmsKeyId)
                .build()

        computeClient.launchInstance(LaunchInstanceRequest.builder()
            .launchInstanceDetails(LaunchInstanceDetails.builder()
                .availabilityDomain(availabilityDomain.name)
                .compartmentId(compartmentId)
                .displayName(instanceName.get())
                .metadata(metadata)
                .shape(shape.shape)
                .sourceDetails(details)
                .createVnicDetails(CreateVnicDetails.builder()
                    .subnetId(subnet.id)
                    .build())
                .agentConfig(LaunchInstanceAgentConfigDetails.builder()
                    .isMonitoringDisabled(false)
                    .build())
                .build())
            .build())
            .instance
    }

    private Instance waitForInstanceProvisioningToComplete(ComputeClient computeClient, String instanceId) {
        computeClient.waiters.forInstance(
            GetInstanceRequest.builder().instanceId(instanceId).build(),
            Instance.LifecycleState.Running)
            .execute()
            .instance
    }

    private void printMonitoringStatus(Instance instance) {
        boolean monitoringEnabled = instance.agentConfig != null && !instance.agentConfig.isMonitoringDisabled

        if (monitoringEnabled) {
            println("Instance ${instance.id} has Monitoring Enabled")
        } else {
            println("Instance ${instance.id} has Monitoring Disabled")
        }
    }

    private BootVolume createBootVolume(BlockstorageClient blockstorageClient,
                                        AvailabilityDomain availabilityDomain,
                                        String imageId,
                                        String displayName,
                                        String kmsKeyId) {
        List<BootVolume> bootVolumes = blockstorageClient.listBootVolumes(ListBootVolumesRequest.builder()
            .availabilityDomain(availabilityDomain.name)
            .compartmentId(compartmentId)
            .build())
            .items

        String bootVolumeId = null
        for (BootVolume bootVolume : bootVolumes) {
            if (bootVolume.lifecycleState.equals(BootVolume.LifecycleState.Available)
                && bootVolume.imageId != null
                && bootVolume.imageId.equals(imageId)) {
                bootVolumeId = bootVolume.id
                break
            }
        }

        BootVolumeSourceDetails bootVolumeSourceDetails = BootVolumeSourceFromBootVolumeDetails.builder().id(bootVolumeId).build()
        CreateBootVolumeDetails details = CreateBootVolumeDetails.builder()
            .availabilityDomain(availabilityDomain.name)
            .compartmentId(compartmentId)
            .displayName(displayName)
            .sourceDetails(bootVolumeSourceDetails)
            .kmsKeyId(kmsKeyId)
            .build()

        blockstorageClient.createBootVolume(CreateBootVolumeRequest.builder().createBootVolumeDetails(details).build())
            .bootVolume
    }

    private static BootVolume waitForBootVolumeToBeReady(BlockstorageClient blockStorage, String bootVolumeId) {
        blockStorage.waiters.forBootVolume(
            GetBootVolumeRequest.builder().bootVolumeId(bootVolumeId).build(),
            BootVolume.LifecycleState.Available)
            .execute()
            .bootVolume
    }
}
