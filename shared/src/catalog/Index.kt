package hl7lookup.catalog

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hl7lookup.controls.ActionButton
import hl7lookup.controls.IconAction
import hl7lookup.controls.Label
import hl7lookup.controls.TextInput
import hl7lookup.dictionary.Hl7Dictionary
import hl7lookup.editor.Editor
import hl7lookup.editor.EditorColors
import hl7lookup.i18n.Language
import hl7lookup.i18n.LocalLanguage
import hl7lookup.i18n.tr
import hl7lookup.theme.IconShape
import hl7lookup.theme.LocalMonoFont
import hl7lookup.theme.LocalPalette
import hl7lookup.theme.Theme

object Catalog {
    fun types(dictionary: Hl7Dictionary): List<WikiType> = wikiTypes(dictionary)
    fun matching(types: List<WikiType>, query: String): List<WikiType> = filterWiki(types, query)
    fun outline(dictionary: Hl7Dictionary, structure: String): List<WikiLine> = wikiOutline(dictionary, structure)
    fun guide(dictionary: Hl7Dictionary, type: String, event: String, structure: String, german: Boolean = false): String =
        wikiGuide(dictionary, type, event, structure, german)
    fun example(dictionary: Hl7Dictionary, type: String, event: String, structure: String, samples: List<String>): String =
        wikiExample(dictionary, type, event, structure, samples)
    fun segments(dictionary: Hl7Dictionary): List<WikiSegmentView> = wikiSegments(dictionary)
    fun matchingSegments(segments: List<WikiSegmentView>, query: String): List<WikiSegmentView> = filterSegments(segments, query)
    fun texts() = CatalogTexts
}

private enum class WikiBrowse { TYPES, SEGMENTS }

