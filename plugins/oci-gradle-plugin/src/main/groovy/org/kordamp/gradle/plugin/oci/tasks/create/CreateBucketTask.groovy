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
package org.kordamp.gradle.plugin.oci.tasks.create

import com.oracle.bmc.model.BmcException
import com.oracle.bmc.objectstorage.ObjectStorage
import com.oracle.bmc.objectstorage.ObjectStorageClient
import com.oracle.bmc.objectstorage.model.Bucket
import com.oracle.bmc.objectstorage.model.CreateBucketDetails
import com.oracle.bmc.objectstorage.requests.CreateBucketRequest
import com.oracle.bmc.objectstorage.requests.GetBucketRequest
import groovy.transform.CompileStatic
import org.kordamp.gradle.plugin.oci.tasks.AbstractOCITask
import org.kordamp.gradle.plugin.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.plugin.oci.tasks.traits.BucketNameAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.CompartmentIdAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.NamespaceNameAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.VerboseAwareTrait
import org.kordamp.jipsy.TypeProviderFor

import static org.kordamp.gradle.plugin.oci.tasks.printers.BucketPrinter.printBucket

/**
 * @author Andres Almiray
 * @since 0.3.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class CreateBucketTask extends AbstractOCITask implements CompartmentIdAwareTrait,
    NamespaceNameAwareTrait,
    BucketNameAwareTrait,
    VerboseAwareTrait {
    static final String TASK_DESCRIPTION = 'Creates a Bucket.'

    @Override
    void doExecuteTask() {
        validateCompartmentId()
        validateNamespaceName()
        validateBucketName()

        ObjectStorageClient client = createObjectStorageClient()

        maybeCreateBucket(this,
            client,
            getResolvedCompartmentId().get(),
            getResolvedNamespaceName().get(),
            getResolvedBucketName().get(),
            getResolvedVerbose().get())
    }

    static Bucket maybeCreateBucket(OCITask owner,
                                    ObjectStorage client,
                                    String compartmentId,
                                    String namespaceName,
                                    String bucketName,
                                    boolean verbose) {
        // 1. Check if it exists
        try {
            List<GetBucketRequest.Fields> fields = new ArrayList<>(2)
            fields.add(GetBucketRequest.Fields.ApproximateCount)
            fields.add(GetBucketRequest.Fields.ApproximateSize)

            Bucket bucket = client.getBucket(GetBucketRequest.builder()
                .namespaceName(namespaceName)
                .bucketName(bucketName)
                .fields(fields)
                .build())
                .bucket
            println("Bucket '${bucketName}' already exists.")
            if (verbose) printBucket(owner, bucket, 0)
            return bucket
        } catch (BmcException e) {
            // exception most likely means the bucket does not exist, continue
        }

        // 2. Create
        println('Provisioning Bucket. This may take a while.')
        Bucket bucket = client.createBucket(CreateBucketRequest.builder()
            .namespaceName(namespaceName)
            .createBucketDetails(CreateBucketDetails.builder()
                .compartmentId(compartmentId)
                .name(bucketName)
                .build())
            .build())
            .bucket

        println("Bucket '${bucketName}' has been provisioned.")
        if (verbose) printBucket(owner, bucket, 0)
        bucket
    }
}
