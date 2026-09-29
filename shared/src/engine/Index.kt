package hl7lookup.engine

import hl7lookup.i18n.Text

object Engines {
    suspend fun <T> attempt(block: suspend () -> T): EngineResult<T> = hl7lookup.engine.attempt(block)
    fun defaultVersion(): String = defaultVersion
    fun knownVersions(): List<String> = knownVersions
    fun matchVersion(raw: String, supported: List<String>): String? = hl7lookup.engine.matchVersion(raw, supported)
    fun emptyReport(): InspectReport = offlineReport()
    fun ackCode(text: String): String? = ackCodeOf(text)
    fun causeText(cause: FailureCause): Text = failureTexts.getValue(cause)
    fun stepText(kind: DiagnosisKind): Text = diagnosisTexts.getValue(kind)
    fun protocolName(protocol: Protocol): Text = protocolTexts.getValue(protocol)
    fun json(): kotlinx.serialization.json.Json = apiJson
    fun defaultPort(): Int = 7780
    fun texts() = EngineTexts
}
