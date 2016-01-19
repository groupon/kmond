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
import io.vertx.core.eventbus.ReplyFailure
import io.vertx.core.spi.metrics.EventBusMetrics
import java.util.Locale

/**
 * Event bus metrics adapter to AINT metrics.
 *
 * @author Gil Markham (gil at groupon dot com)
 */
class AintEventBusMetrics(val metricsFactory: MetricsFactory): EventBusMetrics<Void> {
    override fun isEnabled(): Boolean {
        return true
    }

    override fun close() {
    }

    override fun messageSent(address: String?, publish: Boolean, local: Boolean, remote: Boolean) {
        val metrics = metricsFactory.create()
        metrics.addAnnotation("broadcast", publish.toString())
        metrics.addAnnotation("local", local.toString())
        metrics.incrementCounter("vertx/eventBus/$address/sent")
        metrics.close()
    }

    override fun messageWritten(address: String?, numberOfBytes: Int) {
        val metrics = metricsFactory.create()
        metrics.incrementCounter("vertx/eventBus/$address/bytesWritten", numberOfBytes.toLong())
        metrics.close()
    }

    override fun messageRead(address: String?, numberOfBytes: Int) {
        val metrics = metricsFactory.create()
        metrics.incrementCounter("vertx/eventBus/$address/bytesRead", numberOfBytes.toLong())
        metrics.close()
    }

    override fun replyFailure(address: String?, failure: ReplyFailure?) {
        val metrics = metricsFactory.create()
        metrics.addAnnotation("failure", failure.toString().toLowerCase(Locale.ENGLISH))
        metrics.incrementCounter("vertx/eventBus/$address/replyFailures")
        metrics.close()
    }

    override fun messageReceived(address: String?, publish: Boolean, local: Boolean, handlers: Int) {
        val metrics = metricsFactory.create()
        metrics.addAnnotation("broadcast", publish.toString())
        metrics.addAnnotation("local", local.toString())
        metrics.incrementCounter("vertx/eventBus/$address/received")
        metrics.close()
    }

    override fun handlerRegistered(address: String?, replyHandler: Boolean): Void? {
        if(!replyHandler) {
            val metrics = metricsFactory.create()
            metrics.incrementCounter("vertx/eventBus/$address/registered")
            metrics.close()
        }
        return null
    }

    override fun endHandleMessage(handler: Void?, failure: Throwable?) {
    }

    override fun handlerUnregistered(handler: Void?) {
    }

    override fun beginHandleMessage(handler: Void?, local: Boolean) {
    }
}
