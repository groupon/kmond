/**
 * Copyright 2015 Groupon Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.groupon.aint.kmond.config

import com.groupon.aint.kmond.promise.LoadFile
import com.groupon.aint.kmond.promise.MoveFile
import com.groupon.aint.kmond.promise.promise
import com.groupon.promise.exception.ExceptionUtils
import com.groupon.vertx.utils.Logger
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.Message
import io.vertx.core.shareddata.Shareable
import java.io.File

/**
 * Base class for async loading and verification of configuration data.
 *
 * @author Gil Markham (gil at groupon dot com)
 */
abstract class ConfigLoader<T: Shareable>(val vertx: Vertx, val destDir: File): Handler<Message<String?>> {
    companion object {
        private val log = Logger.getLogger(ConfigLoader::class.java)
    }

    override fun handle(event: Message<String?>) {
        val filePath = event.body()

        if (filePath != null) {
            promise<String> {
                thenSync {log.info("loadConfig", "start", arrayOf("name", "file"), name(), filePath)}

                thenAsync(LoadFile(vertx.fileSystem()))
                        .thenSync {parse(it)}
                        .thenSync {filterNull(it)}
                        .thenSync {verify(it)}
                        .thenSync {vertx.sharedData().getLocalMap<String,T>("configs").put(name(), it)}
                        .thenAsync(MoveFile<T>(vertx.fileSystem(), filePath, destDir)).optional(true)

                after().thenSync( {log.info("loadConfig", "success", arrayOf("name", "file"), name(), filePath)},
                        {log.error("loadConfig", "exception", "unknown", arrayOf("name", "file"), name(), filePath, ExceptionUtils.getMostSignificantCause(it))}
                )

                fulfill(filePath)
            }
        }
    }

    private fun filterNull(value: T?) : T = value ?: throw NullPointerException()

    abstract fun parse(buffer: Buffer): T?
    abstract fun verify(config: T): T
    abstract fun name(): String
}
