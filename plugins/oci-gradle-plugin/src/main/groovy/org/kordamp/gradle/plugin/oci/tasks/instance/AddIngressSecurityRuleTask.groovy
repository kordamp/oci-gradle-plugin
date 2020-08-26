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
package org.kordamp.gradle.plugin.oci.tasks.instance

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
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.options.Option
import org.gradle.api.tasks.options.OptionValues
import org.kordamp.gradle.plugin.oci.tasks.AbstractOCITask
import org.kordamp.gradle.plugin.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.plugin.oci.tasks.printers.SecurityListPrinter
import org.kordamp.gradle.plugin.oci.tasks.traits.SecurityListIdAwareTrait
import org.kordamp.jipsy.TypeProviderFor

import java.util.regex.Matcher
import java.util.regex.Pattern

import static org.kordamp.gradle.PropertyUtils.resolveValue
import static org.kordamp.gradle.util.StringUtils.isBlank
import static org.kordamp.gradle.util.StringUtils.isNotBlank

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class AddIngressSecurityRuleTask extends AbstractOCITask implements SecurityListIdAwareTrait {
    static final String TASK_DESCRIPTION = 'Adds IngressSecurityRules to a SecurityList.'
    static final Pattern PORT_RANGE_PATTERN = ~/(\d{1,5})\-(\d{1,5})/

    static enum PortType {
        TCP, UDP
    }

    @Internal
    final Property<PortType> portType = project.objects.property(PortType)

    @Input
    @Optional
    final ListProperty<String> sourcePorts = project.objects.listProperty(String)

    @Input
    @Optional
    final ListProperty<String> destinationPorts = project.objects.listProperty(String)

    @Option(option = 'port-type', description = 'The port type to use. Defaults to TCP (OPTIONAL).')
    void setPortType(PortType portType) {
        getPortType().set(portType)
    }

    @Input
    @Optional
    Provider<PortType> getResolvedPortType() {
        project.providers.provider {
            String value = resolveValue('OCI_PORT_TYPE', 'oci.port.type', this, project.name)
            isNotBlank(value) ? PortType.valueOf(value) : portType.getOrElse(PortType.TCP)
        }
    }

    @OptionValues("portType")
    List<PortType> getAvailablePortTypes() {
        return new ArrayList<PortType>(Arrays.asList(PortType.values()))
    }

    @Option(option = 'source-port', description = 'The source port type to add. May be defined multiple times (REQUIRED).')
    void setSourcePort(List<String> sourcePorts) {
        for (String port : sourcePorts) {
            Matcher m = PORT_RANGE_PATTERN.matcher(port)
            if (m.matches()) {
                checkPort(m.group(1))
                checkPort(m.group(2))
            } else {
                checkPort(port)
            }
        }
        this.sourcePorts.addAll(sourcePorts)
    }

    void setSourcePort(String port) {
        checkPort(port)
        this.sourcePorts.add(port)
    }

    @Option(option = 'destination-port', description = 'The destination port type to add. May be defined multiple times (REQUIRED).')
    void setDestinationPort(List<String> destinationPorts) {
        for (String port : destinationPorts) {
            Matcher m = PORT_RANGE_PATTERN.matcher(port)
            if (m.matches()) {
                checkPort(m.group(1))
                checkPort(m.group(2))
            } else {
                checkPort(port)
            }
        }
        this.destinationPorts.addAll(destinationPorts)
    }

    void setDestinationPort(String port) {
        checkPort(port)
        this.destinationPorts.add(port)
    }

    private void checkPort(String port) {
        try {
            int p = port.toInteger()
            if (p < 1 || p > 65355) {
                throw new IllegalArgumentException("Port '$port' is out of range.")
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Port '$port' is not a valid integer")
        }
    }

    @Override
    protected void doExecuteTask() {
        validateSecurityListId()

        if (!getSourcePorts().present && !getDestinationPorts().present) {
            throw new IllegalStateException("No ports have been defined in $path")
        }

        VirtualNetworkClient client = createVirtualNetworkClient()

        SecurityList securityList = addIngressSecurityRules(this,
            client,
            getResolvedSecurityListId().get(),
            getResolvedPortType().get(),
            (List<String>) getSourcePorts().getOrElse([]),
            (List<String>) getDestinationPorts().getOrElse([]))

        SecurityListPrinter.printSecurityList(this, securityList, 0)
    }

    static SecurityList addIngressSecurityRules(OCITask owner,
                                                VirtualNetworkClient client,
                                                String securityListId,
                                                PortType portType,
                                                List<String> sourcePorts,
                                                List<String> destinationPorts) {
        SecurityList securityList = client.getSecurityList(GetSecurityListRequest.builder()
            .securityListId(securityListId)
            .build())
            .securityList

        List<IngressSecurityRule> rules = securityList.ingressSecurityRules
        if (!destinationPorts) { // source ports only
            for (String range : sourcePorts.sort(false)) {
                List<Integer> ports = extractRange(range)
                int min = ports[0]
                int max = ports[1]

                IngressSecurityRule.Builder builder = createIngressSecurityRuleBuilder()

                switch (portType) {
                    case PortType.TCP:
                        builder = builder.tcpOptions(TcpOptions.builder()
                            .sourcePortRange(PortRange.builder()
                                .min(min)
                                .max(max)
                                .build())
                            .build())
                        break
                    case PortType.UDP:
                        builder = builder.udpOptions(UdpOptions.builder()
                            .sourcePortRange(PortRange.builder()
                                .min(min)
                                .max(max)
                                .build())
                            .build())
                        break
                    default:
                        throw new IllegalStateException("Invalid port type '$portType'")
                }

                IngressSecurityRule rule = builder.build()
                if (!rules.contains(rule)) rules << rule
            }
        } else if (!sourcePorts) { // dest ports only
            for (String range : destinationPorts.sort(false)) {
                List<Integer> ports = extractRange(range)
                int min = ports[0]
                int max = ports[1]

                IngressSecurityRule.Builder builder = createIngressSecurityRuleBuilder()

                switch (portType) {
                    case PortType.TCP:
                        builder = builder.tcpOptions(TcpOptions.builder()
                            .destinationPortRange(PortRange.builder()
                                .min(min)
                                .max(max)
                                .build())
                            .build())
                        break
                    case PortType.UDP:
                        builder = builder.udpOptions(UdpOptions.builder()
                            .destinationPortRange(PortRange.builder()
                                .min(min)
                                .max(max)
                                .build())
                            .build())
                        break
                    default:
                        throw new IllegalStateException("Invalid port type '$portType'")
                }

                IngressSecurityRule rule = builder.build()
                if (!rules.contains(rule)) rules << rule
            }
        } else { // both
            List<List<String>> combinations = GroovyCollections.combinations(sourcePorts, destinationPorts)
            for (List<String> ranges : combinations) {
                List<Integer> ports = extractRange(ranges[0])
                int smin = ports[0]
                int smax = ports[1]
                ports = extractRange(ranges[1])
                int dmin = ports[0]
                int dmax = ports[1]

                IngressSecurityRule.Builder builder = createIngressSecurityRuleBuilder()

                switch (portType) {
                    case PortType.TCP:
                        builder = builder.tcpOptions(TcpOptions.builder()
                            .sourcePortRange(PortRange.builder()
                                .min(smin)
                                .max(smax)
                                .build())
                            .destinationPortRange(PortRange.builder()
                                .min(dmin)
                                .max(dmax)
                                .build())
                            .build())
                        break
                    case PortType.UDP:
                        builder = builder.udpOptions(UdpOptions.builder()
                            .sourcePortRange(PortRange.builder()
                                .min(smin)
                                .max(smax)
                                .build())
                            .destinationPortRange(PortRange.builder()
                                .min(dmin)
                                .max(dmax)
                                .build())
                            .build())
                        break
                    default:
                        throw new IllegalStateException("Invalid port type '$portType'")
                }

                IngressSecurityRule rule = builder.build()
                if (!rules.contains(rule)) rules << rule
            }
        }

        client.updateSecurityList(UpdateSecurityListRequest.builder()
            .securityListId(securityListId)
            .updateSecurityListDetails(UpdateSecurityListDetails.builder()
                .ingressSecurityRules(rules)
                .build())
            .build())
            .securityList
    }

    private static IngressSecurityRule.Builder createIngressSecurityRuleBuilder() {
        IngressSecurityRule.builder()
            .source('0.0.0.0/0')
            .sourceType(IngressSecurityRule.SourceType.CidrBlock)
            .isStateless(false)
            .protocol('6')
    }

    private static List<Integer> extractRange(String range) {
        int min, max = 0
        Matcher m = PORT_RANGE_PATTERN.matcher(range)
        if (m.matches()) {
            min = Integer.parseInt(m.group(1))
            max = Integer.parseInt(m.group(2))
        } else {
            min = max = Integer.parseInt(range)
        }
        [min, max]
    }
}
