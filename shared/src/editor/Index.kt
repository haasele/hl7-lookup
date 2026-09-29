// Raw message field with tooltip and highlighting. Workspace places it in the message pane.
package hl7lookup.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import hl7lookup.controls.ActionButton
import hl7lookup.controls.CheckOption
import hl7lookup.controls.ControlTexts
import hl7lookup.controls.Dropdown
import hl7lookup.controls.IconAction
import hl7lookup.controls.Label
import hl7lookup.controls.Modal
import hl7lookup.controls.TextInput
import hl7lookup.datetime.DateStyle
import hl7lookup.datetime.Hl7Dates
import hl7lookup.datetime.Hl7Time
import hl7lookup.datetime.Precision
import hl7lookup.datetime.relativeLabel
import hl7lookup.dictionary.Hl7Dictionary
import hl7lookup.dictionary.TableDef
import hl7lookup.dictionary.TableEntry
import hl7lookup.document.Delimiters
import hl7lookup.document.Er7
import hl7lookup.document.FieldPath
import hl7lookup.document.ParsedMessage
import hl7lookup.i18n.tr
import hl7lookup.theme.IconShape
import hl7lookup.theme.LocalMonoFont
import hl7lookup.theme.LocalPalette
import hl7lookup.theme.Theme
import kotlin.math.roundToInt
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

// Facade for caret info, search marks and date writes. Workspace and the catalog call it.
object Editor {
    // Builds tooltip facts for the caret path. RawEditor and CursorTooltip call it.
    fun info(message: ParsedMessage, dictionary: Hl7Dictionary?, path: FieldPath, style: DateStyle, tableOverride: (String?) -> TableDef?): CursorInfo? =
        cursorInfo(message, dictionary, path, style, tableOverride)
    // Turns a search query into colored marks. Workspace paints them in the raw editor.
    fun searchMarks(message: ParsedMessage, query: String, current: Int, normal: androidx.compose.ui.graphics.Color, active: androidx.compose.ui.graphics.Color): List<Mark> =
        hl7lookup.editor.searchMarks(message, query, current, normal, active)
    // Writes a local date into the message. DateEditor and the facade call it.
    fun writeDate(message: ParsedMessage, target: FieldPath, info: CursorInfo, input: String, style: DateStyle): String? =
        hl7lookup.editor.writeDate(message, target, info.date, input, style)
    // Places a tooltip inside the viewport. RawEditor calls it for hover and pinned tips.
    fun tipSpot(anchorX: Float, anchorY: Float, tipWidth: Int, tipHeight: Int, boundsWidth: Int, boundsHeight: Int) =
        hl7lookup.editor.tipSpot(anchorX, anchorY, tipWidth, tipHeight, boundsWidth, boundsHeight)
    // Colors ER7 delimiters and segments. Catalog HighlightedMessage and the facade call it.
    fun highlight(text: String, colors: EditorColors): androidx.compose.ui.text.AnnotatedString =
        styled(text, Er7.parse(text), colors, emptyList())
    // Exposes tooltip wording. Workspace and screens read it via tr().
    fun texts() = EditorTexts
}

// Tracks clickable regions on a tooltip. RawEditor hit-tests pointer presses against it.
private class TipZones {
    private val rects = LinkedHashMap<String, Rect>()
    private val actions = LinkedHashMap<String, () -> Unit>()
    // Records a tooltip child's bounds. zone() registers each interactive piece.
    fun rect(key: String, rect: Rect) {
        rects[key] = rect
    }
    // Binds a click action to a tooltip zone. zone() registers close and date controls.
    fun action(key: String, run: () -> Unit) {
        actions[key] = run
    }
    // Finds the tightest action under a point. Gesture handling calls it on press.
    fun hit(point: Offset): (() -> Unit)? {
        val key = rects.entries.filter { it.value.contains(point) }.minByOrNull { it.value.width * it.value.height }?.key ?: return null
        return actions[key]
    }
}

// Pins a child at pixel coordinates inside the viewport. Hover and pinned tips use it.
private fun Modifier.placeAt(x: Int, y: Int): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
    val left = x.coerceIn(0, (constraints.maxWidth - placeable.width).coerceAtLeast(0))
    val top = y.coerceIn(0, (constraints.maxHeight - placeable.height).coerceAtLeast(0))
    layout(constraints.maxWidth, constraints.maxHeight) {
        placeable.place(left, top)
    }
}

