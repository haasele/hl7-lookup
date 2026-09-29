// Desktop MLLP and HTTP transport. The window passes it to Workspace as Hl7Transport.
package hl7lookup.desktop.transport

import hl7lookup.engine.AckRequest
import hl7lookup.engine.Diagnosis
import hl7lookup.engine.Endpoint
import hl7lookup.engine.Engines
import hl7lookup.engine.Hl7Engine
import hl7lookup.engine.Hl7Transport
import hl7lookup.engine.ReceiverConfig
import hl7lookup.engine.ReceiverStatus
import hl7lookup.engine.SendRequest
import hl7lookup.engine.PollBatch
import hl7lookup.engine.SendResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

// JVM Hl7Transport over MLLP/HTTP with a receiver hub. window/Index passes it to Workspace and Servers.
class DesktopTransport(engine: Hl7Engine) : Hl7Transport {
    private val hub = TransportHub { text ->
        if (Engines.ackCode(text) != null) null else runBlocking { engine.acknowledge(AckRequest(text, "AA", null)) }
    }

    // Sends a message to an endpoint. Workspace senders and /api/send call it.
    override suspend fun send(request: SendRequest): SendResult = withContext(Dispatchers.IO) { send(request.endpoint, request.text) }

    // Probes host resolve, connect and optional HTTP exchange. Workspace diagnosis and /api/diagnose call it.
    override suspend fun diagnose(endpoint: Endpoint): Diagnosis = withContext(Dispatchers.IO) { diagnose(endpoint) }

    // Starts an MLLP or HTTP listener. Workspace receivers and /api/startReceiver call it.
    override suspend fun startReceiver(config: ReceiverConfig): ReceiverStatus = withContext(Dispatchers.IO) { hub.start(config) }

    // Stops a running listener by id. Workspace receivers and /api/stopReceiver call it.
    override suspend fun stopReceiver(id: String): ReceiverStatus = withContext(Dispatchers.IO) { hub.stop(id) }

    // Lists running and failed receivers. Workspace polling and /api/receivers call it.
    override suspend fun receivers(): List<ReceiverStatus> = hub.statuses()

    // Returns transport events newer than a sequence. Workspace poll loop and /api/poll call it.
    override suspend fun poll(after: Long): PollBatch = hub.poll(after)

    // Stops every listener. window close and shutdown hooks call it.
    fun close() = hub.stopAll()
}

// Small helpers for wire formatting and transport wording. Tests and UI can call through this facade.
object Transports {
    // Normalizes ER7 newlines for the wire. Callers that need framed text use it.
    fun wire(text: String): String = toWire(text)
    // Exposes TransportTexts for receiver status lines.
    fun texts() = TransportTexts
}
