package hl7lookup.desktop.server

import hl7lookup.engine.Hl7Engine
import hl7lookup.engine.Hl7Transport
import java.io.File

object Servers {
    fun start(engine: Hl7Engine, transport: Hl7Transport, port: Int, webDir: File?): Result<ServerHandle> =
        runCatching { LocalServer(engine, transport, port, webDir).also { it.start() } }.map(::ServerHandle)
    fun findBundle(start: File): File? = findWebBundle(start)
    fun texts() = ServerTexts
}
