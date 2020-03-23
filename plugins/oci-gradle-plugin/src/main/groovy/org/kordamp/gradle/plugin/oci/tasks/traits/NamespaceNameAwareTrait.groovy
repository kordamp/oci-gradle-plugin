/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019-2020 Andres Almiray.
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
import org.kordamp.gradle.plugin.oci.tasks.interfaces.PathAware
import org.kordamp.gradle.plugin.oci.tasks.interfaces.ProjectAware
import org.kordamp.gradle.plugin.oci.tasks.traits.states.StringState

import static org.kordamp.gradle.StringUtils.isBlank

/**
 * @author Andres Almiray
 * @since 0.3.0
 */
@CompileStatic
trait NamespaceNameAwareTrait implements PathAware, ProjectAware {
    private final StringState state = new StringState(project, 'OCI_NAMESPACE_NAME', 'oci.namespace.name')

    @Internal
    Property<String> getNamespaceName() {
        state.property
    }

    @Input
    Provider<String> getResolvedNamespaceName() {
        state.provider
    }

    @Option(option = 'namespace-name', description = 'The name of the Namespace (REQUIRED).')
    void setNamespaceName(String namespaceName) {
        getNamespaceName().set(namespaceName)
    }

    void validateNamespaceName() {
        if (isBlank(getResolvedNamespaceName().orNull)) {
            throw new IllegalStateException("Missing value for 'namespaceName' in $path")
        }
    }
}