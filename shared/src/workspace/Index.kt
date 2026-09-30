// Composes every pane and dialog. The desktop window and the web host call Workspace.
package hl7lookup.workspace

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hl7lookup.acknowledgements.AcknowledgementsPanel
import hl7lookup.anonymize.Anonymize
import hl7lookup.anonymize.AnonymizeDialog
import hl7lookup.anonymize.AnonymizeScope
import hl7lookup.compare.CompareChoice
import hl7lookup.compare.CompareDialog
import hl7lookup.compare.CompareSource
import hl7lookup.compare.CompareTexts
import hl7lookup.controls.ActionButton
import hl7lookup.controls.CheckOption
import hl7lookup.controls.Dropdown
import hl7lookup.controls.EmptyState
import hl7lookup.controls.FormRow
import hl7lookup.controls.IconAction
import hl7lookup.controls.Label
import hl7lookup.catalog.WikiScreen
import hl7lookup.controls.MenuButton
import hl7lookup.controls.MenuEntry
import hl7lookup.dictionary.DictionaryState
import hl7lookup.layout.LayoutMenu
import hl7lookup.layout.LayoutPresetDialog
import hl7lookup.layout.Workbench
import hl7lookup.samples.SampleTexts
import hl7lookup.controls.Modal
import hl7lookup.controls.Notice
import hl7lookup.controls.Panel
import hl7lookup.controls.PanelHeader
import hl7lookup.controls.TabItem
import hl7lookup.controls.TabStrip
import hl7lookup.controls.TextInput
import hl7lookup.dictionary.Dictionaries
import hl7lookup.dictionary.Hl7Dictionary
import hl7lookup.document.Er7
import hl7lookup.document.FieldPath
import hl7lookup.document.ParsedMessage
import hl7lookup.editor.Editor
import hl7lookup.editor.Mark
import hl7lookup.editor.MarkKind
import hl7lookup.editor.RawEditor
import hl7lookup.engine.Engines
import hl7lookup.engine.Hl7Engine
import hl7lookup.engine.Hl7Transport
import hl7lookup.engine.Severity
import hl7lookup.filters.FilterDialog
import hl7lookup.filters.Filters
import hl7lookup.grid.FieldGrid
import hl7lookup.highlighting.HighlightDialog
import hl7lookup.highlighting.Highlights
import hl7lookup.i18n.LocalLanguage
import hl7lookup.i18n.tr
import hl7lookup.integrations.IntegrationsPanel
import hl7lookup.interfaces.InterfaceDefinition
import hl7lookup.interfaces.Interfaces
import hl7lookup.interfaces.InterfacesDialog
import hl7lookup.license.LicenseDialog
import hl7lookup.messages.MessageListPanel
import hl7lookup.motion.IllustrationKind
import hl7lookup.motion.StartupSplash
import hl7lookup.platform.LocalPlatform
import hl7lookup.platform.Platform
import hl7lookup.platform.PlatformKind
import hl7lookup.receivers.ReceiversPanel
import hl7lookup.samples.Samples
import hl7lookup.senders.SendersPanel
import hl7lookup.session.DocumentTab
import hl7lookup.session.DocumentTabs
import hl7lookup.settings.SettingsDialog
import hl7lookup.statistics.Statistics
import hl7lookup.statistics.StatisticsPanel
import hl7lookup.story.Stories
import hl7lookup.story.StoryContext
import hl7lookup.story.StoryView
import hl7lookup.theme.AppTheme
import hl7lookup.theme.Icon
import hl7lookup.theme.IconShape
import hl7lookup.theme.LocalPalette
import hl7lookup.theme.Palette
import hl7lookup.validation.Finding
import hl7lookup.validation.Validation
import hl7lookup.validation.ValidationPanel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Creates workspace state and type/event helpers. Desktop, web, and Index call it.
object Workspaces {
    // Builds WorkspaceState with platform, engine, and transport. Hosts call Workspaces.create.
    fun create(platform: Platform, engine: Hl7Engine, transport: Hl7Transport, scope: CoroutineScope, appVersion: String): WorkspaceState =
        WorkspaceState(platform, engine, transport, scope, appVersion)
    // Lists message types from a dictionary. New-message UI goes through Workspaces.
    fun types(dictionary: Hl7Dictionary?): List<String> = typesOf(dictionary)
    // Lists events for a message type. New-message UI goes through Workspaces.
    fun events(dictionary: Hl7Dictionary?, type: String): List<String> = eventsOf(dictionary, type)
    // Exposes WorkspaceTexts for other features. Callers that need wording use it.
    fun texts() = WorkspaceTexts
}

// Builds the root app shell and wires every pane. Desktop window and web host call it.
@Composable
fun Workspace(state: WorkspaceState, monoFont: FontFamily, logo: ImageBitmap? = null, uiFont: FontFamily = FontFamily.Default, modifier: Modifier = Modifier) {
    AppTheme(monoFont, uiFont) {
        CompositionLocalProvider(LocalLanguage provides state.settings.current.language, LocalPlatform provides state.platform) {
            if (state.platform.kind == PlatformKind.DESKTOP) StartupSplash { WorkspaceRoot(state, modifier, logo) }
            else WorkspaceRoot(state, modifier, logo)
        }
    }
}

