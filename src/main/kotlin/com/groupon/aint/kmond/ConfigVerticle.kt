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

import com.groupon.aint.kmond.config.ConfigLoader
import com.groupon.aint.kmond.config.HttpFetchHandler
import com.groupon.vertx.utils.Logger
import com.groupon.vertx.utils.RescheduleHandler
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import org.reflections.Reflections
import java.io.File
import java.util.HashMap

/**
 * Verticle for KMonD configuration loading.
 *
 * @author Stuart Siegrist (fsiegrist at groupon dot com)
 */
class ConfigVerticle() : AbstractVerticle() {
    companion object {
        private val log = Logger.getLogger(ConfigVerticle::class.java)
        private const val CONFIG_LOADERS = "configLoaders"
        private const val CONFIG_LOADER_PACKAGE = "com.groupon.aint.kmond.config"
        private const val HTTP_CONFIG_LOADERS = "httpConfigLoaders"
        private const val TEMP_DIR = "tmpDir"
        private const val DEST_DIR = "destDir"
        private const val FILE_NAME = "filenameBase"
        private const val HOST = "host"
        private const val PORT = "port"
        private const val DEFAULT_PORT = 80
        private const val URL_PATH = "urlPath"
        private const val REFRESH_INTERVAL = "refreshInterval"
        private const val DEFAULT_REFRESH_INTERVAL = 60000
        const val RELOAD_CONFIGS = "reloadConfigs"
    }

    override fun start(startFuture: Future<Void>) {
        log.info("start", "serverConfig", arrayOf("config"), config())

        val config = config()
        val eventBus = vertx.eventBus()

        val configLoaders = try {
            enableConfigLoaders(config.getJsonObject(CONFIG_LOADERS, JsonObject()))
        } catch (iae: IllegalArgumentException) {
            startFuture.fail(iae)
            return
        }

        eventBus.consumer<String>(RELOAD_CONFIGS, { configLoaders[it.body()]?.handle(0L) })
        startFuture.complete(null)
    }

    private fun enableConfigLoaders(jsonObject: JsonObject) : Map<String, HttpFetchHandler> {
        val tmpDir = File(jsonObject.getString(TEMP_DIR, "/tmp"))
        if (!tmpDir.exists() && !tmpDir.mkdir()) {
            throw IllegalArgumentException(tmpDir.name + " does not exist")
        } else if (!tmpDir.isDirectory) {
            throw IllegalArgumentException(tmpDir.name + " is not a directory")
        }

        val destDir = File(jsonObject.getString(DEST_DIR, "/tmp"))
        if (!destDir.exists() && !destDir.mkdir()) {
            throw IllegalArgumentException(destDir.name + " does not exist")
        } else if (!destDir.isDirectory) {
            throw IllegalArgumentException(destDir.name + " is not a directory")
        }

        val loaderMap = HashMap<String, HttpFetchHandler>()

        val httpConfigLoaders = jsonObject.getJsonObject(HTTP_CONFIG_LOADERS, JsonObject())
        if (!httpConfigLoaders.isEmpty) {
            val loaderReflections = Reflections(CONFIG_LOADER_PACKAGE)
            val loaderClasses = loaderReflections.getSubTypesOf(ConfigLoader::class.java)
            loaderClasses.forEach {
                val loaderConstructor = it.getConstructor(Vertx::class.java, File::class.java)
                val loaderInstance = loaderConstructor.newInstance(vertx, destDir)
                val loaderConfig: JsonObject? = httpConfigLoaders.getJsonObject(loaderInstance.name())

                if (loaderConfig != null) {
                    vertx.eventBus().localConsumer(loaderInstance.name(), loaderInstance)
                    val producer = vertx.eventBus().sender<String>(loaderInstance.name())
                    val filenameBase = loaderConfig.getString(FILE_NAME)
                    val httpHandler = HttpFetchHandler(vertx,
                            producer = producer,
                            host = loaderConfig.getString(HOST),
                            port = loaderConfig.getInteger(PORT, DEFAULT_PORT),
                            urlPath = loaderConfig.getString(URL_PATH),
                            tmpDir = tmpDir,
                            destDir = destDir,
                            filenameBase = filenameBase)
                    val sourceFile = File(destDir, filenameBase)
                    val rescheduleHandler = RescheduleHandler(vertx, httpHandler,
                            loaderConfig.getInteger(REFRESH_INTERVAL, DEFAULT_REFRESH_INTERVAL))
                    if (sourceFile.exists()) {
                        producer.write(sourceFile.absolutePath)
                        rescheduleHandler.schedule()
                    } else {
                        rescheduleHandler.handle(0L)
                    }
                    loaderMap.put(loaderInstance.name(), httpHandler)
                }
            }
        }

        return loaderMap
    }
}
