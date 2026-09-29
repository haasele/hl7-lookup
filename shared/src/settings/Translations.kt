// Settings wording. settings/Index reads it.
package hl7lookup.settings

import hl7lookup.i18n.Text

// English and German strings for the settings dialog. Index reads them via tr().
object SettingsTexts {
    val title = Text("Settings", "Einstellungen")
    val dateFormat = Text("Date format", "Datumsformat")
    val version = Text("Default HL7 version", "Standard-HL7-Version")
    val versionHint = Text(
        "Used when a message does not declare a version in MSH-12 or declares one without definitions.",
        "Gilt, wenn eine Nachricht in MSH-12 keine oder eine unbekannte Version angibt.",
    )
    val emptyFields = Text("Show empty fields in the grid", "Leere Felder im Gitter zeigen")
    val language = Text("Language", "Sprache")
    val autoValidate = Text("Validate while typing", "Beim Tippen prüfen")
    val relativeDates = Text("Show relative dates", "Relative Datumsangaben zeigen")
}
