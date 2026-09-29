package hl7lookup.validation

import hl7lookup.i18n.Text

object ValidationTexts {
    val title = Text("Validation", "Prüfung")
    val clean = Text("No findings for this message.", "Keine Befunde für diese Nachricht.")
    val noDefinitions = Text("Definitions for this version are not loaded, field checks are paused.", "Definitionen für diese Version fehlen, Feldprüfungen pausieren.")
    val summary = Text("{0} errors, {1} warnings, {2} notes", "{0} Fehler, {1} Warnungen, {2} Hinweise")
    val error = Text("Error", "Fehler")
    val warning = Text("Warning", "Warnung")
    val info = Text("Note", "Hinweis")
    val showNotes = Text("Show notes", "Hinweise zeigen")
    val engineOffline = Text("Structure checks need the local engine.", "Strukturprüfungen benötigen die lokale Engine.")
}

internal val findingTexts = mapOf(
    FindingKind.REQUIRED to Text("Required field {0} is missing.", "Pflichtfeld {0} fehlt."),
    FindingKind.EMPTY to Text("Field {0} contains only delimiters.", "Feld {0} enthält nur Trennzeichen."),
    FindingKind.DATE to Text("\"{0}\" is not a valid HL7 date/time.", "\"{0}\" ist kein gültiges HL7-Datum."),
    FindingKind.TABLE to Text("\"{0}\" is not a value of table {1}.", "\"{0}\" ist kein Wert der Tabelle {1}."),
    FindingKind.LENGTH to Text("Length {0} exceeds the maximum of {1}.", "Länge {0} überschreitet das Maximum {1}."),
    FindingKind.REPETITION to Text("{0} repetitions, at most {1} allowed.", "{0} Wiederholungen, höchstens {1} erlaubt."),
    FindingKind.NUMERIC to Text("\"{0}\" is not a number.", "\"{0}\" ist keine Zahl."),
    FindingKind.UNKNOWN_SEGMENT to Text("Segment is not defined in HL7 {0}.", "Segment ist in HL7 {0} nicht definiert."),
    FindingKind.UNKNOWN_FIELD to Text("Field is not defined in HL7 {0}.", "Feld ist in HL7 {0} nicht definiert."),
    FindingKind.ENGINE to Text("{0}", "{0}"),
    FindingKind.RULE_REQUIRED to Text("Interface requires a value.", "Das Interface verlangt einen Wert."),
    FindingKind.RULE_EXPECTED to Text("Expected \"{0}\", found \"{1}\".", "Erwartet \"{0}\", gefunden \"{1}\"."),
    FindingKind.RULE_DATE to Text("\"{0}\" is not a date as required by the interface.", "\"{0}\" ist kein Datum, wie das Interface verlangt."),
    FindingKind.RULE_TABLE to Text("\"{0}\" is not in interface table {1}.", "\"{0}\" steht nicht in Interface-Tabelle {1}."),
    FindingKind.RULE_TYPE to Text("Interface expects {0}, message is {1}.", "Interface erwartet {0}, Nachricht ist {1}."),
)
