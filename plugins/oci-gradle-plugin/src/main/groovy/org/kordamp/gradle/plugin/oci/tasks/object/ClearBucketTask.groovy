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
package org.kordamp.gradle.plugin.oci.tasks.object

import com.oracle.bmc.objectstorage.ObjectStorageAsyncClient
import com.oracle.bmc.objectstorage.ObjectStorageClient
import com.oracle.bmc.objectstorage.model.Bucket
import com.oracle.bmc.objectstorage.model.ObjectSummary
import com.oracle.bmc.objectstorage.requests.DeleteObjectRequest
import com.oracle.bmc.objectstorage.requests.GetBucketRequest
import com.oracle.bmc.objectstorage.requests.ListObjectsRequest
import com.oracle.bmc.objectstorage.responses.DeleteObjectResponse
import com.oracle.bmc.responses.AsyncHandler
import groovy.transform.CompileStatic
import org.kordamp.gradle.plugin.oci.tasks.AbstractOCITask
import org.kordamp.gradle.plugin.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.plugin.oci.tasks.traits.AsyncAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.BucketNameAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.NamespaceNameAwareTrait
import org.kordamp.jipsy.annotations.TypeProviderFor

/**
 * @author Andres Almiray
 * @since 0.3.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class ClearBucketTask extends AbstractOCITask implements NamespaceNameAwareTrait,
    BucketNameAwareTrait,
    AsyncAwareTrait {
    static final String TASK_DESCRIPTION = 'Clears a Bucket.'

    @Override
    protected void doExecuteTask() {
        validateNamespaceName()
        validateBucketName()

        ObjectStorageClient client = createObjectStorageClient()

        String bucketName = getResolvedBucketName().get()
        String namespaceName = getResolvedNamespaceName().get()

        List<GetBucketRequest.Fields> fields = new ArrayList<>(2)
        fields.add(GetBucketRequest.Fields.ApproximateCount)
        fields.add(GetBucketRequest.Fields.ApproximateSize)

        Bucket bucket = client.getBucket(GetBucketRequest.builder()
            .namespaceName(namespaceName)
            .bucketName(bucketName)
            .fields(fields)
            .build())
            .bucket

        println("Bucket ${bucketName} has ${console.cyan(String.valueOf(bucket.approximateCount))} elements")
        if (bucket.approximateCount < 1) {
            return
        }

        ListObjectsRequest.Builder builder = ListObjectsRequest.builder()
            .namespaceName(namespaceName)
            .bucketName(bucketName)

        if (getResolvedAsync().get()) {
            ObjectStorageAsyncClient asyncClient = createObjectStorageAsyncClient()
            AsyncHandler<DeleteObjectRequest, DeleteObjectResponse> handler = new AsyncHandler<DeleteObjectRequest, DeleteObjectResponse>() {
                @Override
                void onSuccess(DeleteObjectRequest req, DeleteObjectResponse rep) {
                    println("Object ${console.yellow(req.objectName)} deleted ")
                }

                @Override
                void onError(DeleteObjectRequest req, Throwable error) {
                    println("Could not delete object ${console.yellow(req.objectName)}: ${error}")
                }
            }

            for (ObjectSummary objectSummary : client.listObjects(builder.build()).listObjects.objects) {
                asyncClient.deleteObject(DeleteObjectRequest.builder()
                    .namespaceName(namespaceName)
                    .bucketName(bucketName)
                    .objectName(objectSummary.name)
                    .build(),
                    handler)
            }
        } else {
            for (ObjectSummary objectSummary : client.listObjects(builder.build()).listObjects.objects) {
                try {
                    client.deleteObject(DeleteObjectRequest.builder()
                        .namespaceName(namespaceName)
                        .bucketName(bucketName)
                        .objectName(objectSummary.name)
                        .build())
                    println("Object ${console.yellow(objectSummary.name)} deleted ")
                } catch (Exception e) {
                    println("Could not delete object ${console.yellow(objectSummary.name)}: ${e}")
                }
            }
        }
    }
}
