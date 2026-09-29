// Field grid for the segment under the caret. Workspace places it beside the message.
package hl7lookup.grid

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import hl7lookup.controls.ActionButton
import hl7lookup.controls.CheckOption
import hl7lookup.controls.Dropdown
import hl7lookup.controls.EmptyState
import hl7lookup.motion.IllustrationKind
import hl7lookup.controls.IconAction
import hl7lookup.controls.Label
import hl7lookup.controls.Modal
import hl7lookup.controls.TextInput
import hl7lookup.datetime.Hl7Dates
import hl7lookup.datetime.Precision
import hl7lookup.dictionary.Dictionaries
import hl7lookup.dictionary.Hl7Dictionary
import hl7lookup.dictionary.NodeLevel
import hl7lookup.dictionary.TableDef
import hl7lookup.document.Er7
import hl7lookup.document.FieldPath
import hl7lookup.document.ParsedMessage
import hl7lookup.i18n.tr
import hl7lookup.theme.IconShape
import hl7lookup.theme.LocalPalette
import hl7lookup.theme.Theme
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

// Facade for field rows and segment choices. Workspace and the field pane call it.
object Grid {
    // Builds editable grid rows for a segment. FieldGrid and tests call it through the facade.
    fun rows(message: ParsedMessage, dictionary: Hl7Dictionary?, segment: Int, showEmpty: Boolean): List<GridRow> = gridRows(message, dictionary, segment, showEmpty)
    // Lists segments in the message for the picker. FieldGrid and the facade call it.
    fun segments(message: ParsedMessage, dictionary: Hl7Dictionary?): List<SegmentChoice> = segmentChoices(message, dictionary)
    // Exposes grid wording. Workspace reads it via tr().
    fun texts() = GridTexts
}

// Segment picker and field rows for the caret. Workspace places it beside the message.
@Composable
fun FieldGrid(
    message: ParsedMessage,
    dictionary: Hl7Dictionary?,
    selection: FieldPath?,
    showEmpty: Boolean,
    onShowEmpty: (Boolean) -> Unit,
    tableOverride: (String?) -> TableDef?,
    highlight: (FieldPath) -> Color?,
    flag: (FieldPath) -> Color?,
    onSelect: (FieldPath) -> Unit,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val choices = segmentChoices(message, dictionary)
    if (choices.isEmpty()) {
        EmptyState(tr(GridTexts.empty), modifier, illustration = IllustrationKind.FIELDS)
        return
    }
    var picked by remember { mutableStateOf(0) }
    LaunchedEffect(selection?.segment) {
        selection?.segment?.let { segment -> if (choices.any { it.index == segment }) picked = segment }
    }
    val segmentIndex = segmentOfSelection(selection, picked).coerceIn(0, choices.lastIndex)
    val rows = gridRows(message, dictionary, segmentIndex, showEmpty, keep = selection)
    val listState = rememberLazyListState()
    LaunchedEffect(selection, segmentIndex) {
        val target = selection ?: return@LaunchedEffect
        val index = rows.indexOfFirst { Er7.same(it.path, target) }.takeIf { it >= 0 }
            ?: rows.indexOfFirst { Er7.contains(it.path, target) }.takeIf { it >= 0 }
            ?: return@LaunchedEffect
        val visible = listState.layoutInfo.visibleItemsInfo.map { it.index }
        if (index !in visible) listState.animateScrollToItem(index)
    }
    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().background(palette.surfaceRaised).padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Label(tr(GridTexts.segment), color = palette.textDim, size = 11.sp)
            Dropdown(
                choices[segmentIndex], choices,
                label = { c -> "${c.name}${if (c.occurrence > 1 || choices.count { it.name == c.name } > 1) " (${c.occurrence})" else ""}${c.description?.let { " – $it" } ?: ""}" },
                onSelect = { choice -> picked = choice.index; onSelect(FieldPath(choice.index, 0)) },
                modifier = Modifier.weight(1f),
            )
            CheckOption(tr(GridTexts.hideEmpty), !showEmpty, { onShowEmpty(!it) })
        }
        LazyColumn(Modifier.fillMaxSize(), state = listState) {
            itemsIndexed(rows, key = { _, row -> "${row.path}" }) { _, row ->
                GridLine(row, message, dictionary, selection, tableOverride, highlight(row.path), flag(row.path), onSelect, onTextChange)
            }
        }
    }
}

