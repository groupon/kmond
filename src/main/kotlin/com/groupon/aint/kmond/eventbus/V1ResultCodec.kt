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
package com.groupon.aint.kmond.eventbus

import com.groupon.aint.kmond.input.V1Result
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.MessageCodec

/**
 * Marshalling and unmarshalling of V1Result across Vert.x event bus.
 *
 * @author Gil Markham (gil at groupon dot com)
 */
class V1ResultCodec() : MessageCodec<V1Result, V1Result> {
    override fun name(): String? = "V1ResultCodec"

    override fun systemCodecID(): Byte  = -1

    override fun encodeToWire(buffer: Buffer?, s: V1Result?) {
        throw UnsupportedOperationException()
    }

    override fun decodeFromWire(pos: Int, buffer: Buffer?): V1Result? {
        throw UnsupportedOperationException()
    }


    override fun transform(s: V1Result?): V1Result? = s
}
