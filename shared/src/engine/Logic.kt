package hl7lookup.engine

import hl7lookup.dictionary.Hl7Dictionary
import kotlinx.serialization.Serializable

@Serializable
enum class Severity { ERROR, WARNING, INFO }

@Serializable
data class InspectRequest(val text: String, val version: String? = null)

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

@Serializable
data class InspectReport(val structure: String? = null, val findings: List<EngineFinding> = emptyList())

@Serializable
data class AckRequest(val text: String, val code: String, val errorText: String? = null)

@Serializable
data class CreateRequest(val messageType: String, val event: String, val version: String, val processingId: String = "P")

@Serializable
enum class Protocol { MLLP, HTTP, TAB }

@Serializable
data class Endpoint(val protocol: Protocol, val host: String, val port: Int, val path: String = "/", val timeoutMillis: Int = 10_000)

@Serializable
data class SendRequest(val endpoint: Endpoint, val text: String)

@Serializable
enum class FailureCause { UNKNOWN_HOST, REFUSED, TIMEOUT, UNREACHABLE, RESET, NO_RESPONSE, BAD_RESPONSE, HTTP_STATUS, PORT_IN_USE, ENGINE_OFFLINE, OTHER }

@Serializable
data class SendResult(
    val ok: Boolean,
    val response: String? = null,
    val durationMillis: Long = 0,
    val failure: FailureCause? = null,
    val detail: String? = null,
)

@Serializable
enum class DiagnosisKind { RESOLVE, CONNECT, EXCHANGE }

@Serializable
data class DiagnosisStep(val kind: DiagnosisKind, val ok: Boolean, val detail: String, val millis: Long)

@Serializable
data class Diagnosis(val steps: List<DiagnosisStep>, val cause: FailureCause? = null, val detail: String? = null)

@Serializable
data class ReceiverConfig(val id: String, val name: String, val protocol: Protocol, val port: Int, val path: String = "/", val autoAck: Boolean = true)

@Serializable
data class ReceiverStatus(val id: String, val running: Boolean, val received: Int = 0, val failure: FailureCause? = null, val detail: String? = null)

@Serializable
data class TransportEvent(val sequence: Long, val receiverId: String, val text: String, val ack: String? = null, val remote: String = "", val timestamp: Long = 0)

@Serializable
data class PollBatch(val epoch: String, val events: List<TransportEvent> = emptyList())

interface Hl7Engine {
    suspend fun versions(): List<String>
    suspend fun dictionary(version: String): Hl7Dictionary
    suspend fun inspect(request: InspectRequest): InspectReport
    suspend fun acknowledge(request: AckRequest): String
    suspend fun create(request: CreateRequest): String
}

interface Hl7Transport {
    suspend fun send(request: SendRequest): SendResult
    suspend fun diagnose(endpoint: Endpoint): Diagnosis
    suspend fun startReceiver(config: ReceiverConfig): ReceiverStatus
    suspend fun stopReceiver(id: String): ReceiverStatus
    suspend fun receivers(): List<ReceiverStatus>
    suspend fun poll(after: Long): PollBatch
}

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

@Serializable
data class VersionQuery(val version: String)

@Serializable
data class IdQuery(val id: String)

@Serializable
data class PollQuery(val after: Long)

@Serializable
data class TextReply(val text: String)

@Serializable
data class ApiError(val message: String)

internal val apiJson = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = false }

sealed interface EngineResult<out T> {
    data class Ok<T>(val value: T) : EngineResult<T>
    data class Failed(val message: String) : EngineResult<Nothing>
}

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

internal fun offlineReport(): InspectReport = InspectReport()

internal fun ackCodeOf(text: String): String? =
    text.replace("\r\n", "\n").replace('\r', '\n').lines().firstOrNull { it.startsWith("MSA") }
        ?.let { line -> line.getOrNull(3)?.let { d -> line.split(d).getOrNull(1) } }
