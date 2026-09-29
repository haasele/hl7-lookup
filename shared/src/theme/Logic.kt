// Colors and drawn icon shapes. theme/Index exposes them.
package hl7lookup.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

// Color tokens for the dark UI; AppTheme builds one and LocalPalette provides it.
data class Palette(
    val background: Color = Color(0xFF0E1520),
    val surface: Color = Color(0xFF141E2B),
    val surfaceRaised: Color = Color(0xFF1A2636),
    val input: Color = Color(0xFF0B1118),
    val border: Color = Color(0xFF2A3A50),
    val accent: Color = Color(0xFF2F7FD1),
    val accentStrong: Color = Color(0xFF1E66B0),
    val topBar: Color = Color(0xFF1B5E9E),
    val text: Color = Color(0xFFD8E1EC),
    val textDim: Color = Color(0xFF8A9AB0),
    val link: Color = Color(0xFF6CC4FF),
    val selected: Color = Color(0xFF5A6E14),
    val selectedBorder: Color = Color(0xFFB9D23A),
    val fieldRow: Color = Color(0xFF0F5E9C),
    val componentRow: Color = Color(0xFF1B7BBC),
    val subcomponentRow: Color = Color(0xFF3A93CC),
    val success: Color = Color(0xFF3FB950),
    val warning: Color = Color(0xFFD29922),
    val error: Color = Color(0xFFF85149),
    val delimiter: Color = Color(0xFF6EB6F0),
    val componentDelimiter: Color = Color(0xFFE0C36A),
    val repetitionDelimiter: Color = Color(0xFFFF8F6B),
    val subcomponentDelimiter: Color = Color(0xFFB48EF0),
    val search: Color = Color(0x66FFD54F),
    val cursorField: Color = Color(0x553D8BFF),
)

// Named glyph kinds for toolbar icons; Icon and drawIconShape switch on these.
enum class IconShape { Close, Plus, ChevronLeft, ChevronRight, ChevronDown, Filter, Calendar, Search, Send, Play, Stop, Edit, Trash, Check, Warning, Swap, Download, Upload, Menu, File }

private val segmentColors = mapOf(
    "MSH" to Color(0xFFF26D85), "EVN" to Color(0xFFD7A6F2), "PID" to Color(0xFF7FE6D8), "PD1" to Color(0xFF9CD9C4),
    "PV1" to Color(0xFF8FA8F2), "PV2" to Color(0xFFE9F28A), "NK1" to Color(0xFFF7D9A8), "GT1" to Color(0xFFF7B3D9),
    "IN1" to Color(0xFFD9F58F), "IN2" to Color(0xFFA2F5B5), "DG1" to Color(0xFFF79F9F), "AL1" to Color(0xFF8CD9A3),
    "PR1" to Color(0xFFC2A9D1), "ORC" to Color(0xFFF2B872), "OBR" to Color(0xFF7FC8F2), "OBX" to Color(0xFFB3E07A),
    "NTE" to Color(0xFFBFC7D5), "TXA" to Color(0xFF9EE0F0), "SCH" to Color(0xFFF2A07F), "AIS" to Color(0xFFE6C7F2),
    "AIG" to Color(0xFFC7D3F2), "AIL" to Color(0xFFF2E3C7), "AIP" to Color(0xFFC7F2E0), "RGS" to Color(0xFFD5D5A0),
    "RF1" to Color(0xFFF2C77F), "PRD" to Color(0xFFA0D5F2), "RXA" to Color(0xFFF28FC0), "RXR" to Color(0xFFD5A0F2),
    "MSA" to Color(0xFF7FF2B0), "ERR" to Color(0xFFFF7A7A), "ROL" to Color(0xFFB0C4DE), "SFT" to Color(0xFFAAB7C4),
)

private val fallbackSegmentColors = listOf(
    Color(0xFF9AD0EC), Color(0xFFECC79A), Color(0xFFB9EC9A), Color(0xFFEC9AC8), Color(0xFF9AECDD), Color(0xFFD0B4F0),
)

// Resolves a segment name to a tint, falling back by hash; Theme.segmentColor calls this.
internal fun colorForSegment(name: String): Color =
    segmentColors[name] ?: fallbackSegmentColors[(name.hashCode() and 0x7FFFFFFF) % fallbackSegmentColors.size]

internal val highlightColors = listOf(
    Color(0xFFFFD54F), Color(0xFF4FC3F7), Color(0xFFAED581), Color(0xFFF06292), Color(0xFFBA68C8), Color(0xFFFF8A65),
)

// Cycles a highlight tint by index; Theme.highlight calls this.
internal fun highlightColor(index: Int): Color = highlightColors[index.mod(highlightColors.size)]

