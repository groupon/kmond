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
import io.vertx.core.datagram.DatagramSocket

/**
 * Async promise function for sending a buffer to a datagram socket.
 *
 * @author Gil Markham (gil at groupon dot com)
 */
class SendDatagramPacket(val packet: Buffer, val host: String, val port: Int) : AsyncPromiseFunction<DatagramSocket, DatagramSocket> {
    override fun handle(socket: DatagramSocket): PromiseFuture<out DatagramSocket> {
        val future = DefaultPromiseFuture<DatagramSocket>()
        socket.send(packet, port, host) {
            if (it.succeeded()) {
                future.setResult(it.result())
            } else {
                future.setFailure(it.cause())
            }
        }

        return future
    }
}
