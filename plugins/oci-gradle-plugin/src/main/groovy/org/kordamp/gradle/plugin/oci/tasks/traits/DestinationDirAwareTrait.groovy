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
package org.kordamp.gradle.plugin.oci.tasks.traits

import groovy.transform.CompileStatic
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.options.Option
import org.kordamp.gradle.plugin.oci.tasks.interfaces.PathAware
import org.kordamp.gradle.plugin.oci.tasks.interfaces.ProjectAware
import org.kordamp.gradle.plugin.oci.tasks.traits.states.DirectoryState

/**
 * @author Andres Almiray
 * @since 0.3.0
 */
@CompileStatic
trait DestinationDirAwareTrait implements PathAware, ProjectAware {
    private final DirectoryState state = new DirectoryState(project, this, 'OCI_DESTINATION_DIR', 'oci.destination.dir')

    @Internal
    private DirectoryProperty getDestinationDir() {
        state.property
    }

    @InputFile
    Provider<Directory> getResolvedDestinationDir() {
        state.provider
    }

    @Option(option = 'destination-dir', description = 'The destination directory (REQUIRED).')
    void setDestinationDirectory(String file) {
        setDestinationDir(project.file(file))
    }

    void setDestinationDir(File file) {
        getDestinationDir().set(file)
    }

    void validateDestinationDir() {
        if (!getResolvedDestinationDir().present) {
            throw new IllegalStateException("Missing value for 'file' in $path")
        }
    }
}
