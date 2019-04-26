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

import com.oracle.bmc.core.model.BootVolume
import groovy.transform.CompileStatic
import org.kordamp.gradle.oci.tasks.interfaces.ValuePrinter

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
class BootVolumePrinter {
    static void printBootVolume(ValuePrinter printer, BootVolume bootVolume, int offset) {
        printer.printKeyValue('ID', bootVolume.id, offset + 1)
        printer.printKeyValue('Compartment ID', bootVolume.compartmentId, offset + 1)
        printer.printKeyValue('Image ID', bootVolume.imageId, offset + 1)
        printer.printKeyValue('Volume Group ID', bootVolume.volumeGroupId, offset + 1)
        printer.printKeyValue('Availability Domain', bootVolume.availabilityDomain, offset + 1)
        printer.printKeyValue('Size (GBs)', bootVolume.sizeInGBs, offset + 1)
        printer.printKeyValue('Size (MBs)', bootVolume.sizeInMBs, offset + 1)
        printer.printKeyValue('Hydrated', bootVolume.isHydrated, offset + 1)
        printer.printKeyValue('Source Details', bootVolume.sourceDetails, offset + 1)
        printer.printKeyValue('Time Created', bootVolume.timeCreated, offset + 1)
        printer.printKeyValue('Lifecycle State', bootVolume.lifecycleState, offset + 1)
        printer.printMap('Defined Tags', bootVolume.definedTags, offset + 1)
        printer.printMap('Freeform Tags', bootVolume.freeformTags, offset + 1)
    }
}
