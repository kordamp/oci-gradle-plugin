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
package org.kordamp.gradle.oci.tasks.delete

import com.oracle.bmc.auth.AuthenticationDetailsProvider
import com.oracle.bmc.core.VirtualNetworkClient
import com.oracle.bmc.core.model.Vcn
import com.oracle.bmc.core.requests.DeleteVcnRequest
import com.oracle.bmc.core.requests.GetVcnRequest
import com.oracle.bmc.core.requests.ListVcnsRequest
import groovy.transform.CompileStatic
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.kordamp.gradle.oci.tasks.AbstractOCITask
import org.kordamp.gradle.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.oci.tasks.traits.CompartmentAwareTrait
import org.kordamp.gradle.oci.tasks.traits.VcnIdAwareTrait
import org.kordamp.jipsy.TypeProviderFor

import static org.kordamp.gradle.StringUtils.isBlank
import static org.kordamp.gradle.StringUtils.isNotBlank

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class DeleteVcnTask extends AbstractOCITask implements CompartmentAwareTrait, VcnIdAwareTrait {
    static final String DESCRIPTION = 'Deletes a VCN.'

    private final Property<String> vcnName = project.objects.property(String)

    @Optional
    @Input
    @Option(option = 'vcn-name', description = 'The name of the VCN (REQUIRED if vcnId = null).')
    void setVcnName(String vcnName) {
        this.vcnName.set(vcnName)
    }

    String getVcnName() {
        return vcnName.orNull
    }

    @TaskAction
    void executeTask() {
        if (isBlank(getVcnId()) && isBlank(getVcnName())) {
            throw new IllegalStateException("Missing value for either 'vcnId' or 'vncName' in $path")
        }

        AuthenticationDetailsProvider provider = resolveAuthenticationDetailsProvider()
        VirtualNetworkClient vcnClient = new VirtualNetworkClient(provider)

        if (isNotBlank(getVcnId())) {
            Vcn vcn = vcnClient.getVcn(GetVcnRequest.builder()
                .vcnId(getVcnId())
                .build())
                .vcn

            if (vcn) {
                setVcnName(vcn.displayName)
                println("Deleting VCN '${vcn.displayName}' with id = ${vcn.id}")
                vcnClient.deleteVcn(DeleteVcnRequest.builder()
                    .vcnId(getVcnId())
                    .build())

                vcnClient.waiters
                    .forVcn(GetVcnRequest.builder().vcnId(vcn.id).build(),
                        Vcn.LifecycleState.Terminated)
                    .execute()
            }
        } else {
            validateCompartmentId()

            vcnClient.listVcns(ListVcnsRequest.builder()
                .compartmentId(compartmentId)
                .displayName(getVcnName())
                .build())
                .items.each { vcn ->
                setVcnId(vcn.id)
                println("Deleting VCN '${vcn.displayName}' with id = ${vcn.id}")

                vcnClient.deleteVcn(DeleteVcnRequest.builder()
                    .vcnId(vcn.id)
                    .build())

                vcnClient.waiters
                    .forVcn(GetVcnRequest.builder().vcnId(vcn.id).build(),
                        Vcn.LifecycleState.Terminated)
                    .execute()
            }
        }

        vcnClient.close()
    }
}
