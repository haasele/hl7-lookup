// Tab and message-list state. session/Index exposes it.
package hl7lookup.session

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import hl7lookup.document.Er7
import hl7lookup.document.FieldPath
import hl7lookup.platform.Platform
import hl7lookup.platform.Platforms
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Where a message came from. MessageEntry stores it; SessionState sets it when adding text.
@Serializable
enum class MessageOrigin { OPENED, PASTED, CREATED, SAMPLE, RECEIVED, FROM_TAB, ANONYMIZED, TYPED }

// One HL7 payload in a tab. DocumentTab holds these; SessionState creates them via entry().
@Serializable
data class MessageEntry(val id: String, val text: String, val origin: MessageOrigin = MessageOrigin.OPENED, val addedAt: Long = 0, val source: String? = null)

// An open editor tab with its message list. SessionState owns these; Workspace reads them.
@Serializable
data class DocumentTab(
    val id: String,
    val title: String,
    val messages: List<MessageEntry> = emptyList(),
    val current: Int = 0,
    val fileName: String? = null,
    val interfaceId: String? = null,
)

// Persisted tabs and active id. SessionState builds and restores this via load/saveSnapshot.
@Serializable
data class SessionSnapshot(val tabs: List<DocumentTab>, val active: String? = null, val counter: Long = 0)

private val sessionJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

private val sessionKey get() = Platforms.key("session", "v1")

// Restores a saved session from platform storage. SessionState init calls it.
internal fun loadSnapshot(platform: Platform): SessionSnapshot? =
    platform.loadValue(sessionKey)?.let { runCatching { sessionJson.decodeFromString(SessionSnapshot.serializer(), it) }.getOrNull() }

// Writes the session snapshot to platform storage. SessionState.persist calls it.
internal fun saveSnapshot(platform: Platform, snapshot: SessionSnapshot) =
    runCatching { platform.storeValue(sessionKey, sessionJson.encodeToString(SessionSnapshot.serializer(), snapshot)) }

// Basename of a file path for tab titles. setFileName and suggestedFileName call it.
internal fun fileTitle(name: String): String = name.substringAfterLast('/').substringAfterLast('\\')

// Default .hl7 file name for a tab. Sessions.fileName and save flows use it.
internal fun suggestedFileName(tab: DocumentTab): String =
    tab.fileName?.let(::fileTitle) ?: (tab.title.replace(Regex("[^A-Za-z0-9._-]+"), "_").trim('_').ifEmpty { "messages" } + ".hl7")

// Live tab list, selection and persistence. Workspace holds it; Index exposes helpers around it.
class SessionState(private val platform: Platform, private val untitled: () -> String) {
    val tabs = mutableStateListOf<DocumentTab>()
    var activeId by mutableStateOf<String?>(null)
        private set
    var selection by mutableStateOf<FieldPath?>(null)
    var revision by mutableStateOf(0L)
        private set
    private var counter = 0L

    init {
        val snapshot = loadSnapshot(platform)
        if (snapshot != null && snapshot.tabs.isNotEmpty()) {
            tabs.addAll(snapshot.tabs)
            counter = snapshot.counter
            activeId = snapshot.active?.takeIf { id -> snapshot.tabs.any { it.id == id } } ?: snapshot.tabs.first().id
        } else {
            addTab(untitled(), emptyList())
        }
    }

    // Allocates a unique id with the platform clock. Tab and message creators call it.
    fun newId(prefix: String): String {
        counter += 1
        return "$prefix-${platform.nowMillis().toString(36)}-$counter"
    }

    val active: DocumentTab? get() = tabs.firstOrNull { it.id == activeId }

    val currentMessage: MessageEntry? get() = active?.let { it.messages.getOrNull(it.current) }

    // Looks up a tab by id. DocumentTabs and mutators call it.
    fun tab(id: String): DocumentTab? = tabs.firstOrNull { it.id == id }

    // Bumps revision so Compose observers refresh. Mutators call it after changes.
    private fun touch() {
        revision += 1
    }

    // Rewrites one tab in place. Most SessionState mutators call it.
    private fun replace(id: String, change: (DocumentTab) -> DocumentTab) {
        val index = tabs.indexOfFirst { it.id == id }
        if (index >= 0) {
            tabs[index] = change(tabs[index])
            touch()
        }
    }

