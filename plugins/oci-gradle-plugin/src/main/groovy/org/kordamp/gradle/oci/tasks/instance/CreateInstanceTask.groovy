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
import com.oracle.bmc.core.requests.GetBootVolumeRequest
import com.oracle.bmc.core.requests.GetInstanceRequest
import com.oracle.bmc.core.requests.LaunchInstanceRequest
import com.oracle.bmc.core.requests.ListBootVolumesRequest
import com.oracle.bmc.core.requests.ListImagesRequest
import com.oracle.bmc.core.requests.ListInstancesRequest
import com.oracle.bmc.core.requests.ListShapesRequest
import com.oracle.bmc.core.requests.UpdateRouteTableRequest
import com.oracle.bmc.core.responses.ListImagesResponse
import com.oracle.bmc.core.responses.ListShapesResponse
import com.oracle.bmc.identity.IdentityClient
import com.oracle.bmc.identity.model.AvailabilityDomain
import com.oracle.bmc.identity.requests.ListAvailabilityDomainsRequest
import groovy.transform.CompileStatic
import org.apache.commons.codec.binary.Base64
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.internal.hash.HashUtil
import org.kordamp.gradle.oci.tasks.AbstractOCITask
import org.kordamp.gradle.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.oci.tasks.traits.CompartmentIdAwareTrait
import org.kordamp.gradle.oci.tasks.traits.PublicKeyFileAwareTrait
import org.kordamp.gradle.oci.tasks.traits.WaitForCompletionAwareTrait
import org.kordamp.jipsy.TypeProviderFor

