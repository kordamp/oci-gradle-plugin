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

import com.oracle.bmc.core.model.Instance
import groovy.transform.CompileStatic
import org.kordamp.gradle.oci.tasks.interfaces.ValuePrinter

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
class InstancePrinter {
    static void printInstance(ValuePrinter printer, Instance instance, int offset) {
        printer.printKeyValue('ID', instance.id, offset + 1)
        printer.printKeyValue('Compartment ID', instance.compartmentId, offset + 1)
        printer.printKeyValue('Region', instance.region, offset + 1)
        printer.printKeyValue('Availability Domain', instance.availabilityDomain, offset + 1)
        printer.printKeyValue('Image ID', instance.imageId, offset + 1)
        printer.printKeyValue('Shape', instance.shape, offset + 1)
        printer.printKeyValue('Time Created', instance.timeCreated, offset + 1)
        printer.printKeyValue('Lifecycle State', printer.state(instance.lifecycleState.name()), offset + 1)
        printer.printMap('Defined Tags', instance.definedTags, offset + 1)
        printer.printMap('Freeform Tags', instance.freeformTags, offset + 1)
        printer.printMap('Metadata', instance.metadata, offset + 1)
    }
}
