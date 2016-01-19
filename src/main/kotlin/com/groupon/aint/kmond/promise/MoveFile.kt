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
import com.groupon.promise.exception.ExceptionUtils
import com.groupon.vertx.utils.Logger
import io.vertx.core.file.FileSystem
import java.io.File

/**
 * Asynchronously move a file, passes along the absolute destination path on success.
 *
 * @author Stuart Siegrist (fsiegrist at groupon dot com)
 */
class MoveFile<T>(val fileSystem: FileSystem, val tmpFile: String, val destDir: File): AsyncPromiseFunction<T, String> {
    companion object {
        private val log = Logger.getLogger(MoveFile::class.java)
    }

    override fun handle(t: T?): PromiseFuture<String> {
        val future = DefaultPromiseFuture<String>()

        val tempFile = File(tmpFile)

        if ("tmp".equals(tempFile.extension)) {
            val destFile = File(destDir, tempFile.nameWithoutExtension)
            val archFile = File(destDir, tempFile.nameWithoutExtension + ".bak")

            val tempPath = tempFile.absolutePath
            val destPath = destFile.absolutePath
            val archPath = archFile.absolutePath

            val tempExists = tempFile.exists()
            val destExists = destFile.exists()
            val archExists = archFile.exists()

            if (tempExists) {
                try {
                    if (destExists) {
                        if (archExists) {
                            fileSystem.deleteBlocking(archPath)
                            log.info("moveFile", "deletedArchive", arrayOf("archFile"), archPath)
                        }

                        fileSystem.moveBlocking(destPath, archPath)
                        log.info("moveFile", "archivedActiveFile", arrayOf("activeFile", "archFile"), destPath, archPath)
                    }

                    fileSystem.move(tempPath, destPath) { result ->
                        if (result.succeeded()) {
                            log.info("moveFile", "movedTempToActive", arrayOf("tmpFile", "activeFile"), tempPath, destPath)
                            future.setResult(destPath)
                        } else {
                            log.error("moveFile", "exception", "unknown", arrayOf("tmpFile", "destFile", "file"), tempPath,
                                    destPath, tempFile.nameWithoutExtension, ExceptionUtils.getMostSignificantCause(result.cause()))
                            future.setFailure(result.cause())
                        }
                    }
                } catch (ex: Exception) {
                    log.error("moveFile", "exception", "unknown", arrayOf("tmpFile", "destFile", "file"), tempPath,
                            destPath, tempFile.nameWithoutExtension, ExceptionUtils.getMostSignificantCause(ex))
                    future.setFailure(ex)
                }
            } else {
                log.warn("moveFile", "failedNoTempFile", arrayOf("tempFile"), tempPath)
                future.setResult(null)
            }
        } else {
            // Don't archive any files since this wasn't a temp file.
            future.setResult(null)
        }
        return future
    }
}
