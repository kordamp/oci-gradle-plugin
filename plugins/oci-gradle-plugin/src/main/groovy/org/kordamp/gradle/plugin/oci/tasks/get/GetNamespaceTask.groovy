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
package org.kordamp.gradle.plugin.oci.tasks.get

import com.oracle.bmc.objectstorage.ObjectStorageClient
import com.oracle.bmc.objectstorage.requests.GetNamespaceRequest
import groovy.transform.CompileStatic
import org.kordamp.gradle.plugin.oci.tasks.AbstractOCITask
import org.kordamp.gradle.plugin.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.plugin.oci.tasks.traits.OptionalCompartmentIdAwareTrait
import org.kordamp.jipsy.annotations.TypeProviderFor

import static org.kordamp.gradle.util.StringUtils.isNotBlank

/**
 * @author Andres Almiray
 * @since 0.3.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class GetNamespaceTask extends AbstractOCITask implements OptionalCompartmentIdAwareTrait {
    static final String TASK_DESCRIPTION = 'Displays information for a specific Namespace.'

    @Override
    protected void doExecuteTask() {
        validateCompartmentId()

        GetNamespaceRequest.Builder builder = GetNamespaceRequest.builder()
        String compartmentId = getResolvedCompartmentId().orNull
        if (isNotBlank(compartmentId)) {
            builder = builder.compartmentId(compartmentId)
        }

        ObjectStorageClient client = createObjectStorageClient()
        String namespace = client.getNamespace(builder.build())
            .value

        println(namespace)
    }
}
