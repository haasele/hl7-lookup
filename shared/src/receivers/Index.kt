// Incoming MLLP and HTTP listeners. Workspace polls them into the message list.
package hl7lookup.receivers

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
import hl7lookup.controls.CheckOption
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
import hl7lookup.engine.Engines
import hl7lookup.engine.Protocol
import hl7lookup.engine.ReceiverConfig
import hl7lookup.i18n.tr
import hl7lookup.theme.IconShape
import hl7lookup.theme.LocalPalette

// Facade for receiver configs and addresses. Workspace polls listeners through Receivers.
object Receivers {
    // Maps a Receiver to engine ReceiverConfig. Start/stop flows call it; it uses configOf.
    fun config(receiver: Receiver): ReceiverConfig = configOf(receiver)
    // Whether a receiver form can be saved. Dialogs call it; it uses isComplete.
    fun complete(receiver: Receiver): Boolean = isComplete(receiver)
    // Human-readable listen address. Panels call it; it uses addressOf.
    fun address(receiver: Receiver): String = addressOf(receiver)
    // Hands out ReceiverTexts. Workspace and panels read labels through this.
    fun texts() = ReceiverTexts
}

// List of listeners with start/stop controls. Workspace embeds it; it uses ReceiverStore and ReceiverDialog.
@Composable
fun ReceiversPanel(
    store: ReceiverStore,
    activeId: String?,
    tabs: List<Pair<String, String>>,
    newId: () -> String,
    onActivate: (String) -> Unit,
    onToggle: (Receiver, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    var editing by remember { mutableStateOf<Receiver?>(null) }
    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(6.dp)) {
            ActionButton(tr(ReceiverTexts.add), { editing = Receiver(newId(), "") }, icon = IconShape.Plus)
        }
        if (store.items.isEmpty()) {
            EmptyState(tr(ReceiverTexts.empty), Modifier.weight(1f), illustration = IllustrationKind.RECEIVER)
            return@Column
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(store.items, key = { it.id }) { receiver ->
                val status = store.statuses[receiver.id]
                val running = status?.running == true
                TableRow(selected = receiver.id == activeId, onClick = { onActivate(receiver.id) }) {
                    Column(Modifier.weight(1f)) {
                        Label(receiver.name, weight = FontWeight.SemiBold, size = 12.sp, maxLines = 1)
                        Label(tr(Engines.protocolName(receiver.protocol)) + "  " + addressOf(receiver), color = palette.textDim, size = 11.sp, mono = true, maxLines = 1)
                        val failure = status?.failure
                        when {
                            failure != null -> Label(tr(Engines.causeText(failure)) + (status.detail?.let { " ($it)" } ?: ""), color = palette.error, size = 11.sp, maxLines = 2)
                            running -> Label(tr(ReceiverTexts.running, status.received), color = palette.success, size = 11.sp)
                            else -> Label(tr(ReceiverTexts.stopped), color = palette.textDim, size = 11.sp)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        ActionButton(
                            tr(if (running) ReceiverTexts.stop else ReceiverTexts.start),
                            { onToggle(receiver, !running) },
                            icon = if (running) IconShape.Stop else IconShape.Play,
                            primary = !running,
                        )
                        IconAction(IconShape.Edit, { editing = receiver }, color = palette.textDim, enabled = !running)
                        IconAction(IconShape.Trash, { if (running) onToggle(receiver, false); store.remove(receiver.id) }, color = palette.textDim)
                    }
                }
            }
        }
    }
    editing?.let { current ->
        ReceiverDialog(current, tabs, onSave = { store.upsert(it); editing = null }, onDismiss = { editing = null })
    }
}

// Modal to create or edit a receiver. ReceiversPanel opens it; it validates via isComplete.
@Composable
fun ReceiverDialog(initial: Receiver, tabs: List<Pair<String, String>>, onSave: (Receiver) -> Unit, onDismiss: () -> Unit) {
    var draft by remember(initial.id) { mutableStateOf(initial) }
    var portText by remember(initial.id) { mutableStateOf(initial.port.toString()) }
    val candidate = draft.copy(port = portText.toIntOrNull() ?: 0)
    val tabChoices = listOf<Pair<String?, String>>(null to tr(ReceiverTexts.newTab)) + tabs
    Modal(tr(if (initial.name.isBlank()) ReceiverTexts.add else ReceiverTexts.edit), onDismiss, actions = {
        ActionButton(tr(ControlTexts.save), { onSave(candidate) }, primary = true, enabled = isComplete(candidate))
    }) {
        FormRow(tr(ReceiverTexts.name)) { TextInput(draft.name, { draft = draft.copy(name = it) }, Modifier.fillMaxWidth()) }
        FormRow(tr(ReceiverTexts.protocol)) {
            Dropdown(draft.protocol, listOf(Protocol.MLLP, Protocol.HTTP), { tr(Engines.protocolName(it)) }, { draft = draft.copy(protocol = it) })
        }
        FormRow(tr(ReceiverTexts.port)) { TextInput(portText, { portText = it.filter(Char::isDigit).take(5) }, Modifier.fillMaxWidth(), mono = true, error = candidate.port !in 1..65535) }
        if (draft.protocol == Protocol.HTTP) {
            FormRow(tr(ReceiverTexts.path)) { TextInput(draft.path, { draft = draft.copy(path = it) }, Modifier.fillMaxWidth(), mono = true) }
        }
        FormRow(tr(ReceiverTexts.targetTab)) {
            Dropdown(tabChoices.firstOrNull { it.first == draft.targetTab } ?: tabChoices.first(), tabChoices, { it.second }, { draft = draft.copy(targetTab = it.first) })
        }
        CheckOption(tr(ReceiverTexts.autoAck), draft.autoAck, { draft = draft.copy(autoAck = it) })
    }
}
