// Type, event and segment explanations plus the guide message. catalog/Index and CatalogTest call Catalog.
package hl7lookup.catalog

import hl7lookup.dictionary.Hl7Dictionary
import hl7lookup.dictionary.SegmentDef
import hl7lookup.dictionary.StructureElement
import hl7lookup.document.Er7

// Message type with its events. wikiTypes builds these for the type list.
data class WikiType(val type: String, val description: String, val events: List<WikiEvent>)

// One trigger event under a type. wikiTypes fills them from structures and events.
data class WikiEvent(val code: String, val description: String, val structure: String)

// One field or component row in a segment view. segmentPieces builds these.
data class WikiPiece(val code: String, val purposeEn: String, val purposeDe: String, val example: String)

// Segment with plain description and pieces. wikiSegments builds the Segments path list.
data class WikiSegmentView(val name: String, val descriptionEn: String, val descriptionDe: String, val pieces: List<WikiPiece>)

// One outline line for structure walk. walkStructure appends these for wikiOutline.
data class WikiLine(
    val depth: Int,
    val code: String,
    val name: String,
    val datatype: String,
    val required: Boolean,
    val repeating: Boolean,
    val table: String?,
)

// Groups structures into types and events. Catalog.types and WikiScreen call it.
internal fun wikiTypes(dictionary: Hl7Dictionary): List<WikiType> {
    val eventsByType = linkedMapOf<String, MutableMap<String, WikiEvent>>()
    // Gets or creates the event map for a type. wikiTypes uses it while scanning structures.
    fun bucket(type: String) = eventsByType.getOrPut(type) { linkedMapOf() }
    dictionary.messageTypes.keys.forEach { bucket(it) }
    dictionary.structures.keys.forEach { name ->
        val type = name.substringBefore('_')
        val code = name.substringAfter('_', "")
        if (code.isBlank() || code == name) return@forEach
        bucket(type)[code] = WikiEvent(code, dictionary.events[code].orEmpty(), name)
    }
    dictionary.eventStructures.forEach { (key, structure) ->
        val type = key.substringBefore('_')
        val code = key.substringAfter('_', "")
        if (code.isBlank() || code == key) return@forEach
        bucket(type)[code] = WikiEvent(code, dictionary.events[code].orEmpty(), structure.ifBlank { key })
    }
    eventsByType.forEach { (type, events) ->
        if (events.isEmpty() && type in dictionary.structures) {
            events[type] = WikiEvent(type, dictionary.messageTypes[type].orEmpty(), type)
        }
    }
    return eventsByType
        .map { (type, events) -> WikiType(type, dictionary.messageTypes[type].orEmpty(), events.values.sortedBy { eventOrder(it.code) }) }
        .sortedBy { it.type }
}

// Sorts event codes with padded numbers. wikiTypes sorts events with it.
private fun eventOrder(code: String): String {
    val prefix = code.takeWhile { !it.isDigit() }
    val number = code.dropWhile { !it.isDigit() }.toIntOrNull() ?: 0
    return prefix + number.toString().padStart(4, '0')
}

// Keeps types and events that match a query. Catalog.matching and WikiScreen call it.
internal fun filterWiki(types: List<WikiType>, query: String): List<WikiType> {
    val needle = query.trim()
    if (needle.isEmpty()) return types
    return types.mapNotNull { type ->
        val typeHit = type.type.contains(needle, true) || type.description.contains(needle, true)
        val events = type.events.filter {
            it.code.contains(needle, true) || it.description.contains(needle, true) ||
                ('_' in needle && it.structure.contains(needle, true))
        }
        when {
            events.isNotEmpty() && !typeHit -> type.copy(events = events)
            events.isNotEmpty() || typeHit -> type
            else -> null
        }
    }
}

// Walks a structure into indented lines. Catalog.outline delegates to it.
internal fun wikiOutline(dictionary: Hl7Dictionary, structure: String): List<WikiLine> {
    val elements = dictionary.structures[structure]?.elements ?: return emptyList()
    val lines = mutableListOf<WikiLine>()
    elements.forEach { walkStructure(dictionary, it, 0, lines) }
    return lines
}

