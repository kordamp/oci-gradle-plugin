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
import com.oracle.bmc.core.requests.GetVcnRequest
import com.oracle.bmc.core.requests.ListVcnsRequest
import groovy.transform.CompileStatic
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.kordamp.gradle.oci.tasks.AbstractOCITask
import org.kordamp.gradle.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.oci.tasks.traits.CompartmentIdAwareTrait
import org.kordamp.gradle.oci.tasks.traits.WaitForCompletionAwareTrait
import org.kordamp.jipsy.TypeProviderFor

import static org.kordamp.gradle.StringUtils.isBlank
import static org.kordamp.gradle.oci.tasks.printers.VcnPrinter.printVcn

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class CreateVcnTask extends AbstractOCITask implements CompartmentIdAwareTrait,
    WaitForCompletionAwareTrait {
    static final String TASK_DESCRIPTION = 'Creates a Vcn.'

    private final Property<String> vcnName = project.objects.property(String)
    private final Property<String> createdVcnId = project.objects.property(String)

    @Input
    @Option(option = 'vcn-name', description = 'The name of the Vcn to be created.')
    void setVcnName(String vcnName) {
        this.vcnName.set(vcnName)
    }

    String getVcnName() {
        return vcnName.orNull
    }

    String getCreatedVcnId() {
        return createdVcnId.orNull
    }

    @TaskAction
    void executeTask() {
        validateCompartmentId()

        if (isBlank(getVcnName())) {
            setVcnName('vcn-' + UUID.randomUUID().toString())
            project.logger.warn("Missing value for 'vcnName' in $path. Value set to ${vcnName}")
        }

        AuthenticationDetailsProvider provider = resolveAuthenticationDetailsProvider()
        VirtualNetworkClient client = new VirtualNetworkClient(provider)

        Vcn vcn = maybeCreateVcn(this,
            client,
            getCompartmentId(),
            getVcnName(),
            '10.0.0.0/16',
            isWaitForCompletion())
        createdVcnId.set(vcn.id)

        client.close()
    }

    static Vcn maybeCreateVcn(OCITask owner,
                              VirtualNetworkClient client,
                              String compartmentId,
                              String vcnName,
                              String cidrBlock,
                              boolean waitForCompletion) {
        // 1. Check if it exists
        List<Vcn> vcns = client.listVcns(ListVcnsRequest.builder()
            .compartmentId(compartmentId)
            .displayName(vcnName)
            .build())
            .items

        if (!vcns.empty) {
            Vcn vcn = vcns[0]
            println("Vcn '${vcnName}' already exists. id = ${vcn.id}")
            printVcn(owner, vcn, 0)
            return vcn
        }

        // 2. Create
        println('Provisioning Vcn. This may take a while.')
        Vcn vcn = client.createVcn(CreateVcnRequest.builder()
            .createVcnDetails(CreateVcnDetails.builder()
                .cidrBlock(cidrBlock)
                .compartmentId(compartmentId)
                .displayName(vcnName)

                .build())
            .build())
            .vcn

        if (waitForCompletion) {
            println("Waiting for Vcn to be Available")
            client.waiters
                .forVcn(GetVcnRequest.builder()
                    .vcnId(vcn.id)
                    .build(),
                    Vcn.LifecycleState.Available)
                .execute()
        }

        println("Vcn '${vcnName}' has been provisioned. id = ${vcn.id}")
        printVcn(owner, vcn, 0)
        vcn
    }
}
