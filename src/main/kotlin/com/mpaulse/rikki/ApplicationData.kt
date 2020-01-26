/*
 * Copyright (c) 2019 Marlon Paulse
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.mpaulse.rikki

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.mpaulse.mobitra.DEFAULT_MIN_WINDOW_HEIGHT
import com.mpaulse.mobitra.DEFAULT_MIN_WINDOW_WIDTH
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path

class ApplicationData(
    appHomePath: Path
) {

    private val dataFilePath = appHomePath.resolve("application.dat")
    private val jsonHandler = jacksonObjectMapper()
    private val logger = LoggerFactory.getLogger(ApplicationData::class.java)

    var windowSize = DEFAULT_MIN_WINDOW_WIDTH to DEFAULT_MIN_WINDOW_HEIGHT
    var windowPosition: Pair<Double, Double>? = null

    var routerIPAddress: String? = null
    var autoStart = true

    init {
        if (Files.exists(dataFilePath)) {
            try {
                val data = jsonHandler.readValue<MutableMap<String, Any>>(
                    dataFilePath.toFile(),
                    object : TypeReference<MutableMap<String, Any>>() {})

                val windowWidth = data["windowWidth"] as? Double
                val windowHeight = data["windowHeight"] as? Double
                if (windowWidth != null && windowHeight != null) {
                    windowSize = windowWidth to windowHeight
                }

                val windowPosX = data["windowPosX"] as? Double
                val windowPosY = data["windowPosY"] as? Double
                if (windowPosX != null && windowPosY != null) {
                    windowPosition = windowPosX to windowPosY
                }

                routerIPAddress = data["routerIPAddress"] as? String
                autoStart = data["autoStart"] as? Boolean ?: true
            } catch (e: Throwable) {
                logger.error("Error loading application data", e)
            }
        }
    }

    fun save() {
        try {
            val data = mapOf(
                "windowWidth" to windowSize.first,
                "windowHeight" to windowSize.second,
                "windowPosX" to windowPosition?.first,
                "windowPosY" to windowPosition?.second,
                "routerIPAddress" to routerIPAddress,
                "autoStart" to autoStart)
            Files.writeString(dataFilePath, jsonHandler.writeValueAsString(data))
        } catch (e: Throwable) {
            logger.error("Error saving application data", e)
        }
    }

}
