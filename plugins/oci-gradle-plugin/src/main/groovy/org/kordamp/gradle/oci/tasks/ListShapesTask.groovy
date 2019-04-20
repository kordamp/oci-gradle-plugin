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
import com.oracle.bmc.core.model.Shape
import com.oracle.bmc.core.requests.ListShapesRequest
import com.oracle.bmc.core.responses.ListShapesResponse
import groovy.transform.CompileStatic
import org.gradle.api.tasks.TaskAction
import org.kordamp.gradle.AnsiConsole
import org.kordamp.gradle.oci.tasks.traits.CompartmentAwareTrait

import static org.kordamp.gradle.StringUtils.isBlank

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
class ListShapesTask extends AbstractOCITask implements CompartmentAwareTrait {
    static final String NAME = 'listShapes'
    static final String DESCRIPTION = 'Lists shapes available on a compartment'

    @TaskAction
    void listShapes() {
        if (isBlank(compartmentId)) {
            throw new IllegalStateException("Missing value of 'compartmentId' in $path")
        }

        AuthenticationDetailsProvider provider = resolveAuthenticationDetailsProvider()
        ComputeClient client = ComputeClient.builder().build(provider)
        ListShapesResponse response = client.listShapes(ListShapesRequest.builder().compartmentId(compartmentId).build())
        client.close()

        AnsiConsole console = new AnsiConsole(project)
        List<Shape> shapes = response.items.unique().sort { it.shape }
        println("Total shapes available at ${compartmentId}: " + console.cyan(shapes.size().toString()))
        println(' ')
        for (Shape shape : shapes) {
            println(shape.shape)
        }
    }
}