// Applies syntax and search colors to the text field. RawEditor installs it as the visual transform.
private class HighlightTransformation(private val message: ParsedMessage, private val colors: EditorColors, private val marks: List<Mark>) : VisualTransformation {
    // Builds the colored AnnotatedString for display. BasicTextField asks for it on each paint.
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): TransformedText =
        TransformedText(styled(text.text, message, colors, marks), OffsetMapping.Identity)
}

// Editable ER7 field with hover and pinned tooltips. Workspace places it in the message pane.
@Composable
fun RawEditor(
    text: String,
    message: ParsedMessage,
    dictionary: Hl7Dictionary?,
    selection: FieldPath?,
    marks: List<Mark>,
    style: DateStyle,
    tableOverride: (String?) -> TableDef?,
    focusOffset: Int?,
    focusKey: Long,
    onTextChange: (String) -> Unit,
    onCursor: (FieldPath?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val mono = LocalMonoFont.current
    val focus = remember { FocusRequester() }
    var value by remember { mutableStateOf(TextFieldValue(text)) }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var tooltips by remember { mutableStateOf(true) }
    var pinned by remember { mutableStateOf(false) }
    var pinnedPath by remember { mutableStateOf<FieldPath?>(null) }
    var hoverPath by remember { mutableStateOf<FieldPath?>(null) }
    var pointer by remember { mutableStateOf(Offset.Zero) }
    var pinnedAt by remember { mutableStateOf(Offset.Zero) }
    var hoverSize by remember { mutableStateOf(IntSize.Zero) }
    var pinnedSize by remember { mutableStateOf(IntSize.Zero) }
    val fieldBounds = remember { mutableStateOf<LayoutCoordinates?>(null) }
    val viewBounds = remember { mutableStateOf<LayoutCoordinates?>(null) }
    val zones = remember { TipZones() }
    val scroll = rememberScrollState()
    val density = LocalDensity.current

    if (value.text != text) {
        val cursor = value.selection.start.coerceAtMost(text.length)
        value = TextFieldValue(text, TextRange(cursor))
    }

    LaunchedEffect(selection, message) {
        val pin = pinnedPath
        if (pinned && pin != null && selection != null && !(Er7.same(selection, pin) || Er7.contains(pin, selection) || Er7.contains(selection, pin))) {
            pinned = false
            pinnedPath = null
        }
        if (!value.selection.collapsed) return@LaunchedEffect
        val target = selection ?: return@LaunchedEffect
        val here = Er7.locate(message, value.selection.end)
        if (here != null && (Er7.contains(target, here) || Er7.same(here, target) || Er7.contains(here, target))) return@LaunchedEffect
        val span = Er7.span(message, target) ?: return@LaunchedEffect
        value = value.copy(selection = TextRange(span.start))
        layout?.let { l -> scroll.animateScrollTo((l.getCursorRect(span.start).top - 40f).roundToInt().coerceAtLeast(0)) }
    }
    LaunchedEffect(focusOffset, focusKey) {
        val offset = focusOffset ?: return@LaunchedEffect
        if (offset > value.text.length) return@LaunchedEffect
        value = value.copy(selection = TextRange(offset))
        onCursor(Er7.locate(message, offset))
        layout?.let { l -> scroll.animateScrollTo((l.getCursorRect(offset).top - 40f).roundToInt().coerceAtLeast(0)) }
    }

    val colors = EditorColors(palette.text, palette.delimiter, palette.componentDelimiter, palette.repetitionDelimiter, palette.subcomponentDelimiter, palette.warning, Theme::segmentColor)
    val selectedSpan = selection?.let { Er7.span(message, it) }
    val allMarks = if (selectedSpan != null) listOf(Mark(selectedSpan, MarkKind.SELECTION, palette.cursorField)) + marks else marks
    val transformation = remember(message, allMarks, palette) { HighlightTransformation(message, colors, allMarks) }

    // Maps a pointer position to a text offset. Gesture handlers call it on move and press.
    fun hitAt(position: Offset): TextHit {
        val current = layout ?: return TextHit(0, false)
        val pad = with(density) { 10.dp.toPx() }
        return textHit(current, Offset(position.x - pad, position.y - pad), value.text.length)
    }

    // Converts field-local coordinates into the scroll viewport. Hover and pin use it for tip placement.
    fun inView(local: Offset): Offset {
        val field = fieldBounds.value
        val view = viewBounds.value
        if (field == null || view == null || !field.isAttached || !view.isAttached) return local
        val root = field.localToRoot(local)
        val origin = view.localToRoot(Offset.Zero)
        return Offset(root.x - origin.x, root.y - origin.y)
    }

    // Reports the field path under the caret. Value changes and empty clicks call it.
    fun reportCursor(offset: Int) {
        onCursor(Er7.locate(message, offset))
    }

    // Clears pinned and hover tooltips. Escape and range selection call it.
    fun closeTooltip() {
        pinned = false
        pinnedPath = null
        hoverPath = null
    }

    // Pins a tooltip on a field path. Text presses call it when the path resolves.
    fun pin(path: FieldPath, at: Offset) {
        pinned = true
        pinnedPath = path
        pinnedAt = at
        onCursor(path)
    }

    val gestures = remember {
        // Holds gesture callbacks shared with pointerInput. RawEditor fills them each recomposition.
        object {
            var hit: (Offset) -> TextHit = { TextHit(0, false) }
            var moved: (Offset, Boolean, Int) -> Unit = { _, _, _ -> }
            var empty: (TextHit) -> Unit = {}
            var text: (Offset, TextHit) -> Unit = { _, _ -> }
            var claim: (Offset) -> Boolean = { false }
            var covered: (Offset) -> Boolean = { false }
            var select: (Int, Int) -> Unit = { _, _ -> }
        }
    }
    gestures.hit = ::hitAt
    gestures.moved = { position, over, offset ->
        pointer = inView(position)
        hoverPath = if (over) Er7.locate(message, offset) else null
    }
    gestures.empty = { hit ->
        val offset = hit.offset.coerceIn(0, value.text.length)
        value = value.copy(selection = TextRange(offset))
        closeTooltip()
        reportCursor(offset)
        runCatching { focus.requestFocus() }
    }
    gestures.text = { position, hit ->
        val path = Er7.locate(message, hit.offset)
        if (path != null) pin(path, inView(position))
    }
    gestures.covered = { position -> zones.hit(inView(position)) != null }
    gestures.claim = { position ->
        val action = zones.hit(inView(position))
        if (action != null) {
            action()
            true
        } else false
    }

    // Registers a tooltip region and its click. CursorTooltip wraps interactive children with it.
    fun zone(key: String, run: () -> Unit): Modifier {
        zones.action(key, run)
        return Modifier.onGloballyPositioned { coords ->
            val view = viewBounds.value
            if (view == null || !view.isAttached || !coords.isAttached) return@onGloballyPositioned
            val origin = view.localToRoot(Offset.Zero)
            val top = coords.localToRoot(Offset.Zero)
            zones.rect(key, Rect(top.x - origin.x, top.y - origin.y, top.x - origin.x + coords.size.width, top.y - origin.y + coords.size.height))
        }
    }

    // Sets a drag selection range and clears tips. Empty-area press handling calls it.
    fun selectRange(anchor: Int, end: Int) {
        val length = value.text.length
        val start = anchor.coerceIn(0, length)
        val stop = end.coerceIn(0, length)
        value = value.copy(selection = TextRange(start, stop))
        closeTooltip()
        reportCursor(stop)
    }
    gestures.select = ::selectRange

    Column(modifier.fillMaxSize().background(palette.input)) {
        EditorToolbar(message, tooltips, { tooltips = it }, onTextChange)
        BoxWithConstraints(Modifier.fillMaxSize().onGloballyPositioned { viewBounds.value = it }) {
            val viewHeight = maxHeight
            val viewport = with(density) { IntSize(maxWidth.roundToPx(), maxHeight.roundToPx()) }
            Box(Modifier.fillMaxSize().verticalScroll(scroll)) {
                BasicTextField(
                    value = value,
                    onValueChange = { next ->
                        val moved = next.selection != value.selection
                        value = next
                        if (next.text != text) onTextChange(next.text)
                        if (moved || next.text != text) reportCursor(next.selection.end.coerceIn(0, next.text.length))
                    },
                    textStyle = TextStyle(color = palette.text, fontFamily = mono, fontSize = 13.sp, lineHeight = 19.sp),
                    cursorBrush = SolidColor(palette.link),
                    visualTransformation = transformation,
                    onTextLayout = { layout = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = viewHeight)
                        .onGloballyPositioned { fieldBounds.value = it }
                        .pointerInput(gestures) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val change = event.changes.firstOrNull() ?: continue
                                    val hit = gestures.hit(change.position)
                                    if (event.type == PointerEventType.Move || event.type == PointerEventType.Exit) {
                                        gestures.moved(change.position, event.type != PointerEventType.Exit && hit.onText, hit.offset)
                                    }
                                    if (event.type == PointerEventType.Press && gestures.claim(change.position)) {
                                        change.consume()
                                    } else if (event.type == PointerEventType.Press && !hit.onText) {
                                        change.consume()
                                        val anchor = hit.offset
                                        gestures.select(anchor, anchor)
                                        while (true) {
                                            val drag = awaitPointerEvent()
                                            val dragged = drag.changes.firstOrNull() ?: continue
                                            dragged.consume()
                                            gestures.select(anchor, gestures.hit(dragged.position).offset)
                                            if (drag.type == PointerEventType.Release || drag.changes.none { it.pressed }) break
                                        }
                                        continue
                                    }
                                    awaitPointerEvent(PointerEventPass.Final)
                                    if (event.type == PointerEventType.Press && hit.onText && !gestures.covered(change.position)) gestures.text(change.position, hit)
                                }
                            }
                        }
                        .padding(10.dp)
                        .focusRequester(focus)
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                                closeTooltip()
                                true
                            } else false
                        },
                    decorationBox = { inner ->
                        Box {
                            if (value.text.isEmpty()) Label(tr(EditorTexts.placeholder), color = palette.textDim, mono = true)
                            inner()
                        }
                    },
                )
            }
            val tooltipWidth = 330.dp
            val hoverInfo = if (!pinned && tooltips) hoverPath?.let { cursorInfo(message, dictionary, it, style, tableOverride) } else null
            if (hoverInfo != null) {
                val spot = tipSpot(pointer.x, pointer.y, hoverSize.width, hoverSize.height, viewport.width, viewport.height)
                Box(Modifier.fillMaxSize().placeAt(spot.x, spot.y)) {
                    Box(Modifier.onSizeChanged { hoverSize = it }) {
                        CursorTooltip(hoverInfo, message, style, tooltipWidth, onTextChange, interactive = false, onClose = {})
                    }
                }
            }
            val pinnedInfo = if (pinned && tooltips) pinnedPath?.let { cursorInfo(message, dictionary, it, style, tableOverride) } else null
            if (pinnedInfo != null) {
                val spot = tipSpot(pinnedAt.x, pinnedAt.y, pinnedSize.width, pinnedSize.height, viewport.width, viewport.height)
                val xDp = with(density) { spot.x.toDp() }
                val yDp = with(density) { spot.y.toDp() }
                Box(Modifier.fillMaxSize().zIndex(2f)) {
                    Box(Modifier.padding(start = xDp, top = yDp)) {
                        Box(Modifier.onSizeChanged { pinnedSize = it }.onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                                closeTooltip()
                                true
                            } else false
                        }) {
                            CursorTooltip(pinnedInfo, message, style, tooltipWidth, onTextChange, interactive = true, onClose = ::closeTooltip, zone = ::zone)
                        }
                    }
                }
            }
        }
    }
}

