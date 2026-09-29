// Layout menu wording. layout/Index reads it.
package hl7lookup.layout

import hl7lookup.i18n.Text

// Menu and dialog strings for presets; LayoutMenu and LayoutPresetDialog read these via tr.
object LayoutTexts {
    val menu = Text("Layout", "Layout")
    val reset = Text("Standard layout", "Standardlayout")
    val save = Text("Save preset…", "Preset speichern…")
    val title = Text("Save layout", "Layout speichern")
    val name = Text("Preset name", "Preset-Name")
    val empty = Text("No saved presets yet.", "Noch keine Presets.")
    val drag = Text("Drag", "Ziehen")
    val remove = Text("Remove", "Entfernen")
}
