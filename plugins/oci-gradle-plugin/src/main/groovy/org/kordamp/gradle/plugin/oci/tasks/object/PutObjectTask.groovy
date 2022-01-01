/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019-2022 Andres Almiray.
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
import com.oracle.bmc.objectstorage.requests.PutObjectRequest
import com.oracle.bmc.objectstorage.responses.PutObjectResponse
import groovy.transform.CompileStatic
import org.kordamp.gradle.plugin.oci.tasks.AbstractOCITask
import org.kordamp.gradle.plugin.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.plugin.oci.tasks.traits.BucketNameAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.FileAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.NamespaceNameAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.ObjectNameAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.OptionalContentEncodingAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.OptionalContentLanguageAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.OptionalContentMD5AwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.OptionalContentTypeAwareTrait
import org.kordamp.jipsy.annotations.TypeProviderFor

import static org.kordamp.gradle.util.StringUtils.isNotBlank

/**
 * @author Andres Almiray
 * @since 0.3.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class PutObjectTask extends AbstractOCITask implements NamespaceNameAwareTrait,
    BucketNameAwareTrait,
    ObjectNameAwareTrait,
    FileAwareTrait,
    OptionalContentMD5AwareTrait,
    OptionalContentLanguageAwareTrait,
    OptionalContentEncodingAwareTrait,
    OptionalContentTypeAwareTrait {
    static final String TASK_DESCRIPTION = 'Puts an Object on a Bucket.'

    @Override
    protected void doExecuteTask() {
        validateNamespaceName()
        validateBucketName()
        validateObjectName()
        validateFile()

        File theFile = getResolvedFile().get().asFile
        PutObjectRequest.Builder builder = PutObjectRequest.builder()
            .namespaceName(getResolvedNamespaceName().get())
            .bucketName(getResolvedBucketName().get())
            .objectName(getResolvedObjectName().get())
            .contentLength(theFile.size())
            .putObjectBody(new FileInputStream(theFile))

        String s = getResolvedContentEncoding().get()
        if (isNotBlank(s)) {
            builder = builder.contentEncoding(s)
        }
        s = getResolvedContentLanguage().get()
        if (isNotBlank(s)) {
            builder = builder.contentLanguage(s)
        }
        s = getResolvedContentMD5().get()
        if (isNotBlank(s)) {
            builder = builder.contentMD5(s)
        }
        s = getResolvedContentType().get()
        if (isNotBlank(s)) {
            builder = builder.contentType(s)
        }

        ObjectStorageClient client = createObjectStorageClient()
        PutObjectResponse response = client.putObject(builder.build())

        println(getResolvedObjectName().get() + ':')
        printKeyValue('ETag', response.ETag, 1)
        printKeyValue('Last Modified', response.lastModified, 1)
        printKeyValue('Content MD5', response.opcContentMd5, 1)
    }
}