// One labeled field or component row. FieldGrid lists them in a LazyColumn.
@Composable
private fun GridLine(
    row: GridRow,
    message: ParsedMessage,
    dictionary: Hl7Dictionary?,
    selection: FieldPath?,
    tableOverride: (String?) -> TableDef?,
    highlight: Color?,
    flag: Color?,
    onSelect: (FieldPath) -> Unit,
    onTextChange: (String) -> Unit,
) {
    val palette = LocalPalette.current
    val selected = selection != null && Er7.same(row.path, selection)
    val levelColor = when (row.level) {
        NodeLevel.FIELD -> palette.fieldRow
        NodeLevel.COMPONENT -> palette.componentRow
        else -> palette.subcomponentRow
    }
    val indent = when (row.level) {
        NodeLevel.FIELD -> 0.dp
        NodeLevel.COMPONENT -> 12.dp
        else -> 24.dp
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 30.dp)
            .background(if (selected) palette.selected else Color.Transparent)
            .clickable { onSelect(row.path) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(flag ?: Color.Transparent))
        Box(Modifier.width(98.dp).padding(start = indent).background(levelColor.copy(alpha = 0.55f)).padding(horizontal = 6.dp, vertical = 6.dp)) {
            Label(row.label, mono = true, size = 11.sp, color = highlight ?: Color.White, weight = if (row.required) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
        }
        Column(Modifier.width(150.dp).padding(horizontal = 6.dp)) {
            Label(row.name, size = 11.sp, color = if (row.required) palette.text else palette.textDim, maxLines = 2)
            if (row.datatype.isNotEmpty()) Label(row.datatype + (row.table?.let { " · $it" } ?: ""), size = 9.sp, color = palette.textDim, maxLines = 1)
        }
        Box(Modifier.weight(1f).padding(end = 6.dp, top = 2.dp, bottom = 2.dp)) {
            if (row.editable) ValueEditor(row, message, dictionary, tableOverride, onSelect, onTextChange)
            else Label(row.raw, mono = true, size = 12.sp, color = palette.textDim, maxLines = 2)
        }
    }
}

// Text input with table suggestions and a date button. GridLine embeds it for editable cells.
@Composable
private fun ValueEditor(
    row: GridRow,
    message: ParsedMessage,
    dictionary: Hl7Dictionary?,
    tableOverride: (String?) -> TableDef?,
    onSelect: (FieldPath) -> Unit,
    onTextChange: (String) -> Unit,
) {
    val palette = LocalPalette.current
    val table = tableOverride(row.table) ?: Dictionaries.table(dictionary, row.table)
    val entries = table?.entries.orEmpty()
    var suggest by remember { mutableStateOf(false) }
    var calendar by remember { mutableStateOf(false) }
    val header = message.segments.getOrNull(row.path.segment)?.name in setOf("MSH", "FHS", "BHS") && row.path.field <= 2 && row.path.component == 0
    val dateLike = isDateType(row.datatype)
    Box {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextInput(
                value = row.value,
                onChange = { next ->
                    val text = if (header) Er7.writeRaw(message, row.path, next) else Er7.write(message, row.path, next)
                    onTextChange(text)
                    if (entries.isNotEmpty()) suggest = true
                },
                modifier = Modifier.weight(1f).onFocusChanged { if (it.isFocused) onSelect(row.path) },
                mono = true,
                error = entries.isNotEmpty() && row.value.isNotEmpty() && entries.none { it.code == row.value },
            )
            if (entries.isNotEmpty()) IconAction(IconShape.ChevronDown, { suggest = !suggest }, color = palette.textDim, size = 11.dp)
            if (dateLike) IconAction(IconShape.Calendar, { calendar = true }, color = palette.textDim, size = 13.dp)
        }
        val matches = Dictionaries.search(table, if (suggest && entries.any { it.code == row.value }) "" else row.value).take(60)
        DropdownMenu(
            expanded = suggest && matches.isNotEmpty(),
            onDismissRequest = { suggest = false },
            properties = PopupProperties(focusable = false),
            modifier = Modifier.heightIn(max = 320.dp).background(palette.surfaceRaised),
        ) {
            if (table != null) Label(tr(GridTexts.suggestions, table.id) + (table.name.takeIf { it.isNotBlank() }?.let { " – $it" } ?: ""), Modifier.padding(horizontal = 12.dp, vertical = 4.dp), color = palette.textDim, size = 11.sp)
            matches.forEach { entry ->
                DropdownMenuItem(
                    text = {
                        Row {
                            Label(entry.code, Modifier.width(70.dp), mono = true, size = 12.sp, color = palette.link)
                            Label(entry.description, size = 12.sp)
                        }
                    },
                    onClick = { suggest = false; onTextChange(Er7.write(message, row.path, entry.code)) },
                )
            }
        }
    }
    if (calendar) CalendarDialog(row, message, onTextChange) { calendar = false }
}

// Date picker modal that writes HL7. ValueEditor opens it for date-like cells.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarDialog(row: GridRow, message: ParsedMessage, onTextChange: (String) -> Unit, onDismiss: () -> Unit) {
    val palette = LocalPalette.current
    val target = if (row.datatype == "TS") {
        if (row.level == NodeLevel.FIELD) row.path.copy(component = 1) else row.path.copy(subcomponent = 1)
    } else row.path
    val current = Hl7Dates.parse(Er7.value(message, target).substringBefore(message.delimiters.subcomponent))
    val initial = current?.let { Hl7Dates.local(it).date.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds() }
    val state = rememberDatePickerState(initialSelectedDateMillis = initial)
    Modal(tr(GridTexts.pickDate), onDismiss, width = 420.dp, actions = {
        ActionButton(tr(GridTexts.apply), {
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
