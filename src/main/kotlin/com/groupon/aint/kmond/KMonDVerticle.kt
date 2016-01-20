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
package com.groupon.aint.kmond

import com.groupon.aint.kmond.admin.HealthcheckHandler
import com.groupon.aint.kmond.admin.ReloadConfigHandler
import com.groupon.aint.kmond.eventbus.CompositeMessageProducer
import com.groupon.aint.kmond.eventbus.V1ResultCodec
import com.groupon.aint.kmond.input.V1Handler
import com.groupon.aint.kmond.output.GangliaHandler
import com.groupon.aint.kmond.output.LoggingHandler
import com.groupon.aint.kmond.output.NagiosHandler
import com.groupon.vertx.utils.Logger
import io.vertx.core.AbstractVerticle
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Main verticle for KMonD.
 *
 * @author Gil Markham (gil at groupon dot com)
 */
class KMonDVerticle() : AbstractVerticle() {
    companion object {
        private val log = Logger.getLogger(KMonDVerticle::class.java)
        private val codecInitialized = AtomicBoolean(false)
    }

    var httpServer: HttpServer? = null

    override fun start(startFuture: Future<Void>) {
        log.info("start", "serverConfig", arrayOf("config"), config())

        val config = config()
        if (config.getString("nagiosClusterId").isNullOrBlank()) {
            startFuture.fail(IllegalArgumentException("Missing nagiosClusterId in config"))
            return
        }
        val nagiosClusterId = config.getString("nagiosClusterId")

        val eventBus = vertx.eventBus()
        if (codecInitialized.compareAndSet(false, true)) {
            eventBus.registerCodec(V1ResultCodec())
        }

        val router = Router.router(vertx)
        val serverOptions = HttpServerOptions(config)
        val server = vertx.createHttpServer(serverOptions)


        val gangliaConsumer = eventBus.consumer<Metrics>("ganglia", GangliaHandler(vertx))
        val nagiosConsumer = eventBus.consumer<Metrics>("nagios", NagiosHandler(vertx, nagiosClusterId,
                config.getJsonObject("nagiosHttpClient", JsonObject())))
        val loggingConsumer = eventBus.consumer<Metrics>("logger", LoggingHandler())
        loggingConsumer.pause()

        val deliveryOptions = DeliveryOptions()
        deliveryOptions.setCodecName("V1ResultCodec")
        val compositeProducer = CompositeMessageProducer<Metrics>(eventBus.sender(gangliaConsumer.address(), deliveryOptions),
                eventBus.sender(nagiosConsumer.address(), deliveryOptions), eventBus.sender(loggingConsumer.address(), deliveryOptions))

        router.route().handler { context ->
            val startTime = System.nanoTime()
            context.addBodyEndHandler {
                val routePath: String = context.get<String?>("routePath") ?: "null"
                val totalTime = TimeUnit.MILLISECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS)
                log.info("handle", "requestEnd", arrayOf("route", "url", "status", "totalTime"),
                        routePath, context.normalisedPath(), context.response().statusCode, totalTime)
            }
            context.next()
        }

        // We force the content-type header as we only accept url-encoded forms and not all clients send that header.
        // Without that header vertx won't automatically process the body as a form
        router.post("/results").handler {
            it.request().headers().set(HttpHeaders.CONTENT_TYPE, HttpHeaders.APPLICATION_X_WWW_FORM_URLENCODED)
            it.next()
        }

        // Process the body so the handler can just access it
        router.post().handler(BodyHandler.create())

        // Primary handler for metrics
        router.post("/results")
                .consumes(HttpHeaders.APPLICATION_X_WWW_FORM_URLENCODED.toString())
                .handler(V1Handler(compositeProducer))

        // Add the config reload handler for manually triggering a reload
        router.post("/admin/config/:configName/reload").handler(
                ReloadConfigHandler(eventBus.sender<String>(ConfigVerticle.RELOAD_CONFIGS, DeliveryOptions()))
        )

        // Add healthcheck endpoint
        val healthcheckHandler = HealthcheckHandler(config().getString("heartbeatPath", "heartbeat.txt"))
        val healthCheckUrlPath = config().getString("heartbeatUrlPath", "/grpn/healthcheck")
        router.head(healthCheckUrlPath).handler(healthcheckHandler)
        router.get(healthCheckUrlPath).handler(healthcheckHandler)

        router.route().failureHandler {
            it.response().setStatusCode(500)
            it.response().end(it.failure().message)
        }

        server.requestHandler({ request -> router.accept(request) })

        server.listen({ listenResult: AsyncResult<HttpServer> ->
            if (listenResult.succeeded()) {
                httpServer = listenResult.result()
                startFuture.complete()
            } else {
                startFuture.fail(listenResult.cause())
            }
        })
    }

    override fun stop(stopFuture: Future<Void>) {
        val server = httpServer
        if (server != null) {
            server.close({ closeResult: AsyncResult<Void> ->
                if (closeResult.succeeded()) {
                    stopFuture.complete()
                } else {
                    stopFuture.fail(closeResult.cause())
                }
            })
        } else {
            stopFuture.complete()
        }
    }
}
