// Receiver wording. receivers/Index reads it.
package hl7lookup.receivers

import hl7lookup.i18n.Text

// Labels for the receivers panel and dialog. receivers/Index reads it.
object ReceiverTexts {
    val title = Text("Receivers", "Receiver")
    val add = Text("New receiver", "Neuer Receiver")
    val edit = Text("Edit receiver", "Receiver bearbeiten")
    val name = Text("Name", "Name")
    val protocol = Text("Protocol", "Protokoll")
    val port = Text("Listen on port", "Port")
    val path = Text("HTTP path", "HTTP-Pfad")
    val autoAck = Text("Answer with an ACK", "Mit ACK antworten")
    val targetTab = Text("Put messages into tab", "Nachrichten in Tab ablegen")
    val newTab = Text("\"Received\" tab", "Tab \"Empfangen\"")
    val start = Text("Start", "Starten")
    val stop = Text("Stop", "Stoppen")
    val running = Text("listening · {0} received", "lauscht · {0} empfangen")
    val stopped = Text("stopped", "gestoppt")
    val empty = Text("No receivers yet. A receiver listens for MLLP or HTTP messages on this machine.", "Noch keine Receiver. Ein Receiver lauscht auf diesem Rechner auf MLLP- oder HTTP-Nachrichten.")
    val defaultName = Text("Receiver {0}", "Receiver {0}")
}
