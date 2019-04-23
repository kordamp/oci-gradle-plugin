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

import com.oracle.bmc.auth.AuthenticationDetailsProvider
import com.oracle.bmc.core.ComputeClient
import com.oracle.bmc.core.model.Instance
import com.oracle.bmc.core.requests.GetInstanceRequest
import com.oracle.bmc.core.requests.InstanceActionRequest
import com.oracle.bmc.core.requests.ListInstancesRequest
import groovy.transform.CompileStatic
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.api.tasks.options.OptionValues
import org.kordamp.gradle.oci.tasks.AbstractOCITask
import org.kordamp.gradle.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.oci.tasks.traits.CompartmentAwareTrait
import org.kordamp.gradle.oci.tasks.traits.InstanceIdAwareTrait
import org.kordamp.jipsy.TypeProviderFor

import static org.kordamp.gradle.StringUtils.isBlank
import static org.kordamp.gradle.StringUtils.isNotBlank

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class InstanceActionTask extends AbstractOCITask implements CompartmentAwareTrait, InstanceIdAwareTrait {
    static final String DESCRIPTION = 'Performs a given action on an instance.'

    private static enum InstanceAction {
        START(Instance.LifecycleState.Running),
        STOP(Instance.LifecycleState.Stopped),
        SOFTRESET(Instance.LifecycleState.Running),
        SOFTSTOP(Instance.LifecycleState.Stopped),
        RESET(Instance.LifecycleState.Running)

        private Instance.LifecycleState state

        InstanceAction(Instance.LifecycleState state) {
            this.state = state
        }

        Instance.LifecycleState state() {
            this.state
        }
    }

    private final Property<String> instanceName = project.objects.property(String)
    private final Property<InstanceAction> action = project.objects.property(InstanceAction)

    @Input
    @Option(option = 'action', description = 'The action to be executed (REQUIRED).')
    void setAction(InstanceAction action) {
        this.action.set(action)
    }

    InstanceAction getAction() {
        return action.orNull
    }

    @OptionValues("action")
    List<InstanceAction> getAvailableActions() {
        return new ArrayList<InstanceAction>(Arrays.asList(InstanceAction.values()))
    }

    @Optional
    @Input
    @Option(option = 'instance-name', description = 'The name of the instance (REQUIRED if instanceId = null).')
    void setInstanceName(String instanceName) {
        this.instanceName.set(instanceName)
    }

    String getInstanceName() {
        return instanceName.orNull
    }

    @TaskAction
    void executeTask() {
        if (isBlank(getInstanceId()) && isBlank(getInstanceName())) {
            throw new IllegalStateException("Missing value for either 'instanceId' or 'instanceName' in $path")
        }
        if (!getAction()) {
            throw new IllegalStateException("Missing value for 'action' in $path")
        }

        AuthenticationDetailsProvider provider = resolveAuthenticationDetailsProvider()
        ComputeClient computeClient = new ComputeClient(provider)

        if (isNotBlank(getInstanceId())) {
            println("Sending ${getAction().name()} to instance with id ${instanceId}")
            Instance instance = computeClient.instanceAction(InstanceActionRequest.builder()
                .instanceId(getInstanceId())
                .action(getAction().name())
                .build())
                .instance

            if (instance) {
                setInstanceName(instance.displayName)
                println("Waiting for instance to be ${getAction().state()}")

                computeClient.waiters
                    .forInstance(GetInstanceRequest.builder().instanceId(instance.id).build(),
                        getAction().state())
                    .execute()
            }
        } else {
            validateCompartmentId()

            computeClient.listInstances(ListInstancesRequest.builder()
                .compartmentId(compartmentId)
                .displayName(getInstanceName())
                .build())
                .items.each { instance ->
                setInstanceId(instance.id)

                println("Sending ${getAction().name()} to instance with id ${instance.id}")
                computeClient.instanceAction(InstanceActionRequest.builder()
                    .instanceId(getInstanceId())
                    .action(getAction().name())
                    .build())
                    .instance

                println("Waiting for instance to be ${getAction().state()}")

                computeClient.waiters
                    .forInstance(GetInstanceRequest.builder().instanceId(instance.id).build(),
                        getAction().state())
                    .execute()
            }
        }

        computeClient.close()
    }
}
