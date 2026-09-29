// Loads and caches the dictionary from the engine. Workspace asks Dictionaries for the active version.
package hl7lookup.dictionary

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import hl7lookup.engine.EngineResult
import hl7lookup.engine.Engines
import hl7lookup.engine.Hl7Engine
import hl7lookup.platform.Platform
import hl7lookup.platform.PlatformKind
import hl7lookup.platform.Platforms
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Full versioned HL7 definitions for segments, tables and structures. Engine and cache decode it; lookups read it.
@Serializable
data class Hl7Dictionary(
    val version: String,
    val segments: Map<String, SegmentDef> = emptyMap(),
    val datatypes: Map<String, DatatypeDef> = emptyMap(),
    val tables: Map<String, TableDef> = emptyMap(),
    val messageTypes: Map<String, String> = emptyMap(),
    val events: Map<String, String> = emptyMap(),
    val eventStructures: Map<String, String> = emptyMap(),
    val structures: Map<String, StructureDef> = emptyMap(),
)

// One segment and its fields from the dictionary. segmentOf returns it; fieldOf walks its fields.
@Serializable
data class SegmentDef(val name: String, val description: String, val fields: List<FieldDef>)

// One field on a segment: datatype, table and length. fieldOf returns it; describe builds NodeInfo from it.
@Serializable
data class FieldDef(
    val number: Int,
    val name: String,
    val datatype: String,
    val required: Boolean = false,
    val maxRepetitions: Int = 1,
    val length: Int = 0,
    val table: String? = null,
)

// A datatype and its components. componentsOf reads it; describe walks nested datatypes.
@Serializable
data class DatatypeDef(val name: String, val description: String, val components: List<ComponentDef> = emptyList())

// One component inside a datatype. componentsOf lists them; describe uses them for component and sub levels.
@Serializable
data class ComponentDef(val number: Int, val name: String, val datatype: String, val table: String? = null)

// A coded table and its entries. tableOf returns it; searchTable and entryFor read its entries.
@Serializable
data class TableDef(val id: String, val name: String, val entries: List<TableEntry> = emptyList())

// One code and description in a table. entryFor and searchTable return them.
@Serializable
data class TableEntry(val code: String, val description: String)

// A message structure with ordered elements. structureFor returns it; catalog and validation walk it.
@Serializable
data class StructureDef(val name: String, val elements: List<StructureElement>)

// One segment or group in a structure tree. StructureDef holds them; nested children continue the tree.
@Serializable
data class StructureElement(
    val name: String,
    val required: Boolean,
    val repeating: Boolean,
    val group: Boolean = false,
    val children: List<StructureElement> = emptyList(),
)

// Which depth a NodeInfo describes. describe sets it; UI labels switch on it.
enum class NodeLevel { SEGMENT, FIELD, COMPONENT, SUBCOMPONENT }

// Display metadata for a segment, field, component or subcomponent. describe builds it; Dictionaries.node returns it.
data class NodeInfo(
    val level: NodeLevel,
    val name: String,
    val datatype: String,
    val table: String?,
    val required: Boolean,
    val maxRepetitions: Int,
    val length: Int,
)

// Load lifecycle for one dictionary version. DictionaryStore holds it; UI switches on the variant.
sealed interface DictionaryState {
    // In-flight load for a version. DictionaryStore.state sets it while load runs.
    data object Loading : DictionaryState
    // Successfully loaded dictionary. DictionaryStore.get unwraps it; UI reads definitions from it.
    data class Ready(val dictionary: Hl7Dictionary) : DictionaryState
    // Load failed with a reason string. DictionaryStore.load sets it; UI shows retry wording.
    data class Failed(val reason: String) : DictionaryState
}

private val dateTypes = setOf("DT", "DTM", "TS", "DIN", "DR")

internal val cacheJson = Json { ignoreUnknownKeys = true; encodeDefaults = false }

private const val cacheSchema = "5"

// Looks up a segment by name in the dictionary. Dictionaries.segment and fieldOf call it.
internal fun segmentOf(dictionary: Hl7Dictionary?, name: String): SegmentDef? = dictionary?.segments?.get(name)

