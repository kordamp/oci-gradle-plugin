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
package org.kordamp.gradle.plugin.oci.tasks.list

import com.oracle.bmc.objectstorage.ObjectStorageClient
import com.oracle.bmc.objectstorage.model.ObjectSummary
import com.oracle.bmc.objectstorage.requests.ListObjectsRequest
import com.oracle.bmc.objectstorage.responses.ListObjectsResponse
import groovy.transform.CompileStatic
import org.kordamp.gradle.plugin.oci.tasks.AbstractOCITask
import org.kordamp.gradle.plugin.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.plugin.oci.tasks.traits.BucketNameAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.NamespaceNameAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.OptionalDelimiterAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.OptionalEndAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.OptionalFieldsAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.OptionalLimitAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.OptionalPrefixAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.OptionalStartAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.VerboseAwareTrait
import org.kordamp.jipsy.TypeProviderFor

import static org.kordamp.gradle.StringUtils.isNotBlank
import static org.kordamp.gradle.plugin.oci.tasks.printers.ObjectSummaryPrinter.printObjectSummary

/**
 * @author Andres Almiray
 * @since 0.3.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class ListObjectsTask extends AbstractOCITask implements NamespaceNameAwareTrait,
    BucketNameAwareTrait,
    OptionalPrefixAwareTrait,
    OptionalDelimiterAwareTrait,
    OptionalStartAwareTrait,
    OptionalEndAwareTrait,
    OptionalLimitAwareTrait,
    OptionalFieldsAwareTrait,
    VerboseAwareTrait {
    static final String TASK_DESCRIPTION = 'Lists available Objects in a Bucket.'

    @Override
    protected void doExecuteTask() {
        validateNamespaceName()
        validateBucketName()

        ObjectStorageClient client = createObjectStorageClient()
        ListObjectsRequest.Builder builder = ListObjectsRequest.builder()
            .namespaceName(getResolvedNamespaceName().get())
            .bucketName(getResolvedBucketName().get())

        Integer limit = getResolvedLimit().get() ?: 1000
        if (null != limit) {
            builder = builder.limit(limit)
        }

        String s = getResolvedPrefix().orNull
        if (isNotBlank(s)) {
            builder = builder.prefix(s)
        }
        s = getResolvedDelimiter().orNull
        if (isNotBlank(s)) {
            builder = builder.delimiter(s)
        }
        s = getResolvedStart().orNull
        if (isNotBlank(s)) {
            builder = builder.start(s)
        }
        s = getResolvedEnd().orNull
        if (isNotBlank(s)) {
            builder = builder.end(s)
        }
        s = getResolvedFields().orNull
        if (isNotBlank(s)) {
            builder = builder.fields(s)
        }

        ListObjectsResponse response = client.listObjects(builder.build())

        println('Total Objects: ' + console.cyan(response.listObjects.objects.size().toString()))
        println(' ')
        for (ObjectSummary objectSummary : response.listObjects.objects) {
            println(objectSummary.name + (getResolvedVerbose().get() ? ':' : ''))
            if (getResolvedVerbose().get()) {
                printObjectSummary(this, objectSummary, 0)
            }
        }
    }
}
