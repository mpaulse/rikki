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

@file:JvmName("Rikki")

package com.mpaulse.rikki

import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg
import javafx.application.Application
import javafx.application.Platform
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.Border
import javafx.scene.layout.BorderStroke
import javafx.scene.layout.BorderStrokeStyle
import javafx.scene.layout.BorderWidths
import javafx.scene.layout.CornerRadii
import javafx.scene.paint.Paint
import javafx.stage.Popup
import javafx.stage.Stage
import javafx.stage.StageStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jnativehook.GlobalScreen
import org.jnativehook.mouse.NativeMouseEvent
import org.jnativehook.mouse.NativeMouseMotionListener
import org.slf4j.LoggerFactory
import java.awt.SystemTray
import java.awt.Toolkit
import java.awt.TrayIcon
import java.awt.event.MouseAdapter
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.JDialog
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.UIManager
import java.awt.Font as AWTFont
import java.awt.Image as AWTImage
import java.awt.event.ActionEvent as AWTActionEvent
import java.awt.event.MouseEvent as AWTMouseEvent

private const val HIDE_IN_BACKGROUND_PARAMETER = "-b"
private const val RUN_AT_WIN_LOGIN_REGISTRY_KEY = "Software\\Microsoft\\Windows\\CurrentVersion\\Run"

const val DEFAULT_MIN_WINDOW_WIDTH = 900.0
const val DEFAULT_MIN_WINDOW_HEIGHT = 600.0

val devModeEnabled = System.getProperty("dev")?.toBoolean() ?: false

class RikkiApplication: Application(), CoroutineScope by MainScope() {

    val appData = ApplicationData(APP_HOME_PATH)
    private val logger = LoggerFactory.getLogger(RikkiApplication::class.java)

    private lateinit var mainWindow: Stage
    private var sysTrayIcon: TrayIcon? = null


    override fun start(stage: Stage) {
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            logger.error("Application error", e)
        }

        val popupLabel = Label("Rikki")
        popupLabel.background = Background(BackgroundFill(Paint.valueOf("white"), CornerRadii(5.0), Insets(0.0)))
        popupLabel.border = Border(BorderStroke(Paint.valueOf("blue"), BorderStrokeStyle.SOLID, CornerRadii(5.0), BorderWidths(2.0)))

        val popup = Popup()
        popup.content += popupLabel

        stage.initStyle(StageStyle.UTILITY)
        stage.width = 0.0
        stage.height = 0.0
        stage.opacity = 0.0
        stage.show()


        GlobalScreen.registerNativeHook()
        GlobalScreen.setEventDispatcher(UIEventExecutorService())
        GlobalScreen.addNativeMouseMotionListener(object: NativeMouseMotionListener {
            override fun nativeMouseDragged(event: NativeMouseEvent) {
            }

            override fun nativeMouseMoved(event: NativeMouseEvent) {
                popupLabel.text = event.paramString()
                popup.x = event.x.toDouble()
                popup.y = event.y.toDouble()
                popup.show(stage)
                println(event.paramString())
            }
        })

