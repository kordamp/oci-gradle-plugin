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
package org.kordamp.gradle.oci.tasks.traits

import groovy.transform.CompileStatic
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.options.Option
import org.kordamp.gradle.oci.tasks.interfaces.PathAware
import org.kordamp.gradle.oci.tasks.interfaces.ProjectAware

import javax.annotation.PostConstruct

import static org.kordamp.gradle.StringUtils.isBlank

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
trait CompartmentAwareTrait implements PathAware, ProjectAware {
    private Property<String> compartmentId

    @PostConstruct
    void postInitialize() {
        compartmentId = project.objects.property(String)
    }

    @Input
    @Option(option = 'compartmentId', description = 'The id of the compartment to query (REQUIRED).')
    void setCompartmentId(String compartmentId) {
        this.compartmentId.set(compartmentId)
    }

    String getCompartmentId() {
        return compartmentId.orNull
    }

    void validateCompartmentId() {
        if (isBlank(getCompartmentId())) {
            throw new IllegalStateException("Missing value of 'compartmentId' in $path")
        }
    }
}
