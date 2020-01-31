/*
 * Rikki: Japanese-to-English OCR dictionary
 * Copyright (C) 2020 Marlon Paulse
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.mpaulse.rikki

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.mpaulse.rikki.DEFAULT_MIN_WINDOW_HEIGHT
import com.mpaulse.rikki.DEFAULT_MIN_WINDOW_WIDTH
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
                "autoStart" to autoStart)
            Files.writeString(dataFilePath, jsonHandler.writeValueAsString(data))
        } catch (e: Throwable) {
            logger.error("Error saving application data", e)
        }
    }

}
