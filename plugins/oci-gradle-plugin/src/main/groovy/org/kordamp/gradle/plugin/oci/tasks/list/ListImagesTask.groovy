/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019-2023 Andres Almiray.
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

import com.oracle.bmc.core.ComputeClient
import com.oracle.bmc.core.model.Image
import com.oracle.bmc.core.requests.ListImagesRequest
import com.oracle.bmc.core.responses.ListImagesResponse
import groovy.transform.CompileStatic
import org.kordamp.gradle.plugin.oci.tasks.AbstractOCITask
import org.kordamp.gradle.plugin.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.plugin.oci.tasks.traits.CompartmentIdAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.VerboseAwareTrait
import org.kordamp.jipsy.annotations.TypeProviderFor

import static org.kordamp.gradle.plugin.oci.tasks.printers.ImagePrinter.printImage

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class ListImagesTask extends AbstractOCITask implements CompartmentIdAwareTrait, VerboseAwareTrait {
    static final String TASK_DESCRIPTION = 'Lists Images available on a Compartment.'

    @Override
    protected void doExecuteTask() {
        validateCompartmentId()

        ComputeClient client = createComputeClient()
        ListImagesResponse response = client.listImages(ListImagesRequest.builder()
            .compartmentId(getResolvedCompartmentId().get())
            .build())

        println('Total Images: ' + console.cyan(response.items.size().toString()))
        println(' ')
        for (Image image : response.items) {
            println(image.displayName + (getResolvedVerbose().get() ? ':' : ''))
            if (getResolvedVerbose().get()) {
                printImage(this, image, 0)
            }
        }
    }
}
