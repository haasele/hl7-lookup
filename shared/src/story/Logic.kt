// Turns segments into sentences. story/Index calls it.
package hl7lookup.story

import hl7lookup.datetime.DateStyle
import hl7lookup.datetime.Hl7Dates
import hl7lookup.dictionary.Dictionaries
import hl7lookup.dictionary.Hl7Dictionary
import hl7lookup.document.Er7
import hl7lookup.document.FieldPath
import hl7lookup.document.ParsedMessage
import hl7lookup.document.SegmentNode
import hl7lookup.i18n.I18n
import hl7lookup.i18n.Language
import hl7lookup.i18n.TemplatePart
import hl7lookup.i18n.Text

// How a slot value is formatted. Slot and the sentence templates pick a format.
enum class SlotFormat { PLAIN, CODE, DATE, DAY, AGE, NAME, PERSON, ADDRESS, PHONE, LOCATION, TEXT, IDENTIFIER, QUANTITY, SEGMENT_TYPE }

// Field path piece inside a clause template. clause() and resolveSlot use it.
data class Slot(val field: Int, val component: Int = 0, val format: SlotFormat = SlotFormat.PLAIN, val allRepetitions: Boolean = true)

// One wording template with its slots. sentence() groups variants; renderClause fills it.
data class Clause(val text: Text, val slots: List<Slot>)

// Ordered clause variants for one sentence. renderSentence tries them until one fits.
data class Sentence(val variants: List<Clause>)

// Dictionary, date style and language for narration. Stories.build passes it into buildStory.
data class StoryContext(val dictionary: Hl7Dictionary?, val style: DateStyle, val nowMillis: Long, val language: Language, val relative: Boolean = true)

// Plain words or a clickable field value. renderClause and StoryView walk these pieces.
sealed interface StoryPiece {
    // Literal wording between values. renderClause emits these from template parts.
    data class Words(val text: String) : StoryPiece
    // Clickable field value with optional raw detail. resolveSlot builds these for the story.
    data class Value(val text: String, val path: FieldPath, val detail: String? = null) : StoryPiece
}

// One segment's narrated lines. buildStory collects them under the title.
data class StoryParagraph(val segmentIndex: Int, val segmentName: String, val pieces: List<StoryPiece>)

// Title pieces plus paragraphs for a message. Stories.build returns it to Workspace.
data class Story(val title: List<StoryPiece>, val paragraphs: List<StoryParagraph>)

// Room, bed and facility wording. formatLocation and buildStory use it.
internal class LocationWords(val room: Text, val bed: Text, val facility: Text)

// Joiners and generic-field leads. genericParagraph and buildStory use it.
internal class StoryWords(val and: Text, val listSeparator: Text, val fieldIs: Text, val genericLead: Text, val sentenceEnd: Text, val repetitionJoin: Text)

// Reads one component from a segment field. Format helpers call it for names and addresses.
private fun component(message: ParsedMessage, segment: Int, field: Int, repetition: Int, component: Int): String =
    Er7.value(message, FieldPath(segment, field, repetition, component, 0))

// Reads a whole field repetition. resolveSlot and genericParagraph call it.
private fun fieldValue(message: ParsedMessage, segment: Int, field: Int, repetition: Int): String =
    Er7.value(message, FieldPath(segment, field, repetition, 0, 0))

// Joins non-blank trimmed parts. Name, address and phone formatters call it.
private fun joinPresent(parts: List<String>, separator: String): String = parts.map { it.trim() }.filter { it.isNotEmpty() }.joinToString(separator)

// Formats XPN/XCN style names. resolveSlot uses it for NAME and PERSON slots.
internal fun formatName(message: ParsedMessage, segment: Int, field: Int, rep: Int, person: Boolean): String {
    val c = { n: Int -> component(message, segment, field, rep, n) }
    return if (person) joinPresent(listOf(c(6), c(3), c(4), c(2), c(5)), " ").ifEmpty { c(1) }
    else joinPresent(listOf(c(5), c(2), c(3), c(1), c(4)), " ")
}

// Formats XAD address components. resolveSlot uses it for ADDRESS slots.
internal fun formatAddress(message: ParsedMessage, segment: Int, field: Int, rep: Int): String {
    val c = { n: Int -> component(message, segment, field, rep, n) }
    val street = joinPresent(listOf(c(1), c(2)), ", ")
    val city = joinPresent(listOf(c(5), c(3)), " ")
    return joinPresent(listOf(street, city, c(4), c(6)), ", ")
}

