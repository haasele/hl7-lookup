// Engine error wording. engine/Index reads it.
package hl7lookup.engine

import hl7lookup.i18n.Text

// Offline-engine messaging for the UI. Engines.texts exposes it; workspace reads it.
object EngineTexts {
    val offline = Text("HL7 engine unreachable", "HL7-Engine nicht erreichbar")
    val offlineDetail = Text(
        "Run HL7 Lookup with --server to use definitions, validation and transport.",
        "Führe HL7 Lookup mit --server aus, um Definitionen, Prüfung und Versand zu nutzen.",
    )
}

internal val failureTexts = mapOf(
    FailureCause.UNKNOWN_HOST to Text("The host name could not be resolved.", "Der Hostname konnte nicht aufgelöst werden."),
    FailureCause.REFUSED to Text("The connection was refused. Nothing is listening on this port.", "Die Verbindung wurde abgelehnt. Auf diesem Port lauscht niemand."),
    FailureCause.TIMEOUT to Text("The remote side did not answer in time.", "Die Gegenseite hat nicht rechtzeitig geantwortet."),
    FailureCause.UNREACHABLE to Text("The network route to the host is unavailable.", "Keine Netzwerkroute zum Host."),
    FailureCause.RESET to Text("The connection was closed by the remote side.", "Die Verbindung wurde von der Gegenseite geschlossen."),
    FailureCause.NO_RESPONSE to Text("The message was sent but no acknowledgement came back.", "Die Nachricht wurde gesendet, aber es kam keine Quittung zurück."),
    FailureCause.BAD_RESPONSE to Text("The answer is not a framed HL7 message.", "Die Antwort ist keine gerahmte HL7-Nachricht."),
    FailureCause.HTTP_STATUS to Text("The HTTP endpoint answered with an error status.", "Der HTTP-Endpunkt hat mit einem Fehlerstatus geantwortet."),
    FailureCause.PORT_IN_USE to Text("The port is already in use by another program.", "Der Port wird bereits von einem anderen Programm benutzt."),
    FailureCause.ENGINE_OFFLINE to Text("The local HL7 engine is not reachable.", "Die lokale HL7-Engine ist nicht erreichbar."),
    FailureCause.OTHER to Text("The operation failed.", "Der Vorgang ist fehlgeschlagen."),
)

internal val diagnosisTexts = mapOf(
    DiagnosisKind.RESOLVE to Text("Resolve host", "Host auflösen"),
    DiagnosisKind.CONNECT to Text("Open connection", "Verbindung öffnen"),
    DiagnosisKind.EXCHANGE to Text("Exchange test message", "Testnachricht austauschen"),
)

internal val protocolTexts = mapOf(
    Protocol.MLLP to Text("MLLP (TCP)", "MLLP (TCP)"),
    Protocol.HTTP to Text("HTTP", "HTTP"),
    Protocol.TAB to Text("Another tab", "Anderer Tab"),
)
