/*
 * Copyright (C) 2020  Marlon Paulse
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.mpaulse.rikki

import javafx.application.Platform
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.TimeUnit

class UIEventExecutorService: AbstractExecutorService() {

    private var running = true

    override fun execute(task: Runnable) {
        Platform.runLater(task)
    }

    override fun shutdown() {
        running = false
    }

    override fun shutdownNow(): List<Runnable> {
        shutdown()
        return emptyList()
    }

    override fun isShutdown() = !running
    override fun isTerminated() = isShutdown()
    override fun awaitTermination(timeout: Long, unit: TimeUnit) = true

}
