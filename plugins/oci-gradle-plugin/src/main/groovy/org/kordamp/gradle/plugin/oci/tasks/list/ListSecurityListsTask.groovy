/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019-2021 Andres Almiray.
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
import com.oracle.bmc.core.model.SecurityList
import com.oracle.bmc.core.requests.ListSecurityListsRequest
import com.oracle.bmc.core.responses.ListSecurityListsResponse
import groovy.transform.CompileStatic
import org.kordamp.gradle.plugin.oci.tasks.AbstractOCITask
import org.kordamp.gradle.plugin.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.plugin.oci.tasks.traits.CompartmentIdAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.VcnIdAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.VerboseAwareTrait
import org.kordamp.jipsy.TypeProviderFor

import static org.kordamp.gradle.plugin.oci.tasks.printers.SecurityListPrinter.printSecurityList

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class ListSecurityListsTask extends AbstractOCITask implements CompartmentIdAwareTrait,
    VcnIdAwareTrait,
    VerboseAwareTrait {
    static final String TASK_DESCRIPTION = 'Lists SecurityLists available on a Vcn.'

    @Override
    protected void doExecuteTask() {
        validateCompartmentId()
        validateVcnId()

        VirtualNetworkClient client = createVirtualNetworkClient()
        ListSecurityListsResponse response = client.listSecurityLists(ListSecurityListsRequest.builder()
            .compartmentId(getResolvedCompartmentId().get())
            .vcnId(getResolvedVcnId().get())
            .build())

        println('Total SecurityLists: ' + console.cyan(response.items.size().toString()))
        println(' ')
        for (SecurityList securityList : response.items) {
            println(securityList.displayName + (getResolvedVerbose().get() ? ':' : ''))
            if (getResolvedVerbose().get()) {
                printSecurityList(this, securityList, 0)
            }
        }
    }
}
