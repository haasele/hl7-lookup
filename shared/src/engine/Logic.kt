// Request and result types for parse, validate, send and ack. engine/Index exposes them.
package hl7lookup.engine

import hl7lookup.dictionary.Hl7Dictionary
import kotlinx.serialization.Serializable

// How serious an inspect finding is. EngineFinding carries it; UI maps it to colors.
@Serializable
enum class Severity { ERROR, WARNING, INFO }

// Payload for an inspect call. Hl7Engine.inspect and the HTTP API take it.
@Serializable
data class InspectRequest(val text: String, val version: String? = null)

// One validation or structure finding. InspectReport lists these.
@Serializable
data class EngineFinding(
    val severity: Severity,
    val message: String,
    val segment: String? = null,
    val segmentRepetition: Int = 1,
    val field: Int = 0,
    val fieldRepetition: Int = 1,
    val component: Int = 0,
    val subcomponent: Int = 0,
)

// Structure notes and findings from inspect. Hl7Engine.inspect returns it.
@Serializable
data class InspectReport(val structure: String? = null, val findings: List<EngineFinding> = emptyList())

// Payload to build an acknowledgement. Hl7Engine.acknowledge takes it.
@Serializable
data class AckRequest(val text: String, val code: String, val errorText: String? = null)

// Payload to create a skeleton message. Hl7Engine.create takes it.
@Serializable
data class CreateRequest(val messageType: String, val event: String, val version: String, val processingId: String = "P")

// Wire protocol for send and receive. Endpoint and ReceiverConfig use it.
@Serializable
enum class Protocol { MLLP, HTTP, TAB }

// Where to send a message. SendRequest and diagnose take it.
@Serializable
data class Endpoint(val protocol: Protocol, val host: String, val port: Int, val path: String = "/", val timeoutMillis: Int = 10_000)

// Payload for an outbound send. Hl7Transport.send takes it.
@Serializable
data class SendRequest(val endpoint: Endpoint, val text: String)

// Why a transport or engine call failed. SendResult and Diagnosis carry it.
@Serializable
enum class FailureCause { UNKNOWN_HOST, REFUSED, TIMEOUT, UNREACHABLE, RESET, NO_RESPONSE, BAD_RESPONSE, HTTP_STATUS, PORT_IN_USE, ENGINE_OFFLINE, OTHER }

// Outcome of an outbound send. Hl7Transport.send returns it.
@Serializable
data class SendResult(
    val ok: Boolean,
    val response: String? = null,
    val durationMillis: Long = 0,
    val failure: FailureCause? = null,
    val detail: String? = null,
)

// Stage of a connection diagnosis. DiagnosisStep uses it.
@Serializable
enum class DiagnosisKind { RESOLVE, CONNECT, EXCHANGE }

// One timed step in a diagnosis. Diagnosis lists these.
@Serializable
data class DiagnosisStep(val kind: DiagnosisKind, val ok: Boolean, val detail: String, val millis: Long)

// Full connection diagnosis result. Hl7Transport.diagnose returns it.
@Serializable
data class Diagnosis(val steps: List<DiagnosisStep>, val cause: FailureCause? = null, val detail: String? = null)

// Settings to start an inbound listener. Hl7Transport.startReceiver takes it.
@Serializable
data class ReceiverConfig(val id: String, val name: String, val protocol: Protocol, val port: Int, val path: String = "/", val autoAck: Boolean = true)

// Running state of a receiver. start/stop/receivers return it.
@Serializable
data class ReceiverStatus(val id: String, val running: Boolean, val received: Int = 0, val failure: FailureCause? = null, val detail: String? = null)

// One inbound message captured by a receiver. PollBatch lists these.
@Serializable
data class TransportEvent(val sequence: Long, val receiverId: String, val text: String, val ack: String? = null, val remote: String = "", val timestamp: Long = 0)

// Batch of receiver events since a sequence. Hl7Transport.poll returns it.
@Serializable
data class PollBatch(val epoch: String, val events: List<TransportEvent> = emptyList())

// Parse, validate and create contract. HapiEngine and RemoteEngine implement it; workspace calls it.
interface Hl7Engine {
    // Supported HL7 versions from the engine. Workspace and API call it.
    suspend fun versions(): List<String>
    // Dictionary for one version. Workspace and API call it.
    suspend fun dictionary(version: String): Hl7Dictionary
    // Structure and validation report for message text. Workspace and API call it.
    suspend fun inspect(request: InspectRequest): InspectReport
    // Builds an ACK message for the given code. Receivers and API call it.
    suspend fun acknowledge(request: AckRequest): String
    // Builds a skeleton message of the given type. Workspace and API call it.
    suspend fun create(request: CreateRequest): String
}

