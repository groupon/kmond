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
package com.groupon.aint.kmond.input

import com.groupon.aint.kmond.Metrics
import com.groupon.aint.kmond.exception.InvalidParameterException
import io.vertx.core.MultiMap
import java.util.HashMap
import kotlin.text.Regex

/**
 * Input model for classic (v1) form based messages.
 *
 * @author Gil Markham (gil at groupon dot com)
 */
data class V1Result(override val path: String,
                    override val monitor: String,
                    override val status: Int,
                    override val output: String,
                    override val runInterval: Int,
                    override val timestamp: Long,
                    override val host: String,
                    override val cluster: String,
                    override val hasAlert: Boolean,
                    override val metrics: Map<String, Float>) : Metrics {
    companion object {
        fun build(formParams: MultiMap): V1Result {
            val pathParam = formParams.get("path") ?: throw InvalidParameterException("path is required")
            val monitor = formParams.get("monitor") ?: throw InvalidParameterException("monitor is required")
            val status = try {
                formParams.get("status")?.toInt() ?: throw InvalidParameterException("status is required")
            } catch (e: NumberFormatException) {
                throw InvalidParameterException("status must be an integer")
            }
            val output = (formParams.get("output") ?: throw InvalidParameterException("output is required")).replace(Regex("\\s"), " ").trim()
            val runInterval = try {
                formParams.get("runs_every")?.toInt() ?: 300
            } catch (e: NumberFormatException) {
                throw InvalidParameterException("runs_every must be an integer")
            }
            val timestamp = try {
                formParams.get("timestamp")?.toLong() ?: System.currentTimeMillis()
            } catch (e: NumberFormatException) {
                throw InvalidParameterException("timestamp must be a long")
            }
            val hasAlert = formParams.get("has_alert")?.toBoolean() ?: false
            val pathElements = pathParam.split('/')
            val cluster = pathElements.first()
            val host = pathElements.last()

            return V1Result(path = pathParam, monitor = monitor, status = status, output = output,
                    runInterval = runInterval, timestamp = timestamp,
                    host = host, cluster = cluster, hasAlert = hasAlert, metrics = toMetrics(output))
        }

        private fun toMetrics(metricsString: String): Map<String, Float> {
            val outputParts = metricsString.split('|', limit = 2)
            if (outputParts.size == 2) {
                val metricParts = outputParts[1].split(regex = "[ ;]".toRegex())
                val metricMap = HashMap<String, Float>()
                metricParts.filter { it.contains("=") }.map { kvPair ->
                    val (key, value) = kvPair.split("=", limit = 2)
                    metricMap.put(key, value.toFloat())
                }
                return metricMap
            } else {
                return emptyMap()
            }
        }
    }
}