// Formats XTN phone components. resolveSlot uses it for PHONE slots.
internal fun formatPhone(message: ParsedMessage, segment: Int, field: Int, rep: Int): String {
    val c = { n: Int -> component(message, segment, field, rep, n) }
    val composed = joinPresent(listOf(c(5).let { if (it.isNotBlank()) "+$it" else "" }, c(6).let { if (it.isNotBlank()) "($it)" else "" }, c(7), c(8).let { if (it.isNotBlank()) "x$it" else "" }), " ")
    return listOf(c(1), c(12), composed, c(4)).firstOrNull { it.isNotBlank() } ?: ""
}

// Formats PL location with room and bed labels. resolveSlot uses it for LOCATION slots.
internal fun formatLocation(message: ParsedMessage, segment: Int, field: Int, rep: Int, words: LocationWords, language: Language): String {
    val c = { n: Int -> component(message, segment, field, rep, n) }
    return joinPresent(
        listOf(
            c(1),
            c(2).let { if (it.isNotBlank()) I18n.format(words.room, language, it) else "" },
            c(3).let { if (it.isNotBlank()) I18n.format(words.bed, language, it) else "" },
            c(4).let { if (it.isNotBlank()) I18n.format(words.facility, language, it) else "" },
        ),
        ", ",
    )
}

// Finds the table id for a coded field. formatCode looks it up before describing a code.
internal fun codeTable(context: StoryContext, segmentName: String, field: Int, componentNumber: Int): String? {
    val node = Dictionaries.node(context.dictionary, segmentName, field, componentNumber) ?: return null
    if (node.table != null) return node.table
    if (componentNumber == 0) return Dictionaries.node(context.dictionary, segmentName, field, 1)?.table
    return null
}

// Resolves a code to description plus raw. resolveSlot uses it for CODE slots.
internal fun formatCode(message: ParsedMessage, context: StoryContext, segment: SegmentNode, field: Int, componentNumber: Int, rep: Int): Pair<String, String?> {
    val raw = if (componentNumber == 0) component(message, segment.index, field, rep, 1) else component(message, segment.index, field, rep, componentNumber)
    if (raw.isBlank()) return "" to null
    val table = codeTable(context, segment.name, field, componentNumber)
    val described = Dictionaries.code(context.dictionary, table, raw)
    if (described != null) return described to raw
    if (componentNumber == 0) {
        val text = component(message, segment.index, field, rep, 2)
        if (text.isNotBlank()) return text to raw
    }
    return raw to null
}

// Formats a date with optional relative text. resolveSlot uses it for DATE slots.
internal fun formatDate(raw: String, context: StoryContext): Pair<String, String?> {
    val time = Hl7Dates.parse(raw) ?: return raw to null
    val local = Hl7Dates.format(time, context.style)
    if (!context.relative) return local to raw
    val relative = Hl7Dates.describe(Hl7Dates.relative(time, context.nowMillis), context.language)
    return "$local ($relative)" to raw
}

// Years of age from a birth date. resolveSlot uses it for AGE slots.
internal fun formatAge(raw: String, context: StoryContext): String {
    val time = Hl7Dates.parse(raw) ?: return ""
    val age = Hl7Dates.age(time, context.nowMillis)
    return if (age < 0) "" else age.toString()
}

