/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019-2022 Andres Almiray.
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
package org.kordamp.gradle.plugin.oci.tasks

import com.google.common.base.Supplier
import com.oracle.bmc.ConfigFileReader
import com.oracle.bmc.Region
import com.oracle.bmc.apigateway.DeploymentClient
import com.oracle.bmc.apigateway.GatewayClient
import com.oracle.bmc.auth.AuthenticationDetailsProvider
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider
import com.oracle.bmc.core.BlockstorageClient
import com.oracle.bmc.core.ComputeClient
import com.oracle.bmc.core.VirtualNetworkClient
import com.oracle.bmc.database.DatabaseClient
import com.oracle.bmc.identity.IdentityClient
import com.oracle.bmc.objectstorage.ObjectStorageAsyncClient
import com.oracle.bmc.objectstorage.ObjectStorageClient
import com.oracle.bmc.resourcesearch.ResourceSearchClient
import groovy.transform.CompileStatic
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.kordamp.gradle.plugin.base.tasks.AbstractReportingTask
import org.kordamp.gradle.plugin.oci.OCIConfigExtension
import org.kordamp.gradle.plugin.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.util.AnsiConsole

import java.text.SimpleDateFormat

import static org.kordamp.gradle.PropertyUtils.stringProvider
import static org.kordamp.gradle.property.PropertyUtils.booleanProvider
import static org.kordamp.gradle.util.StringUtils.isNotBlank

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
abstract class AbstractOCITask extends AbstractReportingTask implements OCITask {
    protected static final String CONFIG_LOCATION = '~/.oci/config'
    protected static final String TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"

    protected final OCIConfigExtension ociConfig
    protected final List<AutoCloseable> closeables = []
    private AuthenticationDetailsProvider authenticationDetailsProvider

    AbstractOCITask() {
        ociConfig = extensions.create('ociConfig', OCIConfigExtension, project)
    }

    @Internal
    final Property<String> profile = project.objects.property(String)

    @Input
    @Optional
    final Provider<String> resolvedProfile = stringProvider(
        'OCI_PROFILE',
        'oci.profile',
        profile,
        project,
        this)
        .orElse('DEFAULT')

    @Internal
    final Property<String> region = project.objects.property(String)

    @Input
    @Optional
    final Provider<String> resolvedRegion = stringProvider(
        'OCI_REGION',
        'oci.region',
        region,
        project,
        this)

    @Internal
    final Property<Boolean> displayStreamLogs = project.objects.property(Boolean)

    @Input
    @Optional
    final Provider<Boolean> resolvedDisplayStreamLogs = booleanProvider(
        'OCI_DISPLAY_STREAM_LOGS',
        'oci.display.stream.logs',
        displayStreamLogs,
        project,
        this)

    @Option(option = 'oci-profile', description = 'The profile to use. Defaults to DEFAULT (OPTIONAL).')
    void setProfile(String profile) {
        this.profile.set(profile)
    }

    @Option(option = 'region', description = 'The region to use (OPTIONAL).')
    void setRegion(String region) {
        this.region.set(region)
    }

    @Option(option = 'display-stream-logs', description = 'Display extra stream log warnings (OPTIONAL).')
    void setDisplayStreamLogs(Boolean displayStreamLogs) {
        this.displayStreamLogs.set(displayStreamLogs)
    }

    @Internal
    @Override
    AnsiConsole getConsole() {
        this.@console
    }

    @TaskAction
    void executeTask() {
        System.setProperty('sun.net.http.allowRestrictedHeaders', 'true')
        System.setProperty('oci.javasdk.extra.stream.logs.enabled', resolvedDisplayStreamLogs.getOrElse(false).toString())

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
            ConfigFileReader.ConfigFile configFile = ConfigFileReader.parse(CONFIG_LOCATION, getResolvedProfile().get())
            ConfigFileAuthenticationDetailsProvider provider = new ConfigFileAuthenticationDetailsProvider(configFile)
            setRegion(provider.region.regionId)
            return provider
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
        if (map && !map.isEmpty()) {
            println(('    ' * offset) + key + ':')
            doPrintMap(map, offset + 1)
        }
    }

