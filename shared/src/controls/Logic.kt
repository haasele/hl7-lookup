package hl7lookup.controls

import androidx.compose.ui.input.pointer.PointerIcon

internal expect fun resizeCursor(vertical: Boolean): PointerIcon

internal expect fun moveCursor(): PointerIcon

data class TableColumn(val title: String, val weight: Float, val minWidth: Int = 0)

data class TabItem(val id: String, val title: String, val closable: Boolean = false, val badge: String? = null)

internal fun clampFraction(value: Float, min: Float = 0.12f, max: Float = 0.88f): Float = value.coerceIn(min, max)

internal fun dragFraction(current: Float, deltaPx: Float, totalPx: Float): Float =
    if (totalPx <= 0f) current else clampFraction(current + deltaPx / totalPx)

internal fun percent(part: Int, total: Int): String =
    if (total == 0) "0%" else {
        val tenths = (part * 1000L + total / 2) / total
        "${tenths / 10}.${tenths % 10}%"
    }

internal fun ellipsize(text: String, max: Int): String = if (text.length <= max) text else text.take(max - 1) + "…"

internal fun oneLine(text: String): String = text.replace("\r\n", " ⏎ ").replace('\r', '⏎').replace('\n', '⏎')
