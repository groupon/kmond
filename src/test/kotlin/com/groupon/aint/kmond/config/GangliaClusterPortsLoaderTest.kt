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
package com.groupon.aint.kmond.config

import com.groupon.aint.kmond.config.model.GangliaClusterPorts
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.Message
import io.vertx.core.file.FileSystem
import io.vertx.core.shareddata.LocalMap
import io.vertx.core.shareddata.Shareable
import io.vertx.core.shareddata.SharedData
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Matchers
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.io.File
import kotlin.test.assertEquals

/**
 * Tests for GangliaClusterPortsLoader.
 *
 * @author Gil Markham (gil at groupon dot com)
 */
class GangliaClusterPortsLoaderTest {
    val vertx = mock<Vertx>()
    val fileSystem = mock<FileSystem>()
    val sharedData = mock<SharedData>()
    val localMap = mock<LocalMap<String, Shareable>>()
    val message = mock<Message<String?>>()
    val bufferCaptor = captor<Handler<AsyncResult<Buffer>>>()
    val loader = GangliaClusterPortsLoader(vertx, File("/tmp"))
    val configCaptor = captor<GangliaClusterPorts>()

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        Mockito.`when`(vertx.fileSystem()).thenReturn(fileSystem)
        Mockito.`when`(vertx.sharedData()).thenReturn(sharedData)
        Mockito.`when`(sharedData.getLocalMap<String, Shareable>(Matchers.anyString())).thenReturn(localMap)
    }

    @Test
    fun testReadExampleFile() {
        val buffer = Buffer.buffer(javaClass.getResourceAsStream("/ganglia_port_clusters.yml").reader(Charsets.UTF_8).readText())
        Mockito.`when`(message.body()).thenReturn("foo")
        loader.handle(message)
        Mockito.verify(fileSystem).readFile(Matchers.eq("foo"), bufferCaptor.capture())
        bufferCaptor.value.handle(Future.succeededFuture(buffer))
        Mockito.verify(sharedData).getLocalMap<String, Shareable>(Matchers.eq("configs"))
        Mockito.verify(localMap).put(Matchers.eq(GangliaClusterPortsLoader.NAME), configCaptor.capture())
        val config = configCaptor.value
        assertEquals(8691, config.getPort("cluster43"))
    }
}


inline fun <reified T : Any> mock(): T = Mockito.mock(T::class.java)
inline fun <reified T : Any> captor(): ArgumentCaptor<T> = ArgumentCaptor.forClass(T::class.java)
