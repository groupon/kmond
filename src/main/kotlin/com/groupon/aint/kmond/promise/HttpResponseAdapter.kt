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
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpClientResponse

/**
 * Converts an http client response into a Pair<HttpClientResponse, Buffer> on 200, or an exception otherwise.
 *
 * @author Gil Markham (gil at groupon dot com)
 */
class HttpResponseAdapter : AsyncPromiseFunction<HttpClientResponse, Pair<HttpClientResponse, Buffer>> {
    override fun handle(response: HttpClientResponse): PromiseFuture<out Pair<HttpClientResponse, Buffer>>? {
        val future = DefaultPromiseFuture<Pair<HttpClientResponse, Buffer>>()
        response.bodyHandler { body: Buffer ->
            if (response.statusCode() == 200) {
                future.setResult(Pair(response, body))
            } else {
                future.setFailure(HttpResponseException(response, body))
            }
        }
        return future
    }
}

class HttpResponseException(val response: HttpClientResponse, val body: Buffer): Exception(response.statusMessage()) {

}
