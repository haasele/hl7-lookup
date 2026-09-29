package hl7lookup.workspace

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import hl7lookup.acknowledgements.AckStore
import hl7lookup.acknowledgements.Acknowledgements
import hl7lookup.acknowledgements.Direction
import hl7lookup.anonymize.AnonymizeOptions
import hl7lookup.anonymize.AnonymizeScope
import hl7lookup.anonymize.Anonymizer
import hl7lookup.dictionary.DictionaryStore
import hl7lookup.dictionary.Hl7Dictionary
import hl7lookup.document.Er7
import hl7lookup.document.Span
import hl7lookup.engine.AckRequest
import hl7lookup.engine.CreateRequest
import hl7lookup.engine.Diagnosis
import hl7lookup.engine.EngineResult
import hl7lookup.engine.Engines
import hl7lookup.engine.FailureCause
import hl7lookup.engine.Hl7Engine
import hl7lookup.engine.Hl7Transport
import hl7lookup.engine.InspectReport
import hl7lookup.engine.InspectRequest
import hl7lookup.engine.Protocol
import hl7lookup.engine.SendRequest
import hl7lookup.engine.Severity
import hl7lookup.engine.TransportEvent
import hl7lookup.filters.FilterState
import hl7lookup.highlighting.HighlightState
import hl7lookup.layout.LayoutState
import hl7lookup.i18n.I18n
import hl7lookup.i18n.Language
import hl7lookup.i18n.Text
import hl7lookup.integrations.IntegrationStore
import hl7lookup.interfaces.InterfaceDefinition
import hl7lookup.interfaces.InterfaceStore
import hl7lookup.interfaces.Interfaces
import hl7lookup.platform.Platform
import hl7lookup.platform.PlatformTexts
import hl7lookup.platform.Platforms
import hl7lookup.receivers.Receiver
import hl7lookup.receivers.ReceiverStore
import hl7lookup.receivers.Receivers
import hl7lookup.samples.Samples
import hl7lookup.senders.Sender
import hl7lookup.senders.SenderStore
import hl7lookup.senders.Senders
import hl7lookup.session.MessageOrigin
import hl7lookup.session.SessionState
import hl7lookup.session.SessionTexts
import hl7lookup.session.Sessions
import hl7lookup.settings.SettingsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class DialogKind { NONE, SETTINGS, HIGHLIGHT, FILTER, COMPARE, ANONYMIZE, INTERFACES, LICENSE, NEW_MESSAGE, SEND_TO_TAB, DIAGNOSIS, ACK, CATALOG }

enum class BottomTab { MESSAGES, SENDERS, RECEIVERS, INTEGRATIONS, ACKS }

enum class SideTab { STATISTICS, VALIDATION }

data class SearchHit(val tabId: String, val index: Int, val span: Span, val ordinal: Int)

data class Note(val text: Text, val args: List<String>, val severity: Severity, val id: Long)

data class FocusRequest(val offset: Int, val sequence: Long)

internal class View(
    val tab: hl7lookup.session.DocumentTab?,
    val text: String,
    val message: hl7lookup.document.ParsedMessage,
    val version: String,
    val dictionary: Hl7Dictionary?,
    val dictionaryLoading: Boolean,
    val iface: InterfaceDefinition?,
    val findings: List<hl7lookup.validation.Finding>,
    val highlight: (hl7lookup.document.FieldPath) -> androidx.compose.ui.graphics.Color?,
    val flag: (hl7lookup.document.FieldPath) -> androidx.compose.ui.graphics.Color?,
    val marks: List<hl7lookup.editor.Mark>,
    val tableOverride: (String?) -> hl7lookup.dictionary.TableDef?,
)

internal val ackCodes = listOf("AA", "AE", "AR", "CA", "CE", "CR")

internal data class EventCursor(val epoch: String?, val sequence: Long)

internal fun readCursor(raw: String?): EventCursor {
    if (raw.isNullOrBlank()) return EventCursor(null, 0)
    val epoch = raw.substringBefore('\n').ifBlank { null }
    val sequence = raw.substringAfter('\n', "").toLongOrNull() ?: 0
    return EventCursor(epoch, sequence)
}

internal fun writeCursor(cursor: EventCursor): String = "${cursor.epoch.orEmpty()}\n${cursor.sequence}"

internal fun searchSession(tabs: List<hl7lookup.session.DocumentTab>, query: String): List<SearchHit> {
    if (query.isBlank()) return emptyList()
    val hits = mutableListOf<SearchHit>()
    for (tab in tabs) {
        tab.messages.forEachIndexed { index, entry ->
            Er7.find(entry.text, query).forEachIndexed { ordinal, span -> hits += SearchHit(tab.id, index, span, ordinal) }
        }
    }
    return hits
}

