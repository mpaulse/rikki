/*
 * Copyright (c) 2020 Marlon Paulse
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

@file:JvmName("Rikki")

package com.mpaulse.rikki

import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg
import javafx.application.Application
import javafx.application.Platform
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.stage.Stage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
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

        createMainWindow(stage)
        createSystemTrayIcon()
        initControls()
        startMainWindow()
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

    private fun startMainWindow() {
    }

    private fun showMainWindow() {
        mainWindow.show()
        mainWindow.toFront()
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
        appData.windowPosition = mainWindow.x to mainWindow.y
        appData.windowSize = mainWindow.width to mainWindow.height
        appData.save()
    }

    private fun createMainWindow(stage: Stage) {
        mainWindow = stage
        mainWindow.scene = Scene(loadFXMLPane("MainWindow", this))
        mainWindow.scene.stylesheets.add("style.css")
        mainWindow.minWidth = DEFAULT_MIN_WINDOW_WIDTH
        mainWindow.width = if (appData.windowSize.first >= mainWindow.minWidth) appData.windowSize.first else mainWindow.minWidth
        mainWindow.minHeight = DEFAULT_MIN_WINDOW_HEIGHT
        mainWindow.height = if (appData.windowSize.second >= mainWindow.minHeight) appData.windowSize.second else mainWindow.minHeight
        mainWindow.icons.add(Image(APP_ICON))

        val pos = appData.windowPosition
        if (pos != null) {
            mainWindow.x = pos.first
            mainWindow.y = pos.second
        } else {
            mainWindow.centerOnScreen()
            appData.windowPosition = mainWindow.x to mainWindow.y
        }

        mainWindow.title = APP_NAME
    }

    private fun createSystemTrayIcon() {
        if (!SystemTray.isSupported()) {
            return
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

        val openMenuItem = JMenuItem("Open $APP_NAME")
        openMenuItem.font = AWTFont.decode(null).deriveFont(AWTFont.BOLD)
        setupTrayMenuItemMouseListener(openMenuItem)
        openMenuItem.addActionListener(::onOpenMainWindowFromSystemTray)

        val settingsMenuItem = JMenuItem("Settings")
        setupTrayMenuItemMouseListener(settingsMenuItem)
        settingsMenuItem.addActionListener(::onSettingsFromSystemTray)

        val exitMenuItem = JMenuItem("Exit")
        setupTrayMenuItemMouseListener(exitMenuItem)
        exitMenuItem.addActionListener {
            sysTrayMenuInvoker.dispose()
            onExit()
        }

        sysTrayMenu.add(openMenuItem)
        sysTrayMenu.add(settingsMenuItem)
        sysTrayMenu.addSeparator()
        sysTrayMenu.add(exitMenuItem)

        sysTrayIcon?.addActionListener(::onOpenMainWindowFromSystemTray)
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

    private fun initControls() {
    }

    @FXML
    fun onHideMainWindow(event: ActionEvent) {
        mainWindow.hide()
        event.consume()
    }

    private fun onOpenMainWindowFromSystemTray(event: AWTActionEvent) {
        Platform.runLater {
            showMainWindow()
        }
    }

    private fun onSettingsFromSystemTray(event: AWTActionEvent) {
        onOpenMainWindowFromSystemTray(event)
        onSettings()
    }

    @FXML
    fun onSettings(event: ActionEvent? = null) {
        if (event != null) {
            //showSecondaryScreen(settingsScreen)
            event.consume()
        } else {
            Platform.runLater {
                //showSecondaryScreen(settingsScreen)
            }
        }
    }

    fun onExitSettings() {
        appData.save()
        if (appData.autoStart) {
            enableAutoStart()
        } else {
            disableAutoStart()
        }
    }

    @FXML
    fun onAbout(event: ActionEvent) {
        //showSecondaryScreen(aboutScreen)
        event.consume()
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
