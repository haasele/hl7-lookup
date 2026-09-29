// Tab wording. session/Index reads it.
package hl7lookup.session

import hl7lookup.i18n.Text

// Labels for tabs and message counts. session/Index and DocumentTabs read these.
object SessionTexts {
    val untitled = Text("Untitled", "Unbenannt")
    val received = Text("Received", "Empfangen")
    val rename = Text("Rename tab", "Tab umbenennen")
    val tabName = Text("Name", "Name")
    val newTab = Text("New tab", "Neuer Tab")
    val close = Text("Close tab", "Tab schließen")
    val messagesCount = Text("{0} messages", "{0} Nachrichten")
    val messageCount = Text("{0} message", "{0} Nachricht")
}
