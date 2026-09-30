// Browser entry point. It loads the font and logo, then hosts Workspace against the desktop server.
package hl7lookup.web.host

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.window.ComposeViewport
import hl7lookup.workspace.Workspace
import hl7lookup.workspace.Workspaces

// Boots the browser app with RemoteEngine and hosts Workspace against the desktop server.
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val platform = BrowserPlatform()
    val base = platform.serverAddress.orEmpty()
    val engine = RemoteEngine(base)
    val transport = RemoteTransport(base)
    ComposeViewport {
        val scope = rememberCoroutineScope()
        var font by remember { mutableStateOf<FontFamily?>(null) }
        var uiFont by remember { mutableStateOf<FontFamily>(FontFamily.Default) }
        var logo by remember { mutableStateOf<ImageBitmap?>(null) }
        LaunchedEffect(Unit) {
            logo = loadLogo("$base/icons/hl7.png")
            uiFont = loadUiFont("$base/fonts/DroidSans.ttf", "$base/fonts/DroidSans-Bold.ttf") ?: FontFamily.Default
            font = loadMonoFont("$base/fonts/DroidSansMono.ttf") ?: FontFamily.Monospace
        }
        val workspace = remember { Workspaces.create(platform, engine, transport, scope, APP_VERSION) }
        font?.let {
            Workspace(workspace, it, logo, uiFont)
            LaunchedEffect(it) { dismissLoadingScreen() }
        }
    }
}
