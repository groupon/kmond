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

import com.arpnetworking.metrics.Sink
import com.arpnetworking.metrics.impl.TsdLogSink
import com.arpnetworking.metrics.impl.TsdMetricsFactory
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.spi.VertxMetricsFactory
import io.vertx.core.spi.metrics.VertxMetrics
import java.io.File
import java.util.ArrayList

/**
 * Implementation of VertxMetricsFactory using AintMetricsFactory.
 *
 * @author Gil Markham (gil at groupon dot com)
 */
class AintMetricsFactory : VertxMetricsFactory {
    override fun metrics(vertx: Vertx?, options: VertxOptions?): VertxMetrics? {
        val metricsOptions = options?.metricsOptions ?: AintMetricsOptions()

        val aintMetricsOptions: AintMetricsOptions = if (metricsOptions is AintMetricsOptions) {
            metricsOptions
        } else {
            AintMetricsOptions()
        }

        val sinkList = ArrayList<Sink>()
        if (aintMetricsOptions.enableQuerySink) {
            sinkList.add(

                    TsdLogSink.Builder()
                            .setName(aintMetricsOptions.queryLogName)
                            .setExtension(aintMetricsOptions.queryLogExtension)
                            .setMaxHistory(aintMetricsOptions.queryLogHistory)
                            .setDirectory(File(aintMetricsOptions.queryLogPath))
                            .build()
            )
        }


        val metricsAdapter = AintMetricsAdapter(TsdMetricsFactory.Builder()
                .setSinks(sinkList)
                .setClusterName(aintMetricsOptions.clusterName)
                .setServiceName(aintMetricsOptions.serviceName)
                .build())

        vertx?.setPeriodic(aintMetricsOptions.closeFrequency, {l -> metricsAdapter.close() })

        return metricsAdapter
    }

    override fun newOptions(): AintMetricsOptions {
        val metricsOptions = AintMetricsOptions()
        metricsOptions.setEnabled(true)
        return metricsOptions
    }
}
