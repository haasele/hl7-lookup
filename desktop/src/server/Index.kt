// Local HTTP server facade. The desktop window starts Servers.
package hl7lookup.desktop.server

import hl7lookup.engine.Hl7Engine
import hl7lookup.engine.Hl7Transport
import java.io.File

// Starts and locates the local HTTP API for the browser client. window/Index calls start and texts.
object Servers {
    // Binds LocalServer on a port and returns a handle. window/Index starts it with HapiEngine and DesktopTransport.
    fun start(engine: Hl7Engine, transport: Hl7Transport, port: Int, webDir: File?): Result<ServerHandle> =
        runCatching { LocalServer(engine, transport, port, webDir).also { it.start() } }.map(::ServerHandle)
    // Walks build dirs for a packed web client. LocalServer discovery and callers of findBundle use it.
    fun findBundle(start: File): File? = findWebBundle(start)
    // Exposes ServerTexts. window/Index prints startup lines from it.
    fun texts() = ServerTexts
}
