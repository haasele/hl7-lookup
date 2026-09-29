package hl7lookup.senders

import hl7lookup.i18n.Text

object SenderTexts {
    val title = Text("Senders", "Sender")
    val add = Text("New sender", "Neuer Sender")
    val edit = Text("Edit sender", "Sender bearbeiten")
    val name = Text("Name", "Name")
    val protocol = Text("Protocol", "Protokoll")
    val host = Text("Host", "Host")
    val port = Text("Port", "Port")
    val path = Text("HTTP path", "HTTP-Pfad")
    val timeout = Text("Timeout (ms)", "Zeitlimit (ms)")
    val targetTab = Text("Target tab", "Ziel-Tab")
    val send = Text("Send current", "Aktuelle senden")
    val sendAll = Text("Send list", "Liste senden")
    val test = Text("Test connection", "Verbindung testen")
    val empty = Text("No senders yet. A sender delivers messages over MLLP, HTTP or into another tab.", "Noch keine Sender. Ein Sender liefert Nachrichten per MLLP, HTTP oder in einen anderen Tab.")
    val sending = Text("Sending {0} message(s) via {1}…", "Sende {0} Nachricht(en) über {1}…")
    val defaultName = Text("Sender {0}", "Sender {0}")
    val diagnosis = Text("Connection test: {0}", "Verbindungstest: {0}")
    val diagnosisOk = Text("The endpoint answered and the exchange succeeded.", "Der Endpunkt hat geantwortet, der Austausch war erfolgreich.")
    val nothingToSend = Text("There is no message to send.", "Keine Nachricht zum Senden.")
}
