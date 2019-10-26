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

import static com.oracle.bmc.OCID.isValid
import static org.kordamp.gradle.PropertyUtils.stringProperty
import static org.kordamp.gradle.StringUtils.isBlank

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
trait InstanceConsoleConnectionIdAwareTrait implements PathAware, ProjectAware {
    private final Property<String> instanceConsoleConnectionId = project.objects.property(String)

    @Option(option = 'instance-console-connection-id', description = 'The id of the InstanceConsoleConnection (REQUIRED).')
    void setInstanceConsoleConnectionId(String connectionId) {
        this.instanceConsoleConnectionId.set(connectionId)
    }

    @Input
    String getInstanceConsoleConnectionId() {
        stringProperty('OCI_INSTANCE_CONSOLE_CONNECTION_ID', 'oci.instance.console.connection.id', this.@instanceConsoleConnectionId.orNull)
    }

    void validateInstanceConsoleConnectionId() {
        if (isBlank(getInstanceConsoleConnectionId())) {
            throw new IllegalStateException("Missing value for 'connectionId' in $path")
        }
        if (!isValid(getInstanceConsoleConnectionId())) {
            throw new IllegalStateException("InstanceConsoleConnection id '${getInstanceConsoleConnectionId()}' is invalid")
        }
    }
}
