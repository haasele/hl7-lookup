// ACK wording. acknowledgements/Index reads it.
package hl7lookup.acknowledgements

import hl7lookup.i18n.Text

// English and German strings for the ACK panel. Index reads them via tr().
object AckTexts {
    val title = Text("ACKs", "ACKs")
    val empty = Text("Acknowledgements of sent and received messages appear here.", "Quittungen gesendeter und empfangener Nachrichten erscheinen hier.")
    val sent = Text("sent to {0}", "gesendet an {0}")
    val received = Text("received from {0}", "empfangen von {0}")
    val noAck = Text("no acknowledgement", "keine Quittung")
    val open = Text("Open ACK in tab", "ACK im Tab öffnen")
    val clear = Text("Clear", "Leeren")
    val took = Text("{0} ms", "{0} ms")
    val tabTitle = Text("ACKs", "ACKs")
}