// Lays out bars, panes, and dialogs over WorkspaceState. Workspace calls it after theme setup.
@Composable
private fun WorkspaceRoot(state: WorkspaceState, modifier: Modifier, logo: ImageBitmap?) {
    val palette = LocalPalette.current
    val settings = state.settings.current
    val session = state.session
    val tab = session.active
    val entry = session.currentMessage
    val text = entry?.text ?: ""
    val message = remember(text) { Er7.parse(text) }
    val version = state.dictionaries.resolveVersion(Er7.header(message).version, settings.version)
    val dictionaryState = state.dictionaries.state(version)
    val dictionary = (dictionaryState as? DictionaryState.Ready)?.dictionary
    val iface = state.activeInterface()

    LaunchedEffect(Unit) { state.startPolling() }
    LaunchedEffect(session.revision) {
        delay(400)
        session.persist()
        state.refreshSearch()
    }
    LaunchedEffect(text, settings.autoValidate) {
        if (settings.autoValidate) {
            delay(350)
            state.inspect(text)
        }
    }
    LaunchedEffect(iface) { state.highlights.replaceFromSource("interface", Interfaces.highlights(iface)) }
    LaunchedEffect(state.note?.id) {
        if (state.note != null) {
            delay(7000)
            state.dismissNote()
        }
    }

    val report = if (state.reportText == text) state.report else null
    val findings = remember(message, dictionary, report, iface, settings.autoValidate) {
        val rules = Interfaces.findings(iface, message)
        if (!settings.autoValidate) rules else Validation.merge(Validation.check(message, dictionary), Validation.fromEngine(message, report), rules)
    }
    val rules = state.highlights.rules.toList()
    val hits = remember(message, rules) { Highlights.hits(rules, message) }
    val searchMarks = run {
        val current = state.searchHits.getOrNull(state.searchIndex)
        val ordinal = if (current != null && current.tabId == tab?.id && current.index == tab.current) current.ordinal else -1
        if (state.searchQuery.isBlank()) emptyList() else Editor.searchMarks(message, state.searchQuery, ordinal, palette.search, palette.selectedBorder.copy(alpha = 0.6f))
    }
    val marks = remember(message, hits, findings, searchMarks, palette) {
        val highlightMarks = hits.mapNotNull { hit -> Er7.span(message, hit.path)?.let { Mark(it, MarkKind.HIGHLIGHT, hl7lookup.theme.Theme.highlight(hit.color).copy(alpha = 0.35f)) } }
        val findingMarks = findings.mapNotNull { f ->
            val span = f.path?.let { Er7.span(message, it) } ?: return@mapNotNull null
            if (span.length == 0) null
            else Mark(span, if (f.severity == Severity.ERROR) MarkKind.ERROR else MarkKind.WARNING, Validation.color(f.severity, palette))
        }
        highlightMarks + findingMarks + searchMarks
    }
    val view = View(
        tab, text, message, version, dictionary, dictionaryState is DictionaryState.Loading, iface, findings,
        highlight = { path -> Highlights.color(hits, path) },
        flag = { path -> Validation.severityAt(findings, path)?.let { Validation.color(it, palette) } },
        marks = marks,
        tableOverride = { id -> Interfaces.table(iface, id) },
    )

    Box(modifier.fillMaxSize()) {
    Column(
        Modifier.fillMaxSize().background(palette.background).onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            val command = event.isCtrlPressed || event.isMetaPressed
            when {
                command && event.key == Key.O -> { state.openFile(); true }
                command && event.key == Key.S -> { state.save(event.isShiftPressed); true }
                command && event.key == Key.T -> { state.newTab(); true }
                event.key == Key.F3 -> { state.step(if (event.isShiftPressed) -1 else 1); true }
                else -> false
            }
        },
    ) {
        TopBar(state, view, logo)
        DocumentTabs(session, onNewTab = state::newTab)
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            if (maxWidth < 900.dp) CompactLayout(state, view) else WideLayout(state, view)
        }
        StatusBar(state, view)
    }
    if (state.dialog == DialogKind.CATALOG) {
        val version = state.settings.current.version
        val dictState = state.dictionaries.state(version)
        WikiScreen(
            (dictState as? DictionaryState.Ready)?.dictionary,
            loading = dictState is DictionaryState.Loading,
            version = version,
            samples = Samples.list().mapNotNull { Samples.text(it.id) },
            onOpen = { type, example ->
                state.session.addTab(type, listOf(example), hl7lookup.session.MessageOrigin.SAMPLE)
                state.dialog = DialogKind.NONE
            },
            onClose = { state.dialog = DialogKind.NONE },
            logo = logo,
        )
    }
    Dialogs(state, view)
    }
}

