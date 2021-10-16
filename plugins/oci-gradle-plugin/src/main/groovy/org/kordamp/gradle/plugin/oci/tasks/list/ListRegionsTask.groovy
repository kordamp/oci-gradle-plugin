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

import com.oracle.bmc.identity.IdentityClient
import com.oracle.bmc.identity.model.Region
import com.oracle.bmc.identity.requests.ListRegionsRequest
import com.oracle.bmc.identity.responses.ListRegionsResponse
import groovy.transform.CompileStatic
import org.kordamp.gradle.plugin.oci.tasks.AbstractOCITask
import org.kordamp.gradle.plugin.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.plugin.oci.tasks.traits.VerboseAwareTrait
import org.kordamp.jipsy.annotations.TypeProviderFor

import static org.kordamp.gradle.plugin.oci.tasks.printers.RegionPrinter.printRegion

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class ListRegionsTask extends AbstractOCITask implements VerboseAwareTrait {
    static final String TASK_DESCRIPTION = 'Lists available Regions.'

    @Override
    protected void doExecuteTask() {
        IdentityClient client = createIdentityClient()
        ListRegionsResponse response = client.listRegions(ListRegionsRequest.builder()
            .build())

        println('Total Regions: ' + console.cyan(response.items.size().toString()))
        println(' ')
        for (Region region : response.items) {
            println(region.name + (getResolvedVerbose().get() ? ':' : ''))
            if (getResolvedVerbose().get()) {
                printRegion(this, region, 0)
            }
        }
    }
}
