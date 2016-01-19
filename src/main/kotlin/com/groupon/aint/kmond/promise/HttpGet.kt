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
package com.groupon.aint.kmond.promise

import com.groupon.promise.AsyncPromiseFunction
import com.groupon.promise.DefaultPromiseFuture
import com.groupon.promise.PromiseFuture
import io.vertx.core.http.HttpClient
import io.vertx.core.http.HttpClientResponse

/**
 * Asynchronously execute a HTTP GET, passes along the HttpClientResponse on success.
 *
 * @author Gil Markham (gil at groupon dot com)
 */
class HttpGet(val httpClient: HttpClient, val host: String, val port: Int, val urlPath: String): AsyncPromiseFunction<Void, HttpClientResponse> {
    override fun handle(data: Void?): PromiseFuture<out HttpClientResponse>? {
        val future = DefaultPromiseFuture<HttpClientResponse>()
        val request = httpClient.get(port, host, urlPath) { response ->
            response.exceptionHandler { future.setFailure(it) }
            future.setResult(response)
        }
        request.exceptionHandler { future.setFailure(it) }
        request.end()

        return future
    }
}
