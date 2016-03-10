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
package com.groupon.aint.kmond.output

import com.arpnetworking.metrics.MetricsFactory
import com.arpnetworking.metrics.Timer
import com.groupon.aint.kmond.Metrics
import com.groupon.aint.kmond.config.GangliaClusterHostsLoader
import com.groupon.aint.kmond.config.GangliaClusterPortsLoader
import com.groupon.aint.kmond.config.model.GangliaClusterHosts
import com.groupon.aint.kmond.config.model.GangliaClusterPorts
import com.groupon.aint.kmond.output.model.GangliaMetric
import com.groupon.aint.kmond.promise.SendDatagramPacket
import com.groupon.aint.kmond.promise.async
import com.groupon.aint.kmond.promise.promise
import com.groupon.vertx.utils.Logger
import hep.io.xdr.XDROutputStream
import io.netty.buffer.ByteBufOutputStream
import io.netty.buffer.Unpooled
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.datagram.DatagramSocket
import io.vertx.core.eventbus.Message
import java.util.ArrayList
import java.util.concurrent.TimeUnit

/**
 * Output handler for sending events to Ganglia.
 *
 * @param appMetricsFactory Used to create metrics that instrument KMonD itself; unrelated to the metrics being forwarded to Ganglia.
 *
 * @author Gil Markham (gil at groupon dot com)
 */
class GangliaHandler(val vertx: Vertx, val appMetricsFactory: MetricsFactory) : Handler<Message<Metrics>> {
    private val datagramSocket = vertx.createDatagramSocket()
    private val APP_METRICS_PREFIX = "downstream/ganglia"

    companion object {
        private val log = Logger.getLogger(GangliaHandler::class.java)
    }

    val gangliaPortMapping: GangliaClusterPorts?
        get() = vertx.sharedData().getLocalMap<String, GangliaClusterPorts>("configs")?.get(GangliaClusterPortsLoader.NAME)

    val gangliaHostMapping: GangliaClusterHosts?
        get() = vertx.sharedData().getLocalMap<String, GangliaClusterHosts>("configs")?.get(GangliaClusterHostsLoader.NAME)

    override fun handle(msg: Message<Metrics>) {

        val metrics = msg.body()
        val hosts = gangliaHostMapping?.getHosts(metrics.cluster) ?: emptySet()
        val port = gangliaPortMapping?.getPort(metrics.cluster)
        val packets = createXdrs(metrics)

        val appMetrics = appMetricsFactory.create()

        if (port != null && hosts.size > 0) {
            val requestTimer = appMetrics.createTimer(APP_METRICS_PREFIX + "/request")
            promise<DatagramSocket> {
                var packetSizeSum: Long = 0;
                hosts.forEach { host ->
                    packets.forEach { packet ->
                        packetSizeSum += packet.first.length() + packet.second.length()

                        async(SendDatagramPacket(packet.first, host, port)) {
                            thenAsync(SendDatagramPacket(packet.second, host, port))

                            after().thenSync({}, {
                                log.warn("send", "failure", arrayOf("gangliaPort", "gangliaHost", "metricsCluster"),
                                        port, host, metrics.cluster, it)
                            })
                        }
                    }
                }

                appMetrics.setGauge(APP_METRICS_PREFIX + "/bytes_sent", packetSizeSum)

                after().thenSync({
                    closeMetrics(appMetrics, requestTimer, true)
                }, {
                    closeMetrics(appMetrics, requestTimer, false)
                })

                fulfill(datagramSocket)
            }
        } else {
            log.warn("send", "unknownCluster", arrayOf("cluster"), metrics.cluster)
            addSuccessMetric(appMetrics, false)
            appMetrics.close()
        }
    }

    fun createXdrs(metrics: Metrics): List<Pair<Buffer, Buffer>> {
        val xdrs = ArrayList<Pair<Buffer, Buffer>>()
        val hostname = "${metrics.host}:${metrics.host}"
        metrics.metrics.forEach { entry ->
            val (key, value) = entry
            xdrs.add(createXdr(GangliaMetric(
                    name = "${metrics.monitor}_$key",
                    type = metricType(key),
                    value = normalizeValue(value),
                    runInterval = metrics.runInterval,
                    hostname = hostname
            )))
        }

        // Add status metric
        xdrs.add(createXdr(GangliaMetric(
                name = "${metrics.monitor}_status",
                type = metricType("status"),
                value = statusValue(metrics),
                runInterval = metrics.runInterval,
                hostname = hostname
        )))

        return xdrs
    }