    @Override
    void printCollection(String key, Collection<?> collection, int offset) {
        if (collection && !collection.isEmpty()) {
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
                case 'InProgress':
                case 'Canceling':
                case 'Deleting':
                case 'Terminating':
                case 'Moving':
                    return console.yellow(state)
                case 'Available':
                case 'Running':
                case 'Active':
                case 'Completed':
                    return console.green(state)
                case 'Inactive':
                case 'Stopping':
                case 'Stopped':
                case 'Accepted':
                    return console.cyan(state)
                case 'Disabled':
                case 'Deleted':
                case 'Terminated':
                case 'Faulty':
                case 'Failed':
                case 'Canceled':
                    return console.red(state)
            }
        }
        state
    }

    static String format(Date date) {
        return new SimpleDateFormat(TIMESTAMP_FORMAT).format(date)
    }

    protected IdentityClient createIdentityClient() {
        IdentityClient client = new IdentityClient(resolveAuthenticationDetailsProvider())
        if (isNotBlank(getResolvedRegion().orNull)) {
            client.setRegion(getResolvedRegion().get())
        }
        closeables << client
        client
    }

    protected ComputeClient createComputeClient() {
        ComputeClient client = new ComputeClient(resolveAuthenticationDetailsProvider())
        if (isNotBlank(getResolvedRegion().orNull)) {
            client.setRegion(getResolvedRegion().get())
        }
        closeables << client
        client
    }

    protected VirtualNetworkClient createVirtualNetworkClient() {
        VirtualNetworkClient client = new VirtualNetworkClient(resolveAuthenticationDetailsProvider())
        if (isNotBlank(getResolvedRegion().orNull)) {
            client.setRegion(getResolvedRegion().get())
        }
        closeables << client
        client
    }

    protected BlockstorageClient createBlockstorageClient() {
        BlockstorageClient client = new BlockstorageClient(resolveAuthenticationDetailsProvider())
        if (isNotBlank(getResolvedRegion().orNull)) {
            client.setRegion(getResolvedRegion().get())
        }
        closeables << client
        client
    }

    protected ResourceSearchClient createResourceSearchClient() {
        ResourceSearchClient client = new ResourceSearchClient(resolveAuthenticationDetailsProvider())
        if (isNotBlank(getResolvedRegion().orNull)) {
            client.setRegion(getResolvedRegion().get())
        }
        closeables << client
        client
    }

    protected ObjectStorageClient createObjectStorageClient() {
        ObjectStorageClient client = new ObjectStorageClient(resolveAuthenticationDetailsProvider())
        if (isNotBlank(getResolvedRegion().orNull)) {
            client.setRegion(getResolvedRegion().get())
        }
        closeables << client
        client
    }

    protected ObjectStorageAsyncClient createObjectStorageAsyncClient() {
        ObjectStorageAsyncClient client = new ObjectStorageAsyncClient(resolveAuthenticationDetailsProvider())
        if (isNotBlank(getResolvedRegion().orNull)) {
            client.setRegion(getResolvedRegion().get())
        }
        closeables << client
        client
    }

    protected DatabaseClient createDatabaseClient() {
        DatabaseClient client = new DatabaseClient(resolveAuthenticationDetailsProvider())
        if (isNotBlank(getResolvedRegion().orNull)) {
            client.setRegion(getResolvedRegion().get())
        }
        closeables << client
        client
    }

    protected GatewayClient createGatewayClient() {
        GatewayClient client = new GatewayClient(resolveAuthenticationDetailsProvider())
        if (isNotBlank(getResolvedRegion().orNull)) {
            client.setRegion(getResolvedRegion().get())
        }
        closeables << client
        client
    }

    protected DeploymentClient createDeploymentClient() {
        DeploymentClient client = new DeploymentClient(resolveAuthenticationDetailsProvider())
        if (isNotBlank(getResolvedRegion().orNull)) {
            client.setRegion(getResolvedRegion().get())
        }
        closeables << client
        client
    }
}