import static org.kordamp.gradle.StringUtils.isBlank
import static org.kordamp.gradle.StringUtils.isNotBlank
import static org.kordamp.gradle.oci.tasks.create.CreateInternetGatewayTask.maybeCreateInternetGateway
import static org.kordamp.gradle.oci.tasks.create.CreateSubnetTask.maybeCreateSubnet
import static org.kordamp.gradle.oci.tasks.create.CreateVcnTask.maybeCreateVcn

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class CreateInstanceTask extends AbstractOCITask implements CompartmentIdAwareTrait,
    PublicKeyFileAwareTrait,
    WaitForCompletionAwareTrait {
    static final String TASK_DESCRIPTION = 'Creates an Instance with Vcn, Gateway, and Volume.'

    private final Property<String> instanceName = project.objects.property(String)
    private final Property<String> image = project.objects.property(String)
    private final Property<String> shape = project.objects.property(String)
    private final RegularFileProperty userDataFile = project.objects.fileProperty()

    @Input
    @Option(option = 'instance-name', description = 'The name of the Instance to be created (REQUIRED).')
    void setInstanceName(String instanceName) {
        this.instanceName.set(instanceName)
    }

    @Input
    @Option(option = 'image', description = 'The Image to be use (REQUIRED).')
    void setImage(String image) {
        this.image.set(image)
    }

    @Input
    @Option(option = 'shape', description = 'The Shape to use (REQUIRED).')
    void setShape(String shape) {
        this.shape.set(shape)
    }

    @Optional
    @Input
    @Option(option = 'user-data-file', description = 'Location of a user data file (OPTIONAL).')
    void setUserDataFile(String userDataFile) {
        this.userDataFile.set(project.file(userDataFile))
    }

    File getUserDataFile() {
        userDataFile.asFile.orNull
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
            throw new IllegalStateException("Invalid Shape ${shape}")
        }

        String networkCidrBlock = '10.0.0.0/16'
        String publicKeyFile = getPublicKeyFile().text
        String userDataFile = getUserDataFile()?.text
        String subnetDisplayName = getInstanceName() + '-subnet'
        String vcnDisplayName = getInstanceName() + '-vcn'
        String dnsLabel = getInstanceName()
        String internetGatewayDisplayName = getInstanceName() + '-internet-gateway'
        String bootVolumeDisplayName = getInstanceName() + '-boot-volume'
        String kmsKeyId = ''

        VirtualNetworkClient vcnClient = new VirtualNetworkClient(provider)
        BlockstorageClient blockstorageClient = new BlockstorageClient(provider)

        Vcn vcn = maybeCreateVcn(this,
            vcnClient,
            getCompartmentId(),
            vcnDisplayName,
            dnsLabel,
            networkCidrBlock,
            true)

        // TODO: flag for connecting to intranet
        InternetGateway internetGateway = maybeCreateInternetGateway(this,
            vcnClient,
            getCompartmentId(),
            internetGatewayDisplayName,
            vcn.id,
            true)

        // addInternetGatewayToRouteTable(vcnClient, vcn.defaultRouteTableId, internetGateway)

        Subnet subnet = null
        int subnetIndex = 0
        // create a Subnet per AvailabilityDomain
        for (AvailabilityDomain domain : identityClient.listAvailabilityDomains(ListAvailabilityDomainsRequest.builder()
            .compartmentId(getCompartmentId())
            .build()).items) {
            String subnetDnsLabel = 'sub' + HashUtil.sha1(vcn.id.bytes).asCompactString()[0..10] + (subnetIndex.toString().padLeft(3, '0'))
            Subnet s = maybeCreateSubnet(this,
                vcnClient,
                getCompartmentId(),
                vcn.id,
                subnetDnsLabel,
                domain.name,
                subnetDisplayName,
                networkCidrBlock,
                true)

            // save the first one
            if (subnet == null) subnet = s
            subnetIndex++
        }

        Instance instance = maybeCreateInstance(computeClient,
            getCompartmentId(),
            getInstanceName(),
            subnet.availabilityDomain,
            _image.id,
            _shape.shape,
            subnet.id,
            publicKeyFile,
            userDataFile,
            kmsKeyId)

        if (instance.lifecycleState != Instance.LifecycleState.Running) {
            println('Provisioning instance. This may take a while.')
            instance = waitForInstanceProvisioningToComplete(computeClient, instance.id)
            println("Instance is provisioned with id ${instance.id}")
        }

        BootVolume bootVolume = createBootVolume(blockstorageClient,
            getCompartmentId(),
            subnet.availabilityDomain,
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

    private Instance maybeCreateInstance(ComputeClient computeClient,
                                         String compartmentId,
                                         String instanceName,
                                         String availabilityDomain,
                                         String imageId,
                                         String shape,
                                         String subnetId,
                                         String publicKeyFile,
                                         String userDataFile,
                                         String kmsKeyId) {
        List<Instance> instances = computeClient.listInstances(ListInstancesRequest.builder()
            .compartmentId(compartmentId)
            .availabilityDomain(availabilityDomain)
            .displayName(instanceName)
            .build())
            .items

        if (!instances.empty) {
            return instances[0]
        }

        Map<String, String> metadata = new HashMap<>('public_authorized_keys': publicKeyFile)
        if (isNotBlank(userDataFile)) {
            metadata.put("user_data", Base64.encodeBase64String(userDataFile.getBytes()))
        }

        InstanceSourceViaImageDetails details =
            (Strings.isNullOrEmpty(kmsKeyId))
                ? InstanceSourceViaImageDetails.builder()
                .imageId(imageId)
                .build()
                : InstanceSourceViaImageDetails.builder()
                .imageId(imageId)
                .kmsKeyId(kmsKeyId)
                .build()

        computeClient.launchInstance(LaunchInstanceRequest.builder()
            .launchInstanceDetails(LaunchInstanceDetails.builder()
                .availabilityDomain(availabilityDomain)
                .compartmentId(compartmentId)
                .displayName(instanceName)
                .metadata(metadata)
                .shape(shape)
                .sourceDetails(details)
                .createVnicDetails(CreateVnicDetails.builder()
                    .subnetId(subnetId)
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

    private BootVolume createBootVolume(BlockstorageClient blockstorageClient,
                                        String compartmentId,
                                        String availabilityDomain,
                                        String imageId,
                                        String displayName,
                                        String kmsKeyId) {
        List<BootVolume> bootVolumes = blockstorageClient.listBootVolumes(ListBootVolumesRequest.builder()
            .availabilityDomain(availabilityDomain)
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

        BootVolumeSourceDetails bootVolumeSourceDetails = BootVolumeSourceFromBootVolumeDetails.builder()
            .id(bootVolumeId)
            .build()

        CreateBootVolumeDetails.Builder details = CreateBootVolumeDetails.builder()
            .availabilityDomain(availabilityDomain)
            .compartmentId(compartmentId)
            .displayName(displayName)
            .sourceDetails(bootVolumeSourceDetails)

        if (isNotBlank(kmsKeyId)) {
            details = details.kmsKeyId(kmsKeyId)
        }

        blockstorageClient.createBootVolume(CreateBootVolumeRequest.builder()
            .createBootVolumeDetails(details.build())
            .build())
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