// Delimiter editors and the tooltip toggle. RawEditor shows it above the text field.
@Composable
private fun EditorToolbar(message: ParsedMessage, tooltips: Boolean, onTooltips: (Boolean) -> Unit, onTextChange: (String) -> Unit) {
    val palette = LocalPalette.current
    val d = message.delimiters
    var issue by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().background(palette.surfaceRaised)) {
        FlowRow(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Label(tr(EditorTexts.delimiters), color = palette.textDim, size = 11.sp)
            val entries = listOf(
                EditorTexts.delimiterField to d.field, EditorTexts.delimiterComponent to d.component, EditorTexts.delimiterRepetition to d.repetition,
                EditorTexts.delimiterEscape to d.escape, EditorTexts.delimiterSubcomponent to d.subcomponent,
            )
            entries.forEachIndexed { index, (label, char) ->
                DelimiterInput(tr(label), char, enabled = !message.isEmpty) { next ->
                    val updated = when (index) {
                        0 -> d.copy(field = next)
                        1 -> d.copy(component = next)
                        2 -> d.copy(repetition = next)
                        3 -> d.copy(escape = next)
                        else -> d.copy(subcomponent = next)
                    }
                    issue = delimiterIssue(updated)
                    if (!issue) onTextChange(Er7.withDelimiters(message, updated))
                }
            }
            CheckOption(tr(EditorTexts.tooltips), tooltips, onTooltips)
        }
        if (issue) Label(tr(EditorTexts.delimiterInvalid), Modifier.padding(start = 8.dp, bottom = 3.dp), color = palette.warning, size = 11.sp)
    }
}

