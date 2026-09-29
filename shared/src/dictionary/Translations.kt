package hl7lookup.dictionary

import hl7lookup.i18n.Text

object DictionaryTexts {
    val loading = Text("Loading HL7 {0} definitions…", "HL7-{0}-Definitionen werden geladen…")
    val failed = Text("HL7 {0} definitions are unavailable.", "HL7-{0}-Definitionen sind nicht verfügbar.")
    val retry = Text("Retry", "Erneut versuchen")
    val datatype = Text("Data type", "Datentyp")
    val table = Text("Table {0}", "Tabelle {0}")
    val required = Text("Required", "Pflicht")
    val optional = Text("Optional", "Optional")
    val repeats = Text("Repeats", "Wiederholbar")
    val maxLength = Text("Max. length {0}", "Max. Länge {0}")
    val unknownCode = Text("Code not in table {0}", "Code nicht in Tabelle {0}")
}