// Send, diagnose and receive contract. Desktop transport and RemoteEngine implement it.
interface Hl7Transport {
    // Sends a message to an endpoint. Workspace senders call it.
    suspend fun send(request: SendRequest): SendResult
    // Probes connectivity to an endpoint. Workspace diagnose UI calls it.
    suspend fun diagnose(endpoint: Endpoint): Diagnosis
    // Starts an inbound listener. Workspace receivers call it.
    suspend fun startReceiver(config: ReceiverConfig): ReceiverStatus
    // Stops an inbound listener by id. Workspace receivers call it.
    suspend fun stopReceiver(id: String): ReceiverStatus
    // Lists current receiver statuses. Workspace receivers call it.
    suspend fun receivers(): List<ReceiverStatus>
    // Reads new inbound events after a sequence. Workspace polling calls it.
    suspend fun poll(after: Long): PollBatch
}

// HTTP paths for the local engine API. Desktop server and RemoteEngine share them.
enum class ApiRoute(val path: String) {
    HEALTH("/api/health"),
    VERSIONS("/api/versions"),
    DICTIONARY("/api/dictionary"),
    INSPECT("/api/inspect"),
    ACK("/api/ack"),
    CREATE("/api/create"),
    SEND("/api/send"),
    DIAGNOSE("/api/diagnose"),
    START_RECEIVER("/api/receivers/start"),
    STOP_RECEIVER("/api/receivers/stop"),
    RECEIVERS("/api/receivers"),
    POLL("/api/poll"),
}

// Version query parameter for dictionary. API handlers decode it.
@Serializable
data class VersionQuery(val version: String)

// Id query parameter for stop-receiver. API handlers decode it.
@Serializable
data class IdQuery(val id: String)

// After-sequence query for poll. API handlers decode it.
@Serializable
data class PollQuery(val after: Long)

// Plain text body reply. ACK and create API responses use it.
@Serializable
data class TextReply(val text: String)

// Error body for failed API calls. Server and clients share it.
@Serializable
data class ApiError(val message: String)

internal val apiJson = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = false }

// Success or failure wrapper for engine calls. Engines.attempt returns it.
sealed interface EngineResult<out T> {
    // Successful value from attempt. Callers unwrap this branch.
    data class Ok<T>(val value: T) : EngineResult<T>
    // Error message from attempt. Callers show this branch.
    data class Failed(val message: String) : EngineResult<Nothing>
}

// Runs a suspend block and catches failures into EngineResult. Engines.attempt forwards here.
internal suspend fun <T> attempt(block: suspend () -> T): EngineResult<T> =
    try {
        EngineResult.Ok(block())
    } catch (cancel: kotlin.coroutines.cancellation.CancellationException) {
        throw cancel
    } catch (error: Throwable) {
        EngineResult.Failed(error.message ?: error::class.simpleName ?: "error")
    }

internal val defaultVersion = "2.5"

internal val knownVersions = listOf("2.1", "2.2", "2.3", "2.3.1", "2.4", "2.5", "2.5.1", "2.6", "2.7", "2.8", "2.8.1")

// Finds the closest supported version string. Engines.matchVersion forwards here.
internal fun matchVersion(raw: String, supported: List<String>): String? {
    val cleaned = raw.trim().substringBefore('^')
    if (cleaned in supported) return cleaned
    val parts = cleaned.split('.')
    for (length in parts.size - 1 downTo 1) {
        val candidate = parts.take(length).joinToString(".")
        if (candidate in supported) return candidate
    }
    return supported.filter { it.startsWith(parts.firstOrNull() ?: "") && it <= cleaned }.maxOrNull()
}

// Empty InspectReport for offline mode. Engines.emptyReport forwards here.
internal fun offlineReport(): InspectReport = InspectReport()

// Reads MSA-1 acknowledgement code from ACK text. Engines.ackCode forwards here.
internal fun ackCodeOf(text: String): String? =
    text.replace("\r\n", "\n").replace('\r', '\n').lines().firstOrNull { it.startsWith("MSA") }
        ?.let { line -> line.getOrNull(3)?.let { d -> line.split(d).getOrNull(1) } }