// Single-character delimiter field. EditorToolbar repeats it for each delimiter.
@Composable
private fun DelimiterInput(label: String, char: Char, enabled: Boolean, onChange: (Char) -> Unit) {
    val palette = LocalPalette.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        BasicTextField(
            value = char.toString(),
            onValueChange = { typed -> typed.lastOrNull { it != char }?.let(onChange) },
            enabled = enabled,
            singleLine = true,
            textStyle = TextStyle(color = palette.componentDelimiter, fontFamily = LocalMonoFont.current, fontSize = 13.sp, fontWeight = FontWeight.Bold),
            cursorBrush = SolidColor(palette.link),
            modifier = Modifier.width(22.dp).clip(RoundedCornerShape(3.dp)).background(palette.input)
                .border(1.dp, palette.border, RoundedCornerShape(3.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
        )
        Label(" $label", color = palette.textDim, size = 10.sp)
    }
}

// Field facts, table pickers and date edit. RawEditor shows hover and pinned versions.
@Composable
fun CursorTooltip(
    info: CursorInfo,
    message: ParsedMessage,
    style: DateStyle,
    width: androidx.compose.ui.unit.Dp,
    onTextChange: (String) -> Unit,
    interactive: Boolean,
    onClose: () -> Unit,
    zone: (String, () -> Unit) -> Modifier = { _, _ -> Modifier },
) {
    val palette = LocalPalette.current
    Column(
        zone("card") { }.widthIn(max = width).clip(RoundedCornerShape(5.dp)).background(palette.surfaceRaised)
            .border(1.dp, palette.border, RoundedCornerShape(5.dp)).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Label(info.label, color = Theme.segmentColor(info.segmentName), mono = true, weight = FontWeight.SemiBold)
            Label("  " + (info.node?.name ?: info.segmentDescription ?: ""), Modifier.weight(1f), weight = FontWeight.SemiBold, maxLines = 2)
            if (interactive) Box(zone("close", onClose)) { IconAction(IconShape.Close, onClose, color = palette.textDim, size = 10.dp) }
        }
        if (info.path.field > 0 && info.segmentDescription != null) Label(info.segmentDescription, color = palette.textDim, size = 11.sp)
        val node = info.node
        if (node != null && node.datatype.isNotEmpty()) {
            val facts = buildList {
                add(tr(EditorTexts.datatype, node.datatype))
                if (node.table != null) add(info.table?.name?.takeIf { it.isNotBlank() }?.let { tr(EditorTexts.tableNamed, node.table, it) } ?: tr(EditorTexts.table, node.table))
                if (node.required) add(tr(EditorTexts.required))
                if (node.maxRepetitions != 1) add(tr(EditorTexts.repeating))
                if (node.length > 0) add(tr(EditorTexts.maxLength, node.length))
            }
            Label(facts.joinToString(" · "), color = palette.textDim, size = 11.sp)
        }
        val repetitions = message.segments.getOrNull(info.path.segment)?.field(info.path.field)?.repetitions?.size ?: 1
        if (repetitions > 1) Label(tr(EditorTexts.repetitionOf, info.path.repetition, repetitions), color = palette.textDim, size = 11.sp)
        if (info.value.isNotEmpty() && info.path.field > 0) {
            Label(hl7lookup.controls.Controls.ellipsize(info.value, 160), mono = true, size = 12.sp)
        }
        if (info.meaning != null) Label("= ${info.meaning}", color = palette.success, size = 12.sp)
        else if (info.table != null && info.table.entries.isNotEmpty() && info.value.isNotEmpty() && info.path.field > 0) {
            Label(tr(EditorTexts.unknownValue, info.value), color = palette.warning, size = 11.sp)
        }
        if (info.date != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Label("${tr(EditorTexts.localDate)}: ", color = palette.textDim, size = 12.sp)
                Label(info.dateText ?: "", size = 12.sp, weight = FontWeight.SemiBold)
            }
            Label(relativeLabel(info.date, hl7lookup.platform.LocalPlatform.current.nowMillis()), color = palette.textDim, size = 11.sp)
        }
        val target = info.dateTarget
        if (interactive && target != null) DateEditor(info, message, target, style, onTextChange, zone)
        val table = info.table
        if (interactive && table != null && table.entries.isNotEmpty() && info.path.field > 0) {
            val codePath = if (info.node?.datatype in setOf("ID", "IS") || info.path.component > 0) info.path else info.path.copy(component = 1)
            Dropdown(
                selected = table.entries.firstOrNull { it.code == info.value } ?: TableEntry("", tr(EditorTexts.chooseValue)),
                options = table.entries,
                label = { if (it.code.isEmpty()) it.description else "${it.code} – ${it.description}" },
                onSelect = { onTextChange(Er7.write(message, codePath, it.code)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// Date text field and calendar opener inside a tip. CursorTooltip embeds it for date paths.
@Composable
private fun DateEditor(
    info: CursorInfo,
    message: ParsedMessage,
    target: FieldPath,
    style: DateStyle,
    onTextChange: (String) -> Unit,
    zone: (String, () -> Unit) -> Modifier,
) {
    val palette = LocalPalette.current
    val dateFocus = remember { FocusRequester() }
    var picker by remember { mutableStateOf(false) }
    var input by remember(info.path, info.dateText) { mutableStateOf(info.date?.let { Hl7Dates.format(it, style) } ?: "") }
    val valid = Hl7Dates.parseInput(input, style) != null
    // Writes the typed date into the message. Submit on the date input calls it.
    fun apply() {
        writeDate(message, target, info.date, input, style)?.let(onTextChange)
    }
    // Opens the calendar modal. The Pick date button calls it.
    fun openPicker() {
        picker = true
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        TextInput(
            input,
            { input = it },
            Modifier.weight(1f).focusRequester(dateFocus).then(zone("input") { runCatching { dateFocus.requestFocus() } }),
            placeholder = Hl7Dates.inputHint(style),
            error = input.isNotEmpty() && !valid,
            onSubmit = ::apply,
        )
        ActionButton(tr(EditorTexts.changeDate), ::openPicker, zone("date", ::openPicker), icon = IconShape.Calendar)
    }
    if (picker) TooltipDatePicker(message, target, info.date, onTextChange) { picker = false }
    if (input.isNotEmpty() && !valid) Label(tr(EditorTexts.invalidDate, Hl7Dates.inputHint(style)), color = palette.warning, size = 11.sp)
}

// Calendar modal that writes a picked day. DateEditor opens it from the tip.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TooltipDatePicker(
    message: ParsedMessage,
    target: FieldPath,
    current: Hl7Time?,
    onTextChange: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalPalette.current
    val initial = current?.let { Hl7Dates.local(it).date.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds() }
    val state = rememberDatePickerState(initialSelectedDateMillis = initial)
    Modal(tr(EditorTexts.changeDate), onDismiss, width = 420.dp, actions = {
        ActionButton(tr(ControlTexts.apply), {
            val millis = state.selectedDateMillis
            if (millis != null) {
                val date = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date
                val time = current?.let { LocalTime(it.hour, it.minute, it.second) } ?: LocalTime(0, 0)
                val precision = current?.precision?.let { if (it == Precision.FRACTION) Precision.SECOND else it }?.coerceAtLeast(Precision.DAY) ?: Precision.DAY
                onTextChange(Er7.writeRaw(message, target, Hl7Dates.toHl7(LocalDateTime(date, time), precision, current?.offsetMinutes)))
            }
            onDismiss()
        }, primary = true)
    }) {
        DatePicker(
            state = state,
            showModeToggle = false,
            colors = DatePickerDefaults.colors(containerColor = palette.surface),
        )
    }
}
