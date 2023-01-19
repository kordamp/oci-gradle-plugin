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

import com.oracle.bmc.database.model.AutonomousDatabase
import com.oracle.bmc.database.model.AutonomousDatabaseSummary
import groovy.transform.CompileStatic
import org.kordamp.gradle.plugin.oci.tasks.interfaces.ValuePrinter

/**
 * @author Andres Almiray
 * @since 0.5.0
 */
@CompileStatic
class AutonomousDatabasePrinter {
    static void printAutonomousDatabase(ValuePrinter printer, AutonomousDatabase database, int offset) {
        printer.printKeyValue('ID', database.id, offset + 1)
        printer.printKeyValue('Compartment ID', database.compartmentId, offset + 1)
        printer.printKeyValue('Display Name', database.displayName, offset + 1)
        printer.printKeyValue('Lifecycle State', printer.state(database.lifecycleState.name()), offset + 1)
        printer.printKeyValue('Time Created', database.timeCreated, offset + 1)
        printer.printKeyValue('Time Maintenance Begin', database.timeMaintenanceBegin, offset + 1)
        printer.printKeyValue('Time Maintenance End', database.timeMaintenanceEnd, offset + 1)
        printer.printKeyValue('Time Last Failover', database.timeOfLastFailover, offset + 1)
        printer.printKeyValue('Time Last Switchover', database.timeOfLastSwitchover, offset + 1)
        printer.printKeyValue('Db Name', database.dbName, offset + 1)
        printer.printKeyValue('Db Version', database.dbVersion, offset + 1)
        printer.printKeyValue('Db Workload', database.dbWorkload, offset + 1)
        printer.printKeyValue('CPU Core Count', database.cpuCoreCount, offset + 1)
        printer.printKeyValue('Preview', database.isPreview, offset + 1)
        printer.printKeyValue('Free Tier', database.isFreeTier, offset + 1)
        printer.printKeyValue('Dedicated', database.isDedicated, offset + 1)
        printer.printKeyValue('Auto Scaling Enabled', database.isAutoScalingEnabled, offset + 1)
        printer.printKeyValue('Data Guard Enabled', database.isDataGuardEnabled, offset + 1)
        printer.printKeyValue('Whitelisted IPS', database.whitelistedIps, offset + 1)
        printer.printKeyValue('Service Console URL', database.serviceConsoleUrl, offset + 1)
    }

    static void printAutonomousDatabase(ValuePrinter printer, AutonomousDatabaseSummary database, int offset) {
        printer.printKeyValue('ID', database.id, offset + 1)
        printer.printKeyValue('Compartment ID', database.compartmentId, offset + 1)
        printer.printKeyValue('Display Name', database.displayName, offset + 1)
        printer.printKeyValue('Lifecycle State', printer.state(database.lifecycleState.name()), offset + 1)
        printer.printKeyValue('Time Created', database.timeCreated, offset + 1)
        printer.printKeyValue('Time Maintenance Begin', database.timeMaintenanceBegin, offset + 1)
        printer.printKeyValue('Time Maintenance End', database.timeMaintenanceEnd, offset + 1)
        printer.printKeyValue('Time Last Failover', database.timeOfLastFailover, offset + 1)
        printer.printKeyValue('Time Last Switchover', database.timeOfLastSwitchover, offset + 1)
        printer.printKeyValue('Db Name', database.dbName, offset + 1)
        printer.printKeyValue('Db Version', database.dbVersion, offset + 1)
        printer.printKeyValue('Db Workload', database.dbWorkload, offset + 1)
        printer.printKeyValue('CPU Core Count', database.cpuCoreCount, offset + 1)
        printer.printKeyValue('Preview', database.isPreview, offset + 1)
        printer.printKeyValue('Free Tier', database.isFreeTier, offset + 1)
        printer.printKeyValue('Dedicated', database.isDedicated, offset + 1)
        printer.printKeyValue('Auto Scaling Enabled', database.isAutoScalingEnabled, offset + 1)
        printer.printKeyValue('Data Guard Enabled', database.isDataGuardEnabled, offset + 1)
        printer.printKeyValue('Whitelisted IPS', database.whitelistedIps, offset + 1)
        printer.printKeyValue('Service Console URL', database.serviceConsoleUrl, offset + 1)
    }
}
