// Integration wording. integrations/Index reads it.
package hl7lookup.integrations

import hl7lookup.i18n.Text

// English and German strings for the panel and dialog. Index and panels read them via tr().
object IntegrationTexts {
    val title = Text("Integrations", "Integrationen")
    val add = Text("New integration", "Neue Integration")
    val edit = Text("Edit integration", "Integration bearbeiten")
    val name = Text("Name", "Name")
    val sender = Text("Sender", "Sender")
    val receiver = Text("Receiver", "Receiver")
    val iface = Text("Interface", "Interface")
    val activate = Text("Activate", "Aktivieren")
    val active = Text("active", "aktiv")
    val none = Text("—", "—")
    val noIntegration = Text("No integration", "Keine Integration")
    val empty = Text("No integrations yet. An integration bundles a sender, a receiver and an interface so you can switch between systems with one click.", "Noch keine Integrationen. Eine Integration bündelt Sender, Receiver und Interface, damit du mit einem Klick zwischen Systemen wechselst.")
    val summary = Text("Sender: {0} · Receiver: {1} · Interface: {2}", "Sender: {0} · Receiver: {1} · Interface: {2}")
}
