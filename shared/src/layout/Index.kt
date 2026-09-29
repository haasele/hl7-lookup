// Pane arrangement and presets. Workspace splits the screen through Layouts.
package hl7lookup.layout

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import hl7lookup.controls.ActionButton
import hl7lookup.controls.FormRow
import hl7lookup.controls.MenuButton
import hl7lookup.controls.MenuEntry
import hl7lookup.controls.Modal
import hl7lookup.controls.PaneMove
import hl7lookup.controls.SplitPane
import hl7lookup.controls.TextInput
import hl7lookup.i18n.tr
import hl7lookup.platform.Platform
import hl7lookup.theme.LocalPalette
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

// Entry point for pane trees and presets; Workspace calls it, methods forward to Logic.
object Layouts {
    // Builds persisted layout state for a host; Workspace calls this, it constructs LayoutState.
    fun state(platform: Platform): LayoutState = LayoutState(platform)
    // Returns the default split tree; Workspace and resets call this, it forwards to standardLayout.
    fun standard(): LayoutNode = standardLayout()
    // Exchanges two pane ids in a tree; callers use this, it forwards to swapPanes.
    fun swap(root: LayoutNode, from: String, to: String): LayoutNode = swapPanes(root, from, to)
    // Lists leaf pane ids under a root; callers use this, it forwards to paneNames.
    fun panes(root: LayoutNode): List<String> = paneNames(root)
    // Exposes layout menu wording; screens call this, it returns LayoutTexts.
    fun texts() = LayoutTexts
}

// Renders the full split tree into pane content; Workspace composes this, it calls LayoutBranch.
@Composable
fun Workbench(state: LayoutState, panes: Map<String, @Composable () -> Unit>, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize()) {
        LayoutBranch(state, state.root, "", panes)
    }
}

// Walks one split or leaf and draws SplitPane or PaneFrame; Workbench recurses through it.
@Composable
private fun LayoutBranch(state: LayoutState, node: LayoutNode, path: String, panes: Map<String, @Composable () -> Unit>) {
    val pane = node.pane
    if (pane != null) {
        PaneFrame(state, pane) { panes[pane]?.invoke() }
        return
    }
    val first = node.first ?: return
    val second = node.second ?: return
    SplitPane(
        vertical = node.vertical,
        fraction = node.fraction,
        onFraction = { state.resize(path, it) },
        onCommit = state::commit,
        modifier = Modifier.fillMaxSize(),
        first = { LayoutBranch(state, first, path + "a", panes) },
        second = { LayoutBranch(state, second, path + "b", panes) },
    )
}

// Hosts one pane with drag-drop move and drop highlight; LayoutBranch wraps content with it.
@Composable
private fun PaneFrame(state: LayoutState, id: String, content: @Composable () -> Unit) {
    val palette = LocalPalette.current
    val move = remember(state, id) {
        PaneMove(
            onDragStart = { state.beginDrag(id) },
            onDrag = state::hover,
            onDrop = state::finish,
            onCancel = state::cancel,
        )
    }
    val highlight = state.dropTarget == id && state.dragging != null
    Box(
        Modifier.fillMaxSize()
            .onGloballyPositioned { state.place(id, it.boundsInRoot()) }
            .then(if (highlight) Modifier.border(2.dp, palette.accent) else Modifier),
    ) {
        CompositionLocalProvider(hl7lookup.controls.LocalPaneMove provides move) {
            content()
        }
    }
}

// Menu for reset, save and applying presets; Workspace shows it, it calls LayoutState.
@Composable
fun LayoutMenu(state: LayoutState) {
    val texts = LayoutTexts
    val entries = buildList {
        add(MenuEntry(tr(texts.reset)) { state.reset() })
        add(MenuEntry(tr(texts.save)) { state.naming = true })
        state.presets.forEach { preset ->
            add(MenuEntry(preset.name) { state.apply(preset) })
        }
    }
    MenuButton(tr(texts.menu), entries)
}

// Dialog to name or remove a layout preset; Workspace shows it when naming is true.
@Composable
fun LayoutPresetDialog(state: LayoutState) {
    if (!state.naming) return
    var name by remember { mutableStateOf("") }
    Modal(tr(LayoutTexts.title), { state.naming = false }, actions = {
        ActionButton(tr(LayoutTexts.save), { state.savePreset(name) }, primary = true, enabled = name.isNotBlank())
    }) {
        FormRow(tr(LayoutTexts.name)) {
            TextInput(name, { name = it }, Modifier.fillMaxWidth())
        }
        state.presets.forEach { preset ->
            ActionButton(tr(LayoutTexts.remove) + " " + preset.name, { state.remove(preset) })
        }
    }
}
