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
package org.kordamp.gradle.plugin.oci.tasks.printers

import com.oracle.bmc.core.model.Vcn
import groovy.transform.CompileStatic
import org.kordamp.gradle.plugin.oci.tasks.interfaces.ValuePrinter

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
class VcnPrinter {
    static void printVcn(ValuePrinter printer, Vcn vcn, int offset) {
        printer.printKeyValue('ID', vcn.id, offset + 1)
        printer.printKeyValue('Compartment ID', vcn.compartmentId, offset + 1)
        printer.printKeyValue('CIDR Block', vcn.cidrBlock, offset + 1)
        printer.printKeyValue('DNS Label', vcn.dnsLabel, offset + 1)
        printer.printKeyValue('Time Created', vcn.timeCreated, offset + 1)
        printer.printKeyValue('VCN Domain Name', vcn.vcnDomainName, offset + 1)
        printer.printKeyValue('Lifecycle State', printer.state(vcn.lifecycleState.name()), offset + 1)
        printer.printMap('Defined Tags', vcn.definedTags, offset + 1)
        printer.printMap('Freeform Tags', vcn.freeformTags, offset + 1)
    }
}
