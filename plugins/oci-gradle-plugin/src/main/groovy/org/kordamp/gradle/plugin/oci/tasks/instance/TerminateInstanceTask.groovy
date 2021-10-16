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
package org.kordamp.gradle.plugin.oci.tasks.instance

import com.oracle.bmc.core.ComputeClient
import com.oracle.bmc.core.model.Instance
import com.oracle.bmc.core.requests.GetInstanceRequest
import com.oracle.bmc.core.requests.ListInstancesRequest
import com.oracle.bmc.core.requests.TerminateInstanceRequest
import groovy.transform.CompileStatic
import org.kordamp.gradle.plugin.oci.tasks.AbstractOCITask
import org.kordamp.gradle.plugin.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.plugin.oci.tasks.traits.CompartmentIdAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.OptionalInstanceIdAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.OptionalInstanceNameAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.RegexAwareTrait
import org.kordamp.gradle.plugin.oci.tasks.traits.WaitForCompletionAwareTrait
import org.kordamp.jipsy.annotations.TypeProviderFor

import static org.kordamp.gradle.util.StringUtils.isBlank
import static org.kordamp.gradle.util.StringUtils.isNotBlank

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class TerminateInstanceTask extends AbstractOCITask implements CompartmentIdAwareTrait,
    OptionalInstanceIdAwareTrait,
    OptionalInstanceNameAwareTrait,
    RegexAwareTrait,
    WaitForCompletionAwareTrait {
    static final String TASK_DESCRIPTION = 'Terminates an Instance.'

    @Override
    protected void doExecuteTask() {
        validateInstanceId()

        if (isBlank(getResolvedInstanceId().orNull) && isBlank(getResolvedInstanceName().orNull)) {
            throw new IllegalStateException("Missing value for either 'instanceId' or 'instanceName' in $path")
        }

        ComputeClient client = createComputeClient()

        // TODO: check if instance exists
        // TODO: check is instance is in a 'deletable' state

        if (isNotBlank(getResolvedInstanceId().orNull)) {
            Instance instance = client.getInstance(GetInstanceRequest.builder()
                .instanceId(getResolvedInstanceId().get())
                .build())
                .instance

            if (instance) {
                setInstanceName(instance.displayName)
                terminateInstance(client, instance)
            }
        } else {
            validateCompartmentId()

            if (getResolvedRegex().getOrElse(false)) {
                client.listInstances(ListInstancesRequest.builder()
                    .compartmentId(getResolvedCompartmentId().get())
                    .displayName(getResolvedInstanceName().get())
                    .build())
                    .items.each { instance ->
                    setInstanceId(instance.id)
                    terminateInstance(client, instance)
                }
            } else {
                final String instanceNameRegex = getResolvedInstanceName().get()
                client.listInstances(ListInstancesRequest.builder()
                    .compartmentId(getResolvedCompartmentId().get())
                    .build())
                    .items.each { instance ->
                    if (instance.displayName.matches(instanceNameRegex)) {
                        setInstanceId(instance.id)
                        terminateInstance(client, instance)
                    }
                }
            }
        }
    }

    private void terminateInstance(ComputeClient client, Instance instance) {
        println("Terminating Instance '${instance.displayName}' with id ${instance.id}")
        client.terminateInstance(TerminateInstanceRequest.builder()
            .instanceId(instance.id)
            .build())

        if (getResolvedWaitForCompletion().get()) {
            println("Waiting for Instance to be ${state('Terminated')}")
            client.waiters
                .forInstance(GetInstanceRequest.builder()
                    .instanceId(instance.id).build(),
                    Instance.LifecycleState.Terminated)
                .execute()
        }
    }
}
