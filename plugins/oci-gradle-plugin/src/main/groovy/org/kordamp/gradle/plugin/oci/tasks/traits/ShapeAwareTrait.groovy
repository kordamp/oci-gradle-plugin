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
package org.kordamp.gradle.plugin.oci.tasks.traits

import com.oracle.bmc.core.ComputeClient
import com.oracle.bmc.core.model.Shape
import com.oracle.bmc.core.requests.ListShapesRequest
import com.oracle.bmc.core.responses.ListShapesResponse
import groovy.transform.CompileStatic
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.options.Option
import org.kordamp.gradle.plugin.oci.tasks.interfaces.PathAware
import org.kordamp.gradle.plugin.oci.tasks.interfaces.ProjectAware

import static org.kordamp.gradle.PropertyUtils.stringProperty
import static org.kordamp.gradle.StringUtils.isBlank

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
trait ShapeAwareTrait implements PathAware, ProjectAware {
    private final Property<String> shape = stringProperty(
        'OCI_SHAPE', 'oci.shape', project.objects.property(String))

    @Option(option = 'shape', description = 'The Shape of the Instance (REQUIRED).')
    void setShape(String shape) {
        this.shape.set(shape)
    }

    @Input
    Property<String> getShape() {
        this.@shape
    }

    void validateShape() {
        if (isBlank(getShape().orNull)) {
            throw new IllegalStateException("Missing value for 'shape' in $path")
        }
    }

    Shape validateShape(ComputeClient client, String compartmentId) {
        ListShapesResponse response = client.listShapes(ListShapesRequest.builder()
            .compartmentId(compartmentId)
            .build())
        Shape shape = response.items.find { Shape sh -> sh.shape == getShape().get() }
        if (!shape) throw new IllegalStateException("Invalid shape ${getShape().get()}")
        shape
    }
}
