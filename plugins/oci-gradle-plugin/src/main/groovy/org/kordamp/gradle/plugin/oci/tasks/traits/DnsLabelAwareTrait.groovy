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
import org.gradle.internal.hash.HashUtil
import org.kordamp.gradle.plugin.oci.tasks.interfaces.PathAware
import org.kordamp.gradle.plugin.oci.tasks.interfaces.ProjectAware
import org.kordamp.gradle.plugin.oci.tasks.traits.states.StringState

import static org.kordamp.gradle.StringUtils.isBlank

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
trait DnsLabelAwareTrait implements PathAware, ProjectAware {
    private final StringState state = new StringState(project, this, 'OCI_DNS_LABEL', 'oci.dns.label')

    @Internal
    Property<String> getDnsLabel() {
        state.property
    }

    @Input
    Provider<String> getResolvedDnsLabel() {
        state.provider
    }

    @Option(option = 'dns-label', description = 'The DNS label to use (REQUIRED).')
    void setDnsLabel(String dnsLabel) {
        getDnsLabel().set(normalizeDnsLabel(dnsLabel))
    }

    private String normalizeDnsLabel(String dnsLabel) {
        String label = dnsLabel?.replace('.', '')?.replace('-', '')
        if (label?.length() > 15) label = label?.substring(0, 14)
        label
    }

    void validateDnsLabel(String seed) {
        if (isBlank(getResolvedDnsLabel().orNull)) {
            setDnsLabel('dns' + HashUtil.sha1(seed.bytes).asHexString()[0..11])
            project.logger.warn("Missing value for 'dnsLabel' in $path. Value set to ${getResolvedDnsLabel().get()}")
        }
    }
}
