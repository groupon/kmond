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
import com.groupon.aint.kmond.Metrics
import com.groupon.aint.kmond.config.NagiosClusterLoader
import com.groupon.aint.kmond.config.model.NagiosClusters
import com.groupon.vertx.utils.Logger
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.core.http.HttpClient
import io.vertx.core.http.HttpClientOptions
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonObject
import java.util.HashMap
import java.util.concurrent.TimeUnit

/**
 * Output handler for sending events to Nagios.
 *
 * @param appMetricsFactory Used to create metrics with respect to KMonD, e.g. using the AINT Metrics platform;
 * unrelated to the metrics being forwarded to Nagios.
 *
 * @author fsiegrist (fsiegrist at groupon dot com)
 */
class NagiosHandler(val vertx: Vertx, val appMetricsFactory: MetricsFactory, val clusterId: String, val httpClientConfig: JsonObject = JsonObject()): Handler<Message<Metrics>> {
    private val httpClientsMap = HashMap<String, HttpClient>()
    private val APP_METRICS_PREFIX = "downstream/nagios"

    companion object {
        private val log = Logger.getLogger(NagiosHandler::class.java)
        private const val SEMI = ";"
        private const val EQUAL = "="
        private const val DEFAULT_REQUEST_TIMEOUT = 1000L
    }

    override fun handle(event: Message<Metrics>) {
        val metrics = event.body()
        if (!metrics.hasAlert) {
            return event.reply(JsonObject()
                    .put("status", "success")
                    .put("code", 200)
                    .put("message", "No alert present"))
        }

        val nagiosHost = getNagiosHost(clusterId, metrics.host) ?: return
        val httpClient = httpClientsMap[nagiosHost] ?: createHttpClient(nagiosHost)

        // Metrics factory should never return null: unchecked null
        val appMetrics: com.arpnetworking.metrics.Metrics = appMetricsFactory.create()
        appMetrics.setGauge(APP_METRICS_PREFIX + "/metrics_count", metrics.metrics.size.toLong())
        val timer = appMetrics.createTimer(APP_METRICS_PREFIX + "/request")

        val httpRequest = httpClient.request(HttpMethod.POST, "/nagios/cmd.php", {
            timer.stop()
            attachStatusMetrics(appMetrics, it.statusCode())
            appMetrics.close()

            event.reply(JsonObject()
                    .put("status", getRequestStatus(it.statusCode()))
                    .put("code", it.statusCode())
                    .put("message", it.statusMessage())
            )
        })

        httpRequest.exceptionHandler({
            appMetrics.close()
            log.error("handle", "exception", "unknown", it)
            event.reply(JsonObject()
                    .put("status", "error")
                    .put("code", 500)
                    .put("message", it.message))
        })

        httpRequest.putHeader("X-Remote-User", "nagios_messagebus_consumer")
        httpRequest.putHeader("Content-type", "text/plain")
        httpRequest.setTimeout(httpClientConfig.getLong("requestTimeout", DEFAULT_REQUEST_TIMEOUT))
        httpRequest.end(buildPayload(event.body()), "UTF-8")
    }

    private fun createHttpClient(host: String) : HttpClient {
        val httpOptions = HttpClientOptions(httpClientConfig)
        httpOptions.defaultHost = host
        val httpClient = vertx.createHttpClient(httpOptions)
        httpClientsMap[host] = httpClient
        return httpClient
    }

    private fun getRequestStatus(statusCode: Int) : String {
        return if (statusCode < 300) {
            "success"
        } else {
            "fail"
        }
    }

    private fun buildPayload(metric: Metrics) : String {
        val timestamp = metric.timestamp
        val hostname = metric.host
        val runEvery = metric.runInterval
        val prefix = metric.cluster + ":"

        val serviceDescription = if (arrayOf("splunk", "splunk-staging").contains(hostname) &&
                !metric.monitor.startsWith(prefix)) {
            prefix + metric.monitor
        } else {
            metric.monitor
        }

        val delay = TimeUnit.SECONDS.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS) - timestamp
        val outputMessage = if ((runEvery == 0 && delay > 3600) || (runEvery != 0 && delay > 10 * runEvery)) {
            "(lagged " + TimeUnit.MINUTES.convert(delay, TimeUnit.SECONDS) + "mins) " + metric.output
        } else {
            metric.output
        }

        return buildString {
            append("[")
            append(timestamp).append("] PROCESS_SERVICE_CHECK_RESULT;")
            append(hostname)
            append(SEMI)
            append(serviceDescription)
            append(SEMI)
            append(metric.status)
            append(SEMI)
            append(outputMessage)
            append("|")

            if (metric.metrics.isNotEmpty()) {
                metric.metrics.forEach({
                    append(it.key)
                    append(EQUAL)
                    append(it.value)
                    append(SEMI)
                })
                removeSuffix(SEMI)
            }
        }
    }

    private fun getNagiosHost(cluster: String, host: String) : String? {
        val bucket = calculateBucket(host)
        val nagiosClusterConfig = vertx.sharedData().getLocalMap<String, NagiosClusters?>("configs").get(NagiosClusterLoader.NAME)
        return nagiosClusterConfig?.getHost(cluster, bucket)
    }

    private fun calculateBucket(host: String) : Int {
        return host.toByteArray(Charsets.UTF_8).sum() % 100
    }

    private fun attachStatusMetrics(metric: com.arpnetworking.metrics.Metrics, statusCode: Int) {
        var count2xx: Long = 0
        var count4xx: Long = 0
        var count5xx: Long = 0
        var countOther: Long = 0

        when (statusCode) {
            in 200..299 -> count2xx += 1
            in 400..499 -> count4xx += 1
            in 500..599 -> count5xx += 1
            else -> countOther += 1
        }

        metric.incrementCounter(APP_METRICS_PREFIX + "/status/2xx", count2xx)
        metric.incrementCounter(APP_METRICS_PREFIX + "/status/4xx", count4xx)
        metric.incrementCounter(APP_METRICS_PREFIX + "/status/5xx", count5xx)
        metric.incrementCounter(APP_METRICS_PREFIX + "/status/other", countOther)
    }
}
