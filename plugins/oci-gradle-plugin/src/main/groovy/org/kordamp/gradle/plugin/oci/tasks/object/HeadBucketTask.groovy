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
package org.kordamp.gradle.plugin.oci.tasks.object

import com.oracle.bmc.objectstorage.ObjectStorageClient
import com.oracle.bmc.objectstorage.requests.HeadBucketRequest
import com.oracle.bmc.objectstorage.responses.HeadBucketResponse
import groovy.transform.CompileStatic
import org.kordamp.gradle.plugin.oci.tasks.AbstractOCITask
import org.kordamp.gradle.plugin.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.plugin.oci.tasks.traits.BucketNameAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.NamespaceNameAwareTrait
import org.kordamp.jipsy.annotations.TypeProviderFor

/**
 * @author Andres Almiray
 * @since 0.3.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class HeadBucketTask extends AbstractOCITask implements NamespaceNameAwareTrait,
    BucketNameAwareTrait {
    static final String TASK_DESCRIPTION = 'Heads a specific Bucket.'

    @Override
    protected void doExecuteTask() {
        validateNamespaceName()
        validateBucketName()

        ObjectStorageClient client = createObjectStorageClient()
        HeadBucketResponse response = client.headBucket(HeadBucketRequest.builder()
            .namespaceName(getResolvedNamespaceName().get())
            .bucketName(getResolvedBucketName().get())
            .build())

        println(getResolvedBucketName().get() + ':')
        printKeyValue('ETag', response.ETag, 1)
        printKeyValue('Modified', !response.notModified, 1)
    }
}
