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
package org.kordamp.gradle.plugin.oci.tasks.printers

import com.oracle.bmc.core.model.IngressSecurityRule
import groovy.transform.CompileStatic
import org.kordamp.gradle.plugin.oci.tasks.interfaces.ValuePrinter

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
class IngressSecurityRulePrinter {
    static void printIngressSecurityRule(ValuePrinter printer, IngressSecurityRule rule, int offset) {
        printer.printKeyValue('Source', rule.source, offset + 1)
        printer.printKeyValue('Source Type', rule.sourceType, offset + 1)
        printer.printKeyValue('Protocol', rule.protocol, offset + 1)
        printer.printKeyValue('Stateless', rule.isStateless, offset + 1)
        if (rule.icmpOptions) {
            println(('    ' * (offset + 1)) + 'ICMP Options:')
            IcmpOptionsPrinter.printIcmpOptions(printer, rule.icmpOptions, offset + 1)
        }
        if (rule.udpOptions) {
            println(('    ' * (offset + 1)) + 'UDP Options:')
            UdpOptionsPrinter.printUdpOptions(printer, rule.udpOptions, offset + 1)
        }
        if (rule.tcpOptions) {
            println(('    ' * (offset + 1)) + 'TCP Options:')
            TcpOptionsPrinter.printTcpOptions(printer, rule.tcpOptions, offset + 1)
        }
    }
}
