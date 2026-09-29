package hl7lookup.integrations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import hl7lookup.controls.Badge
import hl7lookup.controls.ControlTexts
import hl7lookup.controls.Dropdown
import hl7lookup.controls.EmptyState
import hl7lookup.motion.IllustrationKind
import hl7lookup.controls.FormRow
import hl7lookup.controls.IconAction
import hl7lookup.controls.Label
import hl7lookup.controls.Modal
import hl7lookup.controls.TableRow
import hl7lookup.controls.TextInput
import hl7lookup.i18n.tr
import hl7lookup.theme.IconShape
import hl7lookup.theme.LocalPalette

object Integrations {
    fun complete(integration: Integration): Boolean = isComplete(integration)
    fun texts() = IntegrationTexts
}

@Composable
fun IntegrationsPanel(
    store: IntegrationStore,
    senders: List<Pair<String, String>>,
    receivers: List<Pair<String, String>>,
    interfaces: List<Pair<String, String>>,
    newId: () -> String,
    onActivate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    var editing by remember { mutableStateOf<Integration?>(null) }
    fun nameOf(list: List<Pair<String, String>>, id: String?) = list.firstOrNull { it.first == id }?.second
    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(6.dp)) {
            ActionButton(tr(IntegrationTexts.add), { editing = Integration(newId(), "") }, icon = IconShape.Plus)
        }
        if (store.items.isEmpty()) {
            EmptyState(tr(IntegrationTexts.empty), Modifier.weight(1f), illustration = IllustrationKind.INTEGRATION)
            return@Column
        }
        val dash = tr(IntegrationTexts.none)
        LazyColumn(Modifier.fillMaxSize()) {
            items(store.items, key = { it.id }) { integration ->
                val active = store.active.integrationId == integration.id
                TableRow(selected = active, onClick = { onActivate(integration.id) }) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Label(integration.name, weight = FontWeight.SemiBold, size = 12.sp, maxLines = 1)
                            if (active) Badge(tr(IntegrationTexts.active), palette.success)
                        }
                        Label(
                            tr(IntegrationTexts.summary, nameOf(senders, integration.senderId) ?: dash, nameOf(receivers, integration.receiverId) ?: dash, nameOf(interfaces, integration.interfaceId) ?: dash),
                            color = palette.textDim, size = 11.sp, maxLines = 1,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (!active) ActionButton(tr(IntegrationTexts.activate), { onActivate(integration.id) }, primary = true)
                        IconAction(IconShape.Edit, { editing = integration }, color = palette.textDim)
                        IconAction(IconShape.Trash, { store.remove(integration.id) }, color = palette.textDim)
                    }
                }
            }
        }
    }
    editing?.let { current ->
        IntegrationDialog(current, senders, receivers, interfaces, onSave = { store.upsert(it); editing = null }, onDismiss = { editing = null })
    }
}

@Composable
fun IntegrationDialog(
    initial: Integration,
    senders: List<Pair<String, String>>,
    receivers: List<Pair<String, String>>,
    interfaces: List<Pair<String, String>>,
    onSave: (Integration) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember(initial.id) { mutableStateOf(initial) }
    val dash = tr(IntegrationTexts.none)
    fun options(list: List<Pair<String, String>>) = listOf<Pair<String?, String>>(null to dash) + list
    Modal(tr(if (initial.name.isBlank()) IntegrationTexts.add else IntegrationTexts.edit), onDismiss, actions = {
        ActionButton(tr(ControlTexts.save), { onSave(draft) }, primary = true, enabled = isComplete(draft))
    }) {
        FormRow(tr(IntegrationTexts.name)) { TextInput(draft.name, { draft = draft.copy(name = it) }, Modifier.fillMaxWidth()) }
        FormRow(tr(IntegrationTexts.sender)) {
            val o = options(senders)
            Dropdown(o.firstOrNull { it.first == draft.senderId } ?: o.first(), o, { it.second }, { draft = draft.copy(senderId = it.first) })
        }
        FormRow(tr(IntegrationTexts.receiver)) {
            val o = options(receivers)
            Dropdown(o.firstOrNull { it.first == draft.receiverId } ?: o.first(), o, { it.second }, { draft = draft.copy(receiverId = it.first) })
        }
        FormRow(tr(IntegrationTexts.iface)) {
            val o = options(interfaces)
            Dropdown(o.firstOrNull { it.first == draft.interfaceId } ?: o.first(), o, { it.second }, { draft = draft.copy(interfaceId = it.first) })
        }
    }
}
