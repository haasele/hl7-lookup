// Split fractions and the resize and move cursors. controls/Index uses them; jvm and wasm supply the cursors.
package hl7lookup.controls

import androidx.compose.ui.input.pointer.PointerIcon

// Platform resize pointer for split handles. SplitPane asks for it; jvm/wasm provide the actual.
internal expect fun resizeCursor(vertical: Boolean): PointerIcon

// Platform move pointer for pane grips. paneDrag asks for it; jvm/wasm provide the actual.
internal expect fun moveCursor(): PointerIcon

// Column title, weight and min width for a table. TableHeader reads these from feature screens.
data class TableColumn(val title: String, val weight: Float, val minWidth: Int = 0)

// One tab's id, title and optional badge. TabStrip renders lists of these.
data class TabItem(val id: String, val title: String, val closable: Boolean = false, val badge: String? = null)

// Clamps a split fraction so neither pane collapses. SplitPane calls it while dragging.
internal fun clampFraction(value: Float, min: Float = 0.12f, max: Float = 0.88f): Float = value.coerceIn(min, max)

// Turns a pixel drag into a new split fraction. SplitPane feeds deltas into it.
internal fun dragFraction(current: Float, deltaPx: Float, totalPx: Float): Float =
    if (totalPx <= 0f || totalPx.isInfinite() || totalPx.isNaN()) current else clampFraction(current + deltaPx / totalPx, 0.16f, 0.84f)

// Formats part/total as a one-decimal percent. Controls.percent and statistics UIs call it.
internal fun percent(part: Int, total: Int): String =
    if (total == 0) "0%" else {
        val tenths = (part * 1000L + total / 2) / total
        "${tenths / 10}.${tenths % 10}%"
    }

// Truncates text with an ellipsis when too long. Controls.ellipsize and list labels call it.
internal fun ellipsize(text: String, max: Int): String = if (text.length <= max) text else text.take(max - 1) + "…"

// Replaces newlines with a return mark for single-line display. Controls.oneLine and grids call it.
internal fun oneLine(text: String): String = text.replace("\r\n", " ⏎ ").replace('\r', '⏎').replace('\n', '⏎')