// Renders menus, send, and search. WorkspaceRoot places it at the top.
@Composable
private fun TopBar(state: WorkspaceState, view: View, logo: ImageBitmap?) {
    val palette = LocalPalette.current
    val platform = LocalPlatform.current
    val tab = view.tab
    val file = buildList {
        add(MenuEntry(tr(WorkspaceTexts.newTab)) { state.newTab() })
        add(MenuEntry(tr(WorkspaceTexts.open)) { state.openFile() })
        add(MenuEntry(tr(WorkspaceTexts.paste)) { state.paste() })
        add(MenuEntry(tr(WorkspaceTexts.newMessage)) { state.dialog = DialogKind.NEW_MESSAGE })
        add(MenuEntry(tr(SampleTexts.menu), children = Samples.list().map { sample ->
            MenuEntry(tr(sample.title)) { state.loadSample(sample.id) }
        } + MenuEntry(tr(WorkspaceTexts.allSamples)) { state.loadAllSamples() }))
        if (platform.kind == PlatformKind.DESKTOP) {
            add(MenuEntry(tr(WorkspaceTexts.save), enabled = tab?.messages?.isNotEmpty() == true) { state.save(false) })
            add(MenuEntry(tr(WorkspaceTexts.saveAs), enabled = tab?.messages?.isNotEmpty() == true) { state.save(true) })
        } else {
            add(MenuEntry(tr(WorkspaceTexts.download), enabled = tab?.messages?.isNotEmpty() == true) { state.save(true) })
        }
        add(MenuEntry(tr(WorkspaceTexts.copy), enabled = view.text.isNotBlank()) { state.copyCurrent() })
        add(MenuEntry(tr(WorkspaceTexts.closeTab), enabled = tab != null) { tab?.let { state.session.closeTab(it.id) } })
    }
    val tools = listOf(
        MenuEntry(tr(WorkspaceTexts.filters)) { state.dialog = DialogKind.FILTER },
        MenuEntry(tr(WorkspaceTexts.clearFilter), enabled = state.filters.filter.active) { state.filters.clear() },
        MenuEntry(tr(WorkspaceTexts.highlighting)) { state.dialog = DialogKind.HIGHLIGHT },
        MenuEntry(tr(WorkspaceTexts.compare), enabled = view.text.isNotBlank()) { state.dialog = DialogKind.COMPARE },
        MenuEntry(tr(WorkspaceTexts.sendToTab), enabled = view.text.isNotBlank()) { state.dialog = DialogKind.SEND_TO_TAB },
        MenuEntry(tr(WorkspaceTexts.ack), enabled = view.text.isNotBlank()) { state.dialog = DialogKind.ACK },
        MenuEntry(tr(WorkspaceTexts.settings)) { state.dialog = DialogKind.SETTINGS },
        MenuEntry(tr(WorkspaceTexts.license)) { state.dialog = DialogKind.LICENSE },
    )
    val interfaceTexts = Interfaces.texts()
    val interfaceMenu = buildList {
        add(MenuEntry(tr(interfaceTexts.manage)) { state.dialog = DialogKind.INTERFACES })
        add(MenuEntry(tr(interfaceTexts.import)) { state.importInterfaces() })
        add(MenuEntry(tr(interfaceTexts.export), enabled = view.iface != null) { view.iface?.let { state.exportInterfaces(listOf(it)) } })
        state.interfaces.items.forEach { definition ->
            add(MenuEntry((if (tab?.interfaceId == definition.id) "● " else "") + tr(WorkspaceTexts.useInterface, definition.name)) { state.useInterface(definition.id) })
        }
        add(MenuEntry(tr(WorkspaceTexts.noInterface), enabled = tab?.interfaceId != null) { state.useInterface(null) })
    }
    val anonymizeTexts = Anonymize.texts()
    val anonymize = listOf(
        MenuEntry(tr(anonymizeTexts.current), enabled = view.text.isNotBlank()) { state.anonymizeScope = AnonymizeScope.CURRENT; state.dialog = DialogKind.ANONYMIZE },
        MenuEntry(tr(anonymizeTexts.tab), enabled = (tab?.messages?.size ?: 0) > 0) { state.anonymizeScope = AnonymizeScope.TAB; state.dialog = DialogKind.ANONYMIZE },
    )
    Row(
        Modifier.fillMaxWidth().background(palette.topBar).padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        logo?.let {
            Image(it, tr(WorkspaceTexts.appName), Modifier.padding(start = 4.dp).size(26.dp), contentScale = ContentScale.Fit)
        }
        Label(tr(WorkspaceTexts.appName), Modifier.padding(horizontal = 8.dp), color = Color.White, weight = FontWeight.Bold, size = 14.sp, maxLines = 1)
        MenuButton(tr(WorkspaceTexts.file), file)
        MenuButton(tr(WorkspaceTexts.tools), tools)
        MenuButton(tr(interfaceTexts.menu), interfaceMenu)
        MenuButton(tr(anonymizeTexts.menu), anonymize)
        Row(
            Modifier.clip(RoundedCornerShape(3.dp)).clickable { state.dialog = DialogKind.CATALOG }.pointerHoverIcon(PointerIcon.Hand).padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Label(tr(hl7lookup.catalog.CatalogTexts.menu), color = Color.White, size = 13.sp, maxLines = 1)
        }
        LayoutMenu(state.layouts)
        Spacer(Modifier.weight(1f))
        val integrations = state.integrations
        val activeIntegration = integrations.get(integrations.active.integrationId)
        if (integrations.items.isNotEmpty()) {
            val noIntegration = tr(WorkspaceTexts.noIntegration)
            Dropdown(activeIntegration, listOf(null) + integrations.items, { it?.name ?: noIntegration }, { integrations.activate(it?.id) }, Modifier.width(170.dp))
        }
        if (state.senders.items.isNotEmpty()) {
            ActionButton(tr(WorkspaceTexts.send), { state.sendActive(false) }, icon = IconShape.Send, enabled = view.text.isNotBlank() && !state.busy)
        }
        Spacer(Modifier.width(8.dp))
        SearchBox(state)
    }
}

