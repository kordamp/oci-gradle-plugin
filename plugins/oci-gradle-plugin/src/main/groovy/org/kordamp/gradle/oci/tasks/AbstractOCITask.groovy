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
package org.kordamp.gradle.oci.tasks

import com.google.common.base.Supplier
import com.oracle.bmc.ConfigFileReader
import com.oracle.bmc.Region
import com.oracle.bmc.auth.AuthenticationDetailsProvider
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider
import groovy.transform.CompileStatic
import org.gradle.api.tasks.options.Option
import org.kordamp.gradle.AnsiConsole
import org.kordamp.gradle.oci.OCIConfigExtension
import org.kordamp.gradle.oci.tasks.interfaces.PathAware
import org.kordamp.gradle.plugin.base.tasks.AbstractReportingTask

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
abstract class AbstractOCITask extends AbstractReportingTask implements PathAware {
    protected static final String CONFIG_LOCATION = '~/.oci/config'

    protected final OCIConfigExtension ociConfig
    protected String profile = 'DEFAULT'

    AbstractOCITask() {
        ociConfig = extensions.create('ociConfig', OCIConfigExtension, project)
    }

    @Option(option = 'profile', description = 'The profile to use. Defaults to DEFAULT.')
    void setProfile(String profile) {
        this.profile = profile
    }

    protected AuthenticationDetailsProvider resolveAuthenticationDetailsProvider() {
        if (ociConfig.empty) {
            ConfigFileReader.ConfigFile configFile = ConfigFileReader.parse(CONFIG_LOCATION, profile)
            return new ConfigFileAuthenticationDetailsProvider(configFile)
        }

        List<String> errors = []
        if (!ociConfig.userId.present) {
            errors << "Missing value for 'ociConfig.userId' for Task $path".toString()
        }
        if (!ociConfig.tenantId.present) {
            errors << "Missing value for 'ociConfig.tenantId' for Task $path".toString()
        }
        if (!ociConfig.fingerprint.present) {
            errors << "Missing value for 'ociConfig.fingerprint' for Task $path".toString()
        }
        if (!ociConfig.region.present) {
            errors << "Missing value for 'ociConfig.region' for Task $path".toString()
        }
        if (!ociConfig.keyfile.present) {
            errors << "Missing value for 'ociConfig.keyfile' for Task $path".toString()
        }

        if (errors.size() > 0) {
            throw new IllegalStateException(errors.join('\n'))
        }

        SimpleAuthenticationDetailsProvider.builder()
            .userId(ociConfig.userId.get())
            .tenantId(ociConfig.tenantId.get())
            .fingerprint(ociConfig.fingerprint.get())
            .region(Region.fromRegionId(ociConfig.region.get()))
            .privateKeySupplier(new Supplier<InputStream>() {
                @Override
                InputStream get() {
                    new FileInputStream(ociConfig.keyfile.asFile.get())
                }
            })
            .passPhrase(ociConfig.passphrase.present ? ociConfig.passphrase.get() : '')
            .build()
    }

    @Override
    protected void doPrintMapEntry(AnsiConsole console, String key, value, int offset) {
        if (null != value) {
            super.doPrintMapEntry(console, key, value, offset)
        }
    }
}
