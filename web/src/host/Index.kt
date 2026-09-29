package hl7lookup.web.host

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.window.ComposeViewport
import hl7lookup.workspace.Workspace
import hl7lookup.workspace.Workspaces

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val platform = BrowserPlatform()
    val base = platform.serverAddress.orEmpty()
    val engine = RemoteEngine(base)
    val transport = RemoteTransport(base)
    ComposeViewport {
        val scope = rememberCoroutineScope()
        var font by remember { mutableStateOf<FontFamily?>(null) }
        LaunchedEffect(Unit) { font = loadMonoFont("$base/fonts/JetBrainsMono-Regular.ttf") ?: FontFamily.Monospace }
        val workspace = remember { Workspaces.create(platform, engine, transport, scope, APP_VERSION) }
        font?.let {
            Workspace(workspace, it)
            LaunchedEffect(it) { dismissLoadingScreen() }
        }
    }
}