    // Builds a normalized MessageEntry. addTab and addMessages call it.
    fun entry(text: String, origin: MessageOrigin, source: String? = null) =
        MessageEntry(newId("m"), Er7.normalize(text), origin, platform.nowMillis(), source)

    // Opens a new tab with optional starter texts. Workspace and init call it.
    fun addTab(title: String, texts: List<String>, origin: MessageOrigin = MessageOrigin.OPENED, fileName: String? = null, select: Boolean = true): String {
        val id = newId("t")
        tabs += DocumentTab(id, title, texts.map { entry(it, origin) }, 0, fileName)
        if (select || activeId == null) activate(id)
        touch()
        return id
    }

    // Makes a tab current and clears field selection. DocumentTabs and mutators call it.
    fun activate(id: String) {
        if (activeId != id) {
            activeId = id
            selection = null
            touch()
        }
    }

    // Removes a tab, ensuring one remains. DocumentTabs closes through this.
    fun closeTab(id: String) {
        val index = tabs.indexOfFirst { it.id == id }
        if (index < 0) return
        tabs.removeAt(index)
        if (tabs.isEmpty()) addTab(untitled(), emptyList())
        if (activeId == id) activate(tabs[(index - 1).coerceAtLeast(0).coerceAtMost(tabs.lastIndex)].id)
        touch()
    }

    // Changes a tab's title. DocumentTabs rename dialog calls it.
    fun rename(id: String, title: String) = replace(id) { it.copy(title = title.ifBlank { it.title }) }

    // Sets the backing file path and title. Workspace save flows call it.
    fun setFileName(id: String, name: String) = replace(id) { it.copy(fileName = name, title = fileTitle(name)) }

    // Links a tab to an interface id. Workspace interface picker calls it.
    fun setInterface(id: String, interfaceId: String?) = replace(id) { it.copy(interfaceId = interfaceId) }

    // Appends messages to a tab. Paste, open, receive and sample flows call it.
    fun addMessages(tabId: String, texts: List<String>, origin: MessageOrigin, source: String? = null, select: Boolean = true) {
        if (texts.isEmpty()) return
        replace(tabId) { tab ->
            val messages = tab.messages + texts.map { entry(it, origin, source) }
            tab.copy(messages = messages, current = if (select) messages.lastIndex - texts.size + 1 else tab.current)
        }
        if (select && tabId == activeId) selection = null
    }

    // Deletes one message and adjusts current. Message list remove calls it.
    fun removeMessage(tabId: String, index: Int) = replace(tabId) { tab ->
        val messages = tab.messages.filterIndexed { i, _ -> i != index }
        val current = when {
            messages.isEmpty() -> 0
            tab.current > index -> tab.current - 1
            else -> tab.current.coerceAtMost(messages.lastIndex)
        }
        if (tabId == activeId) selection = null
        tab.copy(messages = messages, current = current)
    }

    // Empties a tab's message list. Workspace clear actions call it.
    fun clearMessages(tabId: String) = replace(tabId) { it.copy(messages = emptyList(), current = 0) }

    // Moves current to another message index. Message list selection calls it.
    fun selectMessage(tabId: String, index: Int) {
        val tab = tab(tabId) ?: return
        if (index !in tab.messages.indices || index == tab.current) return
        replace(tabId) { it.copy(current = index) }
        if (tabId == activeId) selection = null
    }

    // Writes the active message text from the editor. Workspace typing calls it.
    fun updateCurrentText(text: String) {
        val tab = active ?: return
        if (tab.messages.isEmpty()) {
            if (text.isBlank()) return
            replace(tab.id) { it.copy(messages = listOf(entry(text, MessageOrigin.TYPED)), current = 0) }
            return
        }
        val current = tab.messages[tab.current]
        if (current.text == text) return
        replace(tab.id) { t -> t.copy(messages = t.messages.mapIndexed { i, m -> if (i == t.current) m.copy(text = text) else m }) }
    }

    // Maps every message in a tab. Anonymize and similar transforms call it.
    fun transformMessages(tabId: String, transform: (Int, MessageEntry) -> MessageEntry) =
        replace(tabId) { tab -> tab.copy(messages = tab.messages.mapIndexed(transform)) }

    // Captures tabs for persistence. persist() calls it.
    fun snapshot(): SessionSnapshot = SessionSnapshot(tabs.toList(), activeId, counter)

    // Saves the session via saveSnapshot. Workspace persistence hooks call it.
    fun persist() {
        saveSnapshot(platform, snapshot())
    }
}