// Fills one slot into a StoryPiece.Value. renderClause maps every clause slot through it.
internal fun resolveSlot(message: ParsedMessage, context: StoryContext, segment: SegmentNode, slot: Slot, words: LocationWords, repetitionJoin: String): StoryPiece.Value? {
    val field = segment.field(slot.field) ?: return null
    val path = FieldPath(segment.index, slot.field, 1, slot.component, 0)
    val reps = if (slot.allRepetitions) field.repetitions.indices.map { it + 1 } else listOf(1)
    // Renders each repetition and joins them. resolveSlot uses it for multi-rep formats.
    fun perRep(render: (Int) -> String): String = joinPresent(reps.map(render), repetitionJoin)
    val (text, detail) = when (slot.format) {
        SlotFormat.PLAIN -> perRep { rep -> if (slot.component == 0) fieldValue(message, segment.index, slot.field, rep) else component(message, segment.index, slot.field, rep, slot.component) } to null
        SlotFormat.TEXT -> perRep { rep -> fieldValue(message, segment.index, slot.field, rep) } to null
        SlotFormat.IDENTIFIER -> perRep { rep -> component(message, segment.index, slot.field, rep, 1) } to null
        SlotFormat.QUANTITY -> perRep { rep -> joinPresent(listOf(component(message, segment.index, slot.field, rep, 1), component(message, segment.index, slot.field, rep, 2)), " ") } to null
        SlotFormat.CODE -> {
            val results = reps.map { formatCode(message, context, segment, slot.field, slot.component, it) }
            joinPresent(results.map { it.first }, repetitionJoin) to results.mapNotNull { it.second }.joinToString(", ").ifEmpty { null }
        }
        SlotFormat.DATE -> {
            val raw = (if (slot.component == 0) component(message, segment.index, slot.field, 1, 1) else component(message, segment.index, slot.field, 1, slot.component))
                .substringBefore(message.delimiters.subcomponent)
            if (raw.isBlank()) "" to null else formatDate(raw, context)
        }
        SlotFormat.DAY -> {
            val raw = component(message, segment.index, slot.field, 1, 1).substringBefore(message.delimiters.subcomponent)
            val time = Hl7Dates.parse(raw)
            (if (time == null) raw else Hl7Dates.format(time, context.style)) to raw.takeIf { time != null }
        }
        SlotFormat.AGE -> formatAge(component(message, segment.index, slot.field, 1, 1), context) to null
        SlotFormat.NAME -> perRep { formatName(message, segment.index, slot.field, it, person = false) } to null
        SlotFormat.PERSON -> perRep { formatName(message, segment.index, slot.field, it, person = true) } to null
        SlotFormat.ADDRESS -> perRep { formatAddress(message, segment.index, slot.field, it) } to null
        SlotFormat.PHONE -> perRep { formatPhone(message, segment.index, slot.field, it) } to null
        SlotFormat.LOCATION -> perRep { formatLocation(message, segment.index, slot.field, it, words, context.language) } to null
        SlotFormat.SEGMENT_TYPE -> (Dictionaries.segment(context.dictionary, segment.name)?.description ?: segment.name) to null
    }
    return if (text.isBlank()) null else StoryPiece.Value(shorten(text.trim()), path, detail)
}

private const val longestValue = 280

// Truncates long field values for the story. resolveSlot and genericParagraph call it.
internal fun shorten(text: String): String = if (text.length <= longestValue) text else text.take(longestValue - 1) + "…"

// Fills a clause template with slot values. renderSentence tries each variant through it.
internal fun renderClause(message: ParsedMessage, context: StoryContext, segment: SegmentNode, clause: Clause, words: LocationWords, repetitionJoin: String): List<StoryPiece>? {
    val values = clause.slots.map { resolveSlot(message, context, segment, it, words, repetitionJoin) ?: return null }
    val pieces = mutableListOf<StoryPiece>()
    for (part in I18n.parts(I18n.resolve(clause.text, context.language))) {
        when (part) {
            is TemplatePart.Literal -> {
                val previous = pieces.lastOrNull()
                val text = if (previous is StoryPiece.Value && previous.text.endsWith('.') && part.text.startsWith('.')) part.text.drop(1) else part.text
                pieces += StoryPiece.Words(text)
            }
            is TemplatePart.Slot -> pieces += values.getOrNull(part.index) ?: StoryPiece.Words("")
        }
    }
    return pieces
}

// Picks the first clause variant that has all slots. buildStory narrates each sentence with it.
internal fun renderSentence(message: ParsedMessage, context: StoryContext, segment: SegmentNode, sentence: Sentence, words: LocationWords, repetitionJoin: String): List<StoryPiece>? =
    sentence.variants.firstNotNullOfOrNull { renderClause(message, context, segment, it, words, repetitionJoin) }

