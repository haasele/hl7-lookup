package hl7lookup.interfaces

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hl7lookup.controls.ActionButton
import hl7lookup.controls.Dropdown
import hl7lookup.controls.FormRow
import hl7lookup.controls.IconAction
import hl7lookup.controls.Label
import hl7lookup.controls.LinkText
import hl7lookup.controls.Modal
import hl7lookup.controls.TableRow
import hl7lookup.controls.TextInput
import hl7lookup.dictionary.TableDef
import hl7lookup.document.FieldPath
import hl7lookup.document.ParsedMessage
import hl7lookup.highlighting.ColorChoice
import hl7lookup.highlighting.Highlights
import hl7lookup.highlighting.HighlightRule
import hl7lookup.i18n.I18n
import hl7lookup.i18n.Text
import hl7lookup.i18n.tr
import hl7lookup.theme.IconShape
import hl7lookup.theme.LocalPalette
import hl7lookup.theme.Theme
import hl7lookup.validation.Finding

object Interfaces {
    fun findings(definition: InterfaceDefinition?, message: ParsedMessage): List<Finding> = applyInterface(definition, message)
    fun table(definition: InterfaceDefinition?, id: String?): TableDef? = tableOf(definition, id)
    fun tableAt(definition: InterfaceDefinition?, message: ParsedMessage, path: FieldPath): TableDef? = specTable(definition, message, path)
    fun highlights(definition: InterfaceDefinition?): List<HighlightRule> = highlightRulesOf(definition)
    fun encode(definitions: List<InterfaceDefinition>): String = encodeFile(definitions)
    fun decode(content: String): List<InterfaceDefinition>? = decodeFile(content)
    fun draft(id: String, name: String, message: ParsedMessage, ruleId: () -> String): InterfaceDefinition = draftFrom(id, name, message, ruleId)
    fun check(definition: InterfaceDefinition, texts: List<String>): List<ListResult> = runOnList(definition, texts)
    fun ruleName(kind: RuleKind): Text = ruleNames.getValue(kind)
    fun texts() = InterfaceTexts
}

