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
package org.kordamp.gradle.oci

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.kordamp.gradle.oci.tasks.CreateInstanceTask
import org.kordamp.gradle.oci.tasks.DisplayCompartmentTask
import org.kordamp.gradle.oci.tasks.ListAvailabilityDomainsTask
import org.kordamp.gradle.oci.tasks.ListCompartmentsTask
import org.kordamp.gradle.oci.tasks.ListImagesTask
import org.kordamp.gradle.oci.tasks.ListShapesTask
import org.kordamp.gradle.oci.tasks.ListVcnsTask
import org.kordamp.gradle.oci.tasks.SearchResourcesTask
import org.kordamp.gradle.plugin.AbstractKordampPlugin

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
class OCIPlugin extends AbstractKordampPlugin {
    private static final GROUP = 'OCI'

    Project project

    void apply(Project project) {
        this.project = project
        configureProject(project)
    }

    static void applyIfMissing(Project project) {
        if (!project.plugins.findPlugin(OCIPlugin)) {
            project.plugins.apply(OCIPlugin)
        }
    }

    private void configureProject(Project project) {
        if (hasBeenVisited(project)) {
            return
        }
        setVisited(project, true)

        [
            CreateInstanceTask,
            DisplayCompartmentTask,
            ListAvailabilityDomainsTask,
            ListCompartmentsTask,
            ListImagesTask,
            ListShapesTask,
            ListVcnsTask,
            SearchResourcesTask
        ].each { taskType ->
            project.tasks.register(taskType.NAME, taskType,
                new Action<Task>() {
                    @Override
                    void execute(Task t) {
                        t.group = GROUP
                        t.description = taskType.DESCRIPTION
                    }
                })
        }
    }
}
