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

import com.oracle.bmc.identity.model.User
import groovy.transform.CompileStatic
import org.kordamp.gradle.plugin.oci.tasks.interfaces.ValuePrinter

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
class UserPrinter {
    static void printUser(ValuePrinter printer, User user, int offset) {
        printer.printKeyValue('ID', user.id, offset + 1)
        printer.printKeyValue('Compartment ID', user.compartmentId, offset + 1)
        printer.printKeyValue('Name', user.name, offset + 1)
        printer.printKeyValue('Description', user.description, offset + 1)
        printer.printKeyValue('Email', user.email, offset + 1)
        printer.printKeyValue('Time Created', user.timeCreated, offset + 1)
        printer.printKeyValue('Lifecycle State', printer.state(user.lifecycleState.name()), offset + 1)
        printer.printMap('Defined Tags', user.definedTags, offset + 1)
        printer.printMap('Freeform Tags', user.freeformTags, offset + 1)
    }
}
