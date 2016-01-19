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
package com.groupon.aint.kmond.output

import com.groupon.aint.kmond.Metrics
import com.groupon.vertx.utils.Logger
import io.vertx.core.Handler
import io.vertx.core.eventbus.Message

/**
 * Output handler for logging.
 *
 * @author Gil Markham (gil at groupon dot com)
 */
class LoggingHandler(): Handler<Message<Metrics>> {
    companion object {
        private val log = Logger.getLogger(LoggingHandler::class.java)
    }

    override fun handle(event: Message<Metrics>) {
        log.info("handle", "metricsReceived", arrayOf("metrics"), event.body())
    }
}
