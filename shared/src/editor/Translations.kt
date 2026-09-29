package hl7lookup.editor

import hl7lookup.i18n.Text

object EditorTexts {
    val title = Text("Raw message", "Rohtext")
    val placeholder = Text("Paste or type an HL7 message here…", "HL7-Nachricht hier einfügen oder tippen…")
    val delimiters = Text("Delimiters", "Trennzeichen")
    val delimiterField = Text("Field", "Feld")
    val delimiterComponent = Text("Component", "Komponente")
    val delimiterRepetition = Text("Repetition", "Wiederholung")
    val delimiterEscape = Text("Escape", "Escape")
    val delimiterSubcomponent = Text("Subcomponent", "Unterkomponente")
    val delimiterInvalid = Text("Delimiters must be five different symbols.", "Trennzeichen müssen fünf verschiedene Symbole sein.")
    val tooltips = Text("Tooltips", "Tooltips")
    val datatype = Text("Type {0}", "Typ {0}")
    val table = Text("Table {0}", "Tabelle {0}")
    val tableNamed = Text("Table {0} – {1}", "Tabelle {0} – {1}")
    val required = Text("required", "Pflicht")
    val repeating = Text("repeats", "wiederholbar")
    val maxLength = Text("max {0}", "max. {0}")
    val localDate = Text("Local time", "Lokale Zeit")
    val changeDate = Text("Pick date", "Datum wählen")
    val invalidDate = Text("Use {0}", "Format {0}")
    val chooseValue = Text("Choose a table value", "Tabellenwert wählen")
    val unknownValue = Text("\"{0}\" is not in this table", "\"{0}\" steht nicht in dieser Tabelle")
    val searchHits = Text("{0} of {1}", "{0} von {1}")
    val noHits = Text("No hits", "Keine Treffer")
    val copy = Text("Copy", "Kopieren")
    val repetitionOf = Text("repetition {0} of {1}", "Wiederholung {0} von {1}")
}
