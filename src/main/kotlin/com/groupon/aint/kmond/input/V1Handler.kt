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
package com.groupon.aint.kmond.input

import com.groupon.aint.kmond.Metrics
import com.groupon.aint.kmond.util.InstrumentedRouteHandler
import com.groupon.vertx.utils.Logger
import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.eventbus.MessageProducer
import io.vertx.ext.web.RoutingContext

/**
 * Request Handler for classic (v1) form based messages.
 *
 * @author Gil Markham (gil at groupon dot com)
 */
class V1Handler(val producer: MessageProducer<Metrics>) : InstrumentedRouteHandler() {
    companion object {
        val log = Logger.getLogger(V1Handler::class.java)
    }
    override fun internalHandle(context: RoutingContext) {
        val formAttributes = context.request().formAttributes()
        producer.write(V1Result.build(formAttributes))
        context.response().setStatusCode(HttpResponseStatus.OK.code())
        context.response().end(HttpResponseStatus.OK.reasonPhrase())
    }
}
