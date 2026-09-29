package hl7lookup.interfaces

import hl7lookup.i18n.Text

object InterfaceTexts {
    val menu = Text("Interfaces", "Interfaces")
    val manage = Text("Manage interfaces…", "Interfaces verwalten…")
    val import = Text("Import…", "Importieren…")
    val export = Text("Export…", "Exportieren…")
    val exportAll = Text("Export all…", "Alle exportieren…")
    val new = Text("New interface", "Neues Interface")
    val fromMessage = Text("New from current message", "Neu aus aktueller Nachricht")
    val delete = Text("Delete interface", "Interface löschen")
    val name = Text("Name", "Name")
    val description = Text("Description", "Beschreibung")
    val messageType = Text("Message type", "Nachrichtentyp")
    val event = Text("Event", "Ereignis")
    val structure = Text("Structure", "Struktur")
    val version = Text("HL7 version", "HL7-Version")
    val rules = Text("Rules", "Regeln")
    val tables = Text("Custom tables", "Eigene Tabellen")
    val addRule = Text("Add rule", "Regel hinzufügen")
    val addTable = Text("Add table", "Tabelle hinzufügen")
    val tableId = Text("Table ID", "Tabellen-ID")
    val tableName = Text("Table name", "Tabellenname")
    val tableEntries = Text("One entry per line: CODE=Description", "Ein Eintrag pro Zeile: CODE=Beschreibung")
    val tableHint = Text("Use an HL7 table number such as 0004 to replace that table while the interface is active.", "Mit einer HL7-Tabellennummer wie 0004 ersetzt die Tabelle diese, solange das Interface aktiv ist.")
    val useForTab = Text("Use for this tab", "Für diesen Tab verwenden")
    val activeForTab = Text("Active for this tab", "Aktiv für diesen Tab")
    val none = Text("No interface", "Kein Interface")
    val empty = Text("No interface definitions yet.", "Noch keine Interface-Definitionen.")
    val runOnList = Text("Check message list", "Nachrichtenliste prüfen")
    val listResult = Text("#{0} {1}: {2} errors, {3} warnings", "#{0} {1}: {2} Fehler, {3} Warnungen")
    val allPassedOne = Text("All {0} message matches the interface.", "Alle {0} Nachricht erfüllt das Interface.")
    val allPassed = Text("All {0} messages match the interface.", "Alle {0} Nachrichten erfüllen das Interface.")
    val imported = Text("Imported {0} interface(s).", "{0} Interface(s) importiert.")
    val importFailed = Text("The file is not an interface definition.", "Die Datei ist keine Interface-Definition.")
    val untitled = Text("Interface {0}", "Interface {0}")
    val specHint = Text("PID-3.1", "PID-3.1")
    val valueHint = Text("Expected value(s), separated by |", "Erwartete Werte, getrennt durch |")
}

internal val ruleNames = mapOf(
    RuleKind.REQUIRED to Text("Required", "Pflicht"),
    RuleKind.EXPECTED to Text("Expected value", "Erwarteter Wert"),
    RuleKind.DATE to Text("Must be a date", "Muss Datum sein"),
    RuleKind.TABLE to Text("Custom table", "Eigene Tabelle"),
    RuleKind.HIGHLIGHT to Text("Highlight", "Hervorheben"),
)
