// Transport error wording. transport/Index reads it.
package hl7lookup.desktop.transport

import hl7lookup.i18n.Text

// Holds listening/stopped receiver strings. Transports.texts exposes them to the UI.
object TransportTexts {
    val listening = Text("Listening on port {0} ({1})", "Empfang auf Port {0} ({1})")
    val stopped = Text("Stopped receiver on port {0}", "Receiver auf Port {0} gestoppt")
}
