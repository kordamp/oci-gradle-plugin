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
import com.oracle.bmc.objectstorage.requests.GetObjectRequest
import com.oracle.bmc.objectstorage.responses.GetObjectResponse
import groovy.transform.CompileStatic
import org.kordamp.gradle.plugin.oci.tasks.AbstractOCITask
import org.kordamp.gradle.plugin.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.plugin.oci.tasks.traits.BucketNameAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.DestinationDirAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.NamespaceNameAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.ObjectNameAwareTrait
import org.kordamp.jipsy.annotations.TypeProviderFor

import java.nio.file.Files
import java.nio.file.StandardCopyOption

import static org.kordamp.gradle.plugin.oci.tasks.printers.GetObjectResponsePrinter.printObject

/**
 * @author Andres Almiray
 * @since 0.3.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class DownloadObjectTask extends AbstractOCITask implements NamespaceNameAwareTrait,
    BucketNameAwareTrait,
    ObjectNameAwareTrait,
    DestinationDirAwareTrait {
    static final String TASK_DESCRIPTION = 'Downloads an Object to a specific location.'

    @Override
    protected void doExecuteTask() {
        validateNamespaceName()
        validateBucketName()
        validateObjectName()
        validateDestinationDir()

        ObjectStorageClient client = createObjectStorageClient()
        GetObjectResponse object = client.getObject(GetObjectRequest.builder()
            .namespaceName(getResolvedNamespaceName().get())
            .bucketName(getResolvedBucketName().get())
            .objectName(getResolvedObjectName().get())
            .build())

        println(getResolvedObjectName().get() + ':')
        printObject(this, object, 0)

        File destination = new File(getResolvedDestinationDir().get().asFile, getResolvedObjectName().get())
        println("Downloading to $destination.absolutePath")

        Files.copy(
            object.inputStream,
            destination.toPath(),
            StandardCopyOption.REPLACE_EXISTING)
    }
}
