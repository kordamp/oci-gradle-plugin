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

import com.oracle.bmc.core.model.Subnet
import groovy.transform.CompileStatic
import org.kordamp.gradle.plugin.oci.tasks.interfaces.ValuePrinter

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
class SubnetPrinter {
    static void printSubnet(ValuePrinter printer, Subnet subnet, int offset) {
        printer.printKeyValue('ID', subnet.id, offset + 1)
        printer.printKeyValue('Compartment ID', subnet.compartmentId, offset + 1)
        printer.printKeyValue('VCN ID', subnet.vcnId, offset + 1)
        printer.printKeyValue('Availability Domain', subnet.availabilityDomain, offset + 1)
        printer.printKeyValue('CIDR Block', subnet.cidrBlock, offset + 1)
        printer.printKeyValue('DNS Label', subnet.dnsLabel, offset + 1)
        printer.printKeyValue('Time Created', subnet.timeCreated, offset + 1)
        printer.printKeyValue('Lifecycle State', printer.state(subnet.lifecycleState.name()), offset + 1)
        printer.printMap('Defined Tags', subnet.definedTags, offset + 1)
        printer.printMap('Freeform Tags', subnet.freeformTags, offset + 1)
    }
}
