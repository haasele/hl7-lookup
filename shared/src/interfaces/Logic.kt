// Required fields, expected values and custom tables. interfaces/Index and workspace call Interfaces.
package hl7lookup.interfaces

import androidx.compose.runtime.mutableStateListOf
import hl7lookup.datetime.Hl7Dates
import hl7lookup.dictionary.TableDef
import hl7lookup.dictionary.TableEntry
import hl7lookup.document.Er7
import hl7lookup.document.ParsedMessage
import hl7lookup.engine.Severity
import hl7lookup.highlighting.HighlightRule
import hl7lookup.platform.Platform
import hl7lookup.platform.Platforms
import hl7lookup.validation.Finding
import hl7lookup.validation.FindingKind
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

// Kinds of interface rules. InterfaceRule and the editor dropdown use these.
@Serializable
enum class RuleKind { REQUIRED, EXPECTED, DATE, TABLE, HIGHLIGHT }

// One required, expected, date, table or highlight rule. InterfaceDefinition stores these.
@Serializable
data class InterfaceRule(
    val id: String,
    val kind: RuleKind,
    val spec: String,
    val value: String = "",
    val table: String? = null,
    val color: Int = 0,
)

// Custom code table for an interface. InterfaceDefinition stores these; tableOf wraps them.
@Serializable
data class CustomTable(val id: String, val name: String, val entries: List<TableEntry> = emptyList())

// Named interface with type, rules and tables. InterfaceStore persists these.
@Serializable
data class InterfaceDefinition(
    val id: String,
    val name: String,
    val description: String = "",
    val messageType: String = "",
    val event: String = "",
    val structure: String = "",
    val version: String = "",
    val rules: List<InterfaceRule> = emptyList(),
    val tables: List<CustomTable> = emptyList(),
)

// JSON wrapper for export files. encodeFile and decodeFile read and write it.
@Serializable
data class InterfaceFile(val format: String = "hl7lookup-interfaces", val revision: Int = 1, val interfaces: List<InterfaceDefinition>)

// Per-message error and warning counts. runOnList returns these for the dialog.
data class ListResult(val index: Int, val controlId: String, val errors: Int, val warnings: Int)

private val interfaceJson = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }

private val interfacesKey get() = Platforms.key("interfaces", "v1")

// JSON-encodes definitions as an interface file. Interfaces.encode delegates to it.
internal fun encodeFile(definitions: List<InterfaceDefinition>): String =
    interfaceJson.encodeToString(InterfaceFile.serializer(), InterfaceFile(interfaces = definitions))

// Parses a file or single definition. Interfaces.decode delegates to it.
internal fun decodeFile(content: String): List<InterfaceDefinition>? =
    runCatching { interfaceJson.decodeFromString(InterfaceFile.serializer(), content).interfaces }.getOrNull()
        ?: runCatching { listOf(interfaceJson.decodeFromString(InterfaceDefinition.serializer(), content)) }.getOrNull()

// Wraps a custom table as TableDef. Interfaces.table and specTable call it.
internal fun tableOf(definition: InterfaceDefinition?, id: String?): TableDef? {
    if (definition == null || id == null) return null
    val table = definition.tables.firstOrNull { it.id == id } ?: return null
    return TableDef(table.id, table.name, table.entries)
}

// Parses CODE=Description lines. InterfaceEditor saves table text through it.
internal fun parseEntries(text: String): List<TableEntry> = text.lines().mapNotNull { line ->
    val trimmed = line.trim()
    if (trimmed.isEmpty()) return@mapNotNull null
    val separator = listOf('=', '\t', ';').firstOrNull { it in trimmed }
    if (separator == null) TableEntry(trimmed, "") else TableEntry(trimmed.substringBefore(separator).trim(), trimmed.substringAfter(separator).trim())
}

// Formats table entries for the text box. InterfaceEditor seeds the editor from it.
internal fun formatEntries(entries: List<TableEntry>): String = entries.joinToString("\n") { if (it.description.isEmpty()) it.code else "${it.code}=${it.description}" }

// Maps HIGHLIGHT rules to HighlightRule. Interfaces.highlights and Workspace call it.
internal fun highlightRulesOf(definition: InterfaceDefinition?): List<HighlightRule> =
    definition?.rules.orEmpty().filter { it.kind == RuleKind.HIGHLIGHT }.map { HighlightRule("if-${it.id}", it.spec, it.value, it.color) }

