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

@Serializable
data class LayoutNode(
    val pane: String? = null,
    val vertical: Boolean = true,
    val fraction: Float = 0.5f,
    val first: LayoutNode? = null,
    val second: LayoutNode? = null,
)

@Serializable
data class LayoutPreset(val name: String, val root: LayoutNode)

@Serializable
data class LayoutFile(val current: LayoutNode? = null, val presets: List<LayoutPreset> = emptyList())

internal val layoutPanes = listOf("story", "editor", "grid", "session", "side")

private val layoutJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

private fun leaf(pane: String) = LayoutNode(pane = pane)

private fun split(vertical: Boolean, fraction: Float, first: LayoutNode, second: LayoutNode) =
    LayoutNode(vertical = vertical, fraction = fraction, first = first, second = second)

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

internal fun paneNames(node: LayoutNode): List<String> =
    listOfNotNull(node.pane) + node.first?.let(::paneNames).orEmpty() + node.second?.let(::paneNames).orEmpty()

internal fun swapPanes(node: LayoutNode, from: String, to: String): LayoutNode {
    if (from == to) return node
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

internal fun updateFraction(node: LayoutNode, path: String, fraction: Float): LayoutNode {
    if (path.isEmpty()) return node.copy(fraction = fraction.coerceIn(0.16f, 0.84f))
    val rest = path.drop(1)
    return if (path[0] == 'a') node.copy(first = node.first?.let { updateFraction(it, rest, fraction) })
    else node.copy(second = node.second?.let { updateFraction(it, rest, fraction) })
}

internal fun complete(node: LayoutNode): Boolean = paneNames(node).toSet() == layoutPanes.toSet()

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

    fun place(id: String, rect: Rect) {
        bounds[id] = rect
    }

    fun beginDrag(id: String) {
        dragging = id
        dropTarget = null
    }

    fun hover(position: Offset) {
        val from = dragging ?: return
        dropTarget = bounds.entries.firstOrNull { it.key != from && it.value.contains(position) }?.key
    }

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

    fun cancel() {
        dragging = null
        dropTarget = null
    }

    fun resize(path: String, fraction: Float) {
        root = updateFraction(root, path, fraction)
    }

    fun commit() = persist()

    fun reset() {
        root = standardLayout()
        persist()
    }

    fun savePreset(name: String) {
        val title = name.trim()
        if (title.isEmpty()) return
        val next = LayoutPreset(title, root)
        val index = presets.indexOfFirst { it.name.equals(title, ignoreCase = true) }
        if (index >= 0) presets[index] = next else presets += next
        naming = false
        persist()
    }

    fun apply(preset: LayoutPreset) {
        if (!complete(preset.root)) return
        root = preset.root
        persist()
    }

    fun remove(preset: LayoutPreset) {
        presets.removeAll { it.name == preset.name }
        persist()
    }

    private fun persist() {
        runCatching {
            platform.storeValue(key, layoutJson.encodeToString(LayoutFile.serializer(), LayoutFile(root, presets.toList())))
        }
    }
}
