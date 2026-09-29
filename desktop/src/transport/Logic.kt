// Sockets, polls and acknowledgements. transport/Index exposes them.
package hl7lookup.desktop.transport

import com.sun.net.httpserver.HttpServer
import hl7lookup.engine.Diagnosis
import hl7lookup.engine.DiagnosisKind
import hl7lookup.engine.DiagnosisStep
import hl7lookup.engine.Endpoint
import hl7lookup.engine.FailureCause
import hl7lookup.engine.PollBatch
import hl7lookup.engine.Protocol
import hl7lookup.engine.ReceiverConfig
import hl7lookup.engine.ReceiverStatus
import hl7lookup.engine.SendResult
import hl7lookup.engine.TransportEvent
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.BindException
import java.net.ConnectException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NoRouteToHostException
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.URI
import java.net.UnknownHostException
import java.net.http.HttpClient
import java.net.http.HttpConnectTimeoutException
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpTimeoutException
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread

internal const val START_BLOCK = 0x0B
internal const val END_BLOCK = 0x1C
internal const val CARRIAGE_RETURN = 0x0D
internal const val HL7_MEDIA_TYPE = "x-application/hl7-v2+er7; charset=utf-8"
private const val EVENT_LIMIT = 5000

// Normalizes newlines to CR for HL7 wire. frame, sendHttp and Transports.wire call it.
internal fun toWire(text: String): String = text.replace("\r\n", "\r").replace('\n', '\r').trimEnd('\r') + "\r"

// Wraps wire text in MLLP start/end bytes. sendMllp and MllpListener write it.
internal fun frame(text: String): ByteArray {
    val body = toWire(text).toByteArray(Charsets.UTF_8)
    return byteArrayOf(START_BLOCK.toByte()) + body + byteArrayOf(END_BLOCK.toByte(), CARRIAGE_RETURN.toByte())
}

// Reads one MLLP-framed message from a stream. sendMllp and MllpListener call it.
internal fun readFrame(input: InputStream): String? {
    var byte = input.read()
    while (byte != -1 && byte != START_BLOCK) byte = input.read()
    if (byte == -1) return null
    val buffer = ByteArrayOutputStream()
    while (true) {
        byte = input.read()
        if (byte == -1) return null
        if (byte == END_BLOCK) {
            input.read()
            return buffer.toString(Charsets.UTF_8)
        }
        buffer.write(byte)
    }
}

// Maps socket/HTTP errors to FailureCause. failed, diagnose and hub.start call it.
internal fun causeOf(error: Throwable): FailureCause = when (error) {
    is UnknownHostException -> FailureCause.UNKNOWN_HOST
    is BindException -> FailureCause.PORT_IN_USE
    is ConnectException -> if (error.message?.contains("timed out", ignoreCase = true) == true) FailureCause.TIMEOUT else FailureCause.REFUSED
    is HttpConnectTimeoutException, is HttpTimeoutException, is SocketTimeoutException -> FailureCause.TIMEOUT
    is NoRouteToHostException -> FailureCause.UNREACHABLE
    is SocketException -> if (error.message?.contains("reset", ignoreCase = true) == true) FailureCause.RESET else FailureCause.OTHER
    else -> error.cause?.let(::causeOf) ?: FailureCause.OTHER
}

// Builds a failed SendResult from a throwable. sendMllp and sendHttp catch with it.
private fun failed(started: Long, error: Throwable) =
    SendResult(false, null, System.currentTimeMillis() - started, causeOf(error), error.message ?: error::class.simpleName)

// Detects ACK messages from MSH-9. sendMllp and TransportTest call it.
internal fun isAcknowledgement(text: String): Boolean {
    val header = text.lineSequence().firstOrNull()?.takeIf { it.startsWith("MSH") && it.length > 4 } ?: return false
    val fields = header.split(header[3])
    val component = fields.getOrNull(1)?.firstOrNull() ?: '^'
    return fields.getOrNull(8)?.substringBefore(component)?.trim().equals("ACK", ignoreCase = true)
}

// Connects over TCP, writes an MLLP frame and reads the ACK. send() dispatches to it.
internal fun sendMllp(endpoint: Endpoint, text: String): SendResult {
    val started = System.currentTimeMillis()
    return try {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(endpoint.host, endpoint.port), endpoint.timeoutMillis)
            socket.soTimeout = endpoint.timeoutMillis
            socket.getOutputStream().apply { write(frame(text)); flush() }
            if (isAcknowledgement(text)) return@use SendResult(true, null, System.currentTimeMillis() - started)
            val response = readFrame(socket.getInputStream())
            if (response == null) SendResult(false, null, System.currentTimeMillis() - started, FailureCause.NO_RESPONSE, null)
            else SendResult(true, response, System.currentTimeMillis() - started)
        }
    } catch (error: Exception) {
        failed(started, error)
    }
}

private val httpClient: HttpClient by lazy { HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build() }

