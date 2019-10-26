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
import com.oracle.bmc.core.BlockstorageClient
import com.oracle.bmc.core.ComputeClient
import com.oracle.bmc.core.VirtualNetworkClient
import com.oracle.bmc.identity.IdentityClient
import com.oracle.bmc.resourcesearch.ResourceSearchClient
import groovy.transform.CompileStatic
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.kordamp.gradle.AnsiConsole
import org.kordamp.gradle.oci.OCIConfigExtension
import org.kordamp.gradle.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.plugin.base.tasks.AbstractReportingTask

import static org.kordamp.gradle.PropertyUtils.stringProperty
import static org.kordamp.gradle.StringUtils.isNotBlank

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
abstract class AbstractOCITask extends AbstractReportingTask implements OCITask {
    protected static final String CONFIG_LOCATION = '~/.oci/config'

    protected final OCIConfigExtension ociConfig
    protected final List<AutoCloseable> closeables = []
    private AuthenticationDetailsProvider authenticationDetailsProvider

    private Property<String> profile = project.objects.property(String)
    private Property<String> region = project.objects.property(String)

    AbstractOCITask() {
        ociConfig = extensions.create('ociConfig', OCIConfigExtension, project)
    }

    @Option(option = 'profile', description = 'The profile to use. Defaults to DEFAULT (OPTIONAL).')
    void setProfile(String profile) {
        this.profile.set(profile)
    }

    @Input
    @Optional
    String getProfile() {
        stringProperty('OCI_PROFILE', 'oci.profile', this.@profile.getOrElse('DEFAULT'))
    }

    @Option(option = 'region', description = 'The region to use (OPTIONAL).')
    void setRegion(String region) {
        this.region.set(region)
    }

    @Input
    @Optional
    String getRegion() {
        stringProperty('OCI_REGION', 'oci.region', this.@region.orNull)
    }

    @Internal
    @Override
    AnsiConsole getConsole() {
        this.@console
    }

    @TaskAction
    void executeTask() {
        System.setProperty('sun.net.http.allowRestrictedHeaders', 'true')

        doExecuteTask()

        closeables.each { c -> c.close() }
        closeables.clear()
    }

    abstract protected void doExecuteTask()

    protected AuthenticationDetailsProvider resolveAuthenticationDetailsProvider() {
        if (authenticationDetailsProvider) {
            return authenticationDetailsProvider
        }

        if (ociConfig.empty) {
            ConfigFileReader.ConfigFile configFile = ConfigFileReader.parse(CONFIG_LOCATION, getProfile())
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

        authenticationDetailsProvider = SimpleAuthenticationDetailsProvider.builder()
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

        authenticationDetailsProvider
    }

    protected IdentityClient createIdentityClient() {
        IdentityClient client = new IdentityClient(resolveAuthenticationDetailsProvider())
        if (region.present && isNotBlank(getRegion())) {
            client.setRegion(getRegion())
        }
        closeables << client
        client
    }

    protected ComputeClient createComputeClient() {
        ComputeClient client = new ComputeClient(resolveAuthenticationDetailsProvider())
        if (region.present && isNotBlank(getRegion())) {
            client.setRegion(getRegion())
        }
        closeables << client
        client
    }

    protected VirtualNetworkClient createVirtualNetworkClient() {
        VirtualNetworkClient client = new VirtualNetworkClient(resolveAuthenticationDetailsProvider())
        if (region.present && isNotBlank(getRegion())) {
            client.setRegion(getRegion())
        }
        closeables << client
        client
    }

    protected BlockstorageClient createBlockstorageClient() {
        BlockstorageClient client = new BlockstorageClient(resolveAuthenticationDetailsProvider())
        if (region.present && isNotBlank(getRegion())) {
            client.setRegion(getRegion())
        }
        closeables << client
        client
    }

    protected ResourceSearchClient createResourceSearchClient() {
        ResourceSearchClient client = new ResourceSearchClient(resolveAuthenticationDetailsProvider())
        if (region.present && isNotBlank(getRegion())) {
            client.setRegion(getRegion())
        }
        closeables << client
        client
    }

    @Override
    protected void doPrintMapEntry(String key, value, int offset) {
        if (value instanceof CharSequence) {
            if (isNotBlank((String.valueOf(value)))) {
                super.doPrintMapEntry(key, value, offset)
            }
        } else {
            super.doPrintMapEntry(key, value, offset)
        }
    }

    @Override
    void printKeyValue(String key, Object value, int offset) {
        doPrintMapEntry(key, value, offset)
    }

    @Override
    void printMap(String key, Map<String, ?> map, int offset) {
        if (!map.isEmpty()) {
            println(('    ' * offset) + key + ':')
            doPrintMap(map, offset + 1)
        }
    }

    @Override
    void printCollection(String key, Collection<?> collection, int offset) {
        if (!collection.isEmpty()) {
            println(('    ' * offset) + key + ':')
            doPrintCollection(collection, offset + 1)
        }
    }

    @Override
    String state(String state) {
        if (isNotBlank(state)) {
            switch (state) {
                case 'Creating':
                case 'Provisioning':
                case 'Restoring':
                case 'Importing':
                case 'Exporting':
                case 'Starting':
                case 'CreatingImage':
                    return console.yellow(state)
                case 'Available':
                case 'Running':
                case 'Active':
                    return console.green(state)
                case 'Inactive':
                case 'Stopping':
                case 'Stopped':
                    return console.cyan(state)
                case 'Disabled':
                case 'Deleting':
                case 'Deleted':
                case 'Terminating':
                case 'Terminated':
                case 'Faulty':
                case 'Failed':
                    return console.red(state)
            }
        }
        state
    }
}
