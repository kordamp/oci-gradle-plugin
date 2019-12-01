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
package org.kordamp.gradle.plugin.oci.tasks.query

import com.oracle.bmc.resourcesearch.ResourceSearch
import com.oracle.bmc.resourcesearch.model.QueryableFieldDescription
import com.oracle.bmc.resourcesearch.model.ResourceType
import com.oracle.bmc.resourcesearch.requests.GetResourceTypeRequest
import com.oracle.bmc.resourcesearch.requests.ListResourceTypesRequest
import com.oracle.bmc.resourcesearch.responses.GetResourceTypeResponse
import com.oracle.bmc.resourcesearch.responses.ListResourceTypesResponse
import groovy.transform.CompileStatic
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.options.Option
import org.kordamp.gradle.plugin.oci.tasks.AbstractOCITask
import org.kordamp.gradle.plugin.oci.tasks.interfaces.OCITask
import org.kordamp.jipsy.TypeProviderFor

import static org.kordamp.gradle.PropertyUtils.stringProvider
import static org.kordamp.gradle.StringUtils.isBlank

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class SearchResourcesTask extends AbstractOCITask {
    static final String TASK_DESCRIPTION = 'Lists information on resource types.'

    @Internal
    final Property<String> resourceType = project.objects.property(String)

    @Input
    @Optional
    final Provider<String> resolvedResourceType = stringProvider(
        'OCI_RESOURCE_TYPE',
        'oci.resource.type',
        getResourceType(),
        project)

    @Option(option = 'resource-type', description = 'The type to search (OPTIONAL).')
    void setResourceType(String resourceType) {
        getResourceType().set(resourceType)
    }

    @Override
    protected void doExecuteTask() {
        ResourceSearch client = createResourceSearchClient()
        if (isBlank(getResolvedResourceType().orNull)) {
            listTypes(client)
        } else {
            getTypeDetails(client, getResolvedResourceType().get())
        }
    }

    private void listTypes(ResourceSearch client) {
        ListResourceTypesRequest request = ListResourceTypesRequest.builder().build()
        ListResourceTypesResponse response = client.listResourceTypes(request)

        println('Total resources: ' + console.cyan(response.items.size().toString()))
        println(' ')
        for (ResourceType resourceType : response.items) {
            println('Resource: ' + state(resourceType.name))
        }
    }

    private void getTypeDetails(ResourceSearch client, String typeName) {
        GetResourceTypeRequest request = GetResourceTypeRequest.builder().name(typeName).build()
        GetResourceTypeResponse response = client.getResourceType(request)

        println('Resource: ' + state(response.resourceType.name))
        println('fields:')
        doPrint(response.resourceType.fields, 0)
    }

    @Override
    protected void doPrint(Object value, int offset) {
        if (value instanceof QueryableFieldDescription) {
            doPrintQueryableFieldDescription((QueryableFieldDescription) value, offset)
        } else {
            super.doPrint(value, offset)
        }
    }

    @Override
    protected void doPrintElement(Object value, int offset) {
        if (value instanceof QueryableFieldDescription) {
            doPrintQueryableFieldDescription((QueryableFieldDescription) value, offset)
        } else {
            super.doPrintElement(value, offset)
        }
    }

    private void doPrintQueryableFieldDescription(QueryableFieldDescription desc, int offset) {
        println(('    ' * (offset + 1)) + desc.fieldName + ':')
        doPrintMapEntry('type', desc.fieldType, offset + 2)
        if (desc.objectProperties?.size()) {
            println(('    ' * (offset + 2)) + 'fields:')
            doPrint(desc.objectProperties, offset + 2)
        }
    }
}
