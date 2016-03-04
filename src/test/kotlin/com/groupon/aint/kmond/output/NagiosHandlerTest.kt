/*
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
package com.groupon.aint.kmond.output

import com.groupon.aint.kmond.Metrics
import com.groupon.aint.kmond.config.NagiosClusterLoader
import com.groupon.aint.kmond.config.captor
import com.groupon.aint.kmond.config.mock
import com.groupon.aint.kmond.config.model.NagiosClusters
import com.groupon.aint.kmond.input.V1Result
import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.core.http.HttpClient
import io.vertx.core.http.HttpClientOptions
import io.vertx.core.http.HttpClientRequest
import io.vertx.core.http.HttpClientResponse
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonObject
import io.vertx.core.shareddata.LocalMap
import io.vertx.core.shareddata.SharedData
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import java.util.HashMap
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

/**
 * @author Stuart Siegrist (fsiegrist at groupon dot com)
 */
class NagiosHandlerTest {
    private val vertx = mock<Vertx>()
    private val sharedData = mock<SharedData>()
    private val httpClient = mock<HttpClient>()
    private val httpRequest = mock<HttpClientRequest>()
    private val httpResponse = mock<HttpClientResponse>()
    private val message = mock<Message<Metrics>>()
    private val localMap = mock<LocalMap<String, NagiosClusters>>()
    private val replyCaptor = captor<JsonObject>()
    private val httpHandlerCaptor = captor<Handler<HttpClientResponse>>()
    private val metricsFactory = mock<com.arpnetworking.metrics.MetricsFactory>()
    private val metrics = mock<com.arpnetworking.metrics.Metrics>()
    private val timer = mock<com.arpnetworking.metrics.Timer>()


    private val metricsMap = HashMap<String, Float>()
    private val clusterMap = HashMap<String, Map<String, List<Int>>>()

    @Before
    fun setUp() {
        Mockito.`when`(vertx.createHttpClient(Mockito.any())).thenReturn(httpClient)
        Mockito.`when`(vertx.sharedData()).thenReturn(sharedData)
        Mockito.`when`(sharedData.getLocalMap<String, NagiosClusters>(Mockito.anyString())).thenReturn(localMap)
        Mockito.`when`(message.body()).thenReturn(
                V1Result("path", "monitor", 0, "output", 1, TimeUnit.SECONDS.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS),
                        "host", "cluster", true, metricsMap))
        Mockito.`when`(httpClient.request(Mockito.eq(HttpMethod.POST), Mockito.anyString(), httpHandlerCaptor.capture()))
                .thenReturn(httpRequest)
        Mockito.`when`(httpResponse.statusCode()).thenReturn(HttpResponseStatus.OK.code())
        Mockito.`when`(httpResponse.statusMessage()).thenReturn(HttpResponseStatus.OK.reasonPhrase())
        Mockito.`when`(metricsFactory.create()).thenReturn(metrics)
        Mockito.`when`(metrics.createTimer(Mockito.any())).thenReturn(timer)
    }

    @Test
    fun hasAlertsTest() {
        val nagiosHandler = NagiosHandler(vertx, metricsFactory, "clusterId", JsonObject())
        metricsMap.put("mean_critical", 1F)
        clusterMap.put("clusterId", mapOf(Pair("nagiosHost", (0..99).toList())))

        Mockito.`when`(localMap.get(NagiosClusterLoader.NAME)).thenReturn(NagiosClusters(clusterMap))

        nagiosHandler.handle(message)

        httpHandlerCaptor.value.handle(httpResponse)

        Mockito.verify(vertx, Mockito.times(1)).createHttpClient(Mockito.any(HttpClientOptions::class.java))
        Mockito.verify(message, Mockito.times(1)).reply(replyCaptor.capture())

        val reply = replyCaptor.value
        assertEquals("success", reply.getString("status"))
        assertEquals(HttpResponseStatus.OK.code(), reply.getInteger("code"))
        assertEquals(HttpResponseStatus.OK.reasonPhrase(), reply.getString("message"))
    }

    @Test
    fun noAlertsTest() {
        val nagiosHandler = NagiosHandler(vertx, metricsFactory, "clusterId", JsonObject())
        Mockito.`when`(message.body()).thenReturn(
                V1Result("path", "monitor", 0, "output", 1, TimeUnit.SECONDS.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS),
                        "host", "cluster", false, metricsMap))

        nagiosHandler.handle(message)

        Mockito.verify(message, Mockito.times(1)).reply(replyCaptor.capture())

        val reply = replyCaptor.value
        assertEquals("success", reply.getString("status"))
        assertEquals(200, reply.getInteger("code"))
        assertEquals("No alert present", reply.getString("message"))

        Mockito.verifyZeroInteractions(vertx)
    }

    @Test
    fun buildsHttpClientOnceTest() {
        val nagiosHandler = NagiosHandler(vertx, metricsFactory, "clusterId", JsonObject())
        metricsMap.put("warning", 1F)
        clusterMap.put("clusterId", mapOf(Pair("nagiosHost", (0..99).toList())))

        Mockito.`when`(localMap.get(NagiosClusterLoader.NAME)).thenReturn(NagiosClusters(clusterMap))

        nagiosHandler.handle(message)

        httpHandlerCaptor.value.handle(httpResponse)

        Mockito.verify(message, Mockito.times(1)).reply(replyCaptor.capture())

        var reply = replyCaptor.value
        assertEquals("success", reply.getString("status"))
        assertEquals(HttpResponseStatus.OK.code(), reply.getInteger("code"))
        assertEquals(HttpResponseStatus.OK.reasonPhrase(), reply.getString("message"))

        nagiosHandler.handle(message)

        Mockito.verify(message, Mockito.times(1)).reply(replyCaptor.capture())

        reply = replyCaptor.value
        assertEquals("success", reply.getString("status"))
        assertEquals(HttpResponseStatus.OK.code(), reply.getInteger("code"))
        assertEquals(HttpResponseStatus.OK.reasonPhrase(), reply.getString("message"))

        Mockito.verify(vertx, Mockito.times(1)).createHttpClient(Mockito.any(HttpClientOptions::class.java))
    }
}
