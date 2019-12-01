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
package org.kordamp.gradle.plugin.oci.tasks.list

import com.oracle.bmc.core.VirtualNetworkClient
import com.oracle.bmc.core.model.Vcn
import com.oracle.bmc.core.requests.ListVcnsRequest
import com.oracle.bmc.core.responses.ListVcnsResponse
import groovy.transform.CompileStatic
import org.kordamp.gradle.plugin.oci.tasks.AbstractOCITask
import org.kordamp.gradle.plugin.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.plugin.oci.tasks.traits.CompartmentIdAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.VerboseAwareTrait
import org.kordamp.jipsy.TypeProviderFor

import static org.kordamp.gradle.plugin.oci.tasks.printers.VcnPrinter.printVcn

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class ListVcnsTask extends AbstractOCITask implements CompartmentIdAwareTrait, VerboseAwareTrait {
    static final String TASK_DESCRIPTION = 'Lists Vcns available on a Compartment.'

    @Override
    protected void doExecuteTask() {
        validateCompartmentId()

        VirtualNetworkClient client = createVirtualNetworkClient()
        ListVcnsResponse response = client.listVcns(ListVcnsRequest.builder()
            .compartmentId(getCompartmentId().get())
            .build())

        println('Total Vcns: ' + console.cyan(response.items.size().toString()))
        println(' ')
        for (Vcn vcn : response.items) {
            println(vcn.displayName + (isVerbose().get() ? ':' : ''))
            if (isVerbose().get()) {
                printVcn(this, vcn, 0)
            }
        }
    }
}
