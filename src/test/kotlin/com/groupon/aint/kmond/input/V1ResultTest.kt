/*
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

import io.vertx.core.MultiMap
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * @author Gil Markham (gil at groupon dot com)
 */
class V1ResultTest {
    val defaultOutput = "OK|foo=0.1000;bar=0.0000;baz=1.0000"

    @Test(expected = IllegalArgumentException::class)
    fun testFailsOnMissingPathParam() {
        val formParams = buildSampleFormParams()
        formParams.remove("path")
        V1Result.build(formParams)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testFailsOnMissingMonitorParam() {
        val formParams = buildSampleFormParams()
        formParams.remove("monitor")
        V1Result.build(formParams)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testFailsOnMissingStatusParam() {
        val formParams = buildSampleFormParams()
        formParams.remove("status")
        V1Result.build(formParams)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testFailsOnMissingOutputParam() {
        val formParams = buildSampleFormParams()
        formParams.remove("output")
        V1Result.build(formParams)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testFailsOnNonIntegerStatusParam() {
        val formParams = buildSampleFormParams()
        formParams.set("status","foo")
        V1Result.build(formParams)
    }


    @Test
    fun testResultsInExpectedValues() {
        val timestamp = System.currentTimeMillis()
        val result = V1Result.build(buildSampleFormParams(timestamp))
        assertEquals("cluster/host", result.path)
        assertEquals("monitor", result.monitor)
        assertEquals(0, result.status)
        assertEquals(defaultOutput, result.output)
        assertEquals(60, result.runInterval)
        assertEquals(timestamp, result.timestamp)
        assertEquals("host", result.host)
        assertEquals("cluster", result.cluster)
    }

    @Test
    fun testDefaultsMissingValues() {
        val timestamp = System.currentTimeMillis() - 100
        val formParams = buildSampleFormParams(timestamp)
        formParams.remove("runs_every")
        formParams.remove("timestamp")
        val result = V1Result.build(formParams)
        assertEquals("cluster/host", result.path)
        assertEquals("monitor", result.monitor)
        assertEquals(0, result.status)
        assertEquals(defaultOutput, result.output)
        assertEquals(300, result.runInterval)
        assertTrue(result.timestamp > timestamp)
    }

    @Test
    fun testEmptyOutput() {
        val timestamp = System.currentTimeMillis()
        val formParams = buildSampleFormParams(timestamp)
        formParams.set("output","")
        val result = V1Result.build(formParams)
        assertEquals("cluster/host", result.path)
        assertEquals("monitor", result.monitor)
        assertEquals(0, result.status)
        assertEquals("", result.output)
        assertEquals(0, result.metrics.size)
        assertEquals(60, result.runInterval)
        assertEquals(timestamp, result.timestamp)
        assertEquals("host", result.host)
        assertEquals("cluster", result.cluster)
    }

    @Test
    fun testBuildsMetricsObject() {
        val result = V1Result.build(buildSampleFormParams())
        assertEquals(3, result.metrics.size)
        assertEquals(0.1000f, result.metrics["foo"])
    }

    @Test
    fun testBuildsMetricsIgnoringNonKVPairs() {
        val formParams = buildSampleFormParams()
        formParams.set("monitor", "ntp")
        formParams.set("output", "OK|offset=0.1000;0.5000;1.0000;5.0000")
        val result = V1Result.build(formParams)
        assertEquals(1, result.metrics.size)
    }

    fun buildSampleFormParams(timestamp : Long = System.currentTimeMillis()) : MultiMap {
        val formParams = MultiMap.caseInsensitiveMultiMap()
        formParams.set("path", "cluster/host")
        formParams.set("monitor", "monitor")
        formParams.set("status", "0")
        formParams.set("output", defaultOutput)
        formParams.set("runs_every", "60")
        formParams.set("timestamp", timestamp.toString())
        return formParams
    }
}
