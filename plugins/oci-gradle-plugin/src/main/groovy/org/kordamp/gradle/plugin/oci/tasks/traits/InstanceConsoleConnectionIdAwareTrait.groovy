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

import static com.oracle.bmc.OCID.isValid
import static org.kordamp.gradle.util.StringUtils.isBlank

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
trait InstanceConsoleConnectionIdAwareTrait implements PathAware, ProjectAware {
    private final StringState state = SimpleStringState.of(project, this, 'oci.instance.console.connection.id')

    @Internal
    Property<String> getInstanceConsoleConnectionId() {
        state.property
    }

    @Input
    Provider<String> getResolvedInstanceConsoleConnectionId() {
        state.provider
    }

    @Option(option = 'instance-console-connection-id', description = 'The id of the InstanceConsoleConnection (REQUIRED).')
    void setInstanceConsoleConnectionId(String connectionId) {
        getInstanceConsoleConnectionId().set(connectionId)
    }

    void validateInstanceConsoleConnectionId() {
        if (isBlank(getResolvedInstanceConsoleConnectionId().orNull)) {
            throw new IllegalStateException("Missing value for 'connectionId' in $path")
        }
        if (!isValid(getResolvedInstanceConsoleConnectionId().get())) {
            throw new IllegalStateException("InstanceConsoleConnection id '${getResolvedInstanceConsoleConnectionId().get()}' is invalid")
        }
    }
}
