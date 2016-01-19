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
package com.groupon.aint.kmond

import com.groupon.aint.kmond.config.mock
import io.vertx.core.Context
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.MessageProducer
import io.vertx.core.file.FileSystem
import io.vertx.core.http.HttpClient
import io.vertx.core.http.HttpClientRequest
import io.vertx.core.json.JsonObject
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

/**
 * Tests for ConfigVerticle.
 *
 * @author fsiegrist (fsiegrist at groupon dot com)
 */
class ConfigVerticleTest {
    private val vertx = mock<Vertx>()
    private val context = mock<Context>()
    private val eventBus = mock<EventBus>()
    private val httpClient = mock<HttpClient>()
    private val httpRequest = mock<HttpClientRequest>()
    private val fileSystem = mock<FileSystem>()
    private val stringProducer = mock<MessageProducer<String>>()

    private var kmondConfig = JsonObject()

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        Mockito.`when`(vertx.eventBus()).thenReturn(eventBus)
        Mockito.`when`(vertx.createHttpClient()).thenReturn(httpClient)
        Mockito.`when`(vertx.fileSystem()).thenReturn(fileSystem)
        Mockito.`when`(fileSystem.existsBlocking(Mockito.anyString())).thenReturn(true)
        Mockito.`when`(eventBus.sender<String>(Mockito.any())).thenReturn(stringProducer)
        Mockito.`when`(httpClient.get(Mockito.anyInt(), Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(httpRequest)

        kmondConfig = JsonObject(javaClass.getResourceAsStream("/kmondConf.json").reader("UTF8").readText())
        Mockito.`when`(context.config()).thenReturn(kmondConfig)
    }

    @Test
    fun startTest() {
        val configVerticle = ConfigVerticle()
        configVerticle.init(vertx, context)

        val result = Future.future<Void>();
        configVerticle.start(result)

        Mockito.verify(eventBus, Mockito.times(3)).sender<Metrics>(Mockito.anyString())
        Mockito.verify(vertx, Mockito.times(3)).setTimer(Mockito.eq(60000L), Mockito.any())
    }
}
