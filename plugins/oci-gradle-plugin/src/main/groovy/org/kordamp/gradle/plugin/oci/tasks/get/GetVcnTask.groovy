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
package org.kordamp.gradle.plugin.oci.tasks.get

import com.oracle.bmc.core.VirtualNetworkClient
import com.oracle.bmc.core.model.Vcn
import com.oracle.bmc.core.requests.GetVcnRequest
import groovy.transform.CompileStatic
import org.kordamp.gradle.plugin.oci.tasks.AbstractOCITask
import org.kordamp.gradle.plugin.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.plugin.oci.tasks.traits.VcnIdAwareTrait
import org.kordamp.jipsy.TypeProviderFor

import static org.kordamp.gradle.plugin.oci.tasks.printers.VcnPrinter.printVcn

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class GetVcnTask extends AbstractOCITask implements VcnIdAwareTrait {
    static final String TASK_DESCRIPTION = 'Displays information for an specific Vcn.'

    @Override
    protected void doExecuteTask() {
        validateVcnId()

        VirtualNetworkClient client = createVirtualNetworkClient()

        Vcn vcn = client.getVcn(GetVcnRequest.builder()
            .vcnId(getResolvedVcnId().get())
            .build())
            .vcn

        if (vcn) {
            println(vcn.displayName + ':')
            printVcn(this, vcn, 0)
        } else {
            println("Vcn with id ${getResolvedVcnId().get()} was not found")
        }
    }
}