// Builds a guide message with field labels. Catalog.guide and WikiScreen call it.
internal fun wikiGuide(dictionary: Hl7Dictionary, type: String, event: String, structure: String, german: Boolean = false): String {
    val segments = guideSegments(dictionary, structure)
    return segments.joinToString("\n") { segment ->
        if (segment.name == "MSH") mshGuide(dictionary, type, event, structure, german) else segmentGuide(dictionary, segment.name, german)
    }
}

// Finds a matching sample or fakes segments. Catalog.example and WikiScreen call it.
internal fun wikiExample(dictionary: Hl7Dictionary, type: String, event: String, structure: String, samples: List<String>): String {
    val match = samples.firstOrNull { text ->
        val header = Er7.header(Er7.parse(text))
        header.type == type && (event.isBlank() || header.event.isBlank() || header.event == event)
    }
    if (match != null) return match
    val names = segmentNames(dictionary, structure)
    return names.map { fakeLine(it, type, event, structure, dictionary.version) }.joinToString("\n")
}

// Recurses groups and fields into WikiLines. wikiOutline walks each structure element with it.
private fun walkStructure(dictionary: Hl7Dictionary, element: StructureElement, depth: Int, into: MutableList<WikiLine>) {
    if (element.group) {
        into += WikiLine(depth, "", element.name, "", element.required, element.repeating, null)
        element.children.forEach { walkStructure(dictionary, it, depth + 1, into) }
        return
    }
    val segment = dictionary.segments[element.name]
    into += WikiLine(depth, element.name, segment?.description ?: element.name, "", element.required, element.repeating, null)
    segment?.fields?.sortedBy { it.number }?.forEach { field ->
        into += WikiLine(depth + 1, "${element.name}-${field.number}", field.name, field.datatype, field.required, field.maxRepetitions != 1, field.table)
        dictionary.datatypes[field.datatype]?.components?.forEach { component ->
            into += WikiLine(
                depth + 2,
                "${element.name}-${field.number}.${component.number}",
                component.name,
                component.datatype,
                false,
                false,
                component.table,
            )
        }
    }
}

// Segment name plus required flag for the guide. guideSegments collects these.
private data class GuideSegment(val name: String, val required: Boolean)

// Picks required or first segments for the guide. wikiGuide asks it for the segment list.
private fun guideSegments(dictionary: Hl7Dictionary, structure: String): List<GuideSegment> {
    val found = mutableListOf<GuideSegment>()
    // Collects segment names from a structure tree. guideSegments walks elements with it.
    fun walk(element: StructureElement) {
        if (element.group) element.children.forEach(::walk)
        else if (element.name.length == 3 && found.none { it.name == element.name }) found += GuideSegment(element.name, element.required)
    }
    dictionary.structures[structure]?.elements?.forEach(::walk)
    if (found.none { it.name == "MSH" }) found.add(0, GuideSegment("MSH", true))
    val required = found.filter { it.required }
    return if (required.size >= 2) required else found.take(8)
}

// Builds the labeled MSH guide line. wikiGuide uses it for the header segment.
private fun mshGuide(dictionary: Hl7Dictionary, type: String, event: String, structure: String, german: Boolean): String {
    val numbers = chosenNumbers(dictionary, "MSH") + 9
    val max = numbers.maxOrNull() ?: 9
    val values = (3..max).joinToString("|") { number ->
        when (number) {
            9 -> listOf(type, event, structure).filter { it.isNotBlank() }.joinToString("^")
            else -> fieldLabel(dictionary, "MSH", number, german)
        }
    }
    return "MSH|^~\\&|$values"
}

// Builds a labeled guide line for one segment. wikiGuide maps each segment through it.
private fun segmentGuide(dictionary: Hl7Dictionary, name: String, german: Boolean): String {
    val numbers = chosenNumbers(dictionary, name)
    if (numbers.isEmpty()) return name
    val max = numbers.max()
    val values = (1..max).joinToString("|") { number ->
        if (number in numbers) fieldLabel(dictionary, name, number, german) else ""
    }
    return "$name|$values"
}

// Picks required and plain field numbers. mshGuide and segmentGuide fill only those.
private fun chosenNumbers(dictionary: Hl7Dictionary, segment: String): Set<Int> {
    val fields = dictionary.segments[segment]?.fields.orEmpty()
    val chosen = fields.filter { it.required || plainField(segment, it.number) != null }.map { it.number }
    return if (chosen.isNotEmpty()) chosen.toSet() else fields.take(6).map { it.number }.toSet()
}

