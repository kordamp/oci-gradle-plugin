/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019-2021 Andres Almiray.
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
import com.oracle.bmc.objectstorage.model.Bucket
import com.oracle.bmc.objectstorage.requests.GetBucketRequest
import groovy.transform.CompileStatic
import org.kordamp.gradle.plugin.oci.tasks.AbstractOCITask
import org.kordamp.gradle.plugin.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.plugin.oci.tasks.traits.BucketNameAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.NamespaceNameAwareTrait
import org.kordamp.jipsy.annotations.TypeProviderFor

import static org.kordamp.gradle.plugin.oci.tasks.printers.BucketPrinter.printBucket

/**
 * @author Andres Almiray
 * @since 0.3.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class GetBucketTask extends AbstractOCITask implements NamespaceNameAwareTrait,
    BucketNameAwareTrait {
    static final String TASK_DESCRIPTION = 'Displays information for a specific Bucket.'

    @Override
    protected void doExecuteTask() {
        validateNamespaceName()
        validateBucketName()

        List<GetBucketRequest.Fields> fields = new ArrayList<>(2)
        fields.add(GetBucketRequest.Fields.ApproximateCount)
        fields.add(GetBucketRequest.Fields.ApproximateSize)

        ObjectStorageClient client = createObjectStorageClient()
        Bucket bucket = client.getBucket(GetBucketRequest.builder()
            .namespaceName(getResolvedNamespaceName().get())
            .bucketName(getResolvedBucketName().get())
            .fields(fields)
            .build())
            .bucket

        println(bucket.name + ':')
        printBucket(this, bucket, 0)
    }
}
