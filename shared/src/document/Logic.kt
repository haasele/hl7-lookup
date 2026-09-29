package hl7lookup.document

import kotlinx.serialization.Serializable

@Serializable
data class Delimiters(
    val field: Char = '|',
    val component: Char = '^',
    val repetition: Char = '~',
    val escape: Char = '\\',
    val subcomponent: Char = '&',
) {
    val encodingCharacters: String get() = "$component$repetition$escape$subcomponent"
}

@Serializable
data class FieldPath(
    val segment: Int,
    val field: Int,
    val repetition: Int = 1,
    val component: Int = 0,
    val subcomponent: Int = 0,
)

data class Span(val start: Int, val end: Int) {
    val length: Int get() = end - start
    fun touches(offset: Int): Boolean = offset in start..end
}

class SubcomponentNode(val index: Int, val span: Span, val raw: String)

class ComponentNode(val index: Int, val span: Span, val raw: String, val subcomponents: List<SubcomponentNode>)

class RepetitionNode(val index: Int, val span: Span, val raw: String, val components: List<ComponentNode>)

class FieldNode(val number: Int, val span: Span, val raw: String, val repetitions: List<RepetitionNode>)

class SegmentNode(val index: Int, val name: String, val span: Span, val nameSpan: Span, val fields: List<FieldNode>) {
    val lastFieldNumber: Int get() = fields.lastOrNull()?.number ?: 0
    fun field(number: Int): FieldNode? = fields.firstOrNull { it.number == number }
}

class ParsedMessage(val text: String, val delimiters: Delimiters, val segments: List<SegmentNode>) {
    val isEmpty: Boolean get() = segments.isEmpty()
}

data class PathSpec(val segment: String, val field: Int, val repetition: Int? = null, val component: Int = 0, val subcomponent: Int = 0)

data class MessageHeader(
    val type: String,
    val event: String,
    val structure: String,
    val controlId: String,
    val timestamp: String,
    val version: String,
    val sendingApplication: String,
    val sendingFacility: String,
    val receivingApplication: String,
    val receivingFacility: String,
    val processingId: String,
)

data class FlatNode(val path: FieldPath, val segmentName: String, val occurrence: Int, val raw: String)

private val headerSegments = setOf("MSH", "FHS", "BHS")
private val batchSegments = setOf("FHS", "BHS", "BTS", "FTS")

internal fun detectDelimiters(text: String): Delimiters {
    val start = text.indexOfFirst { !it.isWhitespace() && it != '\u000b' }.coerceAtLeast(0)
    val line = text.substring(start)
    if (line.length < 4 || line.substring(0, 3) !in headerSegments) return Delimiters()
    val field = line[3]
    val encEnd = line.indexOf(field, 4).let { if (it < 0) line.indexOfAny(charArrayOf('\r', '\n'), 4) else it }
        .let { if (it < 0) line.length else it }
    val enc = line.substring(4, encEnd)
    val defaults = Delimiters()
    return Delimiters(
        field = field,
        component = enc.getOrNull(0) ?: defaults.component,
        repetition = enc.getOrNull(1) ?: defaults.repetition,
        escape = enc.getOrNull(2) ?: defaults.escape,
        subcomponent = enc.getOrNull(3) ?: defaults.subcomponent,
    )
}

internal fun splitSpans(text: String, start: Int, end: Int, delimiter: Char): List<Span> {
    val spans = mutableListOf<Span>()
    var cursor = start
    for (i in start until end) {
        if (text[i] == delimiter) {
            spans += Span(cursor, i)
            cursor = i + 1
        }
    }
    spans += Span(cursor, end)
    return spans
}

private fun leafField(text: String, number: Int, span: Span): FieldNode {
    val raw = text.substring(span.start, span.end)
    val sub = SubcomponentNode(1, span, raw)
    val comp = ComponentNode(1, span, raw, listOf(sub))
    return FieldNode(number, span, raw, listOf(RepetitionNode(1, span, raw, listOf(comp))))
}

