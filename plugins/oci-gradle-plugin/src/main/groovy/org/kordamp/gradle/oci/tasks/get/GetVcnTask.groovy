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
package org.kordamp.gradle.oci.tasks.get

import com.oracle.bmc.auth.AuthenticationDetailsProvider
import com.oracle.bmc.core.VirtualNetworkClient
import com.oracle.bmc.core.model.Vcn
import com.oracle.bmc.core.requests.GetVcnRequest
import groovy.transform.CompileStatic
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.kordamp.gradle.AnsiConsole
import org.kordamp.gradle.oci.tasks.AbstractOCITask
import org.kordamp.gradle.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.oci.tasks.traits.CompartmentAwareTrait
import org.kordamp.gradle.oci.tasks.traits.VerboseAwareTrait
import org.kordamp.jipsy.TypeProviderFor

import static org.kordamp.gradle.StringUtils.isBlank

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class GetVcnTask extends AbstractOCITask implements CompartmentAwareTrait, VerboseAwareTrait {
    static final String DESCRIPTION = 'Displays information for an specific VCN.'

    private final Property<String> vcnId = project.objects.property(String)

    @Input
    @Option(option = 'vcnId', description = 'The id of the VCN to be queried (REQUIRED).')
    void setVcnId(String vcnId) {
        this.vcnId.set(vcnId)
    }

    String getVcnId() {
        return vcnId.orNull
    }

    @TaskAction
    void executeTask() {
        if (isBlank(getVcnId())) {
            throw new IllegalStateException("Missing value for 'vcnId' in $path")
        }

        AuthenticationDetailsProvider provider = resolveAuthenticationDetailsProvider()
        VirtualNetworkClient vcnClient = new VirtualNetworkClient(provider)

        Vcn vcn = vcnClient.getVcn(GetVcnRequest.builder()
            .vcnId(getVcnId())
            .build())
            .vcn

        if (vcn) {
            AnsiConsole console = new AnsiConsole(project)
            println(vcn.displayName + (verbose ? ':' : ''))
            if (verbose) {
                doPrint(console, vcn, 0)
            }
        } else {
            println("VCN with id ${vcnId} was not found in compartment ${compartmentId}")
        }

        vcnClient.close()
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
        doPrintMapEntry(console, 'ID', vcn.id, offset + 1)
        doPrintMapEntry(console, 'Compartment ID', vcn.compartmentId, offset + 1)
        doPrintMapEntry(console, 'CIDR Block', vcn.cidrBlock, offset + 1)
        doPrintMapEntry(console, 'DNS Label', vcn.dnsLabel, offset + 1)
        doPrintMapEntry(console, 'Time Created', vcn.timeCreated, offset + 1)
        doPrintMapEntry(console, 'VCN Domain Name', vcn.vcnDomainName, offset + 1)
    }
}
