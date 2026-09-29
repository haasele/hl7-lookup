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

class DesktopTransport(engine: Hl7Engine) : Hl7Transport {
    private val hub = TransportHub { text ->
        if (Engines.ackCode(text) != null) null else runBlocking { engine.acknowledge(AckRequest(text, "AA", null)) }
    }

    override suspend fun send(request: SendRequest): SendResult = withContext(Dispatchers.IO) { send(request.endpoint, request.text) }

    override suspend fun diagnose(endpoint: Endpoint): Diagnosis = withContext(Dispatchers.IO) { diagnose(endpoint) }

    override suspend fun startReceiver(config: ReceiverConfig): ReceiverStatus = withContext(Dispatchers.IO) { hub.start(config) }

    override suspend fun stopReceiver(id: String): ReceiverStatus = withContext(Dispatchers.IO) { hub.stop(id) }

    override suspend fun receivers(): List<ReceiverStatus> = hub.statuses()

    override suspend fun poll(after: Long): PollBatch = hub.poll(after)

    fun close() = hub.stopAll()
}

object Transports {
    fun wire(text: String): String = toWire(text)
    fun texts() = TransportTexts
}
