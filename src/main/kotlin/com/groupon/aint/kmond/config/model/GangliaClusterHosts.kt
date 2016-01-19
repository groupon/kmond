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
 * A data structure to store the monitoring-cluster to Ganglia host mapping.
 *
 * @author Gil Markham (gil at groupon dot com)
 */
class GangliaClusterHosts(private val clusterMap: Map<String,Set<String>>) : Shareable {
    fun getHosts(cluster: String) = clusterMap[cluster] ?: emptySet<String>()
}
