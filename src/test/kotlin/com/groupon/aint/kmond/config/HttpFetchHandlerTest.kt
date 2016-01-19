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

import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.MessageProducer
import io.vertx.core.file.FileSystem
import io.vertx.core.http.HttpClient
import io.vertx.core.http.HttpClientRequest
import io.vertx.core.http.HttpClientResponse
import org.junit.Before
import org.junit.Test
import org.mockito.Matchers
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.io.File

/**
 * Tests for HttpFetchHandler.
 *
 * @author Stuart Siegrist (fsiegrist at groupon dot com)
 */
class HttpFetchHandlerTest {
    val vertx = mock<Vertx>()
    val fileSystem = mock<FileSystem>()
    val httpClient = mock<HttpClient>()
    val httpRequest = mock<HttpClientRequest>()
    val responseHandler = captor<Handler<HttpClientResponse>>()
    val httpResponse = mock<HttpClientResponse>()
    val responseBody = captor<Handler<Buffer>>()
    val voidHandler = captor<Handler<AsyncResult<Void>>>()
    val producer = mock<MessageProducer<String>>()

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        Mockito.`when`(vertx.fileSystem()).thenReturn(fileSystem)
        Mockito.`when`(vertx.createHttpClient()).thenReturn(httpClient)
    }

    @Test
    fun testSimpleGet() {
        val buffer = Buffer.buffer(javaClass.getResourceAsStream("/ganglia_cluster.yml").reader("UTF8").readText())
        Mockito.`when`(httpClient.get(Mockito.anyInt(), Mockito.anyString(), Mockito.anyString(),
                responseHandler.capture())).thenReturn(httpRequest)
        Mockito.`when`(httpResponse.statusCode()).thenReturn(200)
        Mockito.`when`(producer.write(Mockito.anyString())).thenReturn(producer)

        val testFile = File("target")
        HttpFetchHandler(vertx, producer, "host", 80, "urlPath", testFile, testFile, "filename").handle(1L)

        responseHandler.value.handle(httpResponse)

        Mockito.verify(httpResponse).bodyHandler(responseBody.capture())
        responseBody.value.handle(buffer)

        Mockito.verify(fileSystem).writeFile(Matchers.eq(File(testFile, "filename.tmp").absolutePath), Matchers.eq(buffer),
                voidHandler.capture())
        voidHandler.value.handle(Future.succeededFuture<Void>())
    }

    @Test
    fun testGetFailure() {
        val buffer = Buffer.buffer(javaClass.getResourceAsStream("/ganglia_cluster.yml").reader("UTF8").readText())
        Mockito.`when`(httpClient.get(Mockito.anyInt(), Mockito.anyString(), Mockito.anyString(),
                responseHandler.capture())).thenReturn(httpRequest)
        Mockito.`when`(httpResponse.statusCode()).thenReturn(400)
        Mockito.`when`(httpResponse.statusMessage()).thenReturn("Bad request")

        val testFile = File("target")
        HttpFetchHandler(vertx, producer, "host", 80, "urlPath", testFile, testFile, "filename").handle(1L)

        responseHandler.value.handle(httpResponse)

        Mockito.verify(httpResponse).bodyHandler(responseBody.capture())
        responseBody.value.handle(buffer)

        Mockito.verifyZeroInteractions(fileSystem)
    }
}