private fun parseField(text: String, number: Int, span: Span, d: Delimiters): FieldNode {
    val reps = splitSpans(text, span.start, span.end, d.repetition).mapIndexed { r, repSpan ->
        val comps = splitSpans(text, repSpan.start, repSpan.end, d.component).mapIndexed { c, compSpan ->
            val subs = splitSpans(text, compSpan.start, compSpan.end, d.subcomponent).mapIndexed { s, subSpan ->
                SubcomponentNode(s + 1, subSpan, text.substring(subSpan.start, subSpan.end))
            }
            ComponentNode(c + 1, compSpan, text.substring(compSpan.start, compSpan.end), subs)
        }
        RepetitionNode(r + 1, repSpan, text.substring(repSpan.start, repSpan.end), comps)
    }
    return FieldNode(number, span, text.substring(span.start, span.end), reps)
}

private fun parseSegment(text: String, start: Int, end: Int, d: Delimiters, index: Int): SegmentNode {
    val line = text.substring(start, end)
    val nameEnd = line.indexOf(d.field).let { if (it < 0) line.length else it }
    val name = line.substring(0, nameEnd)
    val fields = mutableListOf<FieldNode>()
    var pos: Int
    var number: Int
    if (name in headerSegments && line.length > 3) {
        fields += leafField(text, 1, Span(start + 3, start + 4))
        val encEnd = line.indexOf(d.field, 4).let { if (it < 0) line.length else it }
        fields += leafField(text, 2, Span(start + 4, start + encEnd))
        pos = encEnd
        number = 3
    } else {
        pos = nameEnd
        number = 1
    }
    while (pos < line.length) {
        val next = line.indexOf(d.field, pos + 1).let { if (it < 0) line.length else it }
        fields += parseField(text, number++, Span(start + pos + 1, start + next), d)
        pos = next
    }
    return SegmentNode(index, name, Span(start, end), Span(start, start + nameEnd), fields)
}

internal fun parseMessage(text: String): ParsedMessage {
    val delimiters = detectDelimiters(text)
    val segments = mutableListOf<SegmentNode>()
    var lineStart = 0
    var index = 0
    while (lineStart <= text.length) {
        val found = text.indexOfAny(charArrayOf('\n', '\r'), lineStart)
        val lineEnd = if (found < 0) text.length else found
        if (lineEnd > lineStart && text.substring(lineStart, lineEnd).isNotBlank()) {
            segments += parseSegment(text, lineStart, lineEnd, delimiters, index++)
        }
        if (found < 0) break
        lineStart = lineEnd + 1
    }
    return ParsedMessage(text, delimiters, segments)
}

internal fun locateOffset(message: ParsedMessage, offset: Int): FieldPath? {
    val segment = message.segments.firstOrNull { it.span.touches(offset) } ?: return null
    if (offset <= segment.nameSpan.end) return FieldPath(segment.index, 0)
    val field = segment.fields.firstOrNull { it.span.touches(offset) } ?: return FieldPath(segment.index, 0)
    val rep = field.repetitions.firstOrNull { it.span.touches(offset) } ?: field.repetitions.first()
    if (rep.components.size <= 1 && rep.components.first().subcomponents.size <= 1) {
        return FieldPath(segment.index, field.number, rep.index)
    }
    val comp = rep.components.firstOrNull { it.span.touches(offset) } ?: rep.components.first()
    if (comp.subcomponents.size <= 1) return FieldPath(segment.index, field.number, rep.index, comp.index)
    val sub = comp.subcomponents.firstOrNull { it.span.touches(offset) } ?: comp.subcomponents.first()
    return FieldPath(segment.index, field.number, rep.index, comp.index, sub.index)
}

internal fun spanOfPath(message: ParsedMessage, path: FieldPath): Span? {
    val segment = message.segments.getOrNull(path.segment) ?: return null
    if (path.field == 0) return segment.nameSpan
    val field = segment.field(path.field) ?: return null
    val rep = field.repetitions.getOrNull(path.repetition - 1) ?: return null
    if (path.component == 0) return rep.span
    val comp = rep.components.getOrNull(path.component - 1) ?: return null
    if (path.subcomponent == 0) return comp.span
    return comp.subcomponents.getOrNull(path.subcomponent - 1)?.span
}

