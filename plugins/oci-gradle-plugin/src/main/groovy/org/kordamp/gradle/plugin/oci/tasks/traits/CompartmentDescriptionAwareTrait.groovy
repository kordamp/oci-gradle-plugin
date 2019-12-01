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
trait CompartmentDescriptionAwareTrait implements PathAware, ProjectAware {
    private final Property<String> compartmentDescription = stringProperty(
        'OCI_COMPARTMENT_DESCRIPTION', 'oci.compartment.description', project.objects.property(String))

    @Option(option = 'compartment-description', description = 'The Compartment description to use (REQUIRED).')
    void setCompartmentDescription(String compartmentDescription) {
        this.compartmentDescription.set(compartmentDescription)
    }

    @Input
    Property<String> getCompartmentDescription() {
        this.@compartmentDescription
    }

    void validateCompartmentDescription() {
        if (isBlank(getCompartmentDescription().orNull)) {
            throw new IllegalStateException("Missing value for 'compartmentDescription' in $path")
        }
    }
}