@Composable
fun WikiScreen(
    dictionary: Hl7Dictionary?,
    loading: Boolean,
    version: String,
    samples: List<String>,
    onOpen: (String, String) -> Unit,
    onClose: () -> Unit,
) {
    val palette = LocalPalette.current
    val texts = CatalogTexts
    var query by remember { mutableStateOf("") }
    var path by remember { mutableStateOf(WikiBrowse.TYPES) }
    var typeId by remember { mutableStateOf<String?>(null) }
    var eventCode by remember { mutableStateOf<String?>(null) }
    var segmentName by remember { mutableStateOf<String?>(null) }
    var showExample by remember { mutableStateOf(false) }
    val types = remember(dictionary) { dictionary?.let(::wikiTypes).orEmpty() }
    val segments = remember(dictionary) { dictionary?.let(::wikiSegments).orEmpty() }
    val visible = remember(types, query) { filterWiki(types, query) }
    val visibleSegments = remember(segments, query) { filterSegments(segments, query) }
    val type = visible.firstOrNull { it.type == typeId } ?: visible.firstOrNull()
    val event = type?.events?.firstOrNull { it.code == eventCode } ?: type?.events?.firstOrNull()
    val german = LocalLanguage.current == Language.DE
    val guide = remember(dictionary, type?.type, event?.code, event?.structure, german) {
        if (dictionary == null || type == null || event == null) ""
        else wikiGuide(dictionary, type.type, event.code, event.structure, german)
    }
    val example = remember(dictionary, type?.type, event?.code, event?.structure, samples, showExample) {
        if (!showExample || dictionary == null || type == null || event == null) ""
        else wikiExample(dictionary, type.type, event.code, event.structure, samples)
    }
    Column(
        Modifier.fillMaxSize().background(palette.background).onPreviewKeyEvent { key ->
            if (key.type == KeyEventType.KeyDown && key.key == Key.Escape) {
                onClose()
                true
            } else false
        },
    ) {
        Row(
            Modifier.fillMaxWidth().background(palette.topBar).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Label(tr(texts.title), color = palette.text, weight = FontWeight.Bold, size = 16.sp)
            PathChip(tr(texts.messagePath), path == WikiBrowse.TYPES, visible.size.takeIf { query.isNotBlank() }) { path = WikiBrowse.TYPES }
            PathChip(tr(texts.segmentPath), path == WikiBrowse.SEGMENTS, visibleSegments.size.takeIf { query.isNotBlank() }) { path = WikiBrowse.SEGMENTS }
            Label(tr(texts.version, version), color = palette.textDim, size = 12.sp)
            TextInput(query, { query = it }, Modifier.widthIn(max = 420.dp).weight(1f), placeholder = tr(texts.search))
            IconAction(IconShape.Close, onClose, color = palette.text)
        }
        if (loading && dictionary == null) {
            Label(tr(texts.loading), Modifier.padding(16.dp), color = palette.textDim, size = 13.sp)
            return@Column
        }
        if (query.isNotBlank() && path == WikiBrowse.TYPES && visibleSegments.isNotEmpty()) {
            SearchSwitch(tr(texts.alsoSegments), visibleSegments.take(8).map { it.name }) { name ->
                path = WikiBrowse.SEGMENTS
                segmentName = name
            }
        }
        if (query.isNotBlank() && path == WikiBrowse.SEGMENTS && visible.isNotEmpty()) {
            SearchSwitch(tr(texts.alsoTypes), visible.take(8).map { it.type }) { name ->
                path = WikiBrowse.TYPES
                typeId = name
                eventCode = null
            }
        }
        if (path == WikiBrowse.SEGMENTS) {
            SegmentBrowser(visibleSegments, segmentName, query, german) { segmentName = it }
            return@Column
        }
        if (visible.isEmpty()) {
            Label(tr(texts.empty), Modifier.padding(16.dp), color = palette.textDim, size = 13.sp)
            return@Column
        }
        Row(Modifier.weight(1f).fillMaxWidth()) {
            WikiColumn(tr(texts.types), Modifier.width(300.dp)) {
                visible.forEach { item ->
                    val active = item.type == type?.type
                    Column(
                        Modifier.fillMaxWidth()
                            .background(if (active) palette.surfaceRaised else palette.surface)
                            .clickable { typeId = item.type; eventCode = null; showExample = false }
                            .pointerHoverIcon(PointerIcon.Hand)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Label(item.type, weight = FontWeight.SemiBold, color = if (active) palette.link else palette.text, size = 14.sp)
                        Label(tr(CatalogTexts.typeMeaning(item.type)), color = palette.textDim, size = 11.sp, maxLines = 4)
                    }
                }
            }
            WikiColumn(tr(texts.events), Modifier.width(320.dp)) {
                val events = type?.events.orEmpty()
                if (events.isEmpty()) Label(tr(texts.noEvents), Modifier.padding(12.dp), color = palette.textDim, size = 12.sp)
                events.forEach { item ->
                    val chosen = item.code == event?.code
                    Column(
                        Modifier.fillMaxWidth()
                            .background(if (chosen) palette.surfaceRaised else palette.surface)
                            .clickable { typeId = type?.type; eventCode = item.code; showExample = false }
                            .pointerHoverIcon(PointerIcon.Hand)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Label(item.code, weight = FontWeight.SemiBold, color = if (chosen) palette.link else palette.text, mono = true, size = 13.sp)
                        Label(tr(CatalogTexts.eventMeaning(item.code, item.description)), color = palette.textDim, size = 11.sp, maxLines = 3)
                    }
                }
            }
            Column(Modifier.weight(1f).fillMaxHeight()) {
                Row(
                    Modifier.fillMaxWidth().background(palette.surfaceRaised).padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        val headline = if (event == null) type?.type.orEmpty()
                        else tr(texts.aboutEvent, type?.type.orEmpty(), event.code, tr(CatalogTexts.eventMeaning(event.code, event.description)))
                        Label(headline, weight = FontWeight.SemiBold, size = 16.sp)
                        type?.type?.let { Label(tr(CatalogTexts.typeMeaning(it)), color = palette.textDim, size = 12.sp) }
                    }
                    ActionButton(
                        if (showExample) tr(texts.hideExample) else tr(texts.example),
                        { showExample = !showExample },
                        icon = IconShape.File,
                        primary = showExample,
                        enabled = event != null,
                    )
                }
                if (event == null || guide.isBlank()) {
                    Label(tr(texts.noStructure), Modifier.padding(16.dp), color = palette.textDim, size = 13.sp)
                } else {
                    Label(tr(texts.guide), Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = palette.textDim, size = 12.sp)
                    HighlightedMessage(guide, Modifier.weight(1f).fillMaxWidth())
                    if (showExample && example.isNotBlank()) {
                        Row(
                            Modifier.fillMaxWidth().background(palette.surfaceRaised).padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Label(tr(texts.example), Modifier.weight(1f), weight = FontWeight.SemiBold)
                            ActionButton(tr(texts.open), { onOpen(type?.type ?: "HL7", example) }, primary = true)
                        }
                        HighlightedMessage(example, Modifier.weight(1f).fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun PathChip(label: String, selected: Boolean, count: Int?, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val text = if (count != null) "$label ($count)" else label
    Label(
        text,
        Modifier
            .background(if (selected) palette.accent else palette.surfaceRaised, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        color = if (selected) androidx.compose.ui.graphics.Color.White else palette.text,
        size = 12.sp,
        weight = FontWeight.SemiBold,
        maxLines = 1,
    )
}

@Composable
private fun SearchSwitch(title: String, names: List<String>, onPick: (String) -> Unit) {
    val palette = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().background(palette.surface).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Label(title, color = palette.textDim, size = 12.sp, maxLines = 1)
        names.forEach { name ->
            Label(
                name,
                Modifier
                    .background(palette.surfaceRaised, RoundedCornerShape(4.dp))
                    .clickable { onPick(name) }
                    .pointerHoverIcon(PointerIcon.Hand)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                color = palette.link,
                size = 12.sp,
                mono = true,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ColumnScope.SegmentBrowser(segments: List<WikiSegmentView>, selectedName: String?, query: String, german: Boolean, onSelect: (String) -> Unit) {
    val palette = LocalPalette.current
    val texts = CatalogTexts
    var open by remember { mutableStateOf(setOf<String>()) }
    LaunchedEffect(selectedName) {
        if (!selectedName.isNullOrBlank()) open = open + selectedName
    }
    if (segments.isEmpty()) {
        Label(tr(texts.empty), Modifier.padding(16.dp), color = palette.textDim, size = 13.sp)
        return
    }
    val scroll = rememberScrollState()
    Box(Modifier.weight(1f).fillMaxWidth()) {
        Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(bottom = 16.dp)) {
            segments.forEach { segment ->
                val expanded = segment.name in open
                Row(
                    Modifier.fillMaxWidth()
                        .background(if (expanded) palette.surfaceRaised else palette.surface)
                        .clickable {
                            open = if (expanded) open - segment.name else open + segment.name
                            onSelect(segment.name)
                        }
                        .pointerHoverIcon(PointerIcon.Hand)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Label(if (expanded) "▾" else "▸", Modifier.width(14.dp), color = palette.text, size = 14.sp)
                    Label(segment.name, mono = true, weight = FontWeight.SemiBold, color = if (expanded) palette.link else palette.text, size = 15.sp)
                    Label(if (german) segment.descriptionDe else segment.descriptionEn, Modifier.weight(1f), color = palette.textDim, size = 12.sp, maxLines = 1)
                }
                if (expanded) {
                    Row(Modifier.fillMaxWidth().padding(start = 56.dp, end = 16.dp, top = 8.dp, bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Label("", Modifier.width(120.dp))
                        Label(tr(texts.purpose), Modifier.width(280.dp), color = palette.textDim, size = 11.sp, weight = FontWeight.SemiBold)
                        Label(tr(texts.pieceExample), color = palette.textDim, size = 11.sp, weight = FontWeight.SemiBold)
                    }
                    segment.pieces.forEach { piece ->
                        val hit = query.isNotBlank() && (
                            piece.code.contains(query, true) ||
                                piece.purposeEn.contains(query, true) ||
                                piece.purposeDe.contains(query, true) ||
                                piece.example.contains(query, true)
                            )
                        Row(
                            Modifier.fillMaxWidth()
                                .background(if (hit) palette.surfaceRaised else palette.background)
                                .padding(start = 56.dp, end = 16.dp, top = 5.dp, bottom = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Label(piece.code, Modifier.width(120.dp), mono = true, size = 13.sp, color = palette.link, weight = FontWeight.SemiBold, maxLines = 1)
                            Label(if (german) piece.purposeDe else piece.purposeEn, Modifier.width(280.dp), size = 13.sp, maxLines = 2)
                            Label(piece.example, mono = true, size = 13.sp, color = palette.text, maxLines = 1)
                        }
                    }
                }
            }
        }
        VerticalScrollbar(rememberScrollbarAdapter(scroll), Modifier.align(Alignment.CenterEnd).fillMaxHeight(), style = scrollbarStyle())
    }
}

@Composable
private fun HighlightedMessage(text: String, modifier: Modifier) {
    val palette = LocalPalette.current
    val styled = remember(text, palette) {
        Editor.highlight(
            text,
            EditorColors(
                palette.text,
                palette.delimiter,
                palette.componentDelimiter,
                palette.repetitionDelimiter,
                palette.subcomponentDelimiter,
                palette.warning,
                Theme::segmentColor,
            ),
        )
    }
    val horizontal = rememberScrollState()
    val vertical = rememberScrollState()
    val bar = scrollbarStyle()
    BoxWithConstraints(modifier.background(palette.input)) {
        Box(
            Modifier.size(maxWidth, maxHeight)
                .horizontalScroll(horizontal)
                .verticalScroll(vertical)
                .padding(start = 16.dp, top = 12.dp, end = 18.dp, bottom = 18.dp),
        ) {
            Text(
                styled,
                style = TextStyle(
                    color = palette.text,
                    fontFamily = LocalMonoFont.current,
                    fontSize = 13.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Normal,
                ),
                softWrap = false,
            )
        }
        VerticalScrollbar(rememberScrollbarAdapter(vertical), Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(bottom = 12.dp), style = bar)
        HorizontalScrollbar(rememberScrollbarAdapter(horizontal), Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(end = 12.dp), style = bar)
    }
}

@Composable
private fun scrollbarStyle(): ScrollbarStyle {
    val palette = LocalPalette.current
    return ScrollbarStyle(
        minimalHeight = 16.dp,
        thickness = 10.dp,
        shape = RoundedCornerShape(5.dp),
        hoverDurationMillis = 200,
        unhoverColor = palette.textDim.copy(alpha = 0.85f),
        hoverColor = palette.text,
    )
}

@Composable
private fun WikiColumn(title: String, modifier: Modifier, content: @Composable () -> Unit) {
    val palette = LocalPalette.current
    val scroll = rememberScrollState()
    Column(modifier.fillMaxHeight().background(palette.surface).border(1.dp, palette.border)) {
        Label(title, Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = palette.textDim, size = 11.sp, weight = FontWeight.SemiBold)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Column(Modifier.fillMaxSize().verticalScroll(scroll), content = { content() })
            VerticalScrollbar(rememberScrollbarAdapter(scroll), Modifier.align(Alignment.CenterEnd).fillMaxHeight(), style = scrollbarStyle())
        }
    }
}