// Checks type and field rules on a message. Interfaces.findings and runOnList call it.
internal fun applyInterface(definition: InterfaceDefinition?, message: ParsedMessage): List<Finding> {
    if (definition == null || message.isEmpty) return emptyList()
    val findings = mutableListOf<Finding>()
    val header = Er7.header(message)
    val expectedType = listOf(definition.messageType, definition.event).filter { it.isNotBlank() }.joinToString("^")
    val actualType = listOf(header.type, header.event).filter { it.isNotBlank() }.joinToString("^")
    if (expectedType.isNotBlank() && !actualType.startsWith(expectedType, ignoreCase = true)) {
        findings += Finding(Severity.ERROR, FindingKind.RULE_TYPE, hl7lookup.document.FieldPath(0, 9), "MSH-9", listOf(expectedType, actualType), definition.name)
    }
    if (definition.structure.isNotBlank() && header.structure.isNotBlank() && !header.structure.equals(definition.structure, ignoreCase = true)) {
        findings += Finding(Severity.WARNING, FindingKind.RULE_TYPE, hl7lookup.document.FieldPath(0, 9, 1, 3), "MSH-9.3", listOf(definition.structure, header.structure), definition.name)
    }
    for (rule in definition.rules) {
        if (rule.kind == RuleKind.HIGHLIGHT) continue
        val spec = Er7.spec(rule.spec) ?: continue
        val segments = message.segments.filter { it.name == spec.segment }
        if (segments.isEmpty()) {
            if (rule.kind == RuleKind.REQUIRED) findings += Finding(Severity.ERROR, FindingKind.RULE_REQUIRED, null, Er7.label(spec), emptyList(), definition.name)
            continue
        }
        for (segment in segments) {
            val path = Er7.resolve(message, spec, Er7.occurrence(message, segment.index)) ?: continue
            val value = Er7.value(message, path)
            val label = Er7.label(message, path)
            when (rule.kind) {
                RuleKind.REQUIRED -> if (value.isBlank()) findings += Finding(Severity.ERROR, FindingKind.RULE_REQUIRED, path, label, emptyList(), definition.name)
                RuleKind.EXPECTED -> {
                    val allowed = rule.value.split('|').map { it.trim() }.filter { it.isNotEmpty() }
                    if (allowed.isNotEmpty() && value !in allowed) findings += Finding(Severity.ERROR, FindingKind.RULE_EXPECTED, path, label, listOf(rule.value, value), definition.name)
                }
                RuleKind.DATE -> if (value.isNotBlank() && !Hl7Dates.isValid(value.substringBefore(message.delimiters.subcomponent)))
                    findings += Finding(Severity.ERROR, FindingKind.RULE_DATE, path, label, listOf(value), definition.name)
                RuleKind.TABLE -> {
                    val table = definition.tables.firstOrNull { it.id == rule.table }
                    if (table != null && value.isNotBlank() && table.entries.none { it.code == value }) {
                        findings += Finding(Severity.WARNING, FindingKind.RULE_TABLE, path, label, listOf(value, table.name.ifBlank { table.id }), definition.name)
                    }
                }
                RuleKind.HIGHLIGHT -> Unit
            }
        }
    }
    return findings
}

// Custom table bound to the caret path. Interfaces.tableAt delegates to it.
internal fun specTable(definition: InterfaceDefinition?, message: ParsedMessage, path: hl7lookup.document.FieldPath): TableDef? {
    if (definition == null) return null
    val spec = Er7.spec(message, path) ?: return null
    val rule = definition.rules.firstOrNull { rule ->
        rule.kind == RuleKind.TABLE && Er7.spec(rule.spec)?.let { it.segment == spec.segment && it.field == spec.field && it.component == spec.component && it.subcomponent == spec.subcomponent } == true
    } ?: return null
    return tableOf(definition, rule.table)
}

// Runs applyInterface over every list text. Interfaces.check and the dialog call it.
internal fun runOnList(definition: InterfaceDefinition, texts: List<String>): List<ListResult> = texts.mapIndexed { index, text ->
    val message = Er7.parse(text)
    val findings = applyInterface(definition, message)
    ListResult(index, Er7.header(message).controlId, findings.count { it.severity == Severity.ERROR }, findings.count { it.severity == Severity.WARNING })
}

// Drafts required rules from common fields. Interfaces.draft and New from message call it.
internal fun draftFrom(id: String, name: String, message: ParsedMessage, ruleId: () -> String): InterfaceDefinition {
    val header = Er7.header(message)
    val rules = listOf("MSH-9", "MSH-10", "MSH-12", "PID-3", "PID-5").filter { Er7.value(message, it).isNotBlank() }
        .map { InterfaceRule(ruleId(), RuleKind.REQUIRED, it) }
    return InterfaceDefinition(id, name, "", header.type, header.event, header.structure, header.version, rules)
}

// Persisted list of interface definitions. Workspace owns it for the manage dialog.
class InterfaceStore(private val platform: Platform) {
    val items = mutableStateListOf<InterfaceDefinition>().apply {
        addAll(platform.loadValue(interfacesKey)?.let { runCatching { interfaceJson.decodeFromString(ListSerializer(InterfaceDefinition.serializer()), it) }.getOrNull() }.orEmpty())
    }

    // Writes the item list to the platform. upsert, remove and import call it.
    private fun save() = platform.storeValue(interfacesKey, interfaceJson.encodeToString(ListSerializer(InterfaceDefinition.serializer()), items.toList()))

    // Inserts or replaces a definition and saves. InterfacesDialog edits call it.
    fun upsert(definition: InterfaceDefinition) {
        val index = items.indexOfFirst { it.id == definition.id }
        if (index >= 0) items[index] = definition else items += definition
        save()
    }

    // Drops a definition by id and saves. InterfacesDialog delete calls it.
    fun remove(id: String) {
        items.removeAll { it.id == id }
        save()
    }

    // Finds a definition by id. InterfacesDialog and Workspace resolve the active one with it.
    fun get(id: String?): InterfaceDefinition? = items.firstOrNull { it.id == id }

    // Upserts each imported definition. Import actions call it after decode.
    fun import(definitions: List<InterfaceDefinition>) {
        definitions.forEach(::upsert)
    }
}
