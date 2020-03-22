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
import org.kordamp.gradle.plugin.oci.tasks.interfaces.ProjectAware
import org.kordamp.gradle.plugin.oci.tasks.traits.states.BooleanState

/**
 * @author Andres Almiray
 * @since 0.4.0
 */
@CompileStatic
trait RegexAwareTrait implements ProjectAware {
    private final BooleanState state = new BooleanState(project, 'OCI_REGEX', 'oci.regex')

    @Internal
    Property<Boolean> getRegex() {
        state.property
    }

    @Input
    Provider<Boolean> getResolvedRegex() {
        state.provider
    }

    @Option(option = 'regex', description = 'If input should be treated as regex (OPTIONAL).')
    void setRegex(boolean regex) {
        getRegex().set(regex)
    }
}
