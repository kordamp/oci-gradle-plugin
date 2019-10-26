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

import com.google.common.base.Strings
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
import com.oracle.bmc.core.model.LaunchInstanceAgentConfigDetails
import com.oracle.bmc.core.model.LaunchInstanceDetails
import com.oracle.bmc.core.model.Shape
import com.oracle.bmc.core.model.Subnet
import com.oracle.bmc.core.requests.CreateBootVolumeRequest
import com.oracle.bmc.core.requests.GetBootVolumeRequest
import com.oracle.bmc.core.requests.GetInstanceRequest
import com.oracle.bmc.core.requests.GetSubnetRequest
import com.oracle.bmc.core.requests.LaunchInstanceRequest
import com.oracle.bmc.core.requests.ListBootVolumesRequest
import com.oracle.bmc.core.requests.ListInstancesRequest
import groovy.transform.CompileStatic
import org.apache.commons.codec.binary.Base64
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.kordamp.gradle.oci.tasks.AbstractOCITask
import org.kordamp.gradle.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.oci.tasks.traits.CompartmentIdAwareTrait
import org.kordamp.gradle.oci.tasks.traits.ImageAwareTrait
import org.kordamp.gradle.oci.tasks.traits.InstanceNameAwareTrait
import org.kordamp.gradle.oci.tasks.traits.PublicKeyFileAwareTrait
import org.kordamp.gradle.oci.tasks.traits.ShapeAwareTrait
import org.kordamp.gradle.oci.tasks.traits.SubnetIdAwareTrait
import org.kordamp.gradle.oci.tasks.traits.UserDataFileAwareTrait
import org.kordamp.gradle.oci.tasks.traits.VerboseAwareTrait
import org.kordamp.jipsy.TypeProviderFor

import static org.kordamp.gradle.StringUtils.isNotBlank
import static org.kordamp.gradle.oci.tasks.create.CreateInstanceConsoleConnectionTask.createInstanceConsoleConnection
import static org.kordamp.gradle.oci.tasks.get.GetInstancePublicIpTask.getInstancePublicIp
import static org.kordamp.gradle.oci.tasks.printers.BootVolumePrinter.printBootVolume
import static org.kordamp.gradle.oci.tasks.printers.InstancePrinter.printInstance

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class CreateInstanceTask extends AbstractOCITask implements CompartmentIdAwareTrait,
        SubnetIdAwareTrait,
        InstanceNameAwareTrait,
        ImageAwareTrait,
        ShapeAwareTrait,
        PublicKeyFileAwareTrait,
        UserDataFileAwareTrait,
        VerboseAwareTrait {
    static final String TASK_DESCRIPTION = 'Creates an Instance.'

    private final Property<String> createdInstanceId = project.objects.property(String)

    @Internal
    String getCreatedInstanceId() {
        return createdInstanceId.orNull
    }

    @Override
    void doExecuteTask() {
        validateCompartmentId()
        validateSubnetId()
        validateImage()
        validateShape()
        validatePublicKeyFile()

        ComputeClient computeClient = createComputeClient()

        Image _image = validateImage(computeClient, getCompartmentId())
        Shape _shape = validateShape(computeClient, getCompartmentId())

        String publicKeyFile = getPublicKeyFile()?.asFile?.text
        String userDataFile = getUserDataFile()?.asFile?.text
        String kmsKeyId = ''

        VirtualNetworkClient vcnClient = createVirtualNetworkClient()
        BlockstorageClient blockstorageClient = createBlockstorageClient()

        Subnet subnet = vcnClient.getSubnet(GetSubnetRequest.builder()
                .subnetId(getSubnetId())
                .build())
                .subnet

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
                isVerbose())
        createdInstanceId.set(instance.id)
    }

    static Instance maybeCreateInstance(OCITask owner,
                                        ComputeClient computeClient,
                                        VirtualNetworkClient vcnClient,
                                        BlockstorageClient blockstorageClient,
                                        String compartmentId,
                                        String instanceName,
                                        Image image,
                                        Shape shape,
                                        Subnet subnet,
                                        String publicKeyFile,
                                        String userDataFile,
                                        String kmsKeyId,
                                        boolean verbose) {
        Instance instance = doMaybeCreateInstance(owner,
                computeClient,
                vcnClient,
                compartmentId,
                instanceName,
                subnet.availabilityDomain,
                image.id,
                shape.shape,
                subnet.id,
                publicKeyFile,
                userDataFile,
                kmsKeyId,
                verbose)

        createInstanceConsoleConnection(owner,
                computeClient,
                instance.id,
                publicKeyFile,
                true,
                verbose)

        maybeCreateBootVolume(owner,
                blockstorageClient,
                compartmentId,
                subnet.availabilityDomain,
                instance.imageId,
                instance.displayName + '-boot-volume',
                kmsKeyId,
                verbose)

        instance
    }

    private static Instance doMaybeCreateInstance(OCITask owner,
                                                  ComputeClient client,
                                                  VirtualNetworkClient vcnClient,
                                                  String compartmentId,
                                                  String instanceName,
                                                  String availabilityDomain,
                                                  String imageId,
                                                  String shape,
                                                  String subnetId,
                                                  String publicKeyFile,
                                                  String userDataFile,
                                                  String kmsKeyId,
                                                  boolean verbose) {
        // 1. Check if it exists
        List<Instance> instances = client.listInstances(ListInstancesRequest.builder()
                .compartmentId(compartmentId)
                .availabilityDomain(availabilityDomain)
                .displayName(instanceName)
                .build())
                .items

        if (!instances.empty) {
            Instance instance = instances[0]
            println("Instance '${instanceName}' already exists. id = ${owner.console.yellow(instance.id)}")
            if (verbose) printInstance(owner, instance, 0)
            return instances[0]
        }

        Map<String, String> metadata = new HashMap<>('ssh_authorized_keys': publicKeyFile)
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

        Instance instance = client.launchInstance(LaunchInstanceRequest.builder()
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

        println("Waiting for Instance to be ${owner.state('Available')}")
        client.waiters.forInstance(GetInstanceRequest.builder()
                .instanceId(instance.id)
                .build(),
                Instance.LifecycleState.Running)
                .execute()

        println("Instance '${instanceName}' has been provisioned. id = ${owner.console.yellow(instance.id)}")
        if (verbose) printInstance(owner, instance, 0)

        Set<String> publicIps = getInstancePublicIp(owner,
                client,
                vcnClient,
                compartmentId,
                instance.id)

        owner.printCollection('Ip Addresses', publicIps, 0)

        instance
    }

    private static BootVolume maybeCreateBootVolume(OCITask owner,
                                                    BlockstorageClient client,
                                                    String compartmentId,
                                                    String availabilityDomain,
                                                    String imageId,
                                                    String displayName,
                                                    String kmsKeyId,
                                                    boolean verbose) {
        List<BootVolume> bootVolumes = client.listBootVolumes(ListBootVolumesRequest.builder()
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

        BootVolume bootVolume = client.createBootVolume(CreateBootVolumeRequest.builder()
                .createBootVolumeDetails(details.build())
                .build())
                .bootVolume

        println("Waiting for BootVolume to be ${owner.state('Available')}")
        client.waiters.forBootVolume(
                GetBootVolumeRequest.builder().bootVolumeId(bootVolumeId).build(),
                BootVolume.LifecycleState.Available)
                .execute()
                .bootVolume

        println("BootVolume '${bootVolume.displayName}' has been provisioned. id = ${owner.console.yellow(bootVolume.id)}")
        if (verbose) printBootVolume(owner, bootVolume, 0)
        bootVolume
    }
}