internal fun rawAt(message: ParsedMessage, path: FieldPath): String =
    spanOfPath(message, path)?.let { message.text.substring(it.start, it.end) } ?: ""

private fun trimTrailingEmpty(parts: List<String>): List<String> {
    var last = parts.size
    while (last > 1 && parts[last - 1].isEmpty()) last--
    return parts.subList(0, last)
}

internal fun replaceInField(raw: String, d: Delimiters, repetition: Int, component: Int, subcomponent: Int, value: String): String {
    val reps = raw.split(d.repetition).toMutableList()
    while (reps.size < repetition) reps += ""
    if (component == 0) {
        reps[repetition - 1] = value
    } else {
        val comps = reps[repetition - 1].split(d.component).toMutableList()
        while (comps.size < component) comps += ""
        if (subcomponent == 0) {
            comps[component - 1] = value
        } else {
            val subs = comps[component - 1].split(d.subcomponent).toMutableList()
            while (subs.size < subcomponent) subs += ""
            subs[subcomponent - 1] = value
            comps[component - 1] = trimTrailingEmpty(subs).joinToString(d.subcomponent.toString())
        }
        reps[repetition - 1] = trimTrailingEmpty(comps).joinToString(d.component.toString())
    }
    return trimTrailingEmpty(reps).joinToString(d.repetition.toString())
}

internal fun writeRaw(message: ParsedMessage, path: FieldPath, raw: String): String {
    val text = message.text
    val segment = message.segments.getOrNull(path.segment) ?: return text
    if (path.field == 0) return text.replaceRange(segment.nameSpan.start, segment.nameSpan.end, raw)
    if (segment.name in headerSegments && path.field <= 2) {
        val current = message.delimiters
        val next = if (path.field == 1) {
            current.copy(field = raw.firstOrNull() ?: current.field)
        } else {
            val defaults = Delimiters()
            current.copy(
                component = raw.getOrNull(0) ?: defaults.component,
                repetition = raw.getOrNull(1) ?: defaults.repetition,
                escape = raw.getOrNull(2) ?: defaults.escape,
                subcomponent = raw.getOrNull(3) ?: defaults.subcomponent,
            )
        }
        return reencode(message, next)
    }
    val d = message.delimiters
    val field = segment.field(path.field)
    return if (field != null) {
        val updated = replaceInField(field.raw, d, path.repetition, path.component, path.subcomponent, raw)
        text.replaceRange(field.span.start, field.span.end, updated)
    } else {
        val updated = replaceInField("", d, path.repetition, path.component, path.subcomponent, raw)
        val padding = d.field.toString().repeat(path.field - segment.lastFieldNumber)
        text.replaceRange(segment.span.end, segment.span.end, padding + updated)
    }
}

internal fun unescapeValue(raw: String, d: Delimiters): String {
    if (d.escape !in raw) return raw
    val out = StringBuilder()
    var i = 0
    while (i < raw.length) {
        val c = raw[i]
        if (c == d.escape) {
            val close = raw.indexOf(d.escape, i + 1)
            if (close > i) {
                val code = raw.substring(i + 1, close)
                val replacement = when {
                    code == "F" -> d.field.toString()
                    code == "S" -> d.component.toString()
                    code == "T" -> d.subcomponent.toString()
                    code == "R" -> d.repetition.toString()
                    code == "E" -> d.escape.toString()
                    code == ".br" -> "\n"
                    code == "H" || code == "N" -> ""
                    code.startsWith("X") && code.length > 1 && code.length % 2 == 1 ->
                        code.drop(1).chunked(2).mapNotNull { it.toIntOrNull(16)?.toChar() }.joinToString("")
                    else -> null
                }
                if (replacement != null) {
                    out.append(replacement)
                    i = close + 1
                    continue
                }
            }
        }
        out.append(c)
        i++
    }
    return out.toString()
}

