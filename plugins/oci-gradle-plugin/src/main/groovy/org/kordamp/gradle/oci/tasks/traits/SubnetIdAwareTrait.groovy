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

import com.oracle.bmc.OCID
import groovy.transform.CompileStatic
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.options.Option
import org.kordamp.gradle.oci.tasks.interfaces.PathAware
import org.kordamp.gradle.oci.tasks.interfaces.ProjectAware

import static org.kordamp.gradle.StringUtils.isBlank

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
trait SubnetIdAwareTrait implements PathAware, ProjectAware {
    private final Property<String> subnetId = project.objects.property(String)

    @Input
    @Option(option = 'subnet-id', description = 'The id of the Subnet (REQUIRED).')
    void setSubnetId(String subnetId) {
        this.subnetId.set(subnetId)
    }

    String getSubnetId() {
        subnetId.orNull
    }

    void validateSubnetId() {
        if (isBlank(getSubnetId())) {
            throw new IllegalStateException("Missing value for 'subnetId' in $path")
        }
        if (!OCID.isValid(getSubnetId())) {
            throw new IllegalStateException("Subnet id '${subnetId}' is invalid")
        }
    }
}
