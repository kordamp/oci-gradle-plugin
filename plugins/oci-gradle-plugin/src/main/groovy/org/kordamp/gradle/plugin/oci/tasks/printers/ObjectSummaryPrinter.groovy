/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019-2020 Andres Almiray.
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

import com.oracle.bmc.objectstorage.model.ObjectSummary
import groovy.transform.CompileStatic
import org.kordamp.gradle.plugin.oci.tasks.interfaces.ValuePrinter

/**
 * @author Andres Almiray
 * @since 0.3.0
 */
@CompileStatic
class ObjectSummaryPrinter {
    static void printObjectSummary(ValuePrinter printer, ObjectSummary objectSummary, int offset) {
        printer.printKeyValue('Size', objectSummary.size, offset + 1)
        printer.printKeyValue('MD5', objectSummary.md5, offset + 1)
        printer.printKeyValue('Time Created', objectSummary.timeCreated, offset + 1)
    }
}