internal fun escapeValue(value: String, d: Delimiters): String {
    val out = StringBuilder()
    for (c in value) {
        when (c) {
            d.escape -> out.append(d.escape).append('E').append(d.escape)
            d.field -> out.append(d.escape).append('F').append(d.escape)
            d.component -> out.append(d.escape).append('S').append(d.escape)
            d.subcomponent -> out.append(d.escape).append('T').append(d.escape)
            d.repetition -> out.append(d.escape).append('R').append(d.escape)
            '\n' -> out.append(d.escape).append(".br").append(d.escape)
            '\r' -> {}
            else -> out.append(c)
        }
    }
    return out.toString()
}

internal fun reencode(message: ParsedMessage, next: Delimiters): String {
    val old = message.delimiters
    fun leaf(raw: String) = escapeValue(unescapeValue(raw, old), next)
    fun field(node: FieldNode) = node.repetitions.joinToString(next.repetition.toString()) { rep ->
        rep.components.joinToString(next.component.toString()) { comp ->
            comp.subcomponents.joinToString(next.subcomponent.toString()) { leaf(it.raw) }
        }
    }
    val lines = message.segments.map { segment ->
        if (segment.name in headerSegments) {
            val rest = segment.fields.filter { it.number >= 3 }
            buildString {
                append(segment.name).append(next.field).append(next.encodingCharacters)
                rest.forEach { append(next.field).append(field(it)) }
            }
        } else {
            buildString {
                append(segment.name)
                segment.fields.forEach { append(next.field).append(field(it)) }
            }
        }
    }
    return lines.joinToString("\n")
}

internal fun splitMessageFile(content: String): List<String> {
    val cleaned = content.replace("\u000b", "").replace("\u001c", "").replace("\r\n", "\n").replace('\r', '\n')
    val messages = mutableListOf<String>()
    val current = mutableListOf<String>()
    for (line in cleaned.split('\n')) {
        if (line.isBlank()) continue
        val name = line.take(3)
        if (name in batchSegments) continue
        if (name == "MSH" && current.isNotEmpty()) {
            messages += current.joinToString("\n")
            current.clear()
        }
        current += line
    }
    if (current.isNotEmpty()) messages += current.joinToString("\n")
    return messages
}

internal fun segmentLines(text: String): List<String> =
    text.replace("\r\n", "\n").replace('\r', '\n').split('\n').filter { it.isNotBlank() }

internal fun toWireFormat(text: String): String = segmentLines(text).joinToString("\r", postfix = "\r")

internal fun toFileFormat(messages: List<String>): String =
    messages.joinToString("") { message -> segmentLines(message).joinToString("\r\n", postfix = "\r\n") }

internal fun normalizeEditorText(text: String): String = text.replace("\r\n", "\n").replace('\r', '\n').trimEnd('\n')

internal fun firstSegmentIndex(message: ParsedMessage, name: String, occurrence: Int = 1): Int? =
    message.segments.filter { it.name == name }.getOrNull(occurrence - 1)?.index

internal fun occurrenceOf(message: ParsedMessage, segmentIndex: Int): Int {
    val segment = message.segments.getOrNull(segmentIndex) ?: return 1
    return message.segments.take(segmentIndex + 1).count { it.name == segment.name }
}

internal fun parsePathSpec(spec: String): PathSpec? {
    val match = Regex("^([A-Z][A-Z0-9]{2})(?:[-.](\\d+))?(?:\\[(\\d+)])?(?:\\.(\\d+))?(?:\\.(\\d+))?$")
        .matchEntire(spec.trim().uppercase()) ?: return null
    val (segment, field, rep, comp, sub) = match.destructured
    return PathSpec(
        segment = segment,
        field = field.toIntOrNull() ?: 0,
        repetition = rep.toIntOrNull(),
        component = comp.toIntOrNull() ?: 0,
        subcomponent = sub.toIntOrNull() ?: 0,
    )
}

internal fun resolvePathSpec(message: ParsedMessage, spec: PathSpec, occurrence: Int = 1): FieldPath? {
    val index = firstSegmentIndex(message, spec.segment, occurrence) ?: return null
    return FieldPath(index, spec.field, spec.repetition ?: 1, spec.component, spec.subcomponent)
}

