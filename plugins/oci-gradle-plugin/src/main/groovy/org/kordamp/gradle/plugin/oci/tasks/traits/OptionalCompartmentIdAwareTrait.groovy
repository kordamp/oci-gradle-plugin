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

import static com.oracle.bmc.OCID.isValid
import static org.kordamp.gradle.util.StringUtils.isNotBlank

/**
 * @author Andres Almiray
 * @since 0.3.0
 */
@CompileStatic
trait OptionalCompartmentIdAwareTrait implements PathAware, ProjectAware {
    private final StringState state = SimpleStringState.of(project, this, 'oci.compartment.id')

    @Internal
    Property<String> getCompartmentId() {
        state.property
    }

    @Input
    Provider<String> getResolvedCompartmentId() {
        state.provider
    }

    @Option(option = 'compartment-id', description = 'The id of the Compartment (REQUIRED).')
    void setCompartmentId(String compartmentId) {
        getCompartmentId().set(compartmentId)
    }


    void validateCompartmentId() {
        String compartmentId = getResolvedCompartmentId().orNull
        if (isNotBlank(compartmentId) && !isValid(compartmentId)) {
            throw new IllegalStateException("Compartment id '${compartmentId}' is invalid")
        }
    }
}
