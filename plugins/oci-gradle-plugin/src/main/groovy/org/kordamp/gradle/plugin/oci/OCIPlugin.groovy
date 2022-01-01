/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019-2022 Andres Almiray.
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
package org.kordamp.gradle.plugin.oci

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.kordamp.gradle.util.StringUtils
import org.kordamp.gradle.plugin.AbstractKordampPlugin
import org.kordamp.gradle.plugin.oci.tasks.interfaces.OCITask
import org.kordamp.jipsy.util.TypeLoader

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
class OCIPlugin extends AbstractKordampPlugin {
    static {
        System.setProperty('sun.net.http.allowRestrictedHeaders', 'true')
    }

    Project project

    OCIPlugin() {
        super('org.kordamp.gradle.oci')
    }

    void apply(Project project) {
        Banner.display(project)
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

        TypeLoader.load(this.class.classLoader, OCITask, new TypeLoader.LineProcessor() {
            @Override
            @CompileDynamic
            void process(ClassLoader classLoader, Class<?> clazz, String line) {
                Class<? extends Task> taskType = classLoader.loadClass(line.trim(), true)
                String taskName = StringUtils.getPropertyName(taskType.simpleName - 'Task')
                String group = taskType.package.name.split('\\.')[-1]

                project.tasks.register(taskName, taskType,
                    new Action<Task>() {
                        @Override
                        void execute(Task t) {
                            t.group = 'OCI ' + group.capitalize()
                            t.description = taskType.TASK_DESCRIPTION
                        }
                    })
            }
        })
    }
}
