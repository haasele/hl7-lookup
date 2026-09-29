// Open tabs and the current message. Workspace reads the session and persists it.
package hl7lookup.session

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxWidth
import hl7lookup.controls.ActionButton
import hl7lookup.controls.ControlTexts
import hl7lookup.controls.Modal
import hl7lookup.controls.TabItem
import hl7lookup.controls.TabStrip
import hl7lookup.controls.TextInput
import hl7lookup.i18n.I18n
import hl7lookup.i18n.tr
import hl7lookup.theme.IconShape
import hl7lookup.controls.IconAction

// Facade for tab file names and session wording. Workspace calls these helpers.
object Sessions {
    // Suggests a save name for a tab. Callers use it; it delegates to suggestedFileName.
    fun fileName(tab: DocumentTab): String = suggestedFileName(tab)
    // Short display title from a path. Callers use it; it delegates to fileTitle.
    fun title(path: String): String = fileTitle(path)
    // Hands out SessionTexts. Workspace and Index read labels through this.
    fun texts() = SessionTexts
}

// Tab strip for open documents with rename. Workspace embeds it; it drives SessionState and Modal.
@Composable
fun DocumentTabs(state: SessionState, onNewTab: () -> Unit, modifier: Modifier = Modifier) {
    var renaming by remember { mutableStateOf<String?>(null) }
    val items = state.tabs.map { tab ->
        val badge = tab.messages.size.takeIf { it > 1 }?.toString()
        TabItem(tab.id, tab.title, closable = true, badge = badge)
    }
    TabStrip(
        items, state.activeId,
        onSelect = { id -> if (id == state.activeId) renaming = id else state.activate(id) },
        modifier = modifier,
        onClose = state::closeTab,
        trailing = { IconAction(IconShape.Plus, onNewTab, Modifier) },
    )
    val target = renaming?.let(state::tab)
    if (target != null) {
        var name by remember(target.id) { mutableStateOf(target.title) }
        Modal(tr(SessionTexts.rename), { renaming = null }, width = 380.dp, actions = {
            ActionButton(tr(ControlTexts.apply), { state.rename(target.id, name); renaming = null }, primary = true)
        }) {
            TextInput(name, { name = it }, Modifier.fillMaxWidth(), placeholder = tr(SessionTexts.tabName), onSubmit = { state.rename(target.id, name); renaming = null })
        }
    }
}

// Pluralized message-count label for the UI. Workspace calls it; it reads SessionTexts via I18n.
@Composable
fun messageCountLabel(count: Int): String = tr(I18n.plural(count.toLong(), SessionTexts.messageCount, SessionTexts.messagesCount), count)
