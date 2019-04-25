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

import static org.kordamp.gradle.StringUtils.isBlank

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
trait DnsLabelAwareTrait implements PathAware, ProjectAware {
    private final Property<String> dnsLabel = project.objects.property(String)

    @Input
    @Option(option = 'dns-label', description = 'The DNS label to use (REQUIRED).')
    void setDnsLabel(String dnsLabel) {
        this.dnsLabel.set(dnsLabel?.replace('.', '')?.replace('-', ''))
    }

    String getDnsLabel() {
        return dnsLabel.orNull
    }

    void validateDnsLabel() {
        if (isBlank(getDnsLabel())) {
            throw new IllegalStateException("Missing value for 'dnsLabel' in $path")
        }
    }
}
