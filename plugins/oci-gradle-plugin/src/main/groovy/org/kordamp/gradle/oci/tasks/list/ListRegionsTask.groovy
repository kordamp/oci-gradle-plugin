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
package org.kordamp.gradle.oci.tasks.list

import com.oracle.bmc.auth.AuthenticationDetailsProvider
import com.oracle.bmc.identity.IdentityClient
import com.oracle.bmc.identity.model.Region
import com.oracle.bmc.identity.requests.ListRegionsRequest
import com.oracle.bmc.identity.responses.ListRegionsResponse
import groovy.transform.CompileStatic
import org.gradle.api.tasks.TaskAction
import org.kordamp.gradle.AnsiConsole
import org.kordamp.gradle.oci.tasks.AbstractOCITask
import org.kordamp.gradle.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.oci.tasks.traits.VerboseAwareTrait
import org.kordamp.jipsy.TypeProviderFor

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class ListRegionsTask extends AbstractOCITask implements VerboseAwareTrait {
    static final String DESCRIPTION = 'Lists available regions.'

    @TaskAction
    void executeTask() {
        AuthenticationDetailsProvider provider = resolveAuthenticationDetailsProvider()
        IdentityClient client = IdentityClient.builder().build(provider)
        ListRegionsResponse response = client.listRegions(ListRegionsRequest.builder()
            .build())
        client.close()

        AnsiConsole console = new AnsiConsole(project)
        println("Total regions: " + console.cyan(response.items.size().toString()))
        println(' ')
        for (Region region : response.items) {
            println(region.name + (verbose ? ':' : ''))
            if (verbose) {
                doPrint(console, region, 0)
            }
        }
    }

    @Override
    protected void doPrint(AnsiConsole console, Object value, int offset) {
        if (value instanceof Region) {
            printCompartmentDetails(console, (Region) value, offset)
        } else {
            super.doPrint(console, value, offset)
        }
    }

    @Override
    protected void doPrintElement(AnsiConsole console, Object value, int offset) {
        if (value instanceof Region) {
            printCompartmentDetails(console, (Region) value, offset)
        } else {
            super.doPrintElement(console, value, offset)
        }
    }

    private void printCompartmentDetails(AnsiConsole console, Region region, int offset) {
        doPrintMapEntry(console, 'Key', region.key, offset + 1)
    }
}