// Fallback field list when no template fits. buildStory uses it for unknown segments.
internal fun genericParagraph(message: ParsedMessage, context: StoryContext, segment: SegmentNode, words: StoryWords): List<StoryPiece> {
    val description = Dictionaries.segment(context.dictionary, segment.name)?.description ?: segment.name
    val pieces = mutableListOf<StoryPiece>()
    pieces += StoryPiece.Words(I18n.format(words.genericLead, context.language, description))
    val separator = I18n.resolve(words.listSeparator, context.language)
    var first = true
    for (field in segment.fields) {
        if (segment.name == "MSH" && field.number <= 2) continue
        val value = field.repetitions.indices.map { fieldValue(message, segment.index, field.number, it + 1) }
            .map { it.trim() }.filter { it.isNotEmpty() }
        if (value.isEmpty()) continue
        val definition = Dictionaries.field(context.dictionary, segment.name, field.number)
        val name = definition?.name ?: "${segment.name}-${field.number}"
        val code = definition?.table?.let { table ->
            val raw = component(message, segment.index, field.number, 1, 1)
            Dictionaries.code(context.dictionary, table, raw)
        }
        val shown = shorten(code ?: value.joinToString(I18n.resolve(words.repetitionJoin, context.language)) { it.replace("\n", " ") })
        if (!first) pieces += StoryPiece.Words(separator)
        pieces += StoryPiece.Words(I18n.format(words.fieldIs, context.language, name).substringBefore("{1}"))
        pieces += StoryPiece.Value(shown, FieldPath(segment.index, field.number), if (code != null) value.first() else null)
        pieces += StoryPiece.Words(I18n.format(words.fieldIs, context.language, name).substringAfter("{1}", ""))
        first = false
    }
    if (first) return emptyList()
    pieces += StoryPiece.Words(I18n.resolve(words.sentenceEnd, context.language))
    return pieces
}

// Builds title and paragraphs from templates. Stories.build delegates to it.
internal fun buildStory(
    message: ParsedMessage,
    context: StoryContext,
    templates: Map<String, List<Sentence>>,
    title: List<Sentence>,
    words: StoryWords,
    locationWords: LocationWords,
): Story {
    val join = I18n.resolve(words.repetitionJoin, context.language)
    val header = message.segments.firstOrNull()
    val titlePieces = if (header != null && header.name == "MSH") {
        title.mapNotNull { renderSentence(message, context, header, it, locationWords, join) }.flatMap { it + StoryPiece.Words(" ") }
    } else emptyList()
    val paragraphs = message.segments.mapNotNull { segment ->
        val sentences = templates[segment.name]
        val pieces = if (sentences != null) {
            val rendered = sentences.mapNotNull { renderSentence(message, context, segment, it, locationWords, join) }
            when {
                rendered.isNotEmpty() -> rendered.flatMap { it + StoryPiece.Words(" ") }
                segment.index == header?.index -> emptyList()
                else -> genericParagraph(message, context, segment, words)
            }
        } else genericParagraph(message, context, segment, words)
        if (pieces.isEmpty()) null else StoryParagraph(segment.index, segment.name, trimPieces(pieces))
    }
    return Story(trimPieces(titlePieces), paragraphs)
}

// Drops trailing blank word pieces. buildStory cleans title and paragraphs with it.
internal fun trimPieces(pieces: List<StoryPiece>): List<StoryPiece> {
    val result = pieces.toMutableList()
    while (result.lastOrNull().let { it is StoryPiece.Words && it.text.isBlank() }) result.removeAt(result.lastIndex)
    return result
}

// Joins story pieces into copyable text. Stories.plain delegates to it.
internal fun plainText(story: Story): String {
    // Flattens one piece list to a string. plainText maps title and paragraphs through it.
    fun text(pieces: List<StoryPiece>) = pieces.joinToString("") { if (it is StoryPiece.Words) it.text else (it as StoryPiece.Value).text }
    return (listOf(text(story.title)) + story.paragraphs.map { text(it.pieces) }).filter { it.isNotBlank() }.joinToString("\n\n")
}

// Builds a Slot for sentence templates. story/Index templates call it.
internal fun slot(field: Int, component: Int = 0, format: SlotFormat = SlotFormat.PLAIN, all: Boolean = true) = Slot(field, component, format, all)

// Builds a Clause from wording and slots. story/Index templates call it.
internal fun clause(text: Text, vararg slots: Slot) = Clause(text, slots.toList())

// Builds a Sentence from clause variants. story/Index templates call it.
internal fun sentence(vararg variants: Clause) = Sentence(variants.toList())
