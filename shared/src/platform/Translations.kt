// File-dialog wording. platform/Index reads it.
package hl7lookup.platform

import hl7lookup.i18n.Text

// Wording for open/save dialogs and host labels; Platforms.texts() returns this for Index callers.
object PlatformTexts {
    val openMessages = Text("Open HL7 messages", "HL7-Nachrichten öffnen")
    val saveMessages = Text("Save HL7 messages", "HL7-Nachrichten speichern")
    val openInterface = Text("Import interface definition", "Interface-Definition importieren")
    val saveInterface = Text("Export interface definition", "Interface-Definition exportieren")
    val desktop = Text("Desktop", "Desktop")
    val browser = Text("Browser", "Browser")
}