// Cross-tab search field and hit stepping. TopBar embeds it.
@Composable
private fun SearchBox(state: WorkspaceState) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(IconShape.Search, Color.White.copy(alpha = 0.8f), size = 13.dp)
        TextInput(state.searchQuery, { state.search(it); if (it.isNotBlank()) state.step(0) }, Modifier.width(220.dp), placeholder = tr(WorkspaceTexts.search), onSubmit = { state.step(1) })
        val count = state.searchHits.size
        if (state.searchQuery.isNotBlank()) {
            Label(if (count == 0) tr(WorkspaceTexts.searchNone) else tr(WorkspaceTexts.searchCount, state.searchIndex + 1, count), color = Color.White.copy(alpha = 0.85f), size = 11.sp, maxLines = 1)
            IconAction(IconShape.ChevronLeft, { state.step(-1) }, color = Color.White, enabled = count > 0)
            IconAction(IconShape.ChevronRight, { state.step(1) }, color = Color.White, enabled = count > 0)
            IconAction(IconShape.Close, { state.search("") }, color = Color.White)
        }
    }
}

// Multi-pane workbench for wide windows. WorkspaceRoot picks it over CompactLayout.
@Composable
private fun WideLayout(state: WorkspaceState, view: View) {
    Workbench(
        state.layouts,
        mapOf(
            "story" to { StoryPanel(state, view) },
            "editor" to { EditorPanel(state, view) },
            "grid" to { GridPanel(state, view) },
            "session" to { SessionPanel(state, view) },
            "side" to { SidePanel(state, view) },
        ),
        Modifier.fillMaxSize(),
    )
    LayoutPresetDialog(state.layouts)
}

// Tabbed single-pane layout for narrow widths. WorkspaceRoot picks it under 900.dp.
@Composable
private fun CompactLayout(state: WorkspaceState, view: View) {
    var pane by remember { mutableStateOf("editor") }
    val tabs = listOf(
        TabItem("story", tr(WorkspaceTexts.interpretation)),
        TabItem("editor", tr(WorkspaceTexts.rawText)),
        TabItem("grid", tr(WorkspaceTexts.grid)),
        TabItem("session", tr(WorkspaceTexts.messages)),
        TabItem("side", tr(WorkspaceTexts.statistics)),
    )
    Column(Modifier.fillMaxSize()) {
        TabStrip(tabs, pane, { pane = it })
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (pane) {
                "story" -> StoryPanel(state, view)
                "grid" -> GridPanel(state, view)
                "session" -> SessionPanel(state, view)
                "side" -> SidePanel(state, view)
                else -> EditorPanel(state, view)
            }
        }
    }
}

// Shows the human-readable story for the message. WideLayout and CompactLayout host it.
@Composable
private fun StoryPanel(state: WorkspaceState, view: View) {
    val settings = state.settings.current
    val language = LocalLanguage.current
    val palette = LocalPalette.current
    val scope = rememberCoroutineScope()
    val story = remember(view.message, view.dictionary, settings, language) {
        if (view.message.isEmpty) null
        else Stories.build(view.message, StoryContext(view.dictionary, settings.dateStyle, state.platform.nowMillis(), language, settings.relativeDates))
    }
    val selection = state.session.selection
    Panel(Modifier.fillMaxSize()) {
        PanelHeader(tr(WorkspaceTexts.interpretation)) {
            if (story != null) IconAction(IconShape.Download, { scope.launch { state.platform.writeClipboard(Stories.plain(story)) } }, color = palette.textDim)
        }
        if (view.dictionaryLoading && !view.message.isEmpty) Label(tr(WorkspaceTexts.loadingDefinitions), Modifier.padding(8.dp), color = palette.textDim, size = 11.sp)
        StoryView(
            story,
            Modifier.weight(1f),
            isSelected = { path -> selection != null && (Er7.same(path, selection) || Er7.contains(path, selection)) },
            highlight = view.highlight,
            flagged = view.flag,
            onPick = { path -> state.session.selection = path },
        )
    }
}

