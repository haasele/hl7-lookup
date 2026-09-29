package hl7lookup.statistics

import hl7lookup.i18n.Text

object StatisticsTexts {
    val title = Text("Field statistics", "Feldstatistik")
    val pick = Text("Select a field in the story, raw text or grid to count its values across the message list.", "Wähle ein Feld in Story, Rohtext oder Gitter, um seine Werte über die Nachrichtenliste zu zählen.")
    val value = Text("Value", "Wert")
    val count = Text("Count", "Anzahl")
    val share = Text("Share", "Anteil")
    val examined = Text("Messages examined: {0}", "Untersuchte Nachrichten: {0}")
    val missing = Text("Messages without this field: {0}", "Nachrichten ohne dieses Feld: {0}")
    val noValues = Text("No message in the list has a value here.", "Keine Nachricht der Liste hat hier einen Wert.")
}
