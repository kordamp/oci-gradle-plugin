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
import org.kordamp.gradle.plugin.oci.tasks.traits.states.StringState

import static com.oracle.bmc.OCID.isValid
import static org.kordamp.gradle.StringUtils.isBlank

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
trait InternetGatewayIdAwareTrait implements PathAware, ProjectAware {
    private final StringState state = new StringState(project, 'OCI_INTERNET_GATEWAY_ID', 'oci.internet.gateway.id')

    @Internal
    Property<String> getInternetGatewayId() {
        state.property
    }

    @Input
    Provider<String> getResolvedInternetGatewayId() {
        state.provider
    }

    @Option(option = 'internet-gateway-id', description = 'The id of the InternetGateway (REQUIRED).')
    void setInternetGatewayId(String internetGatewayId) {
        getInternetGatewayId().set(internetGatewayId)
    }

    void validateInternetGatewayId() {
        if (isBlank(getResolvedInternetGatewayId().orNull)) {
            throw new IllegalStateException("Missing value for 'internetGatewayId' in $path")
        }
        if (!isValid(getResolvedInternetGatewayId().get())) {
            throw new IllegalStateException("InternetGateway id '${getResolvedInternetGatewayId().get()}' is invalid")
        }
    }
}
