/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019-2023 Andres Almiray.
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
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.options.Option
import org.kordamp.gradle.plugin.oci.tasks.interfaces.ProjectAware
import org.kordamp.gradle.property.PathAware
import org.kordamp.gradle.property.RegularFileState
import org.kordamp.gradle.property.SimpleRegularFileState

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
trait OptionalUserDataFileAwareTrait implements PathAware, ProjectAware {
    private final RegularFileState state = SimpleRegularFileState.of(project, this, 'oci.user.data.file')

    @Internal
    RegularFileProperty getUserDataFile() {
        state.property
    }

    @InputFile
    @Optional
    Provider<RegularFile> getResolvedUserDataFile() {
        state.provider
    }

    @Option(option = 'user-data-file', description = 'Location of cloud init file (OPTIONAL).')
    void setUserDataFile(String userDataFile) {
        setUserDataFile(project.file(userDataFile))
    }

    void setUserDataFile(File userDataFile) {
        getUserDataFile().set(userDataFile)
    }
}