// Looks up a field by segment name and number. Dictionaries.field and describe call it.
internal fun fieldOf(dictionary: Hl7Dictionary?, segment: String, field: Int): FieldDef? =
    segmentOf(dictionary, segment)?.fields?.firstOrNull { it.number == field }

// Lists components for a datatype name. Dictionaries.components, describe and isComposite call it.
internal fun componentsOf(dictionary: Hl7Dictionary?, datatype: String): List<ComponentDef> =
    dictionary?.datatypes?.get(datatype)?.components.orEmpty()

// Builds NodeInfo for a path depth into a segment. Dictionaries.node / Index calls it.
internal fun describe(dictionary: Hl7Dictionary?, segment: String, field: Int, component: Int, subcomponent: Int): NodeInfo? {
    if (dictionary == null) return null
    if (field == 0) {
        val def = segmentOf(dictionary, segment) ?: return null
        return NodeInfo(NodeLevel.SEGMENT, def.description, "", null, false, 0, 0)
    }
    val fieldDef = fieldOf(dictionary, segment, field) ?: return null
    val fieldInfo = NodeInfo(NodeLevel.FIELD, fieldDef.name, fieldDef.datatype, fieldDef.table, fieldDef.required, fieldDef.maxRepetitions, fieldDef.length)
    if (component == 0) return fieldInfo
    val components = componentsOf(dictionary, fieldDef.datatype)
    val compDef = components.getOrNull(component - 1)
        ?: return if (component == 1 && components.isEmpty()) fieldInfo.copy(level = NodeLevel.COMPONENT) else null
    val compInfo = NodeInfo(NodeLevel.COMPONENT, compDef.name, compDef.datatype, compDef.table, false, 1, 0)
    if (subcomponent == 0) return compInfo
    val subs = componentsOf(dictionary, compDef.datatype)
    val subDef = subs.getOrNull(subcomponent - 1)
        ?: return if (subcomponent == 1 && subs.isEmpty()) compInfo.copy(level = NodeLevel.SUBCOMPONENT) else null
    return NodeInfo(NodeLevel.SUBCOMPONENT, subDef.name, subDef.datatype, subDef.table, false, 1, 0)
}

// Looks up a table by id. Dictionaries.table and describeCode call it.
internal fun tableOf(dictionary: Hl7Dictionary?, id: String?): TableDef? = id?.let { dictionary?.tables?.get(it) }

private val codePatterns = mapOf(
    "HL70000" to Regex("HL7\\d{4}"),
    "99zzz" to Regex("99[A-Za-z0-9]{1,3}"),
)

// Finds a table entry by exact code or pattern. Dictionaries.entry and describeCode call it.
internal fun entryFor(table: TableDef?, code: String): TableEntry? {
    val entries = table?.entries ?: return null
    return entries.firstOrNull { it.code == code }
        ?: entries.firstOrNull { entry -> codePatterns[entry.code]?.matches(code) == true }
}

// Resolves a code in a named table to its description. Dictionaries.code / Index calls it.
internal fun describeCode(dictionary: Hl7Dictionary?, table: String?, code: String): String? =
    tableOf(dictionary, table)?.let { entryFor(it, code) ?: it.entries.firstOrNull { e -> e.code.equals(code, ignoreCase = true) } }?.description

// True when the datatype is a known date/time type. Dictionaries.isDate / Index calls it.
internal fun isDateType(datatype: String): Boolean = datatype in dateTypes

// True when the datatype has more than one component. Dictionaries.isComposite / Index calls it.
internal fun isComposite(dictionary: Hl7Dictionary?, datatype: String): Boolean = componentsOf(dictionary, datatype).size > 1

// Looks up an event display name by type and event. Dictionaries.event / Index calls it.
internal fun eventName(dictionary: Hl7Dictionary?, type: String, event: String): String? =
    dictionary?.events?.get("${type}_$event") ?: dictionary?.events?.get(event)