internal fun eventsOf(dictionary: Hl7Dictionary?, type: String): List<String> =
    dictionary?.eventStructures?.keys.orEmpty().filter { it.startsWith("${type}_") }.map { it.substringAfter('_') }.distinct().sorted()

internal fun typesOf(dictionary: Hl7Dictionary?): List<String> {
    val fromStructures = dictionary?.eventStructures?.keys.orEmpty().map { it.substringBefore('_') }
    return (fromStructures + dictionary?.messageTypes?.keys.orEmpty()).filter { it.length == 3 && it.all(Char::isLetter) }.distinct().sorted()
}

class WorkspaceState(
    val platform: Platform,
    val engine: Hl7Engine,
    val transport: Hl7Transport,
    val scope: CoroutineScope,
    val appVersion: String,
) {
    val settings = SettingsState(platform)
    val session = SessionState(platform) { resolve(SessionTexts.untitled) }
    val dictionaries = DictionaryStore(engine, platform, scope)
    val highlights = HighlightState(platform)
    val filters = FilterState()
    val senders = SenderStore(platform)
    val receivers = ReceiverStore(platform)
    val integrations = IntegrationStore(platform)
    val interfaces = InterfaceStore(platform)
    val acks = AckStore()
    val anonymizer = Anonymizer(platform)
    val layouts = LayoutState(platform)

    var dialog by mutableStateOf(DialogKind.NONE)
    var anonymizeScope by mutableStateOf(AnonymizeScope.CURRENT)
    var bottomTab by mutableStateOf(BottomTab.MESSAGES)
    var sideTab by mutableStateOf(SideTab.STATISTICS)
    var searchQuery by mutableStateOf("")
        private set
    var searchHits by mutableStateOf<List<SearchHit>>(emptyList())
        private set
    var searchIndex by mutableStateOf(0)
        private set
    var report by mutableStateOf<InspectReport?>(null)
        private set
    var reportText by mutableStateOf<String?>(null)
        private set
    var engineOnline by mutableStateOf(true)
        private set
    var focus by mutableStateOf<FocusRequest?>(null)
        private set
    var note by mutableStateOf<Note?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    var diagnosis by mutableStateOf<Pair<Sender, Diagnosis>?>(null)
        private set
    var lastAck by mutableStateOf<String?>(null)
        private set
    private var sequence = 0L
    private var receivedTab: String? = null
    private val cursorKey = Platforms.key("transport", "cursor")

    val language: Language get() = settings.current.language

    fun resolve(text: Text, vararg args: Any?): String = I18n.format(text, language, *args)

    fun notify(text: Text, vararg args: Any?, severity: Severity = Severity.INFO) {
        sequence += 1
        note = Note(text, args.map { it.toString() }, severity, sequence)
    }

    fun dismissNote() {
        note = null
    }

    fun versionOf(text: String): String = dictionaries.resolveVersion(Er7.header(Er7.parse(text)).version, settings.current.version)

    fun dictionaryFor(text: String): Hl7Dictionary? = dictionaries.get(versionOf(text))

    fun dictionaryForVersion(version: String): Hl7Dictionary? = dictionaries.get(dictionaries.resolveVersion(version, settings.current.version))

    fun activeInterface(): InterfaceDefinition? {
        val tab = session.active
        return interfaces.get(tab?.interfaceId) ?: interfaces.get(integrations.get(integrations.active.integrationId)?.interfaceId)
    }

    fun focusAt(offset: Int) {
        sequence += 1
        focus = FocusRequest(offset, sequence)
    }

    fun tabChoices(): List<Pair<String, String>> = session.tabs.map { it.id to it.title }

    private fun activeTabId(): String = session.activeId ?: session.addTab(resolve(SessionTexts.untitled), emptyList())

    private fun addToActive(texts: List<String>, origin: MessageOrigin, source: String? = null) {
        val tab = session.active
        session.addMessages(tab?.id ?: activeTabId(), texts, origin, source)
    }

    fun newTab() {
        session.addTab(resolve(SessionTexts.untitled), emptyList())
    }

    fun openFile() = scope.launch {
        val file = platform.openTextFile(resolve(PlatformTexts.openMessages), Platforms.messageExtensions()) ?: return@launch
        openContent(file.name, file.content)
    }

    fun openContent(name: String, content: String) {
        val texts = Er7.splitFile(content)
        if (texts.isEmpty()) {
            notify(WorkspaceTexts.nothingInFile, Sessions.title(name), severity = Severity.WARNING)
            return
        }
        val tab = session.active
        if (tab != null && tab.messages.isEmpty()) {
            session.addMessages(tab.id, texts, MessageOrigin.OPENED)
            session.setFileName(tab.id, name)
        } else {
            session.addTab(Sessions.title(name), texts, MessageOrigin.OPENED, name)
        }
        notify(WorkspaceTexts.opened, texts.size, Sessions.title(name))
    }

    fun paste() = scope.launch {
        val clip = platform.readClipboard()
        val texts = clip?.let(Er7::splitFile).orEmpty()
        if (texts.isEmpty()) {
            notify(WorkspaceTexts.clipboardEmpty, severity = Severity.WARNING)
            return@launch
        }
        addToActive(texts, MessageOrigin.PASTED)
        notify(WorkspaceTexts.pasted, texts.size)
    }

    fun pasteText(text: String) {
        val texts = Er7.splitFile(text)
        if (texts.isNotEmpty()) addToActive(texts, MessageOrigin.PASTED)
    }

    fun save(saveAs: Boolean) = scope.launch {
        val tab = session.active ?: return@launch
        if (tab.messages.isEmpty()) {
            notify(WorkspaceTexts.nothingToSave, severity = Severity.WARNING)
            return@launch
        }
        val suggested = if (saveAs) Sessions.fileName(tab.copy(fileName = null)) else Sessions.fileName(tab)
        val name = platform.saveTextFile(resolve(PlatformTexts.saveMessages), tab.fileName.takeIf { !saveAs } ?: suggested, Er7.toFile(tab.messages.map { it.text }))
        if (name != null) {
            session.setFileName(tab.id, name)
            notify(WorkspaceTexts.saved, Sessions.title(name))
        }
    }

    fun copyCurrent() = scope.launch {
        val entry = session.currentMessage ?: return@launch
        platform.writeClipboard(Er7.toFile(listOf(entry.text)))
        notify(WorkspaceTexts.copied)
    }

    fun loadSample(id: String) {
        val text = Samples.text(id) ?: return
        addToActive(listOf(text), MessageOrigin.SAMPLE)
    }

    fun loadAllSamples() {
        session.addTab(resolve(WorkspaceTexts.samplesTab), Samples.all(), MessageOrigin.SAMPLE)
    }

    fun create(type: String, event: String, version: String) = scope.launch {
        busy = true
        when (val result = Engines.attempt { engine.create(CreateRequest(type, event, version)) }) {
            is EngineResult.Ok -> {
                engineOnline = true
                addToActive(listOf(result.value), MessageOrigin.CREATED)
                dialog = DialogKind.NONE
            }
            is EngineResult.Failed -> {
                engineOnline = false
                notify(WorkspaceTexts.createFailed, result.message, severity = Severity.ERROR)
            }
        }
        busy = false
    }

    fun acknowledge(code: String, errorText: String?, intoTab: Boolean) = scope.launch {
        val entry = session.currentMessage ?: return@launch
        when (val result = Engines.attempt { engine.acknowledge(AckRequest(entry.text, code, errorText)) }) {
            is EngineResult.Ok -> {
                engineOnline = true
                val ack = Er7.normalize(result.value)
                lastAck = ack
                if (intoTab) addToActive(listOf(ack), MessageOrigin.CREATED)
            }
            is EngineResult.Failed -> {
                engineOnline = false
                notify(WorkspaceTexts.ackFailed, result.message, severity = Severity.ERROR)
            }
        }
    }

    suspend fun inspect(text: String) {
        if (text.isBlank()) {
            report = null
            reportText = text
            return
        }
        when (val result = Engines.attempt { engine.inspect(InspectRequest(text, versionOf(text))) }) {
            is EngineResult.Ok -> {
                engineOnline = true
                report = result.value
            }
            is EngineResult.Failed -> {
                engineOnline = false
                report = null
            }
        }
        reportText = text
    }

    fun search(query: String) {
        searchQuery = query
        searchHits = searchSession(session.tabs, query)
        searchIndex = 0
    }

    fun refreshSearch() {
        if (searchQuery.isNotBlank()) {
            val previous = searchHits.getOrNull(searchIndex)
            searchHits = searchSession(session.tabs, searchQuery)
            searchIndex = previous?.let { p -> searchHits.indexOfFirst { it.tabId == p.tabId && it.index == p.index && it.span == p.span } }?.takeIf { it >= 0 }
                ?: searchIndex.coerceAtMost((searchHits.size - 1).coerceAtLeast(0))
        }
    }

    fun step(delta: Int) {
        if (searchHits.isEmpty()) return
        searchIndex = ((searchIndex + delta) % searchHits.size + searchHits.size) % searchHits.size
        goTo(searchHits[searchIndex])
    }

    fun goTo(hit: SearchHit) {
        session.activate(hit.tabId)
        session.selectMessage(hit.tabId, hit.index)
        focusAt(hit.span.start)
    }

    fun sendToTab(tabId: String?, all: Boolean) = scope.launch {
        val tab = session.active ?: return@launch
        val texts = if (all) tab.messages.map { it.text } else listOfNotNull(session.currentMessage?.text)
        if (texts.isEmpty()) {
            notify(WorkspaceTexts.nothingToSend, severity = Severity.WARNING)
            return@launch
        }
        val target = tabId?.takeIf { session.tab(it) != null } ?: session.addTab(resolve(WorkspaceTexts.sentTab, tab.title), emptyList(), select = false)
        session.addMessages(target, texts, MessageOrigin.FROM_TAB, tab.title, select = false)
        val targetTitle = session.tab(target)?.title.orEmpty()
        for (text in texts) {
            val started = platform.nowMillis()
            val ack = (Engines.attempt { engine.acknowledge(AckRequest(text, "AA", null)) } as? EngineResult.Ok)?.value
            acks.add(Acknowledgements.entry(session.newId("ack"), started, Direction.SENT, targetTitle, text, ack, null, null, platform.nowMillis() - started))
        }
        notify(I18n.plural(texts.size.toLong(), WorkspaceTexts.sentToTabOne, WorkspaceTexts.sentToTab), texts.size, targetTitle)
    }

    fun send(sender: Sender, all: Boolean) {
        if (sender.protocol == Protocol.TAB) {
            sendToTab(sender.targetTab, all)
            return
        }
        scope.launch {
            val tab = session.active ?: return@launch
            val texts = if (all) tab.messages.map { it.text } else listOfNotNull(session.currentMessage?.text)
            if (texts.isEmpty()) {
                notify(WorkspaceTexts.nothingToSend, severity = Severity.WARNING)
                return@launch
            }
            busy = true
            val peer = Senders.address(sender) { id -> session.tab(id)?.title }
            var failures = 0
            for (text in texts) {
                val started = platform.nowMillis()
                when (val result = Engines.attempt { transport.send(SendRequest(Senders.endpoint(sender), Er7.toWire(text))) }) {
                    is EngineResult.Ok -> {
                        val r = result.value
                        if (!r.ok) failures += 1
                        acks.add(Acknowledgements.entry(session.newId("ack"), started, Direction.SENT, peer, text, r.response, r.failure, r.detail, r.durationMillis))
                    }
                    is EngineResult.Failed -> {
                        failures += 1
                        acks.add(Acknowledgements.entry(session.newId("ack"), started, Direction.SENT, peer, text, null, FailureCause.ENGINE_OFFLINE, result.message, 0))
                    }
                }
            }
            busy = false
            if (failures == 0) notify(I18n.plural(texts.size.toLong(), WorkspaceTexts.sentOne, WorkspaceTexts.sent), texts.size, peer)
            else notify(WorkspaceTexts.sendFailed, failures, texts.size, peer, severity = Severity.ERROR)
            bottomTab = BottomTab.ACKS
        }
    }

    fun sendActive(all: Boolean) {
        val sender = senders.get(integrations.active.senderId) ?: senders.items.firstOrNull()
        if (sender == null) {
            bottomTab = BottomTab.SENDERS
            notify(WorkspaceTexts.noSender, severity = Severity.WARNING)
            return
        }
        send(sender, all)
    }

    fun test(sender: Sender) = scope.launch {
        busy = true
        when (val result = Engines.attempt { transport.diagnose(Senders.endpoint(sender)) }) {
            is EngineResult.Ok -> diagnosis = sender to result.value
            is EngineResult.Failed -> diagnosis = sender to Diagnosis(emptyList(), FailureCause.ENGINE_OFFLINE, result.message)
        }
        busy = false
        dialog = DialogKind.DIAGNOSIS
    }

    fun toggle(receiver: Receiver, start: Boolean) = scope.launch {
        val result = Engines.attempt { if (start) transport.startReceiver(Receivers.config(receiver)) else transport.stopReceiver(receiver.id) }
        when (result) {
            is EngineResult.Ok -> {
                receivers.update(result.value)
                val failure = result.value.failure
                if (failure != null) notify(WorkspaceTexts.receiverFailed, receiver.name, resolve(Engines.causeText(failure)), severity = Severity.ERROR)
            }
            is EngineResult.Failed -> {
                receivers.update(hl7lookup.engine.ReceiverStatus(receiver.id, false, 0, FailureCause.ENGINE_OFFLINE, result.message))
                notify(WorkspaceTexts.receiverFailed, receiver.name, resolve(Engines.causeText(FailureCause.ENGINE_OFFLINE)), severity = Severity.ERROR)
            }
        }
    }

    fun startPolling() = scope.launch {
        var cursor = readCursor(platform.loadValue(cursorKey))
        var round = 0
        while (isActive) {
            delay(700)
            round += 1
            val anyRunning = receivers.statuses.values.any { it.running }
            if (round % 5 == 0 || anyRunning) {
                (Engines.attempt { transport.receivers() } as? EngineResult.Ok)?.value?.forEach(receivers::update)
            }
            if (!anyRunning) continue
            val batch = (Engines.attempt { transport.poll(cursor.sequence) } as? EngineResult.Ok)?.value ?: continue
            if (batch.epoch != cursor.epoch) {
                val stale = cursor.sequence != 0L
                cursor = EventCursor(batch.epoch, 0)
                platform.storeValue(cursorKey, writeCursor(cursor))
                if (stale) continue
            }
            if (batch.events.isEmpty()) continue
            for (event in batch.events.sortedBy { it.sequence }) {
                cursor = cursor.copy(sequence = maxOf(cursor.sequence, event.sequence))
                deliver(event)
            }
            platform.storeValue(cursorKey, writeCursor(cursor))
        }
    }

    private fun deliver(event: TransportEvent) {
        val receiver = receivers.get(event.receiverId)
        val target = receiver?.targetTab?.takeIf { session.tab(it) != null }
            ?: receivedTab?.takeIf { session.tab(it) != null }
            ?: session.addTab(resolve(SessionTexts.received), emptyList(), select = false).also { receivedTab = it }
        session.addMessages(target, listOf(event.text), MessageOrigin.RECEIVED, event.remote, select = target == session.activeId)
        val peer = event.remote.ifBlank { receiver?.let(Receivers::address).orEmpty() }
        acks.add(Acknowledgements.entry(session.newId("ack"), event.timestamp.takeIf { it > 0 } ?: platform.nowMillis(), Direction.RECEIVED, peer, event.text, event.ack, null, null, 0))
    }

    fun anonymizeInput(scope: AnonymizeScope): List<String> {
        val tab = session.active ?: return emptyList()
        return when (scope) {
            AnonymizeScope.CURRENT -> listOfNotNull(session.currentMessage?.text)
            AnonymizeScope.TAB -> tab.messages.map { it.text }
        }
    }

    fun applyAnonymized(options: AnonymizeOptions, output: List<String>) {
        val tab = session.active ?: return
        if (output.isEmpty()) return
        when {
            options.newTab -> session.addTab(resolve(hl7lookup.anonymize.Anonymize.texts().tabTitle, tab.title), output, MessageOrigin.ANONYMIZED)
            options.scope == AnonymizeScope.CURRENT -> session.updateCurrentText(output.first())
            else -> session.transformMessages(tab.id) { i, entry -> entry.copy(text = output.getOrElse(i) { entry.text }, origin = MessageOrigin.ANONYMIZED) }
        }
    }

    fun importInterfaces() = scope.launch {
        val file = platform.openTextFile(resolve(PlatformTexts.openInterface), Platforms.interfaceExtensions()) ?: return@launch
        val definitions = Interfaces.decode(file.content)
        if (definitions == null) {
            notify(Interfaces.texts().importFailed, severity = Severity.ERROR)
            return@launch
        }
        interfaces.import(definitions)
        notify(Interfaces.texts().imported, definitions.size)
    }

    fun exportInterfaces(definitions: List<InterfaceDefinition>) = scope.launch {
        if (definitions.isEmpty()) return@launch
        val name = (if (definitions.size == 1) definitions.first().name else "interfaces").replace(Regex("[^A-Za-z0-9._-]+"), "_").trim('_').ifEmpty { "interface" } + ".json"
        platform.saveTextFile(resolve(PlatformTexts.saveInterface), name, Interfaces.encode(definitions))?.let { notify(WorkspaceTexts.saved, Sessions.title(it)) }
    }

    fun openAck(entry: hl7lookup.acknowledgements.AckEntry) {
        val ack = entry.ack ?: return
        session.addTab(resolve(WorkspaceTexts.acks), listOf(ack), MessageOrigin.RECEIVED)
    }

    fun useInterface(id: String?) {
        val tab = session.active ?: return
        session.setInterface(tab.id, id)
    }
}
