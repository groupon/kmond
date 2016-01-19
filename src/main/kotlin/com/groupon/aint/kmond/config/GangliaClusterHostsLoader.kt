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
import com.groupon.aint.kmond.config.model.GangliaClusterHosts
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import java.io.File
import java.util.HashMap
import java.util.HashSet

/**
 * Loads the mapping of monitoring-cluster to Ganglia hosts (see the example
 * config in src/test/resources).
 *
 * @author Gil Markham (gil at groupon dot com)
 */
class GangliaClusterHostsLoader(vertx: Vertx, destDir: File): ConfigLoader<GangliaClusterHosts>(vertx, destDir) {
    companion object {
        private val yamlParser = ObjectMapper(YAMLFactory())
        const val NAME = "gangliaClusterHosts"
    }

    override fun parse(buffer: Buffer): GangliaClusterHosts? {
        val typeRef = object : TypeReference<HashMap<String, Map<String, List<String>>>>() {}
        val environments : Map<String, Map<String, List<String>>> = yamlParser.readValue(buffer.bytes, typeRef)
        val multiMap = HashMap<String, HashSet<String>>()
        environments.values.forEach { hostMap ->
            hostMap.forEach { entry ->
                val (host, clusters) = entry
                clusters.forEach { cluster ->
                    val newSet = HashSet<String>()
                    val hosts = multiMap.putIfAbsent(cluster, newSet) ?: newSet
                    hosts.add(host)
                }
            }
        }

        if (multiMap.size > 0) {
            return GangliaClusterHosts(multiMap)
        } else {
            return null
        }
    }

    override fun verify(config: GangliaClusterHosts): GangliaClusterHosts {
        return config;
    }

    override fun name(): String = NAME
}
