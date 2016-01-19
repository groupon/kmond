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
import com.groupon.promise.Promise
import com.groupon.promise.PromiseImpl
import com.groupon.promise.SyncPromiseFunction

/**
 * Extensions to the Groupon Promise library.
 *
 * @author Gil Markham (gil at groupon dot com)
 */

fun <T : Any?> promise(init: Promise<T>.() -> Unit): Promise<T> {
    val promise = PromiseImpl<T>()
    promise.init()
    return promise
}

fun <T : Any?, B : Any?> Promise<T>.async(thenFunc: AsyncPromiseFunction<T, B>, init: (Promise<B>.() -> Any)? = null): Promise<B> {
    val newPromise = this.thenAsync(thenFunc)
    if (init != null) {
        newPromise.init()
    }
    return newPromise
}

fun <T : Any?, B : Any?> Promise<T>.sync(thenFunc: SyncPromiseFunction<T, B>, init: (Promise<B>.() -> Any)? = null): Promise<B> {
    val newPromise = this.thenSync(thenFunc)
    if (init != null) {
        newPromise.init()
    }
    return newPromise
}
