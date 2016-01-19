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
package com.groupon.aint.kmond.admin

import com.groupon.aint.kmond.util.InstrumentedRouteHandler
import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.eventbus.MessageProducer
import io.vertx.ext.web.RoutingContext

/**
 * Simple http request handler for manually triggering a http config reload.
 *
 * @author Gil Markham (gil at groupon dot com)
 */
class ReloadConfigHandler(val configProducer: MessageProducer<String>) : InstrumentedRouteHandler() {

    override fun internalHandle(context: RoutingContext) {
        val configName = context.request().getParam("configName")
        if (configName.isNullOrEmpty()) {
            context.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
            context.response().end()
        } else {
            configProducer.write(configName)
            context.response().setStatusCode(HttpResponseStatus.ACCEPTED.code())
            context.response().end()
        }
    }
}
