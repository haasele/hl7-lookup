// Desktop entry point. It opens the window, loads the font and logo, and hosts Workspace.
package hl7lookup.desktop.window

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import hl7lookup.desktop.engine.HapiEngine
import hl7lookup.desktop.server.Servers
import hl7lookup.desktop.transport.DesktopTransport
import hl7lookup.engine.Engines
import hl7lookup.i18n.I18n
import hl7lookup.workspace.Workspace
import hl7lookup.workspace.Workspaces
import kotlin.concurrent.thread

// Boots the JVM app: starts HapiEngine, the HTTP server, and the Compose window that hosts Workspace.
fun main(args: Array<String>) {
    relaunchForWayland()
    val options = parseArgs(args)
    val engine = HapiEngine()
    val transport = DesktopTransport(engine)
    val serverTexts = Servers.texts()
    val server = if (options.server) {
        Servers.start(engine, transport, options.port, options.webDir)
            .onSuccess { handle ->
                println(I18n.format(serverTexts.started, hl7lookup.i18n.Language.EN, handle.address))
                if (!handle.hasWebClient) println(I18n.format(serverTexts.noBundle, hl7lookup.i18n.Language.EN))
            }
            .onFailure { println(I18n.format(serverTexts.failed, hl7lookup.i18n.Language.EN, options.port, it.message)) }
            .getOrNull()
    } else null
    thread(name = "hapi-warmup", isDaemon = true) { engine.warmUp(Engines.defaultVersion()) }

    if (options.headless) {
        println(I18n.format(serverTexts.headless, hl7lookup.i18n.Language.EN))
        Runtime.getRuntime().addShutdownHook(Thread { transport.close(); server?.stop() })
        Thread.currentThread().join()
        return
    }

    val platform = DesktopPlatform(storageDirectory()).apply { serverAddress = server?.address }
    val font = loadFont() ?: FontFamily.Monospace
    val uiFont = loadUiFont() ?: FontFamily.SansSerif
    val logo = logoBitmap()
    val icon = logo?.let { BitmapPainter(it) }
    val files = readFiles(options.files)

    application {
        val scope = rememberCoroutineScope()
        val workspace = remember {
            Workspaces.create(platform, engine, transport, scope, APP_VERSION).also { ws -> files.forEach { ws.openContent(it.name, it.content) } }
        }
        val tabTitle = workspace.session.active?.title
        val language = workspace.settings.current.language
        val state = rememberWindowState(width = 1280.dp, height = 800.dp)
        Window(
            onCloseRequest = {
                workspace.session.persist()
                transport.close()
                server?.stop()
                exitApplication()
            },
            title = tabTitle?.let { I18n.format(WindowTexts.titleWithTab, language, it) } ?: I18n.resolve(WindowTexts.title, language),
            icon = icon,
            state = state,
        ) {
            LaunchedEffect(window) {
                releaseWindowSizeLimits(window)
                applyWindowIcon(window)
            }
            Workspace(workspace, font, logo, uiFont)
        }
    }
}
