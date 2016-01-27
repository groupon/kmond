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

import com.arpnetworking.metrics.Metrics
import com.arpnetworking.metrics.MetricsFactory
import io.vertx.core.http.HttpClientRequest
import io.vertx.core.http.HttpClientResponse
import io.vertx.core.http.WebSocket
import io.vertx.core.net.SocketAddress
import io.vertx.core.spi.metrics.HttpClientMetrics
import java.util.concurrent.TimeUnit

/**
 * HTTP client metrics adapter to AINT metrics.
 *
 * @author Gil Markham (gil at groupon dot com)
 */
class AintHttpClientMetrics(val metrics: (Metrics.() -> Unit) -> Unit): HttpClientMetrics<TimerWrapper, Void?, Void?> {
    override fun disconnected(socketMetric: Void?, remoteAddress: SocketAddress?) {
        if (remoteAddress != null) {
            metrics {
                addAnnotation("host", remoteAddress.host())
                addAnnotation("port", remoteAddress.port().toString())
                incrementCounter("vertx/httpClient/disconnect")
            }
        }
    }

    override fun connected(remoteAddress: SocketAddress?): Void? {
        if (remoteAddress != null) {
            metrics {
                addAnnotation("host", remoteAddress.host())
                addAnnotation("port", remoteAddress.port().toString())
                incrementCounter("vertx/httpClient/connect")
            }
        }

        return null
    }

    override fun connected(socketMetric: Void?, webSocket: WebSocket?): Void? {
        // Could be extends to pass websocket annotation information via a webSocketMetric object
        metrics {
            incrementCounter("vertx/webSocket/connect")
        }
        return null
    }

    override fun disconnected(webSocketMetric: Void?) {
        metrics {
            incrementCounter("vertx/webSocket/disconnect")
        }
    }

    override fun requestBegin(socketMetric: Void?, localAddress: SocketAddress?, remoteAddress: SocketAddress?, request: HttpClientRequest?): TimerWrapper {
        return TimerWrapper("vertx/httpClient/${remoteAddress?.host()}/request", System.nanoTime())
    }

    override fun responseEnd(requestMetric: TimerWrapper?, response: HttpClientResponse?) {
        if (requestMetric != null) {
            metrics {
                setTimer(requestMetric.metric, System.nanoTime() - requestMetric.timestamp, TimeUnit.NANOSECONDS)
            }
        }
    }

    override fun bytesWritten(socketMetric: Void?, remoteAddress: SocketAddress?, numberOfBytes: Long) {
        metrics {
            incrementCounter("vertx/httpClient/${remoteAddress?.host()}/bytesWritten", numberOfBytes)
        }
    }

    override fun bytesRead(socketMetric: Void?, remoteAddress: SocketAddress?, numberOfBytes: Long) {
        metrics {
            incrementCounter("vertx/httpClient/${remoteAddress?.host()}/bytesRead", numberOfBytes)
        }
    }

    override fun exceptionOccurred(socketMetric: Void?, remoteAddress: SocketAddress?, t: Throwable?) {
        metrics {
            incrementCounter("vertx/httpClient/${remoteAddress?.host()}/exceptions")
        }
    }

    override fun close() {
    }

    override fun isEnabled(): Boolean {
        return true
    }
}