// Builds an HTTP URI from an Endpoint. sendHttp and diagnose call it.
internal fun uriOf(endpoint: Endpoint): URI {
    val path = endpoint.path.ifBlank { "/" }.let { if (it.startsWith("/")) it else "/$it" }
    val scheme = if (endpoint.port == 443) "https" else "http"
    return URI("$scheme://${endpoint.host}:${endpoint.port}$path")
}

// POSTs ER7 over HTTP and maps the status. send() dispatches to it.
internal fun sendHttp(endpoint: Endpoint, text: String): SendResult {
    val started = System.currentTimeMillis()
    return try {
        val request = HttpRequest.newBuilder(uriOf(endpoint))
            .timeout(Duration.ofMillis(endpoint.timeoutMillis.toLong()))
            .header("Content-Type", HL7_MEDIA_TYPE)
            .POST(HttpRequest.BodyPublishers.ofString(toWire(text)))
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        val body = response.body().orEmpty()
        val duration = System.currentTimeMillis() - started
        if (response.statusCode() in 200..299) SendResult(true, body.ifBlank { null }, duration)
        else SendResult(false, body.ifBlank { null }, duration, FailureCause.HTTP_STATUS, "HTTP ${response.statusCode()}")
    } catch (error: Exception) {
        failed(started, error)
    }
}

// Chooses MLLP, HTTP or TAB send. DesktopTransport.send and tests call it.
internal fun send(endpoint: Endpoint, text: String): SendResult = when (endpoint.protocol) {
    Protocol.MLLP -> sendMllp(endpoint, text)
    Protocol.HTTP -> sendHttp(endpoint, text)
    Protocol.TAB -> SendResult(false, null, 0, FailureCause.OTHER, "TAB")
}

// Times a diagnosis probe and captures errors. diagnose uses it for each stage.
private inline fun step(kind: DiagnosisKind, block: () -> String): Pair<DiagnosisStep, Throwable?> {
    val started = System.currentTimeMillis()
    return try {
        DiagnosisStep(kind, true, block(), System.currentTimeMillis() - started) to null
    } catch (error: Exception) {
        DiagnosisStep(kind, false, error.message ?: error::class.simpleName.orEmpty(), System.currentTimeMillis() - started) to error
    }
}

// Resolves, connects and optionally probes HTTP. DesktopTransport.diagnose calls it.
internal fun diagnose(endpoint: Endpoint): Diagnosis {
    val steps = mutableListOf<DiagnosisStep>()
    val (resolve, resolveError) = step(DiagnosisKind.RESOLVE) {
        InetAddress.getAllByName(endpoint.host).joinToString(", ") { it.hostAddress }
    }
    steps += resolve
    if (resolveError != null) return Diagnosis(steps, causeOf(resolveError), resolve.detail)
    val (connect, connectError) = step(DiagnosisKind.CONNECT) {
        Socket().use { it.connect(InetSocketAddress(endpoint.host, endpoint.port), endpoint.timeoutMillis) }
        "${endpoint.host}:${endpoint.port}"
    }
    steps += connect
    if (connectError != null) return Diagnosis(steps, causeOf(connectError), connect.detail)
    if (endpoint.protocol == Protocol.HTTP) {
        var status = 0
        val (exchange, exchangeError) = step(DiagnosisKind.EXCHANGE) {
            val request = HttpRequest.newBuilder(uriOf(endpoint)).timeout(Duration.ofMillis(endpoint.timeoutMillis.toLong()))
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build()
            status = httpClient.send(request, HttpResponse.BodyHandlers.discarding()).statusCode()
            "HTTP $status"
        }
        steps += exchange
        if (exchangeError != null) return Diagnosis(steps, causeOf(exchangeError), exchange.detail)
        if (status >= 500) return Diagnosis(steps, FailureCause.HTTP_STATUS, exchange.detail)
    }
    return Diagnosis(steps)
}

// Closeable receiver socket or HTTP server. MllpListener and HttpListener implement it.
internal interface Listener {
    // Shuts down the listener. TransportHub.stop and stopAll call it.
    fun close()
}

// Accepts MLLP sockets and auto-acks. TransportHub.start creates it for MLLP receivers.
internal class MllpListener(port: Int, private val handle: (String, String) -> String?) : Listener {
    private val server = ServerSocket(port)
    private val pool = Executors.newCachedThreadPool { runnable -> Thread(runnable, "mllp-$port").apply { isDaemon = true } }

    init {
        thread(name = "mllp-accept-$port", isDaemon = true) {
            while (!server.isClosed) {
                val socket = runCatching { server.accept() }.getOrNull() ?: break
                pool.execute { serve(socket) }
            }
        }
    }

