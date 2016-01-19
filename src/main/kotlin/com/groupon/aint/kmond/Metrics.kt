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
package com.groupon.aint.kmond

/**
 * Interface for metrics.
 *
 * @author Gil Markham (gil at groupon dot com)
 */
interface Metrics {
    val path: String
    val monitor: String
    val status: Int
    val output: String
    val runInterval: Int
    val timestamp: Long
    val host: String
    val cluster: String
    val metrics: Map<String, Float>
}
