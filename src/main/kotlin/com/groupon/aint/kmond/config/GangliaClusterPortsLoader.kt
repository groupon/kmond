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

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.groupon.aint.kmond.config.model.GangliaClusterPorts
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import java.io.File
import java.util.HashMap

/**
 * Loads the mapping of Gangilia ports to monitoring-cluster (see the example config in src/test/resources).
 *
 * @author Gil Markham (gil at groupon dot com)
 */
class GangliaClusterPortsLoader(vertx: Vertx, destDir: File): ConfigLoader<GangliaClusterPorts>(vertx, destDir) {
    companion object {
        private val yamlParser = ObjectMapper(YAMLFactory())
        const val NAME = "gangliaClusterPorts"
    }

    override fun parse(buffer: Buffer): GangliaClusterPorts {
        val typeRef = object : TypeReference<HashMap<Int, String>>() {}
        val result : Map<Int, String> = yamlParser.readValue(buffer.bytes, typeRef)
        return GangliaClusterPorts(result)
    }

    override fun verify(config: GangliaClusterPorts): GangliaClusterPorts {
        return config;
    }

    override fun name(): String  = NAME
}
