package hl7lookup.senders

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
import hl7lookup.engine.Endpoint
import hl7lookup.engine.Engines
import hl7lookup.engine.Protocol
import hl7lookup.i18n.tr
import hl7lookup.theme.IconShape
import hl7lookup.theme.LocalPalette

object Senders {
    fun endpoint(sender: Sender): Endpoint = endpointOf(sender)
    fun complete(sender: Sender): Boolean = isComplete(sender)
    fun address(sender: Sender, tabTitle: (String) -> String?): String = addressOf(sender, tabTitle)
    fun texts() = SenderTexts
}

@Composable
fun SendersPanel(
    store: SenderStore,
    activeId: String?,
    tabs: List<Pair<String, String>>,
    newId: () -> String,
    onActivate: (String) -> Unit,
    onSend: (Sender, Boolean) -> Unit,
    onTest: (Sender) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    var editing by remember { mutableStateOf<Sender?>(null) }
    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ActionButton(tr(SenderTexts.add), { editing = Sender(newId(), "", Protocol.MLLP) }, icon = IconShape.Plus)
        }
        if (store.items.isEmpty()) {
            EmptyState(tr(SenderTexts.empty), Modifier.weight(1f), illustration = IllustrationKind.SENDER)
            return@Column
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(store.items, key = { it.id }) { sender ->
                TableRow(selected = sender.id == activeId, onClick = { onActivate(sender.id) }) {
                    Column(Modifier.weight(1f)) {
                        Label(sender.name, weight = FontWeight.SemiBold, size = 12.sp, maxLines = 1)
                        Label(tr(Engines.protocolName(sender.protocol)) + "  " + addressOf(sender) { id -> tabs.firstOrNull { it.first == id }?.second }, color = palette.textDim, size = 11.sp, mono = true, maxLines = 1)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        ActionButton(tr(SenderTexts.send), { onSend(sender, false) }, icon = IconShape.Send, primary = sender.id == activeId)
                        ActionButton(tr(SenderTexts.sendAll), { onSend(sender, true) })
                        if (sender.protocol != Protocol.TAB) IconAction(IconShape.Search, { onTest(sender) }, color = palette.textDim)
                        IconAction(IconShape.Edit, { editing = sender }, color = palette.textDim)
                        IconAction(IconShape.Trash, { store.remove(sender.id) }, color = palette.textDim)
                    }
                }
            }
        }
    }
    editing?.let { current ->
        SenderDialog(current, tabs, onSave = { store.upsert(it); editing = null }, onDismiss = { editing = null })
    }
}

@Composable
fun SenderDialog(initial: Sender, tabs: List<Pair<String, String>>, onSave: (Sender) -> Unit, onDismiss: () -> Unit) {
    var draft by remember(initial.id) { mutableStateOf(initial) }
    var portText by remember(initial.id) { mutableStateOf(initial.port.toString()) }
    var timeoutText by remember(initial.id) { mutableStateOf(initial.timeoutMillis.toString()) }
    val candidate = draft.copy(port = portText.toIntOrNull() ?: 0, timeoutMillis = timeoutText.toIntOrNull() ?: 10_000)
    Modal(tr(if (initial.name.isBlank()) SenderTexts.add else SenderTexts.edit), onDismiss, actions = {
        ActionButton(tr(ControlTexts.save), { onSave(candidate) }, primary = true, enabled = isComplete(candidate))
    }) {
        FormRow(tr(SenderTexts.name)) { TextInput(draft.name, { draft = draft.copy(name = it) }, Modifier.fillMaxWidth()) }
        FormRow(tr(SenderTexts.protocol)) {
            Dropdown(draft.protocol, Protocol.entries, { tr(Engines.protocolName(it)) }, { draft = draft.copy(protocol = it) })
        }
        if (draft.protocol == Protocol.TAB) {
            FormRow(tr(SenderTexts.targetTab)) {
                val selected = tabs.firstOrNull { it.first == draft.targetTab }
                Dropdown(selected ?: ("" to "—"), tabs, { it.second }, { draft = draft.copy(targetTab = it.first) })
            }
        } else {
            FormRow(tr(SenderTexts.host)) { TextInput(draft.host, { draft = draft.copy(host = it) }, Modifier.fillMaxWidth(), mono = true) }
            FormRow(tr(SenderTexts.port)) { TextInput(portText, { portText = it.filter(Char::isDigit).take(5) }, Modifier.fillMaxWidth(), mono = true, error = candidate.port !in 1..65535) }
            if (draft.protocol == Protocol.HTTP) {
                FormRow(tr(SenderTexts.path)) { TextInput(draft.path, { draft = draft.copy(path = it) }, Modifier.fillMaxWidth(), mono = true) }
            }
            FormRow(tr(SenderTexts.timeout)) { TextInput(timeoutText, { timeoutText = it.filter(Char::isDigit).take(6) }, Modifier.fillMaxWidth(), mono = true) }
        }
    }
}