// Plain or simplified field label for the guide. mshGuide and segmentGuide call it.
private fun fieldLabel(dictionary: Hl7Dictionary, segment: String, number: Int, german: Boolean): String {
    plainField(segment, number)?.let { return if (german) it.de else it.en }
    val official = dictionary.segments[segment]?.fields?.firstOrNull { it.number == number }?.name.orEmpty()
    return phrase(simplifyField(official, german))
}

// English and German plain field labels. plainFields stores these; plainField looks them up.
private data class Plain(val en: String, val de: String)

// Looks up a hard-coded plain field label. fieldLabel prefers it over the dictionary name.
private fun plainField(segment: String, number: Int): Plain? = plainFields["$segment-$number"]

private val plainFields = mapOf(
    "MSH-3" to Plain("Sending system", "Sendendes System"),
    "MSH-4" to Plain("Clinic", "Klinik"),
    "MSH-5" to Plain("Receiving system", "Empfangendes System"),
    "MSH-6" to Plain("Receiving clinic", "Empfangende Klinik"),
    "MSH-7" to Plain("Message time", "Zeitpunkt"),
    "MSH-10" to Plain("Message ID", "Nachrichten-ID"),
    "MSH-11" to Plain("Production or test", "Betrieb oder Test"),
    "MSH-12" to Plain("HL7 version", "HL7-Version"),
    "EVN-1" to Plain("Event", "Ereignis"),
    "EVN-2" to Plain("Recorded at", "Erfasst am"),
    "EVN-5" to Plain("Entered by", "Erfasst von"),
    "PID-3" to Plain("Patient ID", "Patienten-ID"),
    "PID-5" to Plain("Patient name", "Patientenname"),
    "PID-7" to Plain("Date of birth", "Geburtsdatum"),
    "PID-8" to Plain("Sex", "Geschlecht"),
    "PID-11" to Plain("Address", "Adresse"),
    "PID-13" to Plain("Phone", "Telefon"),
    "PID-18" to Plain("Case ID", "Fallnummer"),
    "PV1-2" to Plain("Inpatient or outpatient", "Stationär oder ambulant"),
    "PV1-3" to Plain("Ward, room and bed", "Station, Zimmer und Bett"),
    "PV1-7" to Plain("Attending doctor", "Behandelnder Arzt"),
    "PV1-19" to Plain("Case ID", "Fallnummer"),
    "PV1-44" to Plain("Admitted at", "Aufgenommen am"),
    "PV1-45" to Plain("Discharged at", "Entlassen am"),
    "NK1-2" to Plain("Contact name", "Kontaktperson"),
    "NK1-3" to Plain("Relationship", "Beziehung"),
    "ORC-1" to Plain("Order action", "Auftragsaktion"),
    "ORC-2" to Plain("Order number", "Auftragsnummer"),
    "OBR-4" to Plain("Test", "Untersuchung"),
    "OBX-3" to Plain("Observation", "Messgröße"),
    "OBX-5" to Plain("Result", "Ergebnis"),
    "OBX-6" to Plain("Unit", "Einheit"),
    "OBX-7" to Plain("Reference range", "Referenzbereich"),
    "OBX-8" to Plain("High or low", "Zu hoch oder zu niedrig"),
    "OBX-11" to Plain("Result status", "Befundstatus"),
    "AL1-3" to Plain("Allergy", "Allergie"),
    "DG1-3" to Plain("Diagnosis", "Diagnose"),
    "IN1-4" to Plain("Insurance", "Versicherung"),
    "TXA-2" to Plain("Document type", "Dokumentart"),
    "TXA-12" to Plain("Document ID", "Dokument-ID"),
    "SCH-1" to Plain("Appointment ID", "Termin-ID"),
    "MSA-1" to Plain("Acknowledgement", "Bestätigung"),
    "MSA-2" to Plain("Original message ID", "Ursprüngliche Nachrichten-ID"),
)

