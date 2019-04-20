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
package org.kordamp.gradle.oci.tasks

import com.oracle.bmc.auth.AuthenticationDetailsProvider
import com.oracle.bmc.core.VirtualNetworkClient
import com.oracle.bmc.core.model.Vcn
import com.oracle.bmc.core.requests.ListVcnsRequest
import com.oracle.bmc.core.responses.ListVcnsResponse
import groovy.transform.CompileStatic
import org.gradle.api.tasks.TaskAction
import org.kordamp.gradle.AnsiConsole
import org.kordamp.gradle.oci.tasks.traits.CompartmentAwareTrait
import org.kordamp.gradle.oci.tasks.traits.VerboseAwareTrait

import static org.kordamp.gradle.StringUtils.isBlank

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
class ListVcnsTask extends AbstractOCITask implements CompartmentAwareTrait, VerboseAwareTrait {
    static final String NAME = 'listVcns'
    static final String DESCRIPTION = 'Lists vcns available on a compartment'

    @TaskAction
    void listVcns() {
        if (isBlank(compartmentId)) {
            throw new IllegalStateException("Missing value of 'compartmentId' in $path")
        }

        AuthenticationDetailsProvider provider = resolveAuthenticationDetailsProvider()
        VirtualNetworkClient client = new VirtualNetworkClient(provider)
        ListVcnsResponse response = client.listVcns(ListVcnsRequest.builder().compartmentId(compartmentId).build())
        client.close()

        AnsiConsole console = new AnsiConsole(project)
        println("Total vcns available at ${compartmentId}: " + console.cyan(response.items.size().toString()))
        println(' ')
        for (Vcn vcn : response.items) {
            println(vcn.displayName + (verbose ? ':' : ''))
            if (verbose) {
                doPrint(console, vcn, 0)
            }
        }
    }

    @Override
    protected void doPrint(AnsiConsole console, Object value, int offset) {
        if (value instanceof Vcn) {
            printVcnDetails(console, (Vcn) value, offset)
        } else {
            super.doPrint(console, value, offset)
        }
    }

    @Override
    protected void doPrintElement(AnsiConsole console, Object value, int offset) {
        if (value instanceof Vcn) {
            printVcnDetails(console, (Vcn) value, offset)
        } else {
            super.doPrintElement(console, value, offset)
        }
    }

    private void printVcnDetails(AnsiConsole console, Vcn vcn, int offset) {
        doPrintMapEntry(console, 'Id', vcn.id, offset + 1)
        doPrintMapEntry(console, 'CIDR Block', vcn.cidrBlock, offset + 1)
        doPrintMapEntry(console, 'DNS Label', vcn.dnsLabel, offset + 1)
        doPrintMapEntry(console, 'Time Created', vcn.timeCreated, offset + 1)
        doPrintMapEntry(console, 'VCN Domain Name', vcn.vcnDomainName, offset + 1)
    }
}
