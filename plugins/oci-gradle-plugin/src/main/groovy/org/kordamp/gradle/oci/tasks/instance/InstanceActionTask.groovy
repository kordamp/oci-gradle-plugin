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

import com.oracle.bmc.core.ComputeClient
import com.oracle.bmc.core.model.Instance
import com.oracle.bmc.core.requests.GetInstanceRequest
import com.oracle.bmc.core.requests.InstanceActionRequest
import com.oracle.bmc.core.requests.ListInstancesRequest
import groovy.transform.CompileStatic
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.api.tasks.options.OptionValues
import org.kordamp.gradle.oci.tasks.AbstractOCITask
import org.kordamp.gradle.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.oci.tasks.traits.CompartmentIdAwareTrait
import org.kordamp.gradle.oci.tasks.traits.OptionalInstanceIdAwareTrait
import org.kordamp.gradle.oci.tasks.traits.OptionalInstanceNameAwareTrait
import org.kordamp.gradle.oci.tasks.traits.WaitForCompletionAwareTrait
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

    @TaskAction
    void executeTask() {
        validateInstanceId()

        if (isBlank(getInstanceId()) && isBlank(getInstanceName())) {
            throw new IllegalStateException("Missing value for either 'instanceId' or 'instanceName' in $path")
        }
        if (!getAction()) {
            throw new IllegalStateException("Missing value for 'action' in $path")
        }

        ComputeClient client = createComputeClient()

        if (isNotBlank(getInstanceId())) {
            Instance instance = instanceAction(client, getInstanceId(), getAction())
            if (instance) setInstanceName(instance.displayName)
        } else {
            validateCompartmentId()

            client.listInstances(ListInstancesRequest.builder()
                .compartmentId(compartmentId)
                .displayName(getInstanceName())
                .build())
                .items.each { instance ->
                setInstanceId(instance.id)
                instanceAction(client, instance.id, getAction())
            }
        }

        client.close()
    }

    private Instance instanceAction(ComputeClient client, String instanceId, InstanceAction action) {
        println("Sending ${getAction().name()} to Instance with id ${instanceId}")
        Instance instance = client.instanceAction(InstanceActionRequest.builder()
            .instanceId(instanceId)
            .action(action.name())
            .build())
            .instance

        if (isWaitForCompletion()) {
            println("Waiting for Instance to be ${console.green(action.state().name())}")
            client.waiters
                .forInstance(GetInstanceRequest.builder()
                    .instanceId(instance.id).build(),
                    action.state())
                .execute()
        }

        instance
    }
}

