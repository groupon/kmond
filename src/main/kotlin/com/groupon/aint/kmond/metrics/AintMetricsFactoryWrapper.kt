package com.groupon.aint.kmond.metrics

import com.arpnetworking.metrics.MetricsFactory

/**
 * A static holder for AintMetrics to serve the same factory to vertx and the rest of the application.
 *
 * @author Matthew Hayter (mhayter at groupon dot com)
 */
object AintMetricsFactoryWrapper {
    private var metricsFactory: MetricsFactory? = null

    fun initialize(tsdMetricsFactory: MetricsFactory) {
        metricsFactory = tsdMetricsFactory
    }

    fun getMetricsFactory(): MetricsFactory? {
        return metricsFactory
    }
}
