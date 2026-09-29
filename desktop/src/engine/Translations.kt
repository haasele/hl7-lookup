// Desktop engine wording. engine/Index reads it.
package hl7lookup.desktop.engine

import hl7lookup.i18n.Text

// Holds HAPI engine status strings. UI that mentions the desktop engine can read them.
object HapiEngineTexts {
    val name = Text("HAPI HL7v2 engine", "HAPI-HL7v2-Engine")
    val warming = Text("Loading HL7 definitions…", "HL7-Definitionen werden geladen…")
}
