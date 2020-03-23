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
 * @since 0.1.0
 */
@CompileStatic
trait SubnetNameAwareTrait implements PathAware, ProjectAware {
    private final StringState state = new StringState(project, this, 'OCI_SUBNET_NAME', 'oci.subnet.name')

    @Internal
    Property<String> getSubnetName() {
        state.property
    }

    @Input
    Provider<String> getResolvedSubnetName() {
        state.provider
    }

    @Option(option = 'subnet-name', description = 'The name of the Subnet (REQUIRED).')
    void setSubnetName(String subnetName) {
        getSubnetName().set(subnetName)
    }

    void validateSubnetName() {
        if (isBlank(getResolvedSubnetName().orNull)) {
            setSubnetName('subnet-' + UUID.randomUUID().toString())
            project.logger.warn("Missing value for 'subnetName' in $path. Value set to ${getResolvedSubnetName().get()}")
        }
    }
}
