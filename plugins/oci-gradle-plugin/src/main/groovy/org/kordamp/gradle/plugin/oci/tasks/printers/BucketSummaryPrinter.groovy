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

import com.oracle.bmc.objectstorage.model.BucketSummary
import groovy.transform.CompileStatic
import org.kordamp.gradle.plugin.oci.tasks.interfaces.ValuePrinter

/**
 * @author Andres Almiray
 * @since 0.3.0
 */
@CompileStatic
class BucketSummaryPrinter {
    static void printBucketSummary(ValuePrinter printer, BucketSummary bucketSummary, int offset) {
        printer.printKeyValue('Namespace', bucketSummary.namespace, offset + 1)
        printer.printKeyValue('Compartment ID', bucketSummary.compartmentId, offset + 1)
        printer.printKeyValue('Time Created', bucketSummary.timeCreated, offset + 1)
        printer.printKeyValue('Created By', bucketSummary.createdBy, offset + 1)
        printer.printKeyValue('Etag', bucketSummary.etag, offset + 1)
        printer.printMap('Defined Tags', bucketSummary.definedTags, offset + 1)
        printer.printMap('Freeform Tags', bucketSummary.freeformTags, offset + 1)
    }
}
