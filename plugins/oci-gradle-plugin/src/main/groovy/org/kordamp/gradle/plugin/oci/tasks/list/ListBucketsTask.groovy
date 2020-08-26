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
import com.oracle.bmc.objectstorage.model.BucketSummary
import com.oracle.bmc.objectstorage.requests.ListBucketsRequest
import com.oracle.bmc.objectstorage.responses.ListBucketsResponse
import groovy.transform.CompileStatic
import org.kordamp.gradle.plugin.oci.tasks.AbstractOCITask
import org.kordamp.gradle.plugin.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.plugin.oci.tasks.traits.CompartmentIdAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.NamespaceNameAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.OptionalLimitAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.OptionalPageAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.VerboseAwareTrait
import org.kordamp.jipsy.TypeProviderFor

import static org.kordamp.gradle.util.StringUtils.isNotBlank
import static org.kordamp.gradle.plugin.oci.tasks.printers.BucketSummaryPrinter.printBucketSummary

/**
 * @author Andres Almiray
 * @since 0.3.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class ListBucketsTask extends AbstractOCITask implements CompartmentIdAwareTrait,
    NamespaceNameAwareTrait,
    OptionalPageAwareTrait,
    OptionalLimitAwareTrait,
    VerboseAwareTrait {
    static final String TASK_DESCRIPTION = 'Lists available Buckets.'

    @Override
    protected void doExecuteTask() {
        validateCompartmentId()
        validateNamespaceName()

        ObjectStorageClient client = createObjectStorageClient()
        ListBucketsRequest.Builder builder = ListBucketsRequest.builder()
            .compartmentId(getResolvedCompartmentId().get())
            .namespaceName(getResolvedNamespaceName().get())

        Integer limit = getResolvedLimit().get() ?: 1000
        if (null != limit) {
            builder = builder.limit(limit)
        }

        String page = getResolvedPage().orNull
        if (isNotBlank(page)) {
            builder = builder.page(page)
        }

        ListBucketsResponse response = client.listBuckets(builder.build())

        println('Total Buckets: ' + console.cyan(response.items.size().toString()))
        println(' ')
        for (BucketSummary bucketSummary : response.items) {
            println(bucketSummary.name + (getResolvedVerbose().get() ? ':' : ''))
            if (getResolvedVerbose().get()) {
                printBucketSummary(this, bucketSummary, 0)
            }
        }
    }
}