@Composable
fun InterfacesDialog(
    store: InterfaceStore,
    activeId: String?,
    versions: List<String>,
    message: ParsedMessage,
    listTexts: List<String>,
    newId: () -> String,
    onUseForTab: (String?) -> Unit,
    onImport: () -> Unit,
    onExport: (List<InterfaceDefinition>) -> Unit,
    onPickMessage: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalPalette.current
    var selectedId by remember { mutableStateOf(activeId ?: store.items.firstOrNull()?.id) }
    var results by remember { mutableStateOf<List<ListResult>?>(null) }
    val selected = store.get(selectedId)
    val untitled = tr(InterfaceTexts.untitled, store.items.size + 1)
    Modal(tr(InterfaceTexts.manage).trimEnd('…'), onDismiss, width = 900.dp, actions = {
        ActionButton(tr(InterfaceTexts.import), onImport, icon = IconShape.Upload)
        ActionButton(tr(InterfaceTexts.exportAll), { onExport(store.items.toList()) }, icon = IconShape.Download, enabled = store.items.isNotEmpty())
    }) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.width(230.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ActionButton(tr(InterfaceTexts.new), {
                    val id = newId()
                    store.upsert(InterfaceDefinition(id, untitled))
                    selectedId = id; results = null
                }, icon = IconShape.Plus, modifier = Modifier.fillMaxWidth())
                ActionButton(tr(InterfaceTexts.fromMessage), {
                    val id = newId()
                    store.upsert(draftFrom(id, untitled, message, newId))
                    selectedId = id; results = null
                }, enabled = !message.isEmpty, modifier = Modifier.fillMaxWidth())
                if (store.items.isEmpty()) Label(tr(InterfaceTexts.empty), color = palette.textDim, size = 12.sp)
                for (definition in store.items) {
                    TableRow(selected = definition.id == selectedId, onClick = { selectedId = definition.id; results = null }) {
                        Column(Modifier.weight(1f)) {
                            Label(definition.name, weight = FontWeight.SemiBold, size = 12.sp, maxLines = 1)
                            Label(listOf(definition.messageType, definition.event).filter { it.isNotBlank() }.joinToString("^"), color = palette.textDim, size = 11.sp, mono = true)
                        }
                        if (definition.id == activeId) Label("●", color = palette.success, size = 12.sp)
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (selected != null) {
                    InterfaceEditor(selected, versions, newId, onChange = store::upsert)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (selected.id == activeId) {
                            Label(tr(InterfaceTexts.activeForTab), color = palette.success, size = 12.sp)
                            ActionButton(tr(InterfaceTexts.none), { onUseForTab(null) })
                        } else ActionButton(tr(InterfaceTexts.useForTab), { onUseForTab(selected.id) }, primary = true)
                        ActionButton(tr(InterfaceTexts.runOnList), { results = runOnList(selected, listTexts) }, enabled = listTexts.isNotEmpty())
                        ActionButton(tr(InterfaceTexts.export), { onExport(listOf(selected)) }, icon = IconShape.Download)
                        ActionButton(tr(InterfaceTexts.delete), { store.remove(selected.id); selectedId = store.items.firstOrNull()?.id; results = null }, danger = true)
                    }
                    results?.let { list ->
                        val failing = list.filter { it.errors + it.warnings > 0 }
                        if (failing.isEmpty()) Label(tr(I18n.plural(list.size.toLong(), InterfaceTexts.allPassedOne, InterfaceTexts.allPassed), list.size), color = palette.success, size = 12.sp)
                        failing.take(200).forEach { r ->
                            LinkText(tr(InterfaceTexts.listResult, r.index + 1, r.controlId, r.errors, r.warnings), { onPickMessage(r.index) }, size = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InterfaceEditor(definition: InterfaceDefinition, versions: List<String>, newId: () -> String, onChange: (InterfaceDefinition) -> Unit) {
    val palette = LocalPalette.current
    FormRow(tr(InterfaceTexts.name)) { TextInput(definition.name, { onChange(definition.copy(name = it)) }, Modifier.fillMaxWidth()) }
    FormRow(tr(InterfaceTexts.description)) { TextInput(definition.description, { onChange(definition.copy(description = it)) }, Modifier.fillMaxWidth(), singleLine = false) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Label(tr(InterfaceTexts.messageType), color = palette.textDim, size = 12.sp)
        TextInput(definition.messageType, { onChange(definition.copy(messageType = it.uppercase().take(3))) }, Modifier.width(60.dp), mono = true)
        Label(tr(InterfaceTexts.event), color = palette.textDim, size = 12.sp)
        TextInput(definition.event, { onChange(definition.copy(event = it.uppercase().take(3))) }, Modifier.width(60.dp), mono = true)
        Label(tr(InterfaceTexts.structure), color = palette.textDim, size = 12.sp)
        TextInput(definition.structure, { onChange(definition.copy(structure = it.uppercase().take(7))) }, Modifier.width(90.dp), mono = true)
        Label(tr(InterfaceTexts.version), color = palette.textDim, size = 12.sp)
        Dropdown(definition.version.ifBlank { "—" }, listOf("—") + versions, { it }, { onChange(definition.copy(version = if (it == "—") "" else it)) })
    }
    Label(tr(InterfaceTexts.rules), weight = FontWeight.SemiBold)
    for (rule in definition.rules) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            fun update(next: InterfaceRule) = onChange(definition.copy(rules = definition.rules.map { if (it.id == rule.id) next else it }))
            Dropdown(rule.kind, RuleKind.entries, { tr(ruleNames.getValue(it)) }, { update(rule.copy(kind = it)) }, Modifier.width(150.dp))
            TextInput(rule.spec, { update(rule.copy(spec = it.uppercase())) }, Modifier.width(110.dp), mono = true, placeholder = tr(InterfaceTexts.specHint), error = rule.spec.isNotBlank() && !Highlights.valid(rule.spec))
            when (rule.kind) {
                RuleKind.EXPECTED -> TextInput(rule.value, { update(rule.copy(value = it)) }, Modifier.weight(1f), placeholder = tr(InterfaceTexts.valueHint))
                RuleKind.TABLE -> {
                    val ids = definition.tables.map { it.id }
                    if (ids.isEmpty()) Label(tr(InterfaceTexts.addTable), Modifier.weight(1f), color = palette.textDim, size = 11.sp)
                    else Dropdown(rule.table ?: "—", listOf("—") + ids, { it }, { update(rule.copy(table = it.takeIf { id -> id != "—" })) }, Modifier.weight(1f))
                }
                RuleKind.HIGHLIGHT -> {
                    TextInput(rule.value, { update(rule.copy(value = it)) }, Modifier.weight(1f))
                    ColorChoice(rule.color) { update(rule.copy(color = it)) }
                }
                else -> androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            }
            IconAction(IconShape.Trash, { onChange(definition.copy(rules = definition.rules.filter { it.id != rule.id })) }, color = palette.textDim)
        }
    }
    ActionButton(tr(InterfaceTexts.addRule), { onChange(definition.copy(rules = definition.rules + InterfaceRule(newId(), RuleKind.REQUIRED, "", color = definition.rules.size % Theme.highlightCount()))) }, icon = IconShape.Plus)
    Label(tr(InterfaceTexts.tables), weight = FontWeight.SemiBold)
    Label(tr(InterfaceTexts.tableHint), color = palette.textDim, size = 11.sp)
    for (table in definition.tables) {
        var entriesText by remember(table.id, definition.id) { mutableStateOf(formatEntries(table.entries)) }
        fun update(next: CustomTable) = onChange(definition.copy(tables = definition.tables.map { if (it === table) next else it }))
        Column(Modifier.fillMaxWidth().background(palette.surfaceRaised).padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                TextInput(table.id, { update(table.copy(id = it.trim())) }, Modifier.width(90.dp), mono = true, placeholder = tr(InterfaceTexts.tableId))
                TextInput(table.name, { update(table.copy(name = it)) }, Modifier.weight(1f), placeholder = tr(InterfaceTexts.tableName))
                IconAction(IconShape.Trash, { onChange(definition.copy(tables = definition.tables.filter { it !== table })) }, color = palette.textDim)
            }
            TextInput(entriesText, { entriesText = it; update(table.copy(entries = parseEntries(it))) }, Modifier.fillMaxWidth().height(90.dp), singleLine = false, mono = true, placeholder = tr(InterfaceTexts.tableEntries))
        }
    }
    ActionButton(tr(InterfaceTexts.addTable), { onChange(definition.copy(tables = definition.tables + CustomTable("T${definition.tables.size + 1}", ""))) }, icon = IconShape.Plus)
}
