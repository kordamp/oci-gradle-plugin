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
package org.kordamp.gradle.oci.tasks.instance

import com.oracle.bmc.core.VirtualNetworkClient
import com.oracle.bmc.core.model.IngressSecurityRule
import com.oracle.bmc.core.model.PortRange
import com.oracle.bmc.core.model.SecurityList
import com.oracle.bmc.core.model.TcpOptions
import com.oracle.bmc.core.model.UdpOptions
import com.oracle.bmc.core.model.UpdateSecurityListDetails
import com.oracle.bmc.core.requests.GetSecurityListRequest
import com.oracle.bmc.core.requests.UpdateSecurityListRequest
import groovy.transform.CompileStatic
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.api.tasks.options.OptionValues
import org.kordamp.gradle.oci.tasks.AbstractOCITask
import org.kordamp.gradle.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.oci.tasks.printers.SecurityListPrinter
import org.kordamp.gradle.oci.tasks.traits.SecurityListIdAwareTrait
import org.kordamp.jipsy.TypeProviderFor

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class AddIngressSecurityRuleTask extends AbstractOCITask implements SecurityListIdAwareTrait {
    static final String TASK_DESCRIPTION = 'Adds IngressSecurityRules to a SecurityList.'

    static enum PortType {
        TCP, UDP
    }

    private final Property<PortType> portType = project.objects.property(PortType)
    private final ListProperty<Integer> ports = project.objects.listProperty(Integer)

    @Optional
    @Input
    @Option(option = 'port-type', description = 'The port type to use. Defaults to TCP (OPTIONAL).')
    void setPortType(PortType portType) {
        this.portType.set(portType)
    }

    PortType getPortType() {
        return portType.getOrElse(PortType.TCP)
    }

    @OptionValues("portType")
    List<PortType> getAvailablePortTypes() {
        return new ArrayList<PortType>(Arrays.asList(PortType.values()))
    }

    @Input
    @Option(option = 'port', description = 'The port type to add. May be defined multiple times (REQUIRED).')
    void setPort(List<String> ports) {
        List<Integer> converted = []
        for (String port : ports) {
            try {
                int p = port.toInteger()
                if (p < 1 || p > 65355) {
                    throw new IllegalArgumentException("Port '$port' is out of range.")
                }
                converted << p
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Port '$port' is not a valid integer")
            }
        }
        this.ports.addAll(converted)
    }

    List<Integer> getPorts() {
        return ports.get()
    }

    @TaskAction
    void executeTask() {
        validateSecurityListId()

        if (getPorts().empty) {
            throw new IllegalStateException("No ports have been defined in $path")
        }

        VirtualNetworkClient client = createVirtualNetworkClient()

        SecurityList securityList = addIngressSecurityRules(this,
            client,
            getSecurityListId(),
            getPortType(),
            getPorts())

        SecurityListPrinter.printSecurityList(this, securityList, 0)

        client.close()
    }

    static SecurityList addIngressSecurityRules(OCITask owner,
                                                VirtualNetworkClient client,
                                                String securityListId,
                                                PortType portType,
                                                List<Integer> ports) {
        SecurityList securityList = client.getSecurityList(GetSecurityListRequest.builder()
            .securityListId(securityListId)
            .build())
            .securityList

        List<IngressSecurityRule> rules = securityList.ingressSecurityRules
        for (Integer port : ports.sort(false)) {
            IngressSecurityRule.Builder builder = IngressSecurityRule.builder()
                .source('0.0.0.0/0')
                .sourceType(IngressSecurityRule.SourceType.CidrBlock)
                .isStateless(false)
                .protocol('6')

            switch (portType) {
                case PortType.TCP:
                    builder = builder.tcpOptions(TcpOptions.builder()
                        .sourcePortRange(PortRange.builder()
                            .min(port)
                            .max(port)
                            .build())
                        .build())
                    break
                case PortType.UDP:
                    builder = builder.udpOptions(UdpOptions.builder()
                        .sourcePortRange(PortRange.builder()
                            .min(port)
                            .max(port)
                            .build())
                        .build())
                    break
                default:
                    throw new IllegalStateException("Invalid port type '$portType'")
            }

            rules << builder.build()
        }

        client.updateSecurityList(UpdateSecurityListRequest.builder()
            .securityListId(securityListId)
            .updateSecurityListDetails(UpdateSecurityListDetails.builder()
                .ingressSecurityRules(rules)
                .build())
            .build())
            .securityList
    }
}
