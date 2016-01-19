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
import io.vertx.core.net.SocketAddress
import io.vertx.core.spi.metrics.DatagramSocketMetrics

/**
 * Datagram socket metrics adapter to AINT metrics.
 *
 * @author Gil Markham (gil at groupon dot com)
 */
class AintDatagramMetrics(val metricsFactory: MetricsFactory): DatagramSocketMetrics {
    override fun listening(localAddress: SocketAddress?) {
        val metric = metricsFactory.create()
        metric.incrementCounter("vertx/datagram/listening")
        metric.close()
    }

    override fun exceptionOccurred(socketMetric: Void?, remoteAddress: SocketAddress?, t: Throwable?) {
        val metric = metricsFactory.create()
        metric.incrementCounter("vertx/datagram/errors")
        metric.close()
    }

    override fun bytesRead(socketMetric: Void?, remoteAddress: SocketAddress?, numberOfBytes: Long) {
        val metric = metricsFactory.create()
        metric.incrementCounter("vertx/datagram/bytesRead", numberOfBytes)
        metric.close()
    }

    override fun bytesWritten(socketMetric: Void?, remoteAddress: SocketAddress?, numberOfBytes: Long) {
        val metric = metricsFactory.create()
        metric.incrementCounter("vertx/datagram/bytesWritten", numberOfBytes)
        metric.close()
    }

    override fun isEnabled(): Boolean {
        return true
    }

    override fun close() {
    }
}
