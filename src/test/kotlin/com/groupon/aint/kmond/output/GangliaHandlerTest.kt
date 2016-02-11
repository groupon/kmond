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
package com.groupon.aint.kmond.output

import com.groupon.aint.kmond.config.mock
import com.groupon.aint.kmond.input.V1Result
import io.vertx.core.Vertx
import io.vertx.core.datagram.DatagramSocket
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import java.util.HashMap
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for GangliaHandler.
 * 
 * @author Stuart Siegrist (fsiegrist at groupon dot com)
 */
class GangliaHandlerTest {
    private val vertx = mock<Vertx>()
    private val datagramSocket = mock<DatagramSocket>()
    private val metricsMap = HashMap<String, Float>()

    @Before
    fun setUp() {
        Mockito.`when`(vertx.createDatagramSocket()).thenReturn(datagramSocket)
    }

    @Test
    fun isNotLaggingTest() {
        val gangliaHandler = GangliaHandler(vertx)
        assertFalse(gangliaHandler.isLagging(V1Result("path", "monitor", 0, "output", 1,
                TimeUnit.SECONDS.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS), "host", "cluster", false, metricsMap)))
    }

    @Test
    fun isLaggingTest() {
        val gangliaHandler = GangliaHandler(vertx)
        assertTrue(gangliaHandler.isLagging(V1Result("path", "monitor", 0, "output", 1,
                TimeUnit.SECONDS.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS) - 3700, "host", "cluster", false, metricsMap)))
    }

    @Test
    fun statusValueNotLaggingTest() {
        val gangliaHandler = GangliaHandler(vertx)
        assertEquals(0, gangliaHandler.statusValue(V1Result("path", "monitor", 0, "output", 1,
                TimeUnit.SECONDS.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS), "host", "cluster", false, metricsMap)))
    }

    @Test
    fun statusValueLaggingTest() {
        val gangliaHandler = GangliaHandler(vertx)
        assertEquals(4, gangliaHandler.statusValue(V1Result("path", "monitor", 0, "output", 1,
                TimeUnit.SECONDS.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS) - 3700, "host", "cluster", false, metricsMap)))
    }
}
