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
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.options.Option
import org.kordamp.gradle.plugin.oci.tasks.interfaces.PathAware
import org.kordamp.gradle.plugin.oci.tasks.interfaces.ProjectAware

import static com.oracle.bmc.OCID.isValid
import static org.kordamp.gradle.PropertyUtils.stringProvider
import static org.kordamp.gradle.StringUtils.isBlank

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
trait InstanceIdAwareTrait implements PathAware, ProjectAware {
    @Internal
    final Property<String> instanceId = project.objects.property(String)

    @Input
    final Provider<String> resolvedInstanceId = stringProvider(
        'OCI_INSTANCE_ID',
        'oci.instance.id',
        instanceId,
        project)

    @Option(option = 'instance-id', description = 'The id of the Instance (REQUIRED).')
    void setInstanceId(String instanceId) {
        this.instanceId.set(instanceId)
    }

    void validateInstanceId() {
        if (isBlank(getResolvedInstanceId().orNull)) {
            throw new IllegalStateException("Missing value for 'instanceId' in $path")
        }
        if (!isValid(getResolvedInstanceId().get())) {
            throw new IllegalStateException("Instance id '${getResolvedInstanceId().get()}' is invalid")
        }
    }
}
