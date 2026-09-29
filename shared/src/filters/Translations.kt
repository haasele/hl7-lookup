// Filter wording. filters/Index reads it.
package hl7lookup.filters

import hl7lookup.i18n.Text

// Filter dialog wording. filters/Index reads it via texts().
object FilterTexts {
    val title = Text("Filter message list", "Nachrichtenliste filtern")
    val type = Text("Message type", "Nachrichtentyp")
    val typeHint = Text("ADT, ADT^A01 or ADT_A01", "ADT, ADT^A01 oder ADT_A01")
    val from = Text("From (MSH-7)", "Von (MSH-7)")
    val to = Text("Until (MSH-7)", "Bis (MSH-7)")
    val text = Text("Contains text", "Enthält Text")
    val clear = Text("Clear filter", "Filter löschen")
    val shown = Text("{0} of {1} shown", "{0} von {1} angezeigt")
    val invalidDate = Text("Expected format: {0}", "Erwartetes Format: {0}")
}
