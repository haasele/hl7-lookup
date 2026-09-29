package hl7lookup.desktop.server

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import hl7lookup.engine.AckRequest
import hl7lookup.engine.ApiError
import hl7lookup.engine.ApiRoute
import hl7lookup.engine.CreateRequest
import hl7lookup.engine.Endpoint
import hl7lookup.engine.Engines
import hl7lookup.engine.Hl7Engine
import hl7lookup.engine.Hl7Transport
import hl7lookup.engine.IdQuery
import hl7lookup.engine.InspectRequest
import hl7lookup.engine.PollBatch
import hl7lookup.engine.PollQuery
import hl7lookup.engine.ReceiverConfig
import hl7lookup.engine.SendRequest
import hl7lookup.engine.TextReply
import hl7lookup.engine.VersionQuery
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import java.io.File
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.Executors

internal const val CLIENT_HEADER = "X-HL7-Lookup"

private val allowedHosts = setOf("localhost", "127.0.0.1", "[::1]")

internal fun contentTypeOf(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
    "html" -> "text/html; charset=utf-8"
    "mjs", "js" -> "text/javascript; charset=utf-8"
    "wasm" -> "application/wasm"
    "png" -> "image/png"
    "ttf" -> "font/ttf"
    "json", "map" -> "application/json"
    "css" -> "text/css; charset=utf-8"
    else -> "application/octet-stream"
}

internal fun safePath(raw: String): String? {
    val path = raw.substringBefore('?').trimStart('/').ifEmpty { "index.html" }
    if (path.split('/').any { it == ".." || it.startsWith(".") }) return null
    return path
}

private fun resource(name: String): ByteArray? =
    Thread.currentThread().contextClassLoader.getResourceAsStream(name)?.use { it.readBytes() }

internal fun staticFile(path: String, webDir: File?): ByteArray? {
    webDir?.resolve(path)?.takeIf { it.isFile && it.canonicalPath.startsWith(webDir.canonicalPath) }?.let { return it.readBytes() }
    return resource("web/$path") ?: resource(path)
}

internal fun findWebBundle(start: File): File? {
    val roots = listOf(start.resolve("build"), start.resolve("../build"), start.resolve("../../build")).filter { it.isDirectory }
    return roots.asSequence()
        .flatMap { root -> root.walkTopDown().maxDepth(8).filter { it.isFile && it.name == "web.mjs" } }
        .filter { candidate -> candidate.parentFile.listFiles().orEmpty().any { it.extension == "wasm" } }
        .maxByOrNull { it.lastModified() }
        ?.parentFile
}

private fun hostAllowed(exchange: HttpExchange): Boolean {
    val host = exchange.requestHeaders.getFirst("Host")?.substringBeforeLast(':')?.lowercase() ?: return false
    return host in allowedHosts
}

private fun respond(exchange: HttpExchange, status: Int, bytes: ByteArray, type: String) {
    exchange.responseHeaders.add("Content-Type", type)
    exchange.responseHeaders.add("Cache-Control", "no-cache")
    exchange.responseHeaders.add("Cross-Origin-Opener-Policy", "same-origin")
    exchange.sendResponseHeaders(status, if (bytes.isEmpty()) -1 else bytes.size.toLong())
    if (bytes.isNotEmpty()) exchange.responseBody.write(bytes)
}

private fun json(exchange: HttpExchange, status: Int, body: String) = respond(exchange, status, body.toByteArray(Charsets.UTF_8), "application/json; charset=utf-8")

