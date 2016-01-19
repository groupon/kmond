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

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonToken
import com.groupon.aint.kmond.config.model.NagiosClusters
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import java.io.ByteArrayInputStream
import java.io.File
import java.util.ArrayList
import java.util.HashMap
import java.util.zip.GZIPInputStream

/**
 * Loads the mapping of Nagios cluster and bucket (see the example config in src/test/resources).
 *
 * @author fsiegrist (fsiegrist at groupon dot com)
 */
class NagiosClusterLoader(vertx: Vertx, destDir: File, val unzip: Boolean): ConfigLoader<NagiosClusters>(vertx, destDir) {

    constructor(vertx: Vertx, destDir: File): this(vertx, destDir, true)

    companion object {
        private val jsonFactory = JsonFactory()
        const val NAME = "nagiosClusters"
    }

    override fun parse(buffer: Buffer): NagiosClusters {
        val byteStream = ByteArrayInputStream(buffer.bytes)
        val finalStream = if (unzip) {
            GZIPInputStream(byteStream)
        } else {
            byteStream
        }

        val jsonParser = jsonFactory.createParser(finalStream)
        val result = HashMap<String,Map<String,List<Int>>>()

        while (jsonParser.nextToken() != null) {
            if ("nagios_clusters".equals(jsonParser.currentName)) {
                if (jsonParser.nextToken() == JsonToken.START_OBJECT) {
                    while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                        val environment = jsonParser.currentName ?: break;
                        val servers = HashMap<String, List<Int>>()

                        if (jsonParser.nextToken() == JsonToken.START_OBJECT) {
                            while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                                val server = jsonParser.currentName ?: break;
                                val ports = ArrayList<Int>()

                                if (jsonParser.nextToken() == JsonToken.START_ARRAY) {
                                    while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
                                        ports.add(jsonParser.intValue)
                                    }
                                }

                                servers.put(server, ports)
                            }
                        }

                        result.put(environment, servers)
                    }
                }
            }
        }

        return NagiosClusters(result)
    }

    override fun verify(config: NagiosClusters): NagiosClusters {
        return config;
    }

    override fun name(): String = NAME
}
