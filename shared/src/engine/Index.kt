// Engine and transport contracts. Workspace calls Engines; HapiEngine and the browser RemoteEngine implement them.
package hl7lookup.engine

import hl7lookup.i18n.Text

// Public engine helpers for workspace and hosts. Each method forwards into Logic or Translations.
object Engines {
    // Wraps a suspend call as Ok or Failed. Forwards to attempt.
    suspend fun <T> attempt(block: suspend () -> T): EngineResult<T> = hl7lookup.engine.attempt(block)
    // Default HL7 version string. Reads defaultVersion.
    fun defaultVersion(): String = defaultVersion
    // Supported HL7 version list. Reads knownVersions.
    fun knownVersions(): List<String> = knownVersions
    // Picks the closest supported version for raw MSH text. Forwards to matchVersion.
    fun matchVersion(raw: String, supported: List<String>): String? = hl7lookup.engine.matchVersion(raw, supported)
    // Empty inspect report used when the engine is offline. Forwards to offlineReport.
    fun emptyReport(): InspectReport = offlineReport()
    // Extracts MSA acknowledgement code from ACK text. Forwards to ackCodeOf.
    fun ackCode(text: String): String? = ackCodeOf(text)
    // Localized wording for a transport failure cause. Reads failureTexts.
    fun causeText(cause: FailureCause): Text = failureTexts.getValue(cause)
    // Localized label for a diagnosis step kind. Reads diagnosisTexts.
    fun stepText(kind: DiagnosisKind): Text = diagnosisTexts.getValue(kind)
    // Localized name for a transport protocol. Reads protocolTexts.
    fun protocolName(protocol: Protocol): Text = protocolTexts.getValue(protocol)
    // Shared JSON codec for engine API payloads. Returns apiJson.
    fun json(): kotlinx.serialization.json.Json = apiJson
    // Default local engine HTTP port. Hard-coded for hosts.
    fun defaultPort(): Int = 7780
    // Offline-engine wording object. Returns EngineTexts.
    fun texts() = EngineTexts
}
