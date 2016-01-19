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
package com.groupon.aint.kmond.metrics

import com.arpnetworking.metrics.MetricsFactory
import io.vertx.core.Verticle
import io.vertx.core.datagram.DatagramSocket
import io.vertx.core.datagram.DatagramSocketOptions
import io.vertx.core.eventbus.EventBus
import io.vertx.core.http.HttpClient
import io.vertx.core.http.HttpClientOptions
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.metrics.impl.DummyVertxMetrics
import io.vertx.core.net.NetClient
import io.vertx.core.net.NetClientOptions
import io.vertx.core.net.NetServer
import io.vertx.core.net.NetServerOptions
import io.vertx.core.net.SocketAddress
import io.vertx.core.spi.metrics.DatagramSocketMetrics
import io.vertx.core.spi.metrics.EventBusMetrics
import io.vertx.core.spi.metrics.HttpClientMetrics
import io.vertx.core.spi.metrics.HttpServerMetrics
import io.vertx.core.spi.metrics.TCPMetrics

/**
 * Vertx metrics adapter to AINT metrics.
 *
 * @author Gil Markham (gil at groupon dot com)
 */
class AintMetricsAdapter(val metricsFactory: MetricsFactory): DummyVertxMetrics() {

    override fun createMetrics(eventBus: EventBus?): EventBusMetrics<*>? {
        return AintEventBusMetrics(metricsFactory)
    }

    override fun createMetrics(server: HttpServer?, localAddress: SocketAddress?, options: HttpServerOptions?): HttpServerMetrics<*, *, *>? {
        return AintHttpServerMetrics(metricsFactory)
    }

    override fun createMetrics(client: HttpClient?, options: HttpClientOptions?): HttpClientMetrics<*, *, *>? {
        return AintHttpClientMetrics(metricsFactory)
    }

    override fun createMetrics(server: NetServer?, localAddress: SocketAddress?, options: NetServerOptions?): TCPMetrics<*>? {
        return super.createMetrics(server, localAddress, options)
    }

    override fun createMetrics(client: NetClient?, options: NetClientOptions?): TCPMetrics<*>? {
        return super.createMetrics(client, options)
    }

    override fun createMetrics(socket: DatagramSocket?, options: DatagramSocketOptions?): DatagramSocketMetrics? {
        return AintDatagramMetrics(metricsFactory)
    }

    override fun verticleUndeployed(verticle: Verticle?) {
        val metrics = metricsFactory.create()
        metrics.incrementCounter("vertx/verticle/${verticle?.javaClass?.simpleName}/undeployed")
        metrics.close()
    }

    override fun verticleDeployed(verticle: Verticle?) {
        val metrics = metricsFactory.create()
        metrics.incrementCounter("vertx/verticle/${verticle?.javaClass?.simpleName}/deployed")
        metrics.close()
    }

    override fun timerCreated(id: Long) {
        val metrics = metricsFactory.create()
        metrics.incrementCounter("vertx/timers/created")
        metrics.close()
    }

    override fun timerEnded(id: Long, cancelled: Boolean) {
        val metrics = metricsFactory.create()
        if (cancelled) {
            metrics.incrementCounter("vertx/timers/cancelled")
        } else {
            metrics.incrementCounter("vertx/timers/ended")
        }
        metrics.close()
    }

    override fun isMetricsEnabled(): Boolean {
        return true
    }

    override fun isEnabled(): Boolean {
        return true
    }

    override fun close() {
    }
}
