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

import com.oracle.bmc.core.ComputeClient
import com.oracle.bmc.core.model.Instance
import com.oracle.bmc.core.requests.GetInstanceRequest
import com.oracle.bmc.core.requests.InstanceActionRequest
import com.oracle.bmc.core.requests.ListInstancesRequest
import groovy.transform.CompileStatic
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.options.Option
import org.gradle.api.tasks.options.OptionValues
import org.kordamp.gradle.plugin.oci.tasks.AbstractOCITask
import org.kordamp.gradle.plugin.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.plugin.oci.tasks.traits.CompartmentIdAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.OptionalInstanceIdAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.OptionalInstanceNameAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.WaitForCompletionAwareTrait
import org.kordamp.jipsy.TypeProviderFor

import static org.kordamp.gradle.StringUtils.isBlank
import static org.kordamp.gradle.StringUtils.isNotBlank

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class InstanceActionTask extends AbstractOCITask implements CompartmentIdAwareTrait,
    OptionalInstanceIdAwareTrait,
    OptionalInstanceNameAwareTrait,
    WaitForCompletionAwareTrait {
    static final String TASK_DESCRIPTION = 'Performs a given action on an Instance.'

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

    @Internal
    final Property<InstanceAction> action = project.objects.property(InstanceAction)

    @Option(option = 'action', description = 'The action to be executed (REQUIRED).')
    void setAction(InstanceAction action) {
        getAction().set(action)
    }

    @Input
    Provider<InstanceAction> getResolvedAction() {
        project.providers.provider {
            String value = System.getenv('OCI_INSTANCE_ACTION')
            if (isBlank(value)) value = System.getProperty('oci.instance.action')
            isNotBlank(value) ? InstanceAction.valueOf(value) : action.getOrElse(InstanceAction.STOP)
        }
    }

    @OptionValues("action")
    List<InstanceAction> getAvailableActions() {
        return new ArrayList<InstanceAction>(Arrays.asList(InstanceAction.values()))
    }

    @Override
    protected void doExecuteTask() {
        validateInstanceId()

        if (isBlank(getResolvedInstanceId().orNull) && isBlank(getResolvedInstanceName().orNull)) {
            throw new IllegalStateException("Missing value for either 'instanceId' or 'instanceName' in $path")
        }
        if (!getResolvedAction()) {
            throw new IllegalStateException("Missing value for 'action' in $path")
        }

        ComputeClient client = createComputeClient()

        if (isNotBlank(getResolvedInstanceId().orNull)) {
            instanceAction(client, getResolvedInstanceId().get(), getResolvedAction().get())
        } else {
            validateCompartmentId()

            client.listInstances(ListInstancesRequest.builder()
                .compartmentId(getResolvedCompartmentId().get())
                .displayName(getResolvedInstanceName().get())
                .build())
                .items.each { instance ->
                setInstanceId(instance.id)
                instanceAction(client, instance.id, getResolvedAction().get())
            }
        }
    }

    private Instance instanceAction(ComputeClient client, String instanceId, InstanceAction action) {
        println("Sending ${getResolvedAction().get().name()} to Instance with id ${console.yellow(instanceId)}")
        Instance instance = client.instanceAction(InstanceActionRequest.builder()
            .instanceId(instanceId)
            .action(action.name())
            .build())
            .instance

        if (getResolvedWaitForCompletion().get()) {
            println("Waiting for Instance to be ${state(action.state().name())}")
            client.waiters
                .forInstance(GetInstanceRequest.builder()
                    .instanceId(instance.id).build(),
                    action.state())
                .execute()
        }

        instance
    }
}

