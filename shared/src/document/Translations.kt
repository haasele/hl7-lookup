// Document wording. document/Index reads it.
package hl7lookup.document

import hl7lookup.i18n.Text

// Labels for segment, field and delimiter roles. document/Index reads it via Er7.texts.
object DocumentTexts {
    val segment = Text("Segment", "Segment")
    val field = Text("Field", "Feld")
    val component = Text("Component", "Komponente")
    val subcomponent = Text("Subcomponent", "Unterkomponente")
    val repetition = Text("Repetition", "Wiederholung")
    val segmentName = Text("Segment name", "Segmentname")
    val fieldSeparator = Text("Field separator", "Feldtrenner")
    val encodingCharacters = Text("Encoding characters", "Kodierzeichen")
}
