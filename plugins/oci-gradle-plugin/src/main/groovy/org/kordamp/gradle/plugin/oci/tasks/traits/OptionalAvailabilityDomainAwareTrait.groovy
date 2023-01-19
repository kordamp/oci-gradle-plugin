/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019-2023 Andres Almiray.
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

import com.oracle.bmc.identity.IdentityClient
import com.oracle.bmc.identity.model.AvailabilityDomain
import com.oracle.bmc.identity.requests.ListAvailabilityDomainsRequest
import com.oracle.bmc.identity.responses.ListAvailabilityDomainsResponse
import groovy.transform.CompileStatic
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.options.Option
import org.kordamp.gradle.plugin.oci.tasks.interfaces.ProjectAware
import org.kordamp.gradle.property.PathAware
import org.kordamp.gradle.property.SimpleStringState
import org.kordamp.gradle.property.StringState

/**
 * @author Andres Almiray
 * @since 0.5.0
 */
@CompileStatic
trait OptionalAvailabilityDomainAwareTrait implements PathAware, ProjectAware {
    private final StringState state = SimpleStringState.of(project, this, 'oci.availability.domain')

    @Internal
    Property<String> getAvailabilityDomain() {
        state.property
    }

    @Input
    @Optional
    Provider<String> getResolvedAvailabilityDomain() {
        state.provider
    }

    @Option(option = 'availability-domain', description = 'The AvailabilityDomain (OPTIONAL).')
    void setAvailabilityDomain(String availabilityDomain) {
        getAvailabilityDomain().set(availabilityDomain)
    }

    AvailabilityDomain validateAvailabilityDomain(IdentityClient identityClient, String compartmentId) {
        ListAvailabilityDomainsResponse response = identityClient.listAvailabilityDomains(ListAvailabilityDomainsRequest.builder()
            .compartmentId(compartmentId)
            .build())
        AvailabilityDomain ad = response.items.find { AvailabilityDomain ad -> ad.name == getResolvedAvailabilityDomain().get() }
        if (!ad) throw new IllegalStateException("Invalid availability domain ${getResolvedAvailabilityDomain().get()}")
        ad
    }
}