// Raw ER7 editor with marks and empty state. WideLayout and CompactLayout host it.
@Composable
private fun EditorPanel(state: WorkspaceState, view: View) {
    val palette = LocalPalette.current
    val focus = state.focus
    Panel(Modifier.fillMaxSize()) {
        val entry = state.session.currentMessage
        val tab = view.tab
        PanelHeader(
            listOfNotNull(
                tr(WorkspaceTexts.rawText),
                tab?.takeIf { it.messages.size > 1 }?.let { "${it.current + 1}/${it.messages.size}" },
                Er7.header(view.message).let { h -> listOf(h.type, h.event).filter { it.isNotBlank() }.joinToString("^") }.ifBlank { null },
            ).joinToString("  ·  "),
        ) {
            if (tab != null && tab.messages.size > 1) {
                IconAction(IconShape.ChevronLeft, { state.session.selectMessage(tab.id, tab.current - 1) }, enabled = tab.current > 0, color = palette.text)
                IconAction(IconShape.ChevronRight, { state.session.selectMessage(tab.id, tab.current + 1) }, enabled = tab.current < tab.messages.lastIndex, color = palette.text)
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            RawEditor(
                text = view.text,
                message = view.message,
                dictionary = view.dictionary,
                selection = state.session.selection,
                marks = view.marks,
                style = state.settings.current.dateStyle,
                tableOverride = view.tableOverride,
                focusOffset = focus?.offset,
                focusKey = focus?.sequence ?: 0L,
                onTextChange = state.session::updateCurrentText,
                onCursor = { state.session.selection = it },
                modifier = Modifier.fillMaxSize(),
            )
            if (entry == null && tab?.messages.isNullOrEmpty()) {
                EmptyState(tr(WorkspaceTexts.emptyEditor), illustration = IllustrationKind.MESSAGE) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionButton(tr(WorkspaceTexts.open), { state.openFile() }, icon = IconShape.Upload)
                        ActionButton(tr(WorkspaceTexts.paste), { state.paste() })
                        ActionButton(tr(WorkspaceTexts.openSample), { Samples.list().firstOrNull()?.let { state.loadSample(it.id) } }, primary = true)
                    }
                }
            }
        }
    }
}

// Field grid for the active message. WideLayout and CompactLayout host it.
@Composable
private fun GridPanel(state: WorkspaceState, view: View) {
    val settings = state.settings.current
    Panel(Modifier.fillMaxSize()) {
        PanelHeader(tr(WorkspaceTexts.grid))
        FieldGrid(
            message = view.message,
            dictionary = view.dictionary,
            selection = state.session.selection,
            showEmpty = settings.showEmptyFields,
            onShowEmpty = { show -> state.settings.update { it.copy(showEmptyFields = show) } },
            tableOverride = view.tableOverride,
            highlight = view.highlight,
            flag = view.flag,
            onSelect = { state.session.selection = it },
            onTextChange = state.session::updateCurrentText,
            modifier = Modifier.weight(1f),
        )
    }
}