// Shortens official field names for the guide. fieldLabel and rewriteName call it.
private fun simplifyField(name: String, german: Boolean): String = when {
    name.contains("Patient Identifier", ignoreCase = true) -> if (german) "Patienten-ID" else "Patient ID"
    name.contains("Account Number", ignoreCase = true) -> if (german) "Fallnummer" else "Case ID"
    name.contains("Visit Number", ignoreCase = true) -> if (german) "Fallnummer" else "Case ID"
    name.equals("Sending Facility", ignoreCase = true) -> if (german) "Klinik" else "Clinic"
    name.equals("Receiving Facility", ignoreCase = true) -> if (german) "Empfangende Klinik" else "Receiving clinic"
    name.equals("Sending Application", ignoreCase = true) -> if (german) "Sendendes System" else "Sending system"
    name.equals("Receiving Application", ignoreCase = true) -> if (german) "Empfangendes System" else "Receiving system"
    else -> name
}

// Strips delimiter characters from a label. fieldLabel cleans official names with it.
private fun phrase(value: String): String = value.replace(Regex("[|^~\\\\&]"), " ").replace(Regex("\\s+"), " ").trim()

// Builds segment views with pieces. Catalog.segments and WikiScreen call it.
internal fun wikiSegments(dictionary: Hl7Dictionary): List<WikiSegmentView> {
    val preferred = listOf("MSH", "EVN", "PID", "PD1", "NK1", "PV1", "PV2", "IN1", "IN2", "GT1", "AL1", "DG1", "PR1", "ORC", "OBR", "OBX", "NTE", "TXA", "SCH", "MSA", "ERR")
    return dictionary.segments.values
        .sortedWith(compareBy<SegmentDef> { preferred.indexOf(it.name).let { index -> if (index < 0) 1000 else index } }.thenBy { it.name })
        .map { segment ->
            val about = segmentAbout[segment.name]
            WikiSegmentView(
                name = segment.name,
                descriptionEn = about?.en ?: segment.description.ifBlank { segment.name },
                descriptionDe = about?.de ?: segment.description.ifBlank { segment.name },
                pieces = segmentPieces(dictionary, segment),
            )
        }
}

// Keeps segments matching a query. Catalog.matchingSegments and WikiScreen call it.
internal fun filterSegments(segments: List<WikiSegmentView>, query: String): List<WikiSegmentView> {
    val needle = query.trim()
    if (needle.isEmpty()) return segments
    return segments.filter { segment ->
        segment.name.contains(needle, true) ||
            segment.descriptionEn.contains(needle, true) ||
            segment.descriptionDe.contains(needle, true) ||
            segment.pieces.any { piece ->
                piece.code.contains(needle, true) ||
                    piece.purposeEn.contains(needle, true) ||
                    piece.purposeDe.contains(needle, true) ||
                    piece.example.contains(needle, true)
            }
    }
}

// Expands fields and components into pieces. wikiSegments maps each segment through it.
private fun segmentPieces(dictionary: Hl7Dictionary, segment: SegmentDef): List<WikiPiece> {
    val pieces = mutableListOf<WikiPiece>()
    segment.fields.sortedBy { it.number }.forEach { field ->
        val components = dictionary.datatypes[field.datatype]?.components.orEmpty()
        if (components.isEmpty()) {
            pieces += piece(segment.name, field.number, 0, field.name, field.datatype, field.table, dictionary)
        } else {
            components.forEach { component ->
                pieces += piece(segment.name, field.number, component.number, component.name, component.datatype, component.table ?: field.table, dictionary)
            }
        }
    }
    return pieces
}

// Builds one WikiPiece with purpose and example. segmentPieces calls it per field or component.
private fun piece(
    segment: String,
    field: Int,
    component: Int,
    official: String,
    datatype: String,
    table: String?,
    dictionary: Hl7Dictionary,
): WikiPiece {
    val code = if (component == 0) "$segment-$field" else "$segment-$field.$component"
    val plain = if (component <= 1) plainField(segment, field) else null
    val purposeEn = plain?.en ?: rewriteName(official, false)
    val purposeDe = plain?.de ?: rewriteName(official, true)
    return WikiPiece(code, purposeEn, purposeDe, exampleValue(code, segment, field, datatype, table, dictionary))
}

