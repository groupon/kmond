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

import com.arpnetworking.metrics.MetricsFactory
import com.groupon.aint.kmond.config.mock
import com.groupon.aint.kmond.eventbus.V1ResultCodec
import com.groupon.aint.kmond.metrics.AintMetricsFactoryWrapper
import io.vertx.core.Context
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.MessageConsumer
import io.vertx.core.eventbus.MessageProducer
import io.vertx.core.http.HttpServer
import io.vertx.core.json.JsonObject
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for KMonDVerticle.
 *
 * @author fsiegrist (fsiegrist at groupon dot com)
 */
class KMonDVerticleTest {
    private val vertx = mock<Vertx>()
    private val context = mock<Context>()
    private val eventBus = mock<EventBus>()
    private val httpServer = mock<HttpServer>()
    private val metricsConsumer = mock<MessageConsumer<Metrics>>()
    private val metricsProducer = mock<MessageProducer<Metrics>>()
    private val stringProducer = mock<MessageProducer<String>>()
    private val metricsFactory = mock<MetricsFactory>()

    private var kmondConfig = JsonObject()

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        Mockito.`when`(vertx.createHttpServer(Mockito.any())).thenReturn(httpServer)
        Mockito.`when`(vertx.eventBus()).thenReturn(eventBus)
        Mockito.`when`(eventBus.consumer<Metrics>(Mockito.any(), Mockito.any())).thenReturn(metricsConsumer)
        Mockito.`when`(eventBus.sender<Metrics>(Mockito.any(), Mockito.any(DeliveryOptions::class.java))).thenReturn(metricsProducer)
        Mockito.`when`(eventBus.sender<Metrics>(Mockito.any())).thenReturn(metricsProducer)
        Mockito.`when`(eventBus.sender<String>(Mockito.any())).thenReturn(stringProducer)

        kmondConfig = JsonObject(javaClass.getResourceAsStream("/kmondConf.json").reader(Charsets.UTF_8).readText())
        Mockito.`when`(context.config()).thenReturn(kmondConfig)
    }

    @Test
    fun startTest() {
        val kmondVerticle = KMonDVerticle()
        kmondVerticle.init(vertx, context)

        AintMetricsFactoryWrapper.initialize(metricsFactory)

        val result = Future.future<Void>();
        kmondVerticle.start(result)

        Mockito.verify(eventBus, Mockito.times(1)).registerCodec(Mockito.any(V1ResultCodec::class.java))
        Mockito.verify(vertx, Mockito.times(1)).createHttpServer(Mockito.any())
        Mockito.verify(eventBus, Mockito.times(3)).consumer<Metrics>(Mockito.anyString(), Mockito.any())
        Mockito.verify(eventBus, Mockito.times(4)).sender<Metrics>(Mockito.anyString(), Mockito.any(DeliveryOptions::class.java))
    }

    @Test
    fun startMissingNagiosClusterIdTest() {
        kmondConfig.remove("nagiosClusterId")

        val kmondVerticle = KMonDVerticle()
        kmondVerticle.init(vertx, context)

        val result = Future.future<Void>();
        kmondVerticle.start(result)

        assertTrue(result.failed())
        assertNotNull(result.cause())

        Mockito.verifyZeroInteractions(vertx)
        Mockito.verifyZeroInteractions(eventBus)
    }
}