    fun createXdr(metric: GangliaMetric): Pair<Buffer, Buffer> {
        // ganglia can only handle metric names with names matching the pattern below
        val name = metric.name.replace(Regex("[^a-zA-Z0-9\\-_]+"), "_")

        // ganglia does not escape values when building XML, so protect it from itself
        val safeValue: Any = if (metric.value is String) {
            metric.value.replace(Regex("[<>&\"']"), "_")
        } else {
            metric.value
        }

        // Prepend _MONITORD_ to *_output metrics. The nagios poller will then skip
        // over them, since monitord_server pushes *_output and *_status directly to nagios.
        // Once everything is transitioned to monitord, then we can remove this.
        val value: Any = if (name.matches(Regex.fromLiteral("_output$"))) {
            "_MONITORD_ $safeValue"
        } else {
            safeValue
        }

        // metadata packet
        val metadataBuffer = Unpooled.buffer()
        val xdr = XDROutputStream(ByteBufOutputStream(metadataBuffer))
        xdr.writeInt(128) // meta packet id
        xdr.writeString(metric.hostname) // host sending the metric
        xdr.writeString(name) // metric name
        xdr.writeInt(1) // always spoofing host here
        xdr.writeString(metric.type) // type of data (one of: string, int8, unit8, int16, uint16, int32, uint32, float, double)
        xdr.writeString(name) // metric name AGAIN!
        xdr.writeString("") // units
        xdr.writeInt(slopeToInt(metric.slope)) // slope
        xdr.writeInt(metric.runInterval)     // maximum time in seconds between metric calls, default 60
        xdr.writeInt(metric.runInterval * 4) // lifetime in seconds of this metric, default=0, meaning unlimited
        xdr.writeInt(0) // all done

        // data packet
        val dataBuffer = Unpooled.buffer()
        val dataXdr = XDROutputStream(ByteBufOutputStream(dataBuffer))
        dataXdr.writeInt(128 + 5) // data packet id
        dataXdr.writeString(metric.hostname) // host sending the metric
        dataXdr.writeString(name) // metric name
        dataXdr.writeInt(1) // always spoofing host here
        dataXdr.writeString("%s") // output pattern
        dataXdr.writeString(value.toString()) // value of the metric

        return Pair(Buffer.buffer(metadataBuffer), Buffer.buffer(dataBuffer))
    }

    fun normalizeValue(value: Float): Float = if (Math.abs(value) < Math.exp(-200.0)) 0f else value

    fun statusValue(metrics: Metrics): Int = if (isLagging(metrics)) 4 else metrics.status

    fun isLagging(metrics: Metrics): Boolean {
        // make grapher communicate when a service check is lagging (related to PRODTOOLS-1539)
        val delay = TimeUnit.SECONDS.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS)  - metrics.timestamp
        return (metrics.runInterval == 0 && delay > 3600) || (metrics.runInterval != 0 && delay > 10*metrics.runInterval) // 10 magic number analysis can be found in PRODTOOLS-1430
    }

    fun metricType(name: String): String {
        return when (name.split('_').last()) {
            "status" -> "int8"
            "output" -> "string"
            else -> "float"
        }
    }

    fun slopeToInt(slope: String): Int {
        return when (slope) {
            "zero" -> 0
            "positive" -> 1
            "negative" -> 2
            "both" -> 3
            else -> 4
        }
    }

    private fun closeMetrics(metrics: com.arpnetworking.metrics.Metrics, timer: Timer, success: Boolean) {
        timer.stop()
        addSuccessMetric(metrics, success)
        metrics.close()
    }

    private fun addSuccessMetric(metrics: com.arpnetworking.metrics.Metrics, success: Boolean) {
        var successGauge: Long
        var failGauge: Long
        if (success) {
            successGauge = 1
            failGauge = 0
        } else {
            successGauge = 0
            failGauge = 1
        }
        metrics.setGauge(APP_METRICS_PREFIX + "/success", successGauge)
        metrics.setGauge(APP_METRICS_PREFIX + "/failure", failGauge)
    }
}