// Rewrites component names into plain purpose. piece() uses it when no plainField exists.
private fun rewriteName(official: String, german: Boolean): String = when {
    official.equals("Namespace ID", true) -> if (german) "Kürzel" else "Short name"
    official.equals("Universal ID", true) -> if (german) "Amtliche Kennung" else "Official identifier"
    official.equals("Universal ID Type", true) -> if (german) "Art der Kennung" else "Kind of identifier"
    official.contains("Assigning Authority", true) -> if (german) "Aussteller" else "Issued by"
    official.contains("Identifier Type", true) -> if (german) "Art der ID" else "ID kind"
    official.contains("Family Name", true) || official.contains("Surname", true) -> if (german) "Familienname" else "Family name"
    official.contains("Given Name", true) -> if (german) "Vorname" else "Given name"
    official.contains("Check Digit", true) -> if (german) "Prüfziffer" else "Check digit"
    else -> simplifyField(phrase(official), german)
}

// Picks an example value for a piece. piece() stores it for the Segments path.
private fun exampleValue(code: String, segment: String, field: Int, datatype: String, table: String?, dictionary: Hl7Dictionary): String {
    pieceExamples[code]?.let { return it }
    if (code.count { it == '.' } == 1) pieceExamples["$segment-$field"]?.substringBefore('^')?.let { return it }
    dictionary.tables[table]?.entries?.firstOrNull { it.code.isNotBlank() }?.code?.let { return it }
    return when (datatype) {
        "NM" -> "1"
        "DT" -> "19880614"
        "TM" -> "103000"
        "TS", "DTM", "DIN" -> "20260301103000"
        else -> "Example"
    }
}

private val pieceExamples = mapOf(
    "MSH-1" to "|",
    "MSH-2" to "^~\\&",
    "MSH-3" to "WARD",
    "MSH-3.1" to "WARD",
    "MSH-4" to "NORTH",
    "MSH-4.1" to "NORTH",
    "MSH-4.2" to "NORTHCLINIC",
    "MSH-5" to "LAB",
    "MSH-5.1" to "LAB",
    "MSH-6" to "SOUTH",
    "MSH-6.1" to "SOUTH",
    "MSH-7" to "20260301103000",
    "MSH-7.1" to "20260301103000",
    "MSH-10" to "MSG1001",
    "MSH-11" to "P",
    "MSH-11.1" to "P",
    "MSH-12" to "2.5",
    "MSH-12.1" to "2.5",
    "EVN-1" to "A01",
    "EVN-2" to "20260301103000",
    "EVN-5" to "J.WEBER",
    "PID-3" to "4711023",
    "PID-3.1" to "4711023",
    "PID-5" to "Adler^Lina",
    "PID-5.1" to "Adler",
    "PID-5.2" to "Lina",
    "PID-7" to "19880614",
    "PID-8" to "F",
    "PID-11" to "Birkenweg 4^^Hamburg",
    "PID-13" to "040555100",
    "PID-18" to "CASE-1001",
    "PV1-2" to "I",
    "PV1-3" to "WARD^12^A",
    "PV1-3.1" to "WARD",
    "PV1-7" to "0815^Berg^Kai",
    "PV1-19" to "CASE-1001",
    "PV1-44" to "20260301082500",
    "PV1-45" to "20260306150000",
    "IN1-4" to "Nord Insurance",
    "IN1-36" to "DE7719003412",
    "NK1-2" to "Adler^Jonas",
    "NK1-3" to "SPO",
    "AL1-3" to "Penicillin",
    "DG1-3" to "J20.9",
    "ORC-1" to "NW",
    "ORC-2" to "ORD1001",
    "OBR-4" to "CBC",
    "OBX-3" to "WBC",
    "OBX-5" to "7.2",
    "OBX-6" to "10*9/L",
    "OBX-7" to "4.0-10.0",
    "OBX-8" to "N",
    "OBX-11" to "F",
    "TXA-2" to "DS",
    "TXA-12" to "DOC1001",
    "SCH-1" to "APT1001",
    "MSA-1" to "AA",
    "MSA-2" to "MSG1001",
)

