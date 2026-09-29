// Split tree and preset fractions. layout/Index exposes them.
package hl7lookup.layout

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import hl7lookup.platform.Platform
import hl7lookup.platform.Platforms
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// One leaf pane or a split with two children; LayoutState and presets store these trees.
@Serializable
data class LayoutNode(
    val pane: String? = null,
    val vertical: Boolean = true,
    val fraction: Float = 0.5f,
    val first: LayoutNode? = null,
    val second: LayoutNode? = null,
)

// Named saved tree; LayoutState keeps a list and LayoutFile serializes them.
@Serializable
data class LayoutPreset(val name: String, val root: LayoutNode)

// Persisted current tree plus presets; LayoutState loads and stores this via Platform.
@Serializable
data class LayoutFile(val current: LayoutNode? = null, val presets: List<LayoutPreset> = emptyList())

internal val layoutPanes = listOf("story", "editor", "grid", "session", "side")

private val layoutJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

// Builds a single-pane leaf node; standardLayout and helpers call this.
private fun leaf(pane: String) = LayoutNode(pane = pane)

// Builds a split node with fraction and children; standardLayout nests these.
private fun split(vertical: Boolean, fraction: Float, first: LayoutNode, second: LayoutNode) =
    LayoutNode(vertical = vertical, fraction = fraction, first = first, second = second)

// Default workbench split for all panes; Layouts.standard and LayoutState.reset call this.
internal fun standardLayout(): LayoutNode = split(
    vertical = true,
    fraction = 0.66f,
    first = split(
        vertical = false,
        fraction = 0.70f,
        first = split(vertical = true, fraction = 0.42f, first = leaf("story"), second = leaf("editor")),
        second = leaf("grid"),
    ),
    second = split(vertical = false, fraction = 0.62f, first = leaf("session"), second = leaf("side")),
)

// Collects leaf pane names depth-first; Layouts.panes and complete call this.
internal fun paneNames(node: LayoutNode): List<String> =
    listOfNotNull(node.pane) + node.first?.let(::paneNames).orEmpty() + node.second?.let(::paneNames).orEmpty()

// Swaps two pane ids throughout a tree; Layouts.swap and LayoutState.finish call this.
internal fun swapPanes(node: LayoutNode, from: String, to: String): LayoutNode {
    if (from == to) return node
    // Recursively rewrites pane ids in one subtree; swapPanes returns its result.
    fun walk(current: LayoutNode): LayoutNode {
        val pane = when (current.pane) {
            from -> to
            to -> from
            else -> current.pane
        }
        return current.copy(pane = pane, first = current.first?.let(::walk), second = current.second?.let(::walk))
    }
    return walk(node)
}

// Updates the split fraction at a path of a/b steps; LayoutState.resize calls this.
internal fun updateFraction(node: LayoutNode, path: String, fraction: Float): LayoutNode {
    if (path.isEmpty()) return node.copy(fraction = fraction.coerceIn(0.16f, 0.84f))
    val rest = path.drop(1)
    return if (path[0] == 'a') node.copy(first = node.first?.let { updateFraction(it, rest, fraction) })
    else node.copy(second = node.second?.let { updateFraction(it, rest, fraction) })
}

// True when the tree holds exactly the known panes; load, apply and save check this.
internal fun complete(node: LayoutNode): Boolean = paneNames(node).toSet() == layoutPanes.toSet()

// Live root, presets and drag state; Layouts.state builds it, Workbench and menus drive it.
class LayoutState(private val platform: Platform) {
    var root by mutableStateOf(standardLayout())
        private set
    val presets = mutableStateListOf<LayoutPreset>()
    var dragging by mutableStateOf<String?>(null)
        private set
    var dropTarget by mutableStateOf<String?>(null)
        private set
    var naming by mutableStateOf(false)
    private val bounds = mutableMapOf<String, Rect>()
    private val key = Platforms.key("layout", "v1")

    init {
        val file = platform.loadValue(key)?.let { runCatching { layoutJson.decodeFromString(LayoutFile.serializer(), it) }.getOrNull() }
        val current = file?.current
        if (current != null && complete(current)) root = current
        presets.addAll(file?.presets.orEmpty().filter { it.name.isNotBlank() && complete(it.root) })
    }

    // Records a pane's screen bounds for drop hit-testing; PaneFrame calls this on layout.
    fun place(id: String, rect: Rect) {
        bounds[id] = rect
    }

    // Starts a pane drag; PaneFrame's PaneMove calls this on drag start.
    fun beginDrag(id: String) {
        dragging = id
        dropTarget = null
    }

    // Updates which pane the pointer is over while dragging; PaneMove calls this on drag.
    fun hover(position: Offset) {
        val from = dragging ?: return
        dropTarget = bounds.entries.firstOrNull { it.key != from && it.value.contains(position) }?.key
    }

    // Commits a pane swap on drop; PaneMove calls this, it uses swapPanes and persist.
    fun finish() {
        val from = dragging
        val to = dropTarget
        dragging = null
        dropTarget = null
        if (from != null && to != null) {
            root = swapPanes(root, from, to)
            persist()
        }
    }

    // Clears an in-progress drag without swapping; PaneMove calls this on cancel.
    fun cancel() {
        dragging = null
        dropTarget = null
    }

    // Live-updates a split fraction while dragging the divider; LayoutBranch passes this to SplitPane.
    fun resize(path: String, fraction: Float) {
        root = updateFraction(root, path, fraction)
    }

    // Persists after a divider drag ends; SplitPane calls this via onCommit.
    fun commit() = persist()

    // Restores the standard tree and saves; LayoutMenu calls this.
    fun reset() {
        root = standardLayout()
        persist()
    }

    // Adds or replaces a named preset from the current root; LayoutPresetDialog calls this.
    fun savePreset(name: String) {
        val title = name.trim()
        if (title.isEmpty()) return
        val next = LayoutPreset(title, root)
        val index = presets.indexOfFirst { it.name.equals(title, ignoreCase = true) }
        if (index >= 0) presets[index] = next else presets += next
        naming = false
        persist()
    }

    // Loads a preset as the current root; LayoutMenu calls this on a preset entry.
    fun apply(preset: LayoutPreset) {
        if (!complete(preset.root)) return
        root = preset.root
        persist()
    }

    // Deletes a preset by name; LayoutPresetDialog calls this.
    fun remove(preset: LayoutPreset) {
        presets.removeAll { it.name == preset.name }
        persist()
    }

    // Writes current tree and presets to platform storage; mutators call this after changes.
    private fun persist() {
        runCatching {
            platform.storeValue(key, layoutJson.encodeToString(LayoutFile.serializer(), LayoutFile(root, presets.toList())))
        }
    }
}
