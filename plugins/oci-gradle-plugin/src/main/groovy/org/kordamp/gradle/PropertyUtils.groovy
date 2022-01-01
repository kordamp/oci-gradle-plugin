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
package org.kordamp.gradle

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.kordamp.gradle.property.PathAware

import java.nio.file.Paths

import static org.kordamp.gradle.util.StringUtils.isBlank
import static org.kordamp.gradle.util.StringUtils.isNotBlank

/**
 * @author Andres Almiray
 * @since 0.2.0
 */
@CompileStatic
class PropertyUtils {
    private static String normalizePath(String path, String delimiter) {
        if (':' == path) {
            return ''
        }
        return path[1..-1].replace(':', delimiter).replace(' ', '_') + delimiter
    }

    static String resolveValue(String envKey,
                               String propertyKey,
                               PathAware pathAware,
                               String projectName) {
        projectName = projectName.replace(' ', '_').replace('-', '_')
        String value = System.getenv(normalizePath(pathAware.path, '_').toUpperCase() + envKey)
        if (isBlank(value)) value = System.getProperty(normalizePath(pathAware.path, '.') + propertyKey)
        if (isBlank(value)) value = System.getenv(projectName.toUpperCase() + '_' + envKey)
        if (isBlank(value)) value = System.getProperty(projectName + '.' + propertyKey)
        if (isBlank(value)) value = System.getenv(envKey)
        if (isBlank(value)) value = System.getProperty(propertyKey)
        value
    }

    static Provider<String> stringProvider(String envKey,
                                           String propertyKey,
                                           Property<String> property,
                                           Project project,
                                           PathAware pathAware) {
        project.providers.provider {
            String value = resolveValue(envKey, propertyKey, pathAware, project.name)
            isNotBlank(value) ? value : property.orNull
        }
    }

    static Provider<Boolean> booleanProvider(String envKey,
                                             String propertyKey,
                                             Provider<Boolean> property,
                                             Project project,
                                             PathAware pathAware) {
        project.providers.provider {
            String value = resolveValue(envKey, propertyKey, pathAware, project.name)
            if (isNotBlank(value)) {
                return Boolean.parseBoolean(value)
            }
            property.getOrElse(false)
        }
    }

    static Provider<Integer> integerProvider(String envKey,
                                             String propertyKey,
                                             Provider<Integer> property,
                                             Project project,
                                             PathAware pathAware) {
        project.providers.provider {
            String value = resolveValue(envKey, propertyKey, pathAware, project.name)
            if (isNotBlank(value)) {
                return Integer.parseInt(value)
            }
            property.getOrElse(0)
        }
    }

    static Provider<RegularFile> fileProvider(String envKey,
                                              String propertyKey,
                                              RegularFileProperty property,
                                              Project project,
                                              PathAware pathAware) {
        project.providers.provider {
            String value = resolveValue(envKey, propertyKey, pathAware, project.name)
            if (isNotBlank(value)) {
                RegularFileProperty p = project.objects.fileProperty()
                p.set(Paths.get(value).toFile())
                return p.get()
            }
            property.orNull
        }
    }

    static Provider<Directory> directoryProvider(String envKey,
                                                 String propertyKey,
                                                 DirectoryProperty property,
                                                 Project project,
                                                 PathAware pathAware) {
        project.providers.provider {
            String value = resolveValue(envKey, propertyKey, pathAware, project.name)
            if (isNotBlank(value)) {
                DirectoryProperty p = project.objects.directoryProperty()
                p.set(Paths.get(value).toFile())
                return p.get()
            }
            property.orNull
        }
    }
}
