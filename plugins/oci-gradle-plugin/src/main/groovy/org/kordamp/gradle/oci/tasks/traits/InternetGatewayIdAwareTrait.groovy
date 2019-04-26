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
trait InternetGatewayIdAwareTrait implements PathAware, ProjectAware {
    private final Property<String> internetGatewayId = project.objects.property(String)

    @Input
    @Option(option = 'internet-gateway-id', description = 'The id of the InternetGateway (REQUIRED).')
    void setInternetGatewayId(String internetGatewayId) {
        this.internetGatewayId.set(internetGatewayId)
    }

    String getInternetGatewayId() {
        internetGatewayId.orNull
    }

    void validateInternetGatewayId() {
        if (isBlank(getInternetGatewayId())) {
            throw new IllegalStateException("Missing value for 'internetGatewayId' in $path")
        }
        if (!OCID.isValid(getInternetGatewayId())) {
            throw new IllegalStateException("InternetGateway id '${internetGatewayId}' is invalid")
        }
    }
}