        createSystemTrayIcon()
    }

    fun <T> loadFXMLPane(pane: String, controller: Any): T {
        val loader = FXMLLoader()
        loader.setController(controller)
        loader.setControllerFactory {
            APP_NAME
            controller // Needed for imported/nested FXML files
        }
        loader.location = controller.javaClass.getResource("/fxml/$pane.fxml")
        return loader.load<T>()
    }

    private fun verifyAutoStartConfig() {
        if (appData.autoStart) {
            val launcherPath = getLauncherPath()?.toString()
            val expectedRegistryConfig = "$launcherPath $HIDE_IN_BACKGROUND_PARAMETER"

            var registryConfig: String? = null
            try {
                registryConfig = Advapi32Util.registryGetStringValue(
                    WinReg.HKEY_CURRENT_USER,
                    RUN_AT_WIN_LOGIN_REGISTRY_KEY,
                    APP_NAME)
            } catch (e: Exception) {
            }

            if (expectedRegistryConfig != registryConfig) {
                if (launcherPath == null) {
                    disableAutoStart()
                } else {
                    enableAutoStart()
                }
            }
        }
    }

    private fun getLauncherPath(): Path? {
        // CWD is the "app" directory containing the JAR.
        // The EXE launcher should be in the parent directory.
        val path = Paths.get("..", "$APP_NAME.exe").toAbsolutePath().normalize()
        return if (path.toFile().exists()) path else null
    }

    private fun enableAutoStart() {
        if (devModeEnabled) {
            return
        }
        val launcherPath = getLauncherPath()
        if (launcherPath != null) {
            try {
                Advapi32Util.registrySetStringValue(
                    WinReg.HKEY_CURRENT_USER,
                    RUN_AT_WIN_LOGIN_REGISTRY_KEY,
                    APP_NAME,
                    "$launcherPath $HIDE_IN_BACKGROUND_PARAMETER")
            } catch (e: Exception) {
                logger.error("Error enabling auto startup", e)
            }
        }
    }

    private fun disableAutoStart() {
        if (devModeEnabled) {
            return
        }
        try {
            Advapi32Util.registryDeleteValue(
                WinReg.HKEY_CURRENT_USER,
                RUN_AT_WIN_LOGIN_REGISTRY_KEY,
                APP_NAME)
        } catch (e: Exception) {
        }
    }

    override fun stop() {
        //appData.windowPosition = mainWindow.x to mainWindow.y
        //appData.windowSize = mainWindow.width to mainWindow.height
        //appData.save()
        GlobalScreen.unregisterNativeHook()
    }

    private fun createSystemTrayIcon() {
        if (!SystemTray.isSupported()) {
            logger.error("System tray not supported on this platform. Exiting.")
            Platform.exit()
        }

        val sysTray = SystemTray.getSystemTray()
        sysTrayIcon = TrayIcon(
            Toolkit.getDefaultToolkit().getImage(javaClass.getResource(APP_ICON)).getScaledInstance(16, 16, AWTImage.SCALE_DEFAULT),
            APP_NAME,
            null)

        // Use JPopupMenu instead of the AWT PopupMenu for a native system look and feel.
        val sysTrayMenu = JPopupMenu()
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())

        // A hack to allow the system tray popup menu to disappear when clicking outside it.
        // Hiding the hidden invoker dialog window when it loses focus also hides the popup menu.
        // This is only a problem when using JPopupMenu instead of the AWT PopupMenu for the system tray.
        val sysTrayMenuInvoker = JDialog()
        sysTrayMenuInvoker.isUndecorated = true
        sysTrayMenuInvoker.addWindowFocusListener(object: WindowFocusListener {
            override fun windowGainedFocus(e: WindowEvent?) = Unit
            override fun windowLostFocus(e: WindowEvent?) {
                sysTrayMenuInvoker.isVisible = false
            }
        })
        sysTrayMenu.invoker = sysTrayMenuInvoker

        //val settingsMenuItem = JMenuItem("Settings")
        //setupTrayMenuItemMouseListener(settingsMenuItem)
        //settingsMenuItem.addActionListener(::onSettingsFromSystemTray)

        val exitMenuItem = JMenuItem("Exit")
        setupTrayMenuItemMouseListener(exitMenuItem)
        exitMenuItem.addActionListener {
            sysTrayMenuInvoker.dispose()
            onExit()
        }

        //sysTrayMenu.add(settingsMenuItem)
        sysTrayMenu.addSeparator()
        sysTrayMenu.add(exitMenuItem)

        //sysTrayIcon?.addActionListener(::onOpenMainWindowFromSystemTray)
        sysTrayIcon?.addMouseListener(object: MouseAdapter() {
            override fun mouseReleased(event: AWTMouseEvent) {
                if (event.isPopupTrigger) {
                    sysTrayMenu.setLocation(event.x, event.y)
                    sysTrayMenuInvoker.isVisible = true
                    sysTrayMenu.isVisible = true
                }
            }
        })

        sysTray.add(sysTrayIcon)
        Platform.setImplicitExit(false)
    }

    private fun setupTrayMenuItemMouseListener(menuItem: JMenuItem) {
        if (menuItem.mouseListeners.isNotEmpty()) {
            val listener = menuItem.mouseListeners.first()
            menuItem.removeMouseListener(listener)
            menuItem.addMouseListener(LeftClickOnlyMouseListenerDelegate(listener))
        }
    }

    @FXML
    fun onExit(event: ActionEvent? = null) {
        if (sysTrayIcon != null) {
            SystemTray.getSystemTray().remove(sysTrayIcon)
        }
        Platform.exit()
        event?.consume()
    }

}

fun main(args: Array<String>) {
    if (!Files.exists(APP_HOME_PATH)) {
        Files.createDirectories(APP_HOME_PATH)
    }

    Application.launch(RikkiApplication::class.java, *args)
}
