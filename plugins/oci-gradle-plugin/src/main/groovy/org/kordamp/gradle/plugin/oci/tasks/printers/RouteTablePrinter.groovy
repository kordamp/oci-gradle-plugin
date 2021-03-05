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
package org.kordamp.gradle.plugin.oci.tasks.printers

import com.oracle.bmc.core.model.RouteRule
import com.oracle.bmc.core.model.RouteTable
import groovy.transform.CompileStatic
import org.kordamp.gradle.plugin.oci.tasks.interfaces.ValuePrinter

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
class RouteTablePrinter {
    static void printRouteTable(ValuePrinter printer, RouteTable routeTable, int offset) {
        printer.printKeyValue('ID', routeTable.id, offset + 1)
        printer.printKeyValue('Compartment ID', routeTable.compartmentId, offset + 1)
        printer.printKeyValue('VCN ID', routeTable.vcnId, offset + 1)
        printer.printKeyValue('Time Created', routeTable.timeCreated, offset + 1)
        printer.printKeyValue('Lifecycle State', printer.state(routeTable.lifecycleState.name()), offset + 1)
        printer.printMap('Defined Tags', routeTable.definedTags, offset + 1)
        printer.printMap('Freeform Tags', routeTable.freeformTags, offset + 1)
        if (!routeTable.routeRules.empty) {
            println(('    ' * (offset + 1)) + 'Routes:')
            routeTable.routeRules.each { RouteRule rule -> RouteRulePrinter.printRouteRule(printer, rule, offset + 1) }
        }
    }
}