internal fun dispatch(route: ApiRoute, body: String, engine: Hl7Engine, transport: Hl7Transport): String = runBlocking {
    val codec = Engines.json()
    when (route) {
        ApiRoute.HEALTH -> codec.encodeToString(TextReply.serializer(), TextReply("ok"))
        ApiRoute.VERSIONS -> codec.encodeToString(ListSerializer(String.serializer()), engine.versions())
        ApiRoute.DICTIONARY -> codec.encodeToString(hl7lookup.dictionary.Hl7Dictionary.serializer(), engine.dictionary(codec.decodeFromString(VersionQuery.serializer(), body).version))
        ApiRoute.INSPECT -> codec.encodeToString(hl7lookup.engine.InspectReport.serializer(), engine.inspect(codec.decodeFromString(InspectRequest.serializer(), body)))
        ApiRoute.ACK -> codec.encodeToString(TextReply.serializer(), TextReply(engine.acknowledge(codec.decodeFromString(AckRequest.serializer(), body))))
        ApiRoute.CREATE -> codec.encodeToString(TextReply.serializer(), TextReply(engine.create(codec.decodeFromString(CreateRequest.serializer(), body))))
        ApiRoute.SEND -> codec.encodeToString(hl7lookup.engine.SendResult.serializer(), transport.send(codec.decodeFromString(SendRequest.serializer(), body)))
        ApiRoute.DIAGNOSE -> codec.encodeToString(hl7lookup.engine.Diagnosis.serializer(), transport.diagnose(codec.decodeFromString(Endpoint.serializer(), body)))
        ApiRoute.START_RECEIVER -> codec.encodeToString(hl7lookup.engine.ReceiverStatus.serializer(), transport.startReceiver(codec.decodeFromString(ReceiverConfig.serializer(), body)))
        ApiRoute.STOP_RECEIVER -> codec.encodeToString(hl7lookup.engine.ReceiverStatus.serializer(), transport.stopReceiver(codec.decodeFromString(IdQuery.serializer(), body).id))
        ApiRoute.RECEIVERS -> codec.encodeToString(ListSerializer(hl7lookup.engine.ReceiverStatus.serializer()), transport.receivers())
        ApiRoute.POLL -> codec.encodeToString(PollBatch.serializer(), transport.poll(codec.decodeFromString(PollQuery.serializer(), body).after))
    }
}

class ServerHandle internal constructor(private val server: LocalServer) {
    val port: Int get() = server.port
    val address: String get() = "http://localhost:${server.port}/"
    val hasWebClient: Boolean get() = server.hasBundle
    fun stop() = server.stop()
}

internal class LocalServer(
    private val engine: Hl7Engine,
    private val transport: Hl7Transport,
    val port: Int,
    private val configuredWebDir: File?,
) {
    @Volatile
    private var discovered: File? = null
    private val webDir: File?
        get() = configuredWebDir ?: discovered ?: findWebBundle(File("").absoluteFile)?.also { discovered = it }
    private val server: HttpServer = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0)
    private val routes = ApiRoute.entries.associateBy { it.path }

    init {
        server.createContext("/api/") { exchange -> exchange.use { api(it) } }
        server.createContext("/") { exchange -> exchange.use { files(it) } }
        server.executor = Executors.newFixedThreadPool(8) { runnable -> Thread(runnable, "hl7lookup-server").apply { isDaemon = true } }
    }

    private fun api(exchange: HttpExchange) {
        val codec = Engines.json()
        val route = routes[exchange.requestURI.path]
        when {
            !hostAllowed(exchange) -> json(exchange, 403, codec.encodeToString(ApiError.serializer(), ApiError("host")))
            route == null -> json(exchange, 404, codec.encodeToString(ApiError.serializer(), ApiError("route")))
            exchange.requestMethod != "POST" || exchange.requestHeaders.getFirst(CLIENT_HEADER) == null ->
                json(exchange, 405, codec.encodeToString(ApiError.serializer(), ApiError("method")))
            else -> {
                val body = exchange.requestBody.readBytes().toString(Charsets.UTF_8)
                runCatching { dispatch(route, body, engine, transport) }
                    .onSuccess { json(exchange, 200, it) }
                    .onFailure { json(exchange, 500, codec.encodeToString(ApiError.serializer(), ApiError(it.message ?: it::class.simpleName.orEmpty()))) }
            }
        }
    }

    private fun files(exchange: HttpExchange) {
        if (!hostAllowed(exchange)) return respond(exchange, 403, ByteArray(0), "text/plain")
        if (exchange.requestMethod != "GET" && exchange.requestMethod != "HEAD") return respond(exchange, 405, ByteArray(0), "text/plain")
        val path = safePath(exchange.requestURI.path) ?: return respond(exchange, 404, ByteArray(0), "text/plain")
        val bytes = staticFile(path, webDir) ?: return respond(exchange, 404, ByteArray(0), "text/plain")
        respond(exchange, 200, if (exchange.requestMethod == "HEAD") ByteArray(0) else bytes, contentTypeOf(path))
    }

    fun start() = server.start()

    fun stop() = server.stop(0)

    val hasBundle: Boolean get() = staticFile("web.mjs", webDir) != null
}
