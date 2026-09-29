// Caret hit testing, date writing and syntax colors. editor/Index calls them; TooltipSpotTest calls Editor.tipSpot.
package hl7lookup.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import hl7lookup.datetime.DateStyle
import hl7lookup.datetime.Hl7Dates
import hl7lookup.datetime.Hl7Time
import hl7lookup.datetime.Precision
import hl7lookup.dictionary.Dictionaries
import hl7lookup.dictionary.Hl7Dictionary
import hl7lookup.dictionary.NodeInfo
import hl7lookup.dictionary.TableDef
import hl7lookup.document.Delimiters
import hl7lookup.document.Er7
import hl7lookup.document.FieldPath
import hl7lookup.document.ParsedMessage
import hl7lookup.document.Span

// Kinds of colored spans in the raw editor. Mark and searchMarks pick among them.
enum class MarkKind { SELECTION, HIGHLIGHT, SEARCH, SEARCH_CURRENT, ERROR, WARNING }

// A colored span over message text. RawEditor and searchMarks build lists of these.
data class Mark(val span: Span, val kind: MarkKind, val color: Color)

// Palette for ER7 syntax colors. styled() and HighlightTransformation take it.
data class EditorColors(
    val text: Color,
    val field: Color,
    val component: Color,
    val repetition: Color,
    val subcomponent: Color,
    val escape: Color,
    val segment: (String) -> Color,
)

// Dictionary facts for the caret path. cursorInfo builds it; CursorTooltip shows it.
data class CursorInfo(
    val path: FieldPath,
    val label: String,
    val segmentName: String,
    val segmentDescription: String?,
    val node: NodeInfo?,
    val table: TableDef?,
    val value: String,
    val meaning: String?,
    val date: Hl7Time?,
    val dateText: String?,
    val dateTarget: FieldPath?,
)

private val dateTypes = setOf("DT", "DTM", "TS")

// Paints segment names, delimiters and marks. HighlightTransformation and Editor.highlight call it.
internal fun styled(text: String, message: ParsedMessage, colors: EditorColors, marks: List<Mark>): AnnotatedString = buildAnnotatedString {
    append(text)
    val d = message.delimiters
    addStyle(SpanStyle(color = colors.text), 0, text.length)
    for (segment in message.segments) {
        val end = segment.nameSpan.end.coerceAtMost(text.length)
        if (segment.nameSpan.start < end) addStyle(SpanStyle(color = colors.segment(segment.name), fontWeight = FontWeight.SemiBold), segment.nameSpan.start, end)
    }
    val headerEnd = message.segments.firstOrNull()?.takeIf { it.name in setOf("MSH", "FHS", "BHS") }?.let { it.nameSpan.end + 1 + d.encodingCharacters.length } ?: 0
    for (i in text.indices) {
        if (i < headerEnd) continue
        val color = when (text[i]) {
            d.field -> colors.field
            d.component -> colors.component
            d.repetition -> colors.repetition
            d.subcomponent -> colors.subcomponent
            d.escape -> colors.escape
            else -> null
        }
        if (color != null) addStyle(SpanStyle(color = color, fontWeight = FontWeight.Bold), i, i + 1)
    }
    for (mark in marks) {
        val start = mark.span.start.coerceIn(0, text.length)
        val stop = mark.span.end.coerceIn(0, text.length)
        if (start >= stop) continue
        val style = when (mark.kind) {
            MarkKind.ERROR, MarkKind.WARNING -> SpanStyle(textDecoration = TextDecoration.Underline, background = mark.color.copy(alpha = 0.16f))
            MarkKind.HIGHLIGHT -> SpanStyle(background = mark.color.copy(alpha = 0.32f))
            else -> SpanStyle(background = mark.color)
        }
        addStyle(style, start, stop)
    }
}

// Finds which path holds the date for a datatype. cursorInfo uses it for date tips.
internal fun dateTargetOf(path: FieldPath, node: NodeInfo?): FieldPath? {
    if (node == null || path.field == 0) return null
    return when {
        node.datatype == "TS" && path.component == 0 -> path.copy(component = 1)
        node.datatype == "TS" && path.subcomponent == 0 -> path.copy(subcomponent = 1)
        node.datatype in dateTypes -> path
        else -> null
    }
}

