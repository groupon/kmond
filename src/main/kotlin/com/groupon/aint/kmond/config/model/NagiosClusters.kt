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
package com.groupon.aint.kmond.config.model

import io.vertx.core.shareddata.Shareable
import java.util.HashMap

/**
 * A data structure to store the Nagios cluster to bucket to host mapping.
 *
 * @author Stuart Siegrist (fsiegrist at groupon dot com)
 */
class NagiosClusters(initClusterMap: Map<String,Map<String, List<Int>>>) : Shareable {
    private val clusterMap: Map<String, Map<Int, String>>

    init {
        clusterMap = HashMap()
        initClusterMap.forEach {
            val (key, value) = it
            val bucketMap = HashMap<Int, String>()

            value.forEach {
                val (key2, value2) = it
                value2.forEach {
                    bucketMap.put(it, key2)
                }
            }
            clusterMap.put(key, bucketMap)
        }
    }

    fun getHost(cluster: String, bucket: Int) : String? {
        val clusterBuckets = clusterMap[cluster]
        return clusterBuckets?.get(bucket)
    }
}
