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
import com.oracle.bmc.core.ComputeClient
import com.oracle.bmc.core.model.Image
import com.oracle.bmc.core.requests.ListImagesRequest
import com.oracle.bmc.core.responses.ListImagesResponse
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
class ListImagesTask extends AbstractOCITask implements CompartmentAwareTrait, VerboseAwareTrait {
    static final String NAME = 'listImages'
    static final String DESCRIPTION = 'Lists images available on a compartment'

    @TaskAction
    void listImages() {
        if (isBlank(compartmentId)) {
            throw new IllegalStateException("Missing value of 'compartmentId' in $path")
        }

        AuthenticationDetailsProvider provider = resolveAuthenticationDetailsProvider()
        ComputeClient client = ComputeClient.builder().build(provider)
        ListImagesResponse response = client.listImages(ListImagesRequest.builder().compartmentId(compartmentId).build())
        client.close()

        AnsiConsole console = new AnsiConsole(project)
        println("Total images available at ${compartmentId}: " + console.cyan(response.items.size().toString()))
        println(' ')
        for (Image image : response.items) {
            println(image.displayName + (verbose ? ':' : ''))
            if (verbose) {
                doPrint(console, image, 0)
            }
        }
    }

    @Override
    protected void doPrint(AnsiConsole console, Object value, int offset) {
        if (value instanceof Image) {
            printImageDetails(console, (Image) value, offset)
        } else {
            super.doPrint(console, value, offset)
        }
    }

    @Override
    protected void doPrintElement(AnsiConsole console, Object value, int offset) {
        if (value instanceof Image) {
            printImageDetails(console, (Image) value, offset)
        } else {
            super.doPrintElement(console, value, offset)
        }
    }

    private void printImageDetails(AnsiConsole console, Image image, int offset) {
        doPrintMapEntry(console, 'Id', image.id, offset + 1)
        doPrintMapEntry(console, 'Size (MBs)', image.sizeInMBs, offset + 1)
        doPrintMapEntry(console, 'Time Created', image.timeCreated, offset + 1)
        doPrintMapEntry(console, 'Operating System', image.operatingSystem, offset + 1)
        doPrintMapEntry(console, 'Operating System Version', image.operatingSystemVersion, offset + 1)
    }
}
