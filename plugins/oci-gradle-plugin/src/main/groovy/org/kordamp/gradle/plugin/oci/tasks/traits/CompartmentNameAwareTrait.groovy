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
trait CompartmentNameAwareTrait implements PathAware, ProjectAware {
    private final StringState state = SimpleStringState.of(project, this, 'oci.compartment.name')

    @Internal
    Property<String> getCompartmentName() {
        state.property
    }

    @Input
    Provider<String> getResolvedCompartmentName() {
        state.provider
    }

    @Option(option = 'compartment-name', description = 'The name of the Compartment (REQUIRED).')
    void setCompartmentName(String compartmentName) {
        getCompartmentName().set(compartmentName)
    }

    void validateCompartmentName() {
        if (isBlank(getResolvedCompartmentName().orNull)) {
            setCompartmentName('compartment-' + UUID.randomUUID().toString())
            project.logger.warn("Missing value for 'compartmentName' in $path. Value set to ${getResolvedCompartmentName().get()}")
        }
    }
}
