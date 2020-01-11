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

import static com.oracle.bmc.OCID.isValid
import static org.kordamp.gradle.StringUtils.isBlank

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
trait SubnetIdAwareTrait implements PathAware, ProjectAware {
    private final StringState state = new StringState(project, 'OCI_SUBNET_ID', 'oci.subnet.id')

    @Internal
    Property<String> getSubnetId() {
        state.property
    }

    @Input
    Provider<String> getResolvedSubnetId() {
        state.provider
    }

    @Option(option = 'subnet-id', description = 'The id of the Subnet (REQUIRED).')
    void setSubnetId(String subnetId) {
        getSubnetId().set(subnetId)
    }

    void validateSubnetId() {
        if (isBlank(getResolvedSubnetId().orNull)) {
            throw new IllegalStateException("Missing value for 'subnetId' in $path")
        }
        if (!isValid(getResolvedSubnetId().get())) {
            throw new IllegalStateException("Subnet id '${getResolvedSubnetId().get()}' is invalid")
        }
    }
}
