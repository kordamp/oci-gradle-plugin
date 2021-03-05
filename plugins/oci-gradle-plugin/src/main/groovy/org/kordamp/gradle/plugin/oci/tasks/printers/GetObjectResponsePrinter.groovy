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
package org.kordamp.gradle.plugin.oci.tasks.printers

import com.oracle.bmc.objectstorage.responses.GetObjectResponse
import groovy.transform.CompileStatic
import org.kordamp.gradle.plugin.oci.tasks.interfaces.ValuePrinter

/**
 * @author Andres Almiray
 * @since 0.3.0
 */
@CompileStatic
class GetObjectResponsePrinter {
    static void printObject(ValuePrinter printer, GetObjectResponse object, int offset) {
        printer.printKeyValue('ETag', object.ETag, offset + 1)
        printer.printKeyValue('Modified', !object.notModified, 1)
        printer.printKeyValue('Last Modified', object.lastModified, 1)
        printer.printKeyValue('Content Length', object.contentLength, 1)
        printer.printKeyValue('Content Type', object.contentType, 1)
        printer.printKeyValue('Content MD5', object.contentMd5, 1)
        printer.printKeyValue('Content Encoding', object.contentEncoding, 1)
        printer.printKeyValue('Content Language', object.contentLanguage, 1)
        printer.printKeyValue('Archival State', object.archivalState, 1)
        printer.printKeyValue('Time of Archival', object.timeOfArchival, 1)
    }
}
