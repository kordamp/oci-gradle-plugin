/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019-2022 Andres Almiray.
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
import com.oracle.bmc.core.model.RouteTable
import com.oracle.bmc.core.requests.ListRouteTablesRequest
import com.oracle.bmc.core.responses.ListRouteTablesResponse
import groovy.transform.CompileStatic
import org.kordamp.gradle.plugin.oci.tasks.AbstractOCITask
import org.kordamp.gradle.plugin.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.plugin.oci.tasks.traits.CompartmentIdAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.VcnIdAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.VerboseAwareTrait
import org.kordamp.jipsy.annotations.TypeProviderFor

import static org.kordamp.gradle.plugin.oci.tasks.printers.RouteTablePrinter.printRouteTable

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class ListRouteTablesTask extends AbstractOCITask implements CompartmentIdAwareTrait,
    VcnIdAwareTrait,
    VerboseAwareTrait {
    static final String TASK_DESCRIPTION = 'Lists RouteTables available on a Vcn.'

    @Override
    protected void doExecuteTask() {
        validateCompartmentId()
        validateVcnId()

        VirtualNetworkClient client = createVirtualNetworkClient()
        ListRouteTablesResponse response = client.listRouteTables(ListRouteTablesRequest.builder()
            .compartmentId(getResolvedCompartmentId().get())
            .vcnId(getResolvedVcnId().get())
            .build())

        println('Total RouteTables: ' + console.cyan(response.items.size().toString()))
        println(' ')
        for (RouteTable routeTable : response.items) {
            println(routeTable.displayName + (getResolvedVerbose().get() ? ':' : ''))
            if (getResolvedVerbose().get()) {
                printRouteTable(this, routeTable, 0)
            }
        }
    }
}
