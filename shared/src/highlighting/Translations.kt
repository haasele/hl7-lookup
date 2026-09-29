package hl7lookup.highlighting

import hl7lookup.i18n.Text

object HighlightTexts {
    val title = Text("Highlighting", "Hervorhebung")
    val spec = Text("Field (e.g. PID-5 or OBX-5.1)", "Feld (z. B. PID-5 oder OBX-5.1)")
    val value = Text("Only when value contains (optional)", "Nur wenn der Wert enthält (optional)")
    val add = Text("Add highlight", "Hervorhebung hinzufügen")
    val highlightSelection = Text("Highlight selected field", "Gewähltes Feld hervorheben")
    val empty = Text("No highlights yet. Add a field path to color it in the story, raw text, grid and message list.", "Noch keine Hervorhebungen. Füge einen Feldpfad hinzu, um ihn in Story, Rohtext, Gitter und Liste zu färben.")
    val invalid = Text("Use a path like PID-5, PID-3.1 or OBX-5[2].1", "Nutze einen Pfad wie PID-5, PID-3.1 oder OBX-5[2].1")
    val fromInterface = Text("from interface", "aus Interface")
    val clear = Text("Remove all", "Alle entfernen")
    val anyValue = Text("any value", "jeder Wert")
}