internal fun specOf(message: ParsedMessage, path: FieldPath): PathSpec? {
    val segment = message.segments.getOrNull(path.segment) ?: return null
    return PathSpec(segment.name, path.field, path.repetition, path.component, path.subcomponent)
}

internal fun labelOf(spec: PathSpec, showRepetition: Boolean = false): String = buildString {
    append(spec.segment)
    if (spec.field > 0) append('-').append(spec.field)
    if (showRepetition && (spec.repetition ?: 1) > 1) append('[').append(spec.repetition).append(']')
    if (spec.component > 0) append('.').append(spec.component)
    if (spec.subcomponent > 0) append('.').append(spec.subcomponent)
}

internal fun labelOfPath(message: ParsedMessage, path: FieldPath): String {
    val spec = specOf(message, path) ?: return ""
    val repeats = (message.segments[path.segment].field(path.field)?.repetitions?.size ?: 1) > 1
    return labelOf(spec, showRepetition = repeats || path.repetition > 1)
}

internal fun valueOfSpec(message: ParsedMessage, spec: String, occurrence: Int = 1): String {
    val parsed = parsePathSpec(spec) ?: return ""
    val path = resolvePathSpec(message, parsed, occurrence) ?: return ""
    return unescapeValue(rawAt(message, path), message.delimiters)
}

internal fun headerOf(message: ParsedMessage): MessageHeader = MessageHeader(
    type = valueOfSpec(message, "MSH-9.1"),
    event = valueOfSpec(message, "MSH-9.2"),
    structure = valueOfSpec(message, "MSH-9.3"),
    controlId = valueOfSpec(message, "MSH-10"),
    timestamp = valueOfSpec(message, "MSH-7.1"),
    version = valueOfSpec(message, "MSH-12.1"),
    sendingApplication = valueOfSpec(message, "MSH-3.1"),
    sendingFacility = valueOfSpec(message, "MSH-4.1"),
    receivingApplication = valueOfSpec(message, "MSH-5.1"),
    receivingFacility = valueOfSpec(message, "MSH-6.1"),
    processingId = valueOfSpec(message, "MSH-11.1"),
)

internal fun flattenMessage(message: ParsedMessage): List<FlatNode> {
    val nodes = mutableListOf<FlatNode>()
    val seen = mutableMapOf<String, Int>()
    for (segment in message.segments) {
        val occurrence = (seen[segment.name] ?: 0) + 1
        seen[segment.name] = occurrence
        for (field in segment.fields) {
            for (rep in field.repetitions) {
                if (rep.components.size <= 1) {
                    nodes += FlatNode(FieldPath(segment.index, field.number, rep.index), segment.name, occurrence, rep.raw)
                } else {
                    for (comp in rep.components) {
                        nodes += FlatNode(FieldPath(segment.index, field.number, rep.index, comp.index), segment.name, occurrence, comp.raw)
                    }
                }
            }
        }
    }
    return nodes
}

internal fun findOccurrences(text: String, query: String): List<Span> {
    if (query.isBlank()) return emptyList()
    val spans = mutableListOf<Span>()
    var from = 0
    while (true) {
        val index = text.indexOf(query, from, ignoreCase = true)
        if (index < 0) break
        spans += Span(index, index + query.length)
        from = index + query.length
    }
    return spans
}

internal fun samePosition(a: FieldPath?, b: FieldPath?): Boolean =
    a != null && b != null && a.segment == b.segment && a.field == b.field && a.repetition == b.repetition &&
        a.component == b.component && a.subcomponent == b.subcomponent

internal fun contains(outer: FieldPath, inner: FieldPath): Boolean =
    outer.segment == inner.segment && outer.field == inner.field &&
        (outer.field == 0 || outer.repetition == inner.repetition) &&
        (outer.component == 0 || outer.component == inner.component) &&
        (outer.subcomponent == 0 || outer.subcomponent == inner.subcomponent)