private val segmentAbout = mapOf(
    "MSH" to Plain("Message header. Who sent the message, when, and which event it is.", "Nachrichtenkopf. Wer die Nachricht geschickt hat, wann, und welches Ereignis es ist."),
    "EVN" to Plain("Event. Why this message was created.", "Ereignis. Warum diese Nachricht erzeugt wurde."),
    "PID" to Plain("Patient. The person this message is about.", "Patient. Die Person, um die es in der Nachricht geht."),
    "PD1" to Plain("Extra patient details such as the usual practice.", "Weitere Patientenangaben, etwa die Stammpraxis."),
    "NK1" to Plain("Next of kin or another contact person.", "Angehöriger oder eine andere Kontaktperson."),
    "PV1" to Plain("Visit. The stay: ward, doctors and case.", "Aufenthalt. Station, Ärzte und Fall."),
    "PV2" to Plain("More about the visit, such as the reason for admission.", "Weiteres zum Aufenthalt, etwa der Aufnahmegrund."),
    "IN1" to Plain("Insurance. Who pays for this case.", "Versicherung. Wer diesen Fall bezahlt."),
    "IN2" to Plain("Further insurance details.", "Weitere Versicherungsangaben."),
    "GT1" to Plain("Guarantor. The person or organisation responsible for the bill.", "Zahlungspflichtiger. Person oder Stelle, die die Rechnung trägt."),
    "AL1" to Plain("Allergy.", "Allergie."),
    "DG1" to Plain("Diagnosis.", "Diagnose."),
    "PR1" to Plain("Procedure that was performed.", "Durchgeführte Prozedur."),
    "ORC" to Plain("Order control. New, changed or cancelled order.", "Auftragssteuerung. Neuer, geänderter oder stornierter Auftrag."),
    "OBR" to Plain("The requested or reported test.", "Die angeforderte oder berichtete Untersuchung."),
    "OBX" to Plain("One observation result.", "Ein einzelner Befundwert."),
    "NTE" to Plain("A free-text note.", "Ein Freitext-Hinweis."),
    "TXA" to Plain("Document header, such as a discharge summary.", "Dokumentkopf, etwa ein Entlassungsbericht."),
    "SCH" to Plain("Appointment.", "Termin."),
    "MSA" to Plain("Acknowledgement of another message.", "Bestätigung einer anderen Nachricht."),
    "ERR" to Plain("Error details when a message was rejected.", "Fehlerdetails, wenn eine Nachricht abgelehnt wurde."),
)

// Lists segment names from a structure. wikiExample uses it when faking a message.
private fun segmentNames(dictionary: Hl7Dictionary, structure: String): List<String> {
    val names = mutableListOf<String>()
    // Collects three-letter segment names. segmentNames walks the structure with it.
    fun walk(element: StructureElement) {
        if (element.group) element.children.forEach(::walk)
        else if (element.name.length == 3) names += element.name
    }
    dictionary.structures[structure]?.elements?.forEach(::walk)
    if (names.none { it == "MSH" }) names.add(0, "MSH")
    return names.distinct().take(16)
}

// Fakes one sample segment line. wikiExample joins these when no real sample matches.
private fun fakeLine(segment: String, type: String, event: String, structure: String, version: String): String = when (segment) {
    "MSH" -> "MSH|^~\\&|WARD|NORTH|LAB|SOUTH|20260301103000||$type^$event^$structure|MSG1001|P|$version"
    "EVN" -> "EVN|$event|20260301103000"
    "PID" -> "PID|1||10004567^^^WARD^MR||Adler^Lina^M||19880614|F|||Birkenweg 4^^Hamburg^^22041^DE"
    "PV1" -> "PV1|1|I|WARD^12^A||||0815^Berg^Kai"
    "PV2" -> "PV2||||||||20260301110000"
    "NK1" -> "NK1|1|Adler^Jonas|BRO|Hauptstr 2^^Hamburg^^22041|040555100"
    "ORC" -> "ORC|NW|ORD1001||||||20260301103000"
    "OBR" -> "OBR|1|ORD1001||CBC^Blood count|||20260301103000"
    "OBX" -> "OBX|1|NM|WBC^Leukocytes||7.2|10*9/L|4.0-10.0|N|||F"
    "AL1" -> "AL1|1|DA|70618^Penicillin||Rash"
    "DG1" -> "DG1|1||J20.9^Acute bronchitis"
    "IN1" -> "IN1|1|PLAN^Standard|10421|Nord Insurance||||||||20250101|20261231"
    "TXA" -> "TXA|1|DS|TX|20260301103000|||||DOC1001||||AU"
    "MSA" -> "MSA|AA|MSG1001"
    "SCH" -> "SCH|APT1001||||||ROUTINE|120|MIN"
    else -> "$segment|1"
}
