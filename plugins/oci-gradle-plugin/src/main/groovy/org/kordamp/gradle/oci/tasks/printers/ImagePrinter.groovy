/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019 Andres Almiray.
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
package org.kordamp.gradle.oci.tasks.printers

import com.oracle.bmc.core.model.Image
import groovy.transform.CompileStatic
import org.kordamp.gradle.oci.tasks.interfaces.ValuePrinter

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
class ImagePrinter {
    static void printImage(ValuePrinter printer, Image image, int offset) {
        printer.printKeyValue('Id', image.id, offset + 1)
        printer.printKeyValue('Compartment ID', image.compartmentId, offset + 1)
        printer.printKeyValue('Size (MBs)', image.sizeInMBs, offset + 1)
        printer.printKeyValue('Time Created', image.timeCreated, offset + 1)
        printer.printKeyValue('Operating System', image.operatingSystem, offset + 1)
        printer.printKeyValue('Operating System Version', image.operatingSystemVersion, offset + 1)
        printer.printKeyValue('Lifecycle State', printer.state(image.lifecycleState.name()), offset + 1)
        printer.printMap('Freeform Tags', image.freeformTags, offset + 1)
    }
}
