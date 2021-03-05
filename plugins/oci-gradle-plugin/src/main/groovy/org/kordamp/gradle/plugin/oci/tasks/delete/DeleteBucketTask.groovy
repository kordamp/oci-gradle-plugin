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
package org.kordamp.gradle.plugin.oci.tasks.delete

import com.oracle.bmc.objectstorage.ObjectStorageClient
import com.oracle.bmc.objectstorage.requests.DeleteBucketRequest
import groovy.transform.CompileStatic
import org.kordamp.gradle.plugin.oci.tasks.AbstractOCITask
import org.kordamp.gradle.plugin.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.plugin.oci.tasks.traits.BucketNameAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.NamespaceNameAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.WaitForCompletionAwareTrait
import org.kordamp.jipsy.TypeProviderFor

/**
 * @author Andres Almiray
 * @since 0.3.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class DeleteBucketTask extends AbstractOCITask implements NamespaceNameAwareTrait,
    BucketNameAwareTrait,
    WaitForCompletionAwareTrait {
    static final String TASK_DESCRIPTION = 'Deletes a Bucket.'

    @Override
    protected void doExecuteTask() {
        validateNamespaceName()
        validateBucketName()

        ObjectStorageClient client = createObjectStorageClient()

        println("Deleting Bucket ${getResolvedBucketName().get()}")

        client.deleteBucket(DeleteBucketRequest.builder()
            .namespaceName(getResolvedNamespaceName().get())
            .bucketName(getResolvedBucketName().get())
            .build())
    }
}
