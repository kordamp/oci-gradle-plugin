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
import com.oracle.bmc.objectstorage.model.CopyObjectDetails
import com.oracle.bmc.objectstorage.model.WorkRequest
import com.oracle.bmc.objectstorage.model.WorkRequestLogEntry
import com.oracle.bmc.objectstorage.requests.CopyObjectRequest
import com.oracle.bmc.objectstorage.requests.GetWorkRequestRequest
import com.oracle.bmc.objectstorage.requests.ListWorkRequestLogsRequest
import com.oracle.bmc.objectstorage.responses.CopyObjectResponse
import groovy.transform.CompileStatic
import org.kordamp.gradle.plugin.oci.tasks.AbstractOCITask
import org.kordamp.gradle.plugin.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.plugin.oci.tasks.traits.BucketNameAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.DestinationObjectNameAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.NamespaceNameAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.ObjectNameAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.OptionalDestinationBucketNameAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.OptionalDestinationNamespaceNameAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.OptionalDestinationRegionAwareTrait
import org.kordamp.jipsy.TypeProviderFor

import static org.kordamp.gradle.plugin.oci.tasks.object.HeadObjectTask.headObject

/**
 * @author Andres Almiray
 * @since 0.3.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class CopyObjectTask extends AbstractOCITask implements NamespaceNameAwareTrait,
    BucketNameAwareTrait,
    ObjectNameAwareTrait,
    OptionalDestinationRegionAwareTrait,
    OptionalDestinationNamespaceNameAwareTrait,
    OptionalDestinationBucketNameAwareTrait,
    DestinationObjectNameAwareTrait {
    static final String TASK_DESCRIPTION = 'Copies an Object.'

    @Override
    protected void doExecuteTask() {
        validateNamespaceName()
        validateBucketName()
        validateObjectName()
        validateDestinationObjectName()

        // TODO: check if destination exists

        ObjectStorageClient client = createObjectStorageClient()

        String _destinationRegion = getResolvedDestinationRegion().orNull ?: getRegion().get()
        String _destinationNamespace = getResolvedDestinationNamespaceName().orNull ?: getResolvedNamespaceName().get()
        String _destinationBucket = getResolvedDestinationBucketName().orNull ?: getResolvedBucketName().get()

        CopyObjectDetails.Builder details = CopyObjectDetails.builder()
            .sourceObjectName(getResolvedObjectName().get())
            .destinationRegion(_destinationRegion)
            .destinationNamespace(_destinationNamespace)
            .destinationBucket(_destinationBucket)
            .destinationObjectName(getResolvedDestinationObjectName().get())

        CopyObjectResponse response = client.copyObject(CopyObjectRequest.builder()
            .namespaceName(getResolvedNamespaceName().get())
            .bucketName(getResolvedBucketName().get())
            .copyObjectDetails(details.build())
            .build())

        println("Waiting for copy to finish.")
        WorkRequest workRequest = client.waiters
            .forWorkRequest(GetWorkRequestRequest.builder()
                .workRequestId(response.getOpcWorkRequestId())
                .build())
            .execute().
            workRequest
        println("Work request is in ${state(workRequest.status.name())} state.")

        println("Verifying that the object has been copied.")
        switch (workRequest.status) {
            case WorkRequest.Status.Completed:
                client.setRegion(_destinationRegion)

                headObject(this,
                    createObjectStorageClient(),
                    _destinationNamespace,
                    _destinationBucket,
                    getResolvedDestinationObjectName().get())
                break
            default:
                List<WorkRequestLogEntry> entries = client.listWorkRequestLogs(ListWorkRequestLogsRequest.builder()
                    .workRequestId(workRequest.id)
                    .build())
                    .items
                for (WorkRequestLogEntry entry : entries) {
                    println("[${console.cyan(format(entry.timestamp))}] $entry.message")
                }
                break
        }
    }
}
