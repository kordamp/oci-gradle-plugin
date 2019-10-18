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
package org.kordamp.gradle.oci.tasks.list

import com.oracle.bmc.identity.IdentityClient
import com.oracle.bmc.identity.model.User
import com.oracle.bmc.identity.requests.ListUsersRequest
import com.oracle.bmc.identity.responses.ListUsersResponse
import groovy.transform.CompileStatic
import org.kordamp.gradle.oci.tasks.AbstractOCITask
import org.kordamp.gradle.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.oci.tasks.traits.CompartmentIdAwareTrait
import org.kordamp.gradle.oci.tasks.traits.VerboseAwareTrait
import org.kordamp.jipsy.TypeProviderFor

import static org.kordamp.gradle.oci.tasks.printers.UserPrinter.printUser

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class ListUsersTask extends AbstractOCITask implements CompartmentIdAwareTrait, VerboseAwareTrait {
    static final String TASK_DESCRIPTION = 'Lists available Users.'

    @Override
    protected void doExecuteTask() {
        validateCompartmentId()

        IdentityClient client = createIdentityClient()
        ListUsersResponse response = client.listUsers(ListUsersRequest.builder()
                .compartmentId(getCompartmentId())
                .build())

        println('Total Users: ' + console.cyan(response.items.size().toString()))
        println(' ')
        for (User user : response.items) {
            println(user.name + (isVerbose() ? ':' : ''))
            if (isVerbose()) {
                printUser(this, user, 0)
            }
        }
    }
}
