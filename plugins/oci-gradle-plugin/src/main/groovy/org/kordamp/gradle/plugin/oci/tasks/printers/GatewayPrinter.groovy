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

import com.oracle.bmc.apigateway.model.GatewaySummary
import groovy.transform.CompileStatic
import org.kordamp.gradle.plugin.oci.tasks.interfaces.ValuePrinter

/**
 * @author Andres Almiray
 * @since 0.5.0
 */
@CompileStatic
class GatewayPrinter {
    static void printGateway(ValuePrinter printer, GatewaySummary Gateway, int offset) {
        printer.printKeyValue('ID', Gateway.id, offset + 1)
        printer.printKeyValue('Compartment ID', Gateway.compartmentId, offset + 1)
        printer.printKeyValue('Subnet ID', Gateway.subnetId, offset + 1)
        printer.printKeyValue('Endpoint Type', Gateway.endpointType, offset + 1)
        printer.printKeyValue('Hostname', Gateway.hostname, offset + 1)
        printer.printKeyValue('Time Created', Gateway.timeCreated, offset + 1)
        printer.printKeyValue('Time Updated', Gateway.timeUpdated, offset + 1)
        printer.printKeyValue('Lifecycle State', printer.state(Gateway.lifecycleState.name()), offset + 1)
        printer.printMap('Defined Tags', Gateway.definedTags, offset + 1)
        printer.printMap('Freeform Tags', Gateway.freeformTags, offset + 1)
    }
}
