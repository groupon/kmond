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
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.http.ServerWebSocket
import io.vertx.core.net.SocketAddress
import io.vertx.core.spi.metrics.HttpServerMetrics
import java.util.concurrent.TimeUnit

/**
 * HTTP server metrics adapter to AINT metrics.
 *
 * @author Gil Markham (gil at groupon dot com)
 */
class AintHttpServerMetrics(val metrics: (Metrics.() -> Unit) -> Unit): HttpServerMetrics<TimerWrapper, Void, Void> {
    override fun upgrade(requestMetric: TimerWrapper?, serverWebSocket: ServerWebSocket?): Void? {
        metrics {
            incrementCounter("vertx/httpServer/websocket/upgrade")
        }
        return null
    }

    override fun connected(socketMetric: Void?, serverWebSocket: ServerWebSocket?): Void? {
        metrics {
            incrementCounter("vertx/httpServer/websocket/connect")
        }
        return null
    }

    override fun disconnected(serverWebSocketMetric: Void?) {
        metrics {
            incrementCounter("vertx/httpServer/websocket/disconnect")
        }
    }

    override fun requestBegin(socketMetric: Void?, request: HttpServerRequest?): TimerWrapper? {
        return TimerWrapper("vertx/httpServer/request", System.nanoTime())
    }

    override fun responseEnd(requestMetric: TimerWrapper?, response: HttpServerResponse?) {
        if (requestMetric != null) {
            metrics {
               setTimer(requestMetric.metric, System.nanoTime() - requestMetric.timestamp, TimeUnit.NANOSECONDS)
            }
        }
    }

    override fun bytesRead(socketMetric: Void?, remoteAddress: SocketAddress?, numberOfBytes: Long) {
        metrics {
            incrementCounter("vertx/httpServer/bytesRead", numberOfBytes)
        }
    }

    override fun exceptionOccurred(socketMetric: Void?, remoteAddress: SocketAddress?, t: Throwable?) {
        metrics {
            incrementCounter("vertx/httpServer/exceptions")
        }
    }

    override fun bytesWritten(socketMetric: Void?, remoteAddress: SocketAddress?, numberOfBytes: Long) {
        metrics {
            incrementCounter("vertx/httpServer/bytesWritten", numberOfBytes)
        }
    }

    override fun connected(remoteAddress: SocketAddress?): Void? {
        metrics {
            incrementCounter("vertx/httpServer/connect")
        }
        return null
    }

    override fun disconnected(socketMetric: Void?, remoteAddress: SocketAddress?) {
        metrics {
            incrementCounter("vertx/httpServer/disconnect")
        }
    }

    override fun isEnabled(): Boolean {
        return true
    }

    override fun close() {
    }
}