// Bottom tabs for messages, senders, receivers, integrations, ACKs. Layouts host it.
@Composable
private fun SessionPanel(state: WorkspaceState, view: View) {
    val palette = LocalPalette.current
    val tab = view.tab
    val settings = state.settings.current
    val tabs = listOf(
        TabItem(BottomTab.MESSAGES.name, tr(WorkspaceTexts.messages), badge = tab?.messages?.size?.takeIf { it > 0 }?.toString()),
        TabItem(BottomTab.SENDERS.name, tr(WorkspaceTexts.senders)),
        TabItem(BottomTab.RECEIVERS.name, tr(WorkspaceTexts.receivers), badge = state.receivers.statuses.values.count { it.running }.takeIf { it > 0 }?.toString()),
        TabItem(BottomTab.INTEGRATIONS.name, tr(WorkspaceTexts.integrations)),
        TabItem(BottomTab.ACKS.name, tr(WorkspaceTexts.acks), badge = state.acks.entries.size.takeIf { it > 0 }?.toString()),
    )
    val newId = { state.session.newId("c") }
    val tabChoices = state.tabChoices()
    Panel(Modifier.fillMaxSize()) {
        TabStrip(tabs, state.bottomTab.name, { state.bottomTab = BottomTab.valueOf(it) }) {
            if (state.bottomTab == BottomTab.MESSAGES && state.filters.filter.active) {
                IconAction(IconShape.Filter, { state.dialog = DialogKind.FILTER }, color = palette.warning)
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (state.bottomTab) {
                BottomTab.MESSAGES -> {
                    if (tab == null || tab.messages.isEmpty()) EmptyState(tr(WorkspaceTexts.emptyEditor), illustration = IllustrationKind.MESSAGES)
                    else {
                        val filter = state.filters.filter
                        val visible = remember(tab.messages, filter, settings.dateStyle) {
                            tab.messages.indices.filter { Filters.accepts(filter, tab.messages[it].text, settings.dateStyle) }
                        }
                        val rules = state.highlights.rules.toList()
                        Column(Modifier.fillMaxSize()) {
                            if (filter.active) Label(tr(WorkspaceTexts.filtered, visible.size, tab.messages.size), Modifier.padding(horizontal = 8.dp, vertical = 3.dp), color = palette.warning, size = 11.sp)
                            MessageListPanel(
                                messages = tab.messages,
                                visible = visible,
                                current = tab.current,
                                style = settings.dateStyle,
                                dictionary = state::dictionaryForVersion,
                                marker = { entry -> Highlights.messageColor(rules, entry.text) },
                                onSelect = { state.session.selectMessage(tab.id, it) },
                                onRemove = { state.session.removeMessage(tab.id, it) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                BottomTab.SENDERS -> SendersPanel(
                    state.senders, state.integrations.active.senderId, tabChoices, newId,
                    onActivate = state.integrations::activateSender,
                    onSend = { sender, all -> state.send(sender, all) },
                    onTest = { state.test(it) },
                )
                BottomTab.RECEIVERS -> ReceiversPanel(
                    state.receivers, state.integrations.active.receiverId, tabChoices, newId,
                    onActivate = state.integrations::activateReceiver,
                    onToggle = { receiver, start -> state.toggle(receiver, start) },
                )
                BottomTab.INTEGRATIONS -> IntegrationsPanel(
                    state.integrations,
                    state.senders.items.map { it.id to it.name },
                    state.receivers.items.map { it.id to it.name },
                    state.interfaces.items.map { it.id to it.name },
                    newId,
                    onActivate = { state.integrations.activate(it) },
                )
                BottomTab.ACKS -> AcknowledgementsPanel(
                    state.acks, settings.dateStyle,
                    nowIso = { millis -> hl7lookup.datetime.Hl7Dates.now(millis) },
                    onOpen = state::openAck,
                )
            }
        }
    }
}

// Side tabs for statistics and validation. WideLayout and CompactLayout host it.
@Composable
private fun SidePanel(state: WorkspaceState, view: View) {
    val palette = LocalPalette.current
    val counts = Validation.counts(view.findings)
    val problems = (counts[Severity.ERROR] ?: 0) + (counts[Severity.WARNING] ?: 0)
    val tabs = listOf(
        TabItem(SideTab.STATISTICS.name, tr(WorkspaceTexts.statistics)),
        TabItem(SideTab.VALIDATION.name, tr(WorkspaceTexts.validation), badge = problems.takeIf { it > 0 }?.toString()),
    )
    Panel(Modifier.fillMaxSize()) {
        TabStrip(tabs, state.sideTab.name, { state.sideTab = SideTab.valueOf(it) })
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (state.sideTab) {
                SideTab.STATISTICS -> StatisticsSection(state, view)
                SideTab.VALIDATION -> ValidationPanel(
                    view.findings,
                    definitionsReady = view.dictionary != null,
                    engineOnline = state.engineOnline,
                    selected = state.session.selection,
                    onPick = { state.session.selection = it },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

// Field value statistics across the tab. SidePanel shows it for STATISTICS.
@Composable
private fun StatisticsSection(state: WorkspaceState, view: View) {
    val tab = view.tab
    val selection = state.session.selection
    val spec = selection?.let { Er7.spec(view.message, it) }
    if (tab == null || spec == null || selection.field == 0) {
        EmptyState(tr(WorkspaceTexts.selectField), illustration = IllustrationKind.STATISTICS)
        return
    }
    val texts = tab.messages.map { it.text }
    val statistics = remember(texts, spec) { Statistics.compute(texts, spec) }
    val segmentName = view.message.segments.getOrNull(selection.segment)?.name.orEmpty()
    val node = Dictionaries.node(view.dictionary, segmentName, selection.field, selection.component, selection.subcomponent)
    val table = node?.table
    StatisticsPanel(
        statistics,
        fieldName = listOfNotNull(Er7.label(spec), node?.name).joinToString(" "),
        modifier = Modifier.fillMaxSize(),
        describe = { value -> view.tableOverride(table)?.entries?.firstOrNull { it.code == value }?.description ?: Dictionaries.code(view.dictionary, table, value) },
        onPick = { row -> state.session.selectMessage(tab.id, row.firstMessage) },
    )
}

// Engine status, findings, and notes. WorkspaceRoot places it at the bottom.
@Composable
private fun StatusBar(state: WorkspaceState, view: View) {
    val palette = LocalPalette.current
    val counts = Validation.counts(view.findings)
    Row(
        Modifier.fillMaxWidth().background(palette.surfaceRaised).padding(horizontal = 10.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        val online = state.engineOnline && state.dictionaries.online
        Icon(if (online) IconShape.Check else IconShape.Warning, if (online) palette.success else palette.warning, size = 11.dp)
        Label(if (online) tr(WorkspaceTexts.engineOnline, view.version) else tr(WorkspaceTexts.engineOffline), color = if (online) palette.textDim else palette.warning, size = 11.sp, maxLines = 1)
        if (!view.message.isEmpty) {
            Label(tr(WorkspaceTexts.findings, counts[Severity.ERROR] ?: 0, counts[Severity.WARNING] ?: 0), color = if ((counts[Severity.ERROR] ?: 0) > 0) palette.error else palette.textDim, size = 11.sp, maxLines = 1)
        }
        view.iface?.let { Label(tr(WorkspaceTexts.interfaceActive, it.name), color = palette.link, size = 11.sp, maxLines = 1) }
        Spacer(Modifier.weight(1f))
        state.note?.let { note ->
            val color = when (note.severity) {
                Severity.ERROR -> palette.error
                Severity.WARNING -> palette.warning
                Severity.INFO -> palette.text
            }
            Label(tr(note.text, *note.args.toTypedArray()), color = color, size = 11.sp, maxLines = 1)
            IconAction(IconShape.Close, state::dismissNote, color = palette.textDim, size = 10.dp)
        }
    }
}

// Routes DialogKind to the matching modal. WorkspaceRoot overlays it.
@Composable
private fun Dialogs(state: WorkspaceState, view: View) {
    val close = { state.dialog = DialogKind.NONE }
    val tab = view.tab
    when (state.dialog) {
        DialogKind.NONE -> Unit
        DialogKind.SETTINGS -> SettingsDialog(state.settings, state.dictionaries.versions, close)
        DialogKind.HIGHLIGHT -> HighlightDialog(
            state.highlights, { state.session.newId("hl") },
            suggestion = state.session.selection?.takeIf { !view.message.isEmpty }?.let { Er7.spec(view.message, it) }?.let(Er7::label),
            onDismiss = close,
        )
        DialogKind.FILTER -> FilterDialog(state.filters, state.settings.current.dateStyle, close)
        DialogKind.COMPARE -> CompareSection(state, view, close)
        DialogKind.ANONYMIZE -> AnonymizeDialog(
            state.anonymizer, state.anonymizeScope, tab?.messages?.size ?: 0,
            texts = state::anonymizeInput,
            dictionary = state::dictionaryFor,
            onResult = state::applyAnonymized,
            onDismiss = close,
        )
        DialogKind.INTERFACES -> InterfacesDialog(
            state.interfaces, tab?.interfaceId, state.dictionaries.versions, view.message, tab?.messages?.map { it.text }.orEmpty(),
            newId = { state.session.newId("if") },
            onUseForTab = state::useInterface,
            onImport = { state.importInterfaces() },
            onExport = { state.exportInterfaces(it) },
            onPickMessage = { index -> tab?.let { state.session.selectMessage(it.id, index) } },
            onDismiss = close,
        )
        DialogKind.LICENSE -> LicenseDialog(state.appVersion, close)
        DialogKind.NEW_MESSAGE -> NewMessageDialog(state, close)
        DialogKind.SEND_TO_TAB -> SendToTabDialog(state, view, close)
        DialogKind.DIAGNOSIS -> DiagnosisDialog(state, close)
        DialogKind.ACK -> AckDialog(state, close)
        DialogKind.CATALOG -> Unit
    }
}

// Builds compare choices and opens CompareDialog. Dialogs opens it for COMPARE.
@Composable
private fun CompareSection(state: WorkspaceState, view: View, close: () -> Unit) {
    val choices = mutableListOf<CompareChoice>()
    for (tab in state.session.tabs) {
        tab.messages.forEachIndexed { index, entry ->
            val header = Er7.header(Er7.parse(entry.text))
            val summary = listOf(listOf(header.type, header.event).filter { it.isNotBlank() }.joinToString("^"), header.controlId).filter { it.isNotBlank() }.joinToString(" ")
            choices += CompareChoice(CompareSource(tab.id, index), tr(CompareTexts.source, tab.title, index + 1, summary))
        }
    }
    val tab = view.tab
    val left = tab?.let { CompareSource(it.id, it.current) }
    val right = tab?.let { t ->
        when {
            t.messages.size > 1 -> CompareSource(t.id, if (t.current > 0) t.current - 1 else 1)
            else -> choices.firstOrNull { it.source?.tabId != t.id }?.source
        }
    }
    CompareDialog(
        choices, left, right,
        textOf = { source -> state.session.tab(source.tabId)?.messages?.getOrNull(source.index)?.text },
        describe = { message, path ->
            val segment = message.segments.getOrNull(path.segment)?.name ?: return@CompareDialog null
            Dictionaries.node(state.dictionaryFor(message.text), segment, path.field, path.component)?.name
        },
        findings = { message -> Interfaces.findings(view.iface, message) },
        onPickLeft = { source, path ->
            state.session.activate(source.tabId)
            state.session.selectMessage(source.tabId, source.index)
            state.session.selection = path
            close()
        },
        onDismiss = close,
    )
}

// Picks type, event, and version then creates a message. Dialogs opens it.
@Composable
private fun NewMessageDialog(state: WorkspaceState, close: () -> Unit) {
    val palette = LocalPalette.current
    var version by remember { mutableStateOf(state.dictionaries.resolveVersion(state.settings.current.version, Engines.defaultVersion())) }
    val dictionary = state.dictionaryForVersion(version)
    val types = typesOf(dictionary)
    var type by remember { mutableStateOf("ADT") }
    val events = eventsOf(dictionary, type)
    var event by remember { mutableStateOf("A01") }
    if (events.isNotEmpty() && event !in events) event = events.first()
    Modal(tr(WorkspaceTexts.newMessageTitle), close, width = 520.dp, actions = {
        ActionButton(tr(WorkspaceTexts.create), { state.create(type, event, version) }, primary = true, enabled = !state.busy && type.isNotBlank() && event.isNotBlank())
    }) {
        FormRow(tr(WorkspaceTexts.hl7Version)) { Dropdown(version, state.dictionaries.versions, { it }, { version = it }) }
        if (dictionary == null) Label(tr(WorkspaceTexts.loadingDefinitions), color = palette.textDim, size = 12.sp)
        FormRow(tr(WorkspaceTexts.messageType)) {
            Dropdown(type, types.ifEmpty { listOf(type) }, { t -> listOfNotNull(t, Dictionaries.messageType(dictionary, t)).joinToString(" – ") }, { type = it }, Modifier.weight(1f))
        }
        FormRow(tr(WorkspaceTexts.event)) {
            Dropdown(event, events.ifEmpty { listOf(event) }, { e -> listOfNotNull(e, Dictionaries.event(dictionary, type, e)).joinToString(" – ") }, { event = it }, Modifier.weight(1f))
        }
        if (!state.engineOnline) Notice(tr(WorkspaceTexts.engineOffline), palette.warning)
    }
}

// Chooses a target tab and copies messages. Dialogs opens it for SEND_TO_TAB.
@Composable
private fun SendToTabDialog(state: WorkspaceState, view: View, close: () -> Unit) {
    val tab = view.tab ?: return
    val others = state.session.tabs.filter { it.id != tab.id }
    val newTarget = tr(WorkspaceTexts.newTargetTab)
    var target by remember { mutableStateOf(others.firstOrNull()?.id) }
    var all by remember { mutableStateOf(false) }
    Modal(tr(WorkspaceTexts.sendToTabTitle), close, width = 460.dp, actions = {
        ActionButton(tr(WorkspaceTexts.send), { state.sendToTab(target, all); close() }, primary = true, icon = IconShape.Send)
    }) {
        FormRow(tr(WorkspaceTexts.targetTab)) {
            Dropdown(target, listOf<String?>(null) + others.map { it.id }, { id -> others.firstOrNull { it.id == id }?.title ?: newTarget }, { target = it }, Modifier.weight(1f))
        }
        CheckOption(tr(WorkspaceTexts.currentOnly), !all, { all = false })
        CheckOption(tr(WorkspaceTexts.allInTab, tab.messages.size), all, { all = true })
    }
}

// Shows connection test steps from WorkspaceState.diagnosis. Dialogs opens it.
@Composable
private fun DiagnosisDialog(state: WorkspaceState, close: () -> Unit) {
    val palette = LocalPalette.current
    val (sender, diagnosis) = state.diagnosis ?: return
    Modal(tr(WorkspaceTexts.diagnosisTitle, sender.name), close, width = 520.dp) {
        for (step in diagnosis.steps) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (step.ok) IconShape.Check else IconShape.Close, if (step.ok) palette.success else palette.error, size = 12.dp)
                Label(tr(Engines.stepText(step.kind)), Modifier.width(150.dp), weight = FontWeight.SemiBold, size = 12.sp)
                Label(step.detail, Modifier.weight(1f), size = 12.sp, mono = true)
                Label("${step.millis} ms", color = palette.textDim, size = 11.sp)
            }
        }
        val cause = diagnosis.cause
        if (cause == null) Notice(tr(WorkspaceTexts.diagnosisOk), palette.success, icon = IconShape.Check)
        else Notice(tr(WorkspaceTexts.diagnosisCause, tr(Engines.causeText(cause))) + (diagnosis.detail?.let { " — $it" } ?: ""), palette.error)
    }
}

// Form to generate an ACK for the current message. Dialogs opens it for ACK.
@Composable
private fun AckDialog(state: WorkspaceState, close: () -> Unit) {
    val palette = LocalPalette.current
    var code by remember { mutableStateOf("AA") }
    var error by remember { mutableStateOf("") }
    var intoTab by remember { mutableStateOf(false) }
    Modal(tr(WorkspaceTexts.ackTitle), close, width = 620.dp, actions = {
        ActionButton(tr(WorkspaceTexts.ackGenerate), { state.acknowledge(code, error.ifBlank { null }, intoTab) }, primary = true)
    }) {
        FormRow(tr(WorkspaceTexts.ackCode)) { Dropdown(code, ackCodes, { it }, { code = it }) }
        FormRow(tr(WorkspaceTexts.ackError)) { TextInput(error, { error = it }, Modifier.weight(1f)) }
        CheckOption(tr(WorkspaceTexts.ackIntoTab), intoTab, { intoTab = it })
        state.lastAck?.let { ack ->
            Label(tr(WorkspaceTexts.ackPreview), color = palette.textDim, size = 12.sp)
            Box(Modifier.fillMaxWidth().background(palette.input).padding(8.dp)) { Label(ack, mono = true, size = 12.sp) }
        }
        if (!state.engineOnline) Notice(tr(WorkspaceTexts.engineOffline), palette.warning)
    }
}