// Strokes or fills one icon glyph into a DrawScope; Icon calls this.
internal fun drawIconShape(scope: DrawScope, shape: IconShape, color: Color) {
    val w = scope.size.width
    val h = scope.size.height
    val stroke = Stroke(width = (w / 8f).coerceAtLeast(1.5f), cap = StrokeCap.Round)
    // Draws one scaled stroke segment; drawIconShape uses this for line icons.
    fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
        scope.drawLine(color, Offset(w * x1, h * y1), Offset(w * x2, h * y2), stroke.width, StrokeCap.Round)
    // Builds and draws a scaled path; drawIconShape uses this for filled or stroked shapes.
    fun path(vararg points: Pair<Float, Float>, close: Boolean = false, fill: Boolean = false) {
        val p = Path()
        points.forEachIndexed { i, (x, y) -> if (i == 0) p.moveTo(w * x, h * y) else p.lineTo(w * x, h * y) }
        if (close) p.close()
        if (fill) scope.drawPath(p, color) else scope.drawPath(p, color, style = stroke)
    }
    when (shape) {
        IconShape.Close -> { line(0.25f, 0.25f, 0.75f, 0.75f); line(0.75f, 0.25f, 0.25f, 0.75f) }
        IconShape.Plus -> { line(0.5f, 0.2f, 0.5f, 0.8f); line(0.2f, 0.5f, 0.8f, 0.5f) }
        IconShape.ChevronLeft -> path(0.62f to 0.22f, 0.36f to 0.5f, 0.62f to 0.78f)
        IconShape.ChevronRight -> path(0.38f to 0.22f, 0.64f to 0.5f, 0.38f to 0.78f)
        IconShape.ChevronDown -> path(0.22f to 0.36f, 0.5f to 0.66f, 0.78f to 0.36f, close = true, fill = true)
        IconShape.Filter -> path(0.15f to 0.22f, 0.85f to 0.22f, 0.58f to 0.52f, 0.58f to 0.8f, 0.42f to 0.72f, 0.42f to 0.52f, close = true)
        IconShape.Calendar -> {
            path(0.18f to 0.28f, 0.82f to 0.28f, 0.82f to 0.82f, 0.18f to 0.82f, close = true)
            line(0.18f, 0.44f, 0.82f, 0.44f); line(0.36f, 0.16f, 0.36f, 0.3f); line(0.64f, 0.16f, 0.64f, 0.3f)
        }
        IconShape.Search -> {
            scope.drawCircle(color, radius = w * 0.24f, center = Offset(w * 0.44f, h * 0.44f), style = stroke)
            line(0.62f, 0.62f, 0.82f, 0.82f)
        }
        IconShape.Send -> path(0.16f to 0.2f, 0.86f to 0.5f, 0.16f to 0.8f, 0.3f to 0.5f, close = true)
        IconShape.Play -> path(0.3f to 0.2f, 0.78f to 0.5f, 0.3f to 0.8f, close = true, fill = true)
        IconShape.Stop -> path(0.26f to 0.26f, 0.74f to 0.26f, 0.74f to 0.74f, 0.26f to 0.74f, close = true, fill = true)
        IconShape.Edit -> { line(0.24f, 0.76f, 0.7f, 0.3f); path(0.62f to 0.22f, 0.78f to 0.38f); line(0.2f, 0.82f, 0.36f, 0.8f) }
        IconShape.Trash -> {
            path(0.28f to 0.32f, 0.34f to 0.82f, 0.66f to 0.82f, 0.72f to 0.32f)
            line(0.2f, 0.28f, 0.8f, 0.28f); line(0.42f, 0.18f, 0.58f, 0.18f)
        }
        IconShape.Check -> path(0.2f to 0.52f, 0.42f to 0.74f, 0.8f to 0.28f)
        IconShape.Warning -> {
            path(0.5f to 0.16f, 0.86f to 0.82f, 0.14f to 0.82f, close = true)
            line(0.5f, 0.42f, 0.5f, 0.6f); line(0.5f, 0.7f, 0.5f, 0.71f)
        }
        IconShape.Swap -> { path(0.22f to 0.36f, 0.78f to 0.36f, 0.62f to 0.2f); path(0.78f to 0.64f, 0.22f to 0.64f, 0.38f to 0.8f) }
        IconShape.Download -> { line(0.5f, 0.16f, 0.5f, 0.64f); path(0.3f to 0.46f, 0.5f to 0.66f, 0.7f to 0.46f); line(0.2f, 0.84f, 0.8f, 0.84f) }
        IconShape.Upload -> { line(0.5f, 0.66f, 0.5f, 0.18f); path(0.3f to 0.38f, 0.5f to 0.18f, 0.7f to 0.38f); line(0.2f, 0.84f, 0.8f, 0.84f) }
        IconShape.Menu -> { line(0.2f, 0.3f, 0.8f, 0.3f); line(0.2f, 0.5f, 0.8f, 0.5f); line(0.2f, 0.7f, 0.8f, 0.7f) }
        IconShape.File -> {
            path(0.28f to 0.14f, 0.58f to 0.14f, 0.76f to 0.32f, 0.76f to 0.86f, 0.28f to 0.86f, close = true)
            line(0.58f, 0.14f, 0.58f, 0.32f); line(0.58f, 0.32f, 0.76f, 0.32f)
            line(0.38f, 0.48f, 0.66f, 0.48f); line(0.38f, 0.60f, 0.66f, 0.60f); line(0.38f, 0.72f, 0.54f, 0.72f)
        }
    }
}
