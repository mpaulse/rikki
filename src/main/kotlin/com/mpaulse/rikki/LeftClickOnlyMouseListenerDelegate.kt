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

import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.SwingUtilities

class LeftClickOnlyMouseListenerDelegate(
    private val mouseListener: MouseListener
): MouseListener by mouseListener {

    override fun mouseReleased(event: MouseEvent) {
        if (SwingUtilities.isLeftMouseButton(event)) {
            mouseListener.mouseReleased(event)
        }
    }

}
