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
import com.groupon.vertx.utils.Logger
import io.netty.handler.codec.http.HttpHeaders
import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServerResponse
import io.vertx.ext.web.RoutingContext

/**
 * Simple heartbeat handler based on existence of a file.
 *
 * @author Gil Markham (gil at groupon dot com)
 */
class HealthcheckHandler(val filePath: String) : InstrumentedRouteHandler() {
    companion object {
        val log = Logger.getLogger(HealthcheckHandler::class.java)
        const val CONTENT_TYPE = "plain/text"
        const val CACHE_CONTROL = "private, no-cache, no-store, must-revalidate"
    }

    override fun internalHandle(context: RoutingContext) {
        context.vertx().fileSystem().exists(filePath) {
            if( it.succeeded()) {
                processHealthcheckResponse(it.result(), context)
            } else {
                processExceptionResponse(context, it.cause())
            }
        }
    }

    fun processHealthcheckResponse(exists: Boolean, context: RoutingContext) {
        val response = context.response()
        val requestMethod = context.request().method()
        val includeBody = requestMethod == HttpMethod.GET

        val status = if(exists) {
            HttpResponseStatus.OK
        } else {
            HttpResponseStatus.SERVICE_UNAVAILABLE
        }

        setCommonHttpResponse(response, status)

        val responseBody = status.reasonPhrase()
        if (includeBody) {
            response.end(responseBody)
        } else {
            response.putHeader(HttpHeaders.Names.CONTENT_LENGTH, "" + responseBody.length)
            response.end()
        }
    }

    fun processExceptionResponse(context: RoutingContext, ex: Throwable) {
        val status = HttpResponseStatus.SERVICE_UNAVAILABLE;
        val response = context.response()
        val requestMethod = context.request().method()
        val includeBody = requestMethod == HttpMethod.GET

        val responseBody = status.reasonPhrase() + ": " + ex.message

        setCommonHttpResponse(response, status)
        if (includeBody) {
            response.end(responseBody)
        } else {
            response.putHeader(HttpHeaders.Names.CONTENT_LENGTH, "" + responseBody.length)
            response.end()
        }
    }

    private fun setCommonHttpResponse(response: HttpServerResponse, status: HttpResponseStatus) {
        response.putHeader(HttpHeaders.Names.CONTENT_TYPE, CONTENT_TYPE)
        response.putHeader(HttpHeaders.Names.CACHE_CONTROL, CACHE_CONTROL)
        response.setStatusCode(status.code())
        response.setStatusMessage(status.reasonPhrase())
    }
}
