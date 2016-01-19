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
import io.vertx.core.http.HttpClientRequest
import io.vertx.core.http.HttpClientResponse
import io.vertx.core.http.WebSocket
import io.vertx.core.net.SocketAddress
import io.vertx.core.spi.metrics.HttpClientMetrics

/**
 * HTTP client metrics adapter to AINT metrics.
 *
 * @author Gil Markham (gil at groupon dot com)
 */
class AintHttpClientMetrics(val metricsFactory: MetricsFactory): HttpClientMetrics<TimerWrapper, Void?, Void?> {
    override fun disconnected(socketMetric: Void?, remoteAddress: SocketAddress?) {
        if (remoteAddress != null) {
            val metrics = metricsFactory.create()
            metrics.addAnnotation("host", remoteAddress.host())
            metrics.addAnnotation("port", remoteAddress.port().toString())
            metrics.incrementCounter("vertx/httpClient/disconnect")
        }
    }

    override fun connected(remoteAddress: SocketAddress?): Void? {
        if (remoteAddress != null) {
            val metrics = metricsFactory.create()
            metrics.addAnnotation("host", remoteAddress.host())
            metrics.addAnnotation("port", remoteAddress.port().toString())
            metrics.incrementCounter("vertx/httpClient/connect")
        }

        return null
    }

    override fun connected(socketMetric: Void?, webSocket: WebSocket?): Void? {
        // Could be extends to pass websocket annotation information via a webSocketMetric object
        val metrics = metricsFactory.create()
        metrics.incrementCounter("vertx/webSocket/connect")

        return null
    }

    override fun disconnected(webSocketMetric: Void?) {
        val metrics = metricsFactory.create()
        metrics.incrementCounter("vertx/webSocket/disconnect")
    }

    override fun requestBegin(socketMetric: Void?, localAddress: SocketAddress?, remoteAddress: SocketAddress?, request: HttpClientRequest?): TimerWrapper {
        val metrics = metricsFactory.create()
        return TimerWrapper(metrics, metrics.createTimer("vertx/httpClient/${remoteAddress?.host()}/request"))
    }

    override fun responseEnd(requestMetric: TimerWrapper?, response: HttpClientResponse?) {
        if (requestMetric != null) {
            requestMetric.timer.close()
            requestMetric.metrics.addAnnotation("status", response?.statusCode().toString())
            requestMetric.metrics.close()
        }
    }

    override fun bytesWritten(socketMetric: Void?, remoteAddress: SocketAddress?, numberOfBytes: Long) {
        val metrics = metricsFactory.create()
        metrics.incrementCounter("vertx/httpClient/${remoteAddress?.host()}/bytesWritten", numberOfBytes)
        metrics.close()
    }

    override fun bytesRead(socketMetric: Void?, remoteAddress: SocketAddress?, numberOfBytes: Long) {
        val metrics = metricsFactory.create()
        metrics.incrementCounter("vertx/httpClient/${remoteAddress?.host()}/bytesRead", numberOfBytes)
        metrics.close()
    }

    override fun exceptionOccurred(socketMetric: Void?, remoteAddress: SocketAddress?, t: Throwable?) {
        val metrics = metricsFactory.create()
        metrics.incrementCounter("vertx/httpClient/${remoteAddress?.host()}/exceptions")
        metrics.close()
    }

    override fun close() {
    }

    override fun isEnabled(): Boolean {
        return true
    }
}
