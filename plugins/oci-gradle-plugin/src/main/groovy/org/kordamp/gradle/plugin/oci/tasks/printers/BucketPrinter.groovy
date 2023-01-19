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
package org.kordamp.gradle.plugin.oci.tasks.printers

import com.oracle.bmc.objectstorage.model.Bucket
import groovy.transform.CompileStatic
import org.kordamp.gradle.plugin.oci.tasks.interfaces.ValuePrinter

/**
 * @author Andres Almiray
 * @since 0.3.0
 */
@CompileStatic
class BucketPrinter {
    static void printBucket(ValuePrinter printer, Bucket bucket, int offset) {
        printer.printKeyValue('Namespace', bucket.namespace, offset + 1)
        printer.printKeyValue('Compartment ID', bucket.compartmentId, offset + 1)
        printer.printKeyValue('Time Created', bucket.timeCreated, offset + 1)
        printer.printKeyValue('Created By', bucket.createdBy, offset + 1)
        printer.printKeyValue('Object Events Enabled', bucket.objectEventsEnabled, offset + 1)
        printer.printKeyValue('Kms Key ID', bucket.kmsKeyId, offset + 1)
        printer.printKeyValue('Storage Tier', bucket.storageTier, offset + 1)
        printer.printKeyValue('Public Access Type', bucket.publicAccessType, offset + 1)
        printer.printKeyValue('Etag', bucket.etag, offset + 1)
        printer.printKeyValue('Object Liofecycle Policy Etag', bucket.objectLifecyclePolicyEtag, offset + 1)
        printer.printKeyValue('Approximate Count', bucket.approximateCount, offset + 1)
        printer.printKeyValue('Approximate Size', bucket.approximateSize, offset + 1)
        printer.printMap('Defined Tags', bucket.definedTags, offset + 1)
        printer.printMap('Freeform Tags', bucket.freeformTags, offset + 1)
    }
}