// Assembles CursorInfo for a path. Editor.info and RawEditor tooltips call it.
internal fun cursorInfo(
    message: ParsedMessage,
    dictionary: Hl7Dictionary?,
    path: FieldPath,
    style: DateStyle,
    tableOverride: (String?) -> TableDef?,
): CursorInfo? {
    val segment = message.segments.getOrNull(path.segment) ?: return null
    val node = Dictionaries.node(dictionary, segment.name, path.field, path.component, path.subcomponent)
    val table = tableOverride(node?.table) ?: Dictionaries.table(dictionary, node?.table)
    val value = if (path.field == 0) segment.name else Er7.value(message, path)
    val meaning = Dictionaries.entry(table, value)?.description
    val target = dateTargetOf(path, node)
    val dateRaw = target?.let { Er7.value(message, it).substringBefore(message.delimiters.subcomponent) }
    val date = dateRaw?.let(Hl7Dates::parse)
    return CursorInfo(
        path = path,
        label = Er7.label(message, path),
        segmentName = segment.name,
        segmentDescription = Dictionaries.segment(dictionary, segment.name)?.description,
        node = node,
        table = table,
        value = value,
        meaning = meaning,
        date = date,
        dateText = date?.let { Hl7Dates.format(it, style) },
        dateTarget = target,
    )
}

// Parses local date input and writes HL7. Editor.writeDate and DateEditor call it.
internal fun writeDate(message: ParsedMessage, target: FieldPath, previous: Hl7Time?, input: String, style: DateStyle): String? {
    val parsed = Hl7Dates.parseInput(input, style) ?: return null
    val precision = when {
        !Hl7Dates.inputHasTime(input) -> Precision.DAY
        previous != null && previous.precision >= Precision.SECOND -> Precision.SECOND
        previous != null && previous.precision >= Precision.MINUTE -> Precision.MINUTE
        else -> Precision.SECOND
    }
    val value = Hl7Dates.toHl7(parsed, precision, previous?.offsetMinutes)
    return Er7.writeRaw(message, target, value)
}

// True when delimiters collide or look like letters. EditorToolbar blocks bad edits with it.
internal fun delimiterIssue(next: Delimiters): Boolean {
    val all = listOf(next.field, next.component, next.repetition, next.escape, next.subcomponent)
    return all.toSet().size != all.size || all.any { it.isLetterOrDigit() || it.isWhitespace() }
}

// Offset and whether the pointer is on text. textHit returns it for gesture handling.
data class TextHit(val offset: Int, val onText: Boolean)

// Clamps tip coordinates inside the viewport. Editor.tipSpot and RawEditor call it.
internal fun tipSpot(anchorX: Float, anchorY: Float, tipWidth: Int, tipHeight: Int, boundsWidth: Int, boundsHeight: Int, gap: Int = 16): IntOffset {
    val margin = 8
    var x = anchorX.toInt() + gap
    var y = anchorY.toInt() + gap
    if (tipWidth > 0 && x + tipWidth > boundsWidth - margin) x = anchorX.toInt() - gap - tipWidth
    if (tipHeight > 0 && y + tipHeight > boundsHeight - margin) y = anchorY.toInt() - gap - tipHeight
    val maxX = (boundsWidth - tipWidth - margin).coerceAtLeast(margin)
    val maxY = (boundsHeight - tipHeight - margin).coerceAtLeast(margin)
    return IntOffset(x.coerceIn(margin, maxX), y.coerceIn(margin, maxY))
}

// Resolves a pointer position to a caret hit. RawEditor hitAt calls it.
internal fun textHit(layout: TextLayoutResult, position: Offset, length: Int): TextHit {
    if (length <= 0 || layout.lineCount == 0) return TextHit(0, false)
    val lastLine = layout.lineCount - 1
    if (position.y > layout.getLineBottom(lastLine) + 2f) return TextHit(length, false)
    val line = layout.getLineForVerticalPosition(position.y.coerceAtLeast(0f)).coerceIn(0, lastLine)
    val start = layout.getLineStart(line)
    val end = layout.getLineEnd(line, true).coerceAtMost(length)
    val left = layout.getLineLeft(line)
    val right = layout.getLineRight(line)
    if (position.x > right + 4f) return TextHit(end, false)
    if (position.x < left - 2f) return TextHit(start, false)
    return TextHit(layout.getOffsetForPosition(Offset(position.x.coerceIn(left, right), position.y.coerceAtLeast(0f))).coerceIn(0, length), true)
}

// Builds search marks for the current hit. Editor.searchMarks and Workspace call it.
internal fun searchMarks(message: ParsedMessage, query: String, current: Int, normal: Color, active: Color): List<Mark> =
    Er7.find(message.text, query).mapIndexed { i, span -> Mark(span, if (i == current) MarkKind.SEARCH_CURRENT else MarkKind.SEARCH, if (i == current) active else normal) }
