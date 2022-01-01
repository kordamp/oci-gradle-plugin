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
package org.kordamp.gradle.plugin.oci.tasks.traits

import com.oracle.bmc.core.ComputeClient
import com.oracle.bmc.core.model.Image
import com.oracle.bmc.core.requests.ListImagesRequest
import com.oracle.bmc.core.responses.ListImagesResponse
import groovy.transform.CompileStatic
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.options.Option
import org.kordamp.gradle.plugin.oci.tasks.interfaces.ProjectAware
import org.kordamp.gradle.property.PathAware
import org.kordamp.gradle.property.SimpleStringState
import org.kordamp.gradle.property.StringState

import static org.kordamp.gradle.util.StringUtils.isBlank

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
trait ImageAwareTrait implements PathAware, ProjectAware {
    private final StringState state = SimpleStringState.of(project, this, 'oci.image')

    @Internal
    Property<String> getImage() {
        state.property
    }

    @Input
    Provider<String> getResolvedImage() {
        state.provider
    }

    @Option(option = 'image', description = 'The Image of the Instance (REQUIRED).')
    void setImage(String image) {
        getImage().set(image)
    }

    void validateImage() {
        if (isBlank(getResolvedImage().orNull)) {
            throw new IllegalStateException("Missing value for 'image' in $path")
        }
    }

    Image validateImage(ComputeClient client, String compartmentId) {
        ListImagesResponse response = client.listImages(ListImagesRequest.builder()
            .compartmentId(compartmentId)
            .displayName(getResolvedImage().get())
            .build())
        Image image = response.items.find { Image img -> img.displayName == getResolvedImage().get() }
        if (!image) throw new IllegalStateException("Invalid image ${getResolvedImage().get()}")
        image
    }
}
