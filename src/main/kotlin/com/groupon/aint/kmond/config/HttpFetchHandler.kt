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

import com.groupon.aint.kmond.promise.BufferFileWriter
import com.groupon.aint.kmond.promise.HttpGet
import com.groupon.aint.kmond.promise.HttpResponseAdapter
import com.groupon.aint.kmond.promise.MoveFile
import com.groupon.aint.kmond.promise.promise
import com.groupon.promise.exception.ExceptionUtils
import com.groupon.vertx.utils.Logger
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.eventbus.MessageProducer
import java.io.File

/**
 * Fetches a file from an http server and writes it to disk.
 *
 * @author Gil Markham (gil at groupon dot com)
 */
class HttpFetchHandler(val vertx: Vertx,
                       val producer: MessageProducer<String>,
                       val host: String,
                       val port: Int,
                       val urlPath: String,
                       val tmpDir: File,
                       val destDir: File,
                       val filenameBase: String): Handler<Long> {
    companion object {
        private val log = Logger.getLogger(HttpFetchHandler::class.java)
    }
    val httpClient = vertx.createHttpClient()

    override fun handle(event: Long) {
        promise<Void> {
            thenSync({log.info("httpFetch", "started", arrayOf("host", "port", "urlPath"), host, port, urlPath)},
                    {log.error("httpFetch", "exception", "unknown", arrayOf("host", "port", "urlPath"), host, port, urlPath,
                            ExceptionUtils.getMostSignificantCause(it))})
            thenAsync(HttpGet(httpClient, host, port, urlPath))
                    .thenAsync(HttpResponseAdapter())
                    .thenSync {it.second}
                    .thenAsync(BufferFileWriter(vertx.fileSystem(), tmpDir, filenameBase + ".tmp"))
                    .thenSync {producer.write(it)}

            after()
                .thenSync({log.info("httpFetch", "success", arrayOf("host", "port", "urlPath"), host, port, urlPath)},
                    {log.error("httpFetch", "exception", "unknown", arrayOf("host", "port", "urlPath"), host, port, urlPath,
                            ExceptionUtils.getMostSignificantCause(it))})
        }.fulfill(null)
    }
}
