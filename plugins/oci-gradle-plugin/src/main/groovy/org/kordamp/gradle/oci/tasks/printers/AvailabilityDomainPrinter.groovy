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

import com.oracle.bmc.identity.model.AvailabilityDomain
import org.kordamp.gradle.oci.tasks.interfaces.ValuePrinter

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
class AvailabilityDomainPrinter {
    static void printAvailabilityDomain(ValuePrinter printer, AvailabilityDomain domain, int offset) {
        printer.printKeyValue('ID', domain.id, offset + 1)
        printer.printKeyValue('Compartment ID', domain.compartmentId, offset + 1)
    }
}
