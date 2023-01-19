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

import com.oracle.bmc.core.model.EgressSecurityRule
import com.oracle.bmc.core.model.IngressSecurityRule
import com.oracle.bmc.core.model.SecurityList
import groovy.transform.CompileStatic
import org.kordamp.gradle.plugin.oci.tasks.interfaces.ValuePrinter

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
class SecurityListPrinter {
    static void printSecurityList(ValuePrinter printer, SecurityList securityList, int offset) {
        printer.printKeyValue('ID', securityList.id, offset + 1)
        printer.printKeyValue('Compartment ID', securityList.compartmentId, offset + 1)
        printer.printKeyValue('VCN ID', securityList.vcnId, offset + 1)
        printer.printKeyValue('Time Created', securityList.timeCreated, offset + 1)
        printer.printKeyValue('Lifecycle State', printer.state(securityList.lifecycleState.name()), offset + 1)
        printer.printMap('Defined Tags', securityList.definedTags, offset + 1)
        printer.printMap('Freeform Tags', securityList.freeformTags, offset + 1)
        if (!securityList.ingressSecurityRules.empty) {
            int index = 0
            println(('    ' * (offset + 1)) + 'Ingress Security Rules:')
            securityList.ingressSecurityRules.each { IngressSecurityRule rule ->
                println(('    ' * (offset + 2)) + "Ingress Security Rule ${(index++).toString().padLeft(3, '0')}: ")
                IngressSecurityRulePrinter.printIngressSecurityRule(printer, rule, offset + 2)
            }
        }
        if (!securityList.egressSecurityRules.empty) {
            int index = 0
            println(('    ' * (offset + 1)) + 'Egress Security Rules:')
            securityList.egressSecurityRules.each { EgressSecurityRule rule ->
                println(('    ' * (offset + 2)) + "Egress Security Rule ${(index++).toString().padLeft(3, '0')}: ")
                EgressSecurityRulePrinter.printEgressSecurityRule(printer, rule, offset + 2)
            }
        }
    }
}
