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
import com.oracle.bmc.core.model.CreateVcnDetails
import com.oracle.bmc.core.model.Vcn
import com.oracle.bmc.core.requests.CreateVcnRequest
import com.oracle.bmc.core.requests.ListVcnsRequest
import groovy.transform.CompileStatic
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.kordamp.gradle.oci.tasks.AbstractOCITask
import org.kordamp.gradle.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.oci.tasks.traits.CompartmentAwareTrait
import org.kordamp.jipsy.TypeProviderFor

import static org.kordamp.gradle.StringUtils.isBlank

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class CreateVcnTask extends AbstractOCITask implements CompartmentAwareTrait {
    static final String DESCRIPTION = 'Creates a VCN.'

    private final Property<String> vcnName = project.objects.property(String)
    private final Property<String> vcnId = project.objects.property(String)

    @Option(option = 'vcnName', description = 'The name of the VCN to be created.')
    void setVcnName(String vcnName) {
        this.vcnName.set(vcnName)
    }

    String getVcnName() {
        return vcnName.orNull
    }

    String getVcnId() {
        return vcnId.orNull
    }

    @TaskAction
    void executeTask() {
        validateCompartmentId()

        if (isBlank(getVcnName())) {
            setVcnName(UUID.randomUUID().toString())
            project.logger.warn("Missing value of 'vcnName' in $path. Value set to ${vcnName}")
        }

        AuthenticationDetailsProvider provider = resolveAuthenticationDetailsProvider()
        VirtualNetworkClient vcnClient = new VirtualNetworkClient(provider)

        // 1. Check if it exists
        List<Vcn> vcns = vcnClient.listVcns(ListVcnsRequest.builder()
            .compartmentId(compartmentId)
            .displayName(vcnName.get())
            .build())
            .items

        if (!vcns.empty) {
            vcnId.set(vcns[0].id)
            println("VCN '${vcnName}' already exists. id = ${vcnId}")
        } else {
            // 2. Create
            println('Provisioning VCN. This may take a while.')
            Vcn vcn = createVcn(vcnClient, compartmentId, getVcnName(), '10.0.0.0/16')
            vcnId.set(vcn.id)
            println("VCN '${vcnName}' is provisioned with id = ${vcn.id}")
        }

        vcnClient.close()
    }

    private Vcn createVcn(VirtualNetworkClient vcnClient,
                          String compartmentId,
                          String vcnName,
                          String cidrBlock) {
        vcnClient.createVcn(CreateVcnRequest.builder()
            .createVcnDetails(CreateVcnDetails.builder()
                .cidrBlock(cidrBlock)
                .compartmentId(compartmentId)
                .displayName(vcnName)
                .build())
            .build())
            .vcn
    }
}
