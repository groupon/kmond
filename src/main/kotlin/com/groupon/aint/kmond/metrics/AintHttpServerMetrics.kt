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
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.http.ServerWebSocket
import io.vertx.core.net.SocketAddress
import io.vertx.core.spi.metrics.HttpServerMetrics

/**
 * HTTP server metrics adapter to AINT metrics.
 *
 * @author Gil Markham (gil at groupon dot com)
 */
class AintHttpServerMetrics(val metricsFactory: MetricsFactory): HttpServerMetrics<TimerWrapper, Void, Void> {
    override fun upgrade(requestMetric: TimerWrapper?, serverWebSocket: ServerWebSocket?): Void? {
        val metrics = metricsFactory.create()
        metrics.incrementCounter("vertx/httpServer/websocket/upgrade")
        metrics.close()
        return null
    }

    override fun connected(socketMetric: Void?, serverWebSocket: ServerWebSocket?): Void? {
        val metrics = metricsFactory.create()
        metrics.incrementCounter("vertx/httpServer/websocket/connect")
        metrics.close()
        return null
    }

    override fun disconnected(serverWebSocketMetric: Void?) {
        val metrics = metricsFactory.create()
        metrics.incrementCounter("vertx/httpServer/websocket/disconnect")
        metrics.close()
    }

    override fun requestBegin(socketMetric: Void?, request: HttpServerRequest?): TimerWrapper? {
        val metrics = metricsFactory.create()
        return TimerWrapper(metrics, metrics.createTimer("vertx/httpServer/request"))
    }

    override fun responseEnd(requestMetric: TimerWrapper?, response: HttpServerResponse?) {
        if (requestMetric != null) {
            requestMetric.timer.close()
            requestMetric.metrics.close()
        }
    }

    override fun bytesRead(socketMetric: Void?, remoteAddress: SocketAddress?, numberOfBytes: Long) {
        val metrics = metricsFactory.create()
        metrics.incrementCounter("vertx/httpServer/bytesRead", numberOfBytes)
        metrics.close()
    }

    override fun exceptionOccurred(socketMetric: Void?, remoteAddress: SocketAddress?, t: Throwable?) {
        val metrics = metricsFactory.create()
        metrics.incrementCounter("vertx/httpServer/exceptions")
        metrics.close()
    }

    override fun bytesWritten(socketMetric: Void?, remoteAddress: SocketAddress?, numberOfBytes: Long) {
        val metrics = metricsFactory.create()
        metrics.incrementCounter("vertx/httpServer/bytesWritten", numberOfBytes)
        metrics.close()
    }

    override fun connected(remoteAddress: SocketAddress?): Void? {
        val metrics = metricsFactory.create()
        metrics.incrementCounter("vertx/httpServer/connect")
        metrics.close()
        return null
    }

    override fun disconnected(socketMetric: Void?, remoteAddress: SocketAddress?) {
        val metrics = metricsFactory.create()
        metrics.incrementCounter("vertx/httpServer/disconnect")
        metrics.close()
    }

    override fun isEnabled(): Boolean {
        return true
    }

    override fun close() {
    }
}