    // Reads frames from one client socket and writes ACKs. MllpListener accept loop runs it.
    private fun serve(socket: Socket) = socket.use {
        val remote = "${socket.inetAddress.hostAddress}:${socket.port}"
        val input = socket.getInputStream()
        val output = socket.getOutputStream()
        while (!socket.isClosed) {
            val message = runCatching { readFrame(input) }.getOrNull() ?: break
            val ack = handle(message, remote)
            if (ack != null) runCatching { output.write(frame(ack)); output.flush() }.onFailure { return@use }
        }
    }

    // Shuts down the listener. TransportHub.stop and stopAll call it.
    override fun close() {
        runCatching { server.close() }
        pool.shutdownNow()
    }
}

// Serves POST HL7 on a path and returns ACKs. TransportHub.start creates it for HTTP receivers.
internal class HttpListener(port: Int, path: String, private val handle: (String, String) -> String?) : Listener {
    private val server = HttpServer.create(InetSocketAddress(port), 0)

    init {
        server.createContext(path.ifBlank { "/" }.let { if (it.startsWith("/")) it else "/$it" }) { exchange ->
            exchange.use {
                if (exchange.requestMethod.equals("OPTIONS", ignoreCase = true)) {
                    exchange.sendResponseHeaders(204, -1)
                    return@use
                }
                if (!exchange.requestMethod.equals("POST", ignoreCase = true)) {
                    exchange.sendResponseHeaders(405, -1)
                    return@use
                }
                val body = exchange.requestBody.readBytes().toString(Charsets.UTF_8)
                val remote = exchange.remoteAddress?.let { "${it.address.hostAddress}:${it.port}" }.orEmpty()
                val ack = handle(body, remote)
                val bytes = (ack?.let(::toWire) ?: "").toByteArray(Charsets.UTF_8)
                exchange.responseHeaders.add("Content-Type", HL7_MEDIA_TYPE)
                exchange.sendResponseHeaders(200, if (bytes.isEmpty()) -1 else bytes.size.toLong())
                if (bytes.isNotEmpty()) exchange.responseBody.write(bytes)
            }
        }
        server.executor = Executors.newCachedThreadPool { runnable -> Thread(runnable, "http-$port").apply { isDaemon = true } }
        server.start()
    }

    // Shuts down the listener. TransportHub.stop and stopAll call it.
    override fun close() = server.stop(0)
}

// Tracks a live receiver config, listener and count. TransportHub keeps them in a map.
private class Running(val config: ReceiverConfig, val listener: Listener, val received: AtomicInteger)

// Starts receivers, stores events and polls them. DesktopTransport owns one hub.
internal class TransportHub(private val acknowledge: (String) -> String?) {
    private val running = ConcurrentHashMap<String, Running>()
    private val failures = ConcurrentHashMap<String, ReceiverStatus>()
    private val events = ConcurrentLinkedDeque<TransportEvent>()
    private val sequence = AtomicLong()
    private val epoch = java.util.UUID.randomUUID().toString()

    // Stores a received message and optional ACK. Listener handlers call it per message.
    private fun record(config: ReceiverConfig, counter: AtomicInteger, text: String, remote: String): String? {
        val ack = if (config.autoAck) runCatching { acknowledge(text) }.getOrNull() else null
        counter.incrementAndGet()
        events.addLast(TransportEvent(sequence.incrementAndGet(), config.id, text.replace("\r\n", "\n").replace('\r', '\n').trimEnd('\n'), ack, remote, System.currentTimeMillis()))
        while (events.size > EVENT_LIMIT) events.pollFirst()
        return ack
    }

    // Opens an MLLP or HTTP listener for a config. DesktopTransport.startReceiver calls it.
    fun start(config: ReceiverConfig): ReceiverStatus {
        stop(config.id)
        return try {
            val counter = AtomicInteger()
            val handler = { text: String, remote: String -> record(config, counter, text, remote) }
            val listener = when (config.protocol) {
                Protocol.HTTP -> HttpListener(config.port, config.path, handler)
                else -> MllpListener(config.port, handler)
            }
            running[config.id] = Running(config, listener, counter)
            failures.remove(config.id)
            ReceiverStatus(config.id, true, 0)
        } catch (error: Exception) {
            ReceiverStatus(config.id, false, 0, causeOf(error), error.message).also { failures[config.id] = it }
        }
    }

    // Closes one receiver by id. DesktopTransport.stopReceiver and start call it.
    fun stop(id: String): ReceiverStatus {
        val current = running.remove(id)
        current?.listener?.close()
        return ReceiverStatus(id, false, current?.received?.get() ?: 0)
    }

    // Lists running and failed receiver statuses. DesktopTransport.receivers calls it.
    fun statuses(): List<ReceiverStatus> =
        running.values.map { ReceiverStatus(it.config.id, true, it.received.get()) } + failures.values.filter { !running.containsKey(it.id) }

    // Returns events after a sequence number. DesktopTransport.poll calls it.
    fun poll(after: Long): PollBatch = PollBatch(epoch, events.filter { it.sequence > after })

    // Stops every running receiver. DesktopTransport.close calls it.
    fun stopAll() = running.keys.toList().forEach(::stop)
}
