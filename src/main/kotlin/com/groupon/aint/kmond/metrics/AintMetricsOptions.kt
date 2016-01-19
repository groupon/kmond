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
package com.groupon.aint.kmond.metrics

import io.vertx.core.metrics.MetricsOptions

/**
 * Structure for configuring AINT metrics.
 *
 * @author Gil Markham (gil at groupon dot com)
 */
class AintMetricsOptions(var enableQuerySink: Boolean = true,
                         var queryLogName: String = "query",
                         var queryLogExtension: String = ".log",
                         var queryLogHistory: Int = 4,
                         var queryLogPath: String = "log",
                         var clusterName: String = "change_me",
                         var serviceName: String = "vertx") : MetricsOptions()