// Looks up a message type display name. Dictionaries.messageType / Index calls it.
internal fun messageTypeName(dictionary: Hl7Dictionary?, type: String): String? = dictionary?.messageTypes?.get(type)

// Resolves which structure applies for a type/event. Dictionaries.structure / Index calls it.
internal fun structureFor(dictionary: Hl7Dictionary?, type: String, event: String, declared: String): StructureDef? {
    if (dictionary == null) return null
    val name = declared.ifBlank { dictionary.eventStructures["${type}_$event"] ?: "${type}_$event" }
    return dictionary.structures[name] ?: dictionary.structures[dictionary.eventStructures[name] ?: ""]
}

// Filters and ranks table entries by a query. Dictionaries.search / Index calls it.
internal fun searchTable(table: TableDef?, query: String): List<TableEntry> {
    val entries = table?.entries.orEmpty()
    if (query.isBlank()) return entries
    val q = query.trim()
    val starts = entries.filter { it.code.startsWith(q, ignoreCase = true) }
    val rest = entries.filter { it !in starts && (it.code.contains(q, true) || it.description.contains(q, true)) }
    return starts + rest
}

// Reads a versioned dictionary from platform storage. DictionaryStore.load calls it before the engine.
internal fun readCached(platform: Platform, version: String): Hl7Dictionary? =
    platform.loadValue(Platforms.key("dictionary", "$cacheSchema-$version"))
        ?.let { runCatching { cacheJson.decodeFromString(Hl7Dictionary.serializer(), it) }.getOrNull() }

// Writes a dictionary into platform storage. DictionaryStore.load calls it after a successful engine fetch.
internal fun writeCached(platform: Platform, dictionary: Hl7Dictionary) {
    runCatching {
        platform.storeValue(Platforms.key("dictionary", "$cacheSchema-${dictionary.version}"), cacheJson.encodeToString(Hl7Dictionary.serializer(), dictionary))
    }
}

// Loads, caches and exposes dictionary state per version. Workspace builds it; UI calls state, get and retry.
class DictionaryStore(private val engine: Hl7Engine, private val platform: Platform, private val scope: CoroutineScope) {
    private val states = mutableStateMapOf<String, DictionaryState>()
    var versions by mutableStateOf(Engines.knownVersions())
        private set
    var online by mutableStateOf(true)
        private set

    init {
        scope.launch {
            when (val result = Engines.attempt { engine.versions() }) {
                is EngineResult.Ok -> versions = result.value.ifEmpty { Engines.knownVersions() }
                is EngineResult.Failed -> online = false
            }
        }
    }

    // Picks the closest known version string. Workspace and callers normalize MSH-12 with it.
    fun resolveVersion(raw: String, fallback: String): String =
        Engines.matchVersion(raw, versions) ?: Engines.matchVersion(fallback, versions) ?: fallback

    // Returns or starts loading state for a version. UI observes it; kicks off load when missing.
    fun state(version: String): DictionaryState {
        val current = states[version]
        if (current != null) return current
        states[version] = DictionaryState.Loading
        scope.launch { load(version) }
        return DictionaryState.Loading
    }

    // Returns the ready dictionary for a version, or null. Callers that need defs call it after state settles.
    fun get(version: String): Hl7Dictionary? = (state(version) as? DictionaryState.Ready)?.dictionary

    // Clears cached state and reloads a version. UI retry actions call it after Failed.
    fun retry(version: String) {
        states.remove(version)
        state(version)
    }

    // Fetches from cache or engine and updates states. state and retry launch it on the store scope.
    private suspend fun load(version: String) {
        val cached = readCached(platform, version)
        if (cached != null) {
            states[version] = DictionaryState.Ready(cached)
            return
        }
        when (val result = Engines.attempt { engine.dictionary(version) }) {
            is EngineResult.Ok -> {
                online = true
                states[version] = DictionaryState.Ready(result.value)
                if (platform.kind == PlatformKind.BROWSER) writeCached(platform, result.value)
            }
            is EngineResult.Failed -> {
                online = false
                states[version] = DictionaryState.Failed(result.message)
            }
        }
    }
}
