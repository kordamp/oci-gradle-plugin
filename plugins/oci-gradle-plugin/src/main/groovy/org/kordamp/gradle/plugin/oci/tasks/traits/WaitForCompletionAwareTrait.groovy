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
 * @since 0.1.0
 */
@CompileStatic
trait WaitForCompletionAwareTrait implements ProjectAware {
    private final BooleanState state = new BooleanState(project, 'OCI_WAIT_FOR_COMPLETION', 'oci.wait.for.completion')

    @Internal
    Property<Boolean> getWaitForCompletion() {
        state.property
    }

    @Input
    Provider<Boolean> getResolvedWaitForCompletion() {
        state.provider
    }

    @Option(option = 'wait-for-completion', description = 'Wait for operation to finish.')
    void setWaitForCompletion(boolean waitForCompletion) {
        getWaitForCompletion().set(waitForCompletion)
    }
}