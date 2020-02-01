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
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path

class Settings(
    appHomePath: Path
) {

    private val dataFilePath = appHomePath.resolve("settings.cfg")
    private val jsonHandler = jacksonObjectMapper()
    private val logger = LoggerFactory.getLogger(Settings::class.java)

    var autoStart = false

    init {
        if (Files.exists(dataFilePath)) {
            try {
                val data = jsonHandler.readValue<MutableMap<String, Any>>(
                    dataFilePath.toFile(),
                    object : TypeReference<MutableMap<String, Any>>() {})

                autoStart = data["autoStart"] as? Boolean ?: false
            } catch (e: Throwable) {
                logger.error("Error loading application settings", e)
            }
        }
    }

    fun save() {
        try {
            val data = mapOf(
                "autoStart" to autoStart)
            Files.writeString(dataFilePath, jsonHandler.writeValueAsString(data))
        } catch (e: Throwable) {
            logger.error("Error saving application settings", e)
        }
    }

}
