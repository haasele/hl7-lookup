package hl7lookup.compare

import hl7lookup.i18n.Text

object CompareTexts {
    val title = Text("Compare messages", "Nachrichten vergleichen")
    val menu = Text("Compare…", "Vergleichen…")
    val left = Text("Left", "Links")
    val right = Text("Right", "Rechts")
    val pasted = Text("Pasted text", "Eingefügter Text")
    val pasteHint = Text("Paste an HL7 message to compare against", "HL7-Nachricht zum Vergleich einfügen")
    val ignore = Text("Ignore fields", "Felder ignorieren")
    val ignoreHint = Text("e.g. MSH-7, MSH-10, EVN-2", "z. B. MSH-7, MSH-10, EVN-2")
    val onlyDifferences = Text("Only differences", "Nur Unterschiede")
    val ignoreCase = Text("Ignore case", "Groß-/Kleinschreibung ignorieren")
    val identical = Text("The messages are identical apart from ignored fields.", "Die Nachrichten sind bis auf ignorierte Felder identisch.")
    val summary = Text("{0} changed, {1} only right, {2} only left", "{0} geändert, {1} nur rechts, {2} nur links")
    val position = Text("Position", "Position")
    val interfaceCheck = Text("Interface: {0} errors left, {1} errors right", "Interface: {0} Fehler links, {1} Fehler rechts")
    val source = Text("{0} · #{1} {2}", "{0} · #{1} {2}")
    val swap = Text("Swap", "Tauschen")
}

internal val diffNames = mapOf(
    DiffKind.SAME to Text("same", "gleich"),
    DiffKind.CHANGED to Text("changed", "geändert"),
    DiffKind.ADDED to Text("only right", "nur rechts"),
    DiffKind.REMOVED to Text("only left", "nur links"),
)
