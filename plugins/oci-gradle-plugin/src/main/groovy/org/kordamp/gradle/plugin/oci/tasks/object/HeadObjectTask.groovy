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


import com.oracle.bmc.objectstorage.ObjectStorageClient
import com.oracle.bmc.objectstorage.requests.HeadObjectRequest
import com.oracle.bmc.objectstorage.responses.HeadObjectResponse
import groovy.transform.CompileStatic
import org.kordamp.gradle.plugin.oci.tasks.AbstractOCITask
import org.kordamp.gradle.plugin.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.plugin.oci.tasks.traits.BucketNameAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.NamespaceNameAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.ObjectNameAwareTrait
import org.kordamp.jipsy.TypeProviderFor

/**
 * @author Andres Almiray
 * @since 0.3.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class HeadObjectTask extends AbstractOCITask implements NamespaceNameAwareTrait,
    BucketNameAwareTrait,
    ObjectNameAwareTrait {
    static final String TASK_DESCRIPTION = 'Heads a specific Object.'

    @Override
    protected void doExecuteTask() {
        validateNamespaceName()
        validateBucketName()
        validateObjectName()

        headObject(this,
            createObjectStorageClient(),
            getResolvedNamespaceName().get(),
            getResolvedBucketName().get(),
            getResolvedObjectName().get())
    }

    static HeadObjectResponse headObject(OCITask owner,
                                         ObjectStorageClient storageClient,
                                         String namespaceName,
                                         String bucketName,
                                         String objectName) {
        HeadObjectResponse response = storageClient.headObject(HeadObjectRequest.builder()
            .namespaceName(namespaceName)
            .bucketName(bucketName)
            .objectName(objectName)
            .build())

        println(objectName + ':')
        owner.printKeyValue('ETag', response.ETag, 1)
        owner.printKeyValue('Modified', !response.notModified, 1)
        owner.printKeyValue('Last Modified', response.lastModified, 1)
        owner.printKeyValue('Content Length', response.contentLength, 1)
        owner.printKeyValue('Content Type', response.contentType, 1)
        owner.printKeyValue('Content MD5', response.contentMd5, 1)
        owner.printKeyValue('Content Encoding', response.contentEncoding, 1)
        owner.printKeyValue('Content Language', response.contentLanguage, 1)
        owner.printKeyValue('Archival State', response.archivalState, 1)
        owner.printKeyValue('Time of Archival', response.timeOfArchival, 1)
    }
}
