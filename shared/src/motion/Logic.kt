package hl7lookup.motion

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform

enum class IllustrationKind { STORY, MESSAGE, MESSAGES, SENDER, RECEIVER, INTEGRATION, ACK, FIELDS, STATISTICS, VALIDATION }

data class VectorScene(val width: Float, val height: Float, val shapes: List<VectorShape>)

data class VectorShape(
    val path: Path,
    val stroke: Color?,
    val fill: Color?,
    val strokeWidth: Float,
    val cap: StrokeCap,
    val join: StrokeJoin,
)

internal const val splashDrawMs = 1100
internal const val splashHoldMs = 420
internal const val splashFadeMs = 420
internal const val entranceMs = 920

private val elementPattern = Regex("""<(circle|ellipse|rect|line|polyline|polygon|path)\b([^>]*?)/?>""", RegexOption.IGNORE_CASE)
private val attributePattern = Regex("""([\w:-]+)\s*=\s*(?:"([^"]*)"|'([^']*)')""")
private val pathTokenPattern = Regex("""[MmLlHhVvCcSsQqTtAaZz]|[-+]?(?:\d+\.\d+|\d+|\.\d+)(?:[eE][-+]?\d+)?""")
private val numberPattern = Regex("""[-+]?(?:\d+\.\d+|\d+|\.\d+)""")

internal fun illustrationScene(kind: IllustrationKind): VectorScene = parseSvg(plate(illustrationBody(kind)))

internal fun splashScene(): VectorScene = parseSvg(splashSvg)

internal fun parseSvg(source: String): VectorScene {
    val box = Regex("""viewBox\s*=\s*"([^"]+)"""").find(source)?.groupValues?.get(1)
        ?.split(Regex("[,\\s]+"))?.mapNotNull { it.toFloatOrNull() }.orEmpty()
    val width = box.getOrNull(2) ?: 80f
    val height = box.getOrNull(3) ?: width
    val shapes = elementPattern.findAll(source).flatMap { match ->
        styled(match.groupValues[1].lowercase(), attributes(match.groupValues[2]))
    }.toList()
    return VectorScene(width, height, shapes)
}

internal fun drawScene(scope: DrawScope, scene: VectorScene, reveal: Float) {
    val sx = scope.size.width / scene.width.coerceAtLeast(1f)
    val sy = scope.size.height / scene.height.coerceAtLeast(1f)
    val count = scene.shapes.size.coerceAtLeast(1)
    scope.withTransform({ scale(sx, sy, pivot = Offset.Zero) }) {
        scene.shapes.forEachIndexed { index, shape ->
            val start = if (count == 1) 0f else 0.42f * index / (count - 1)
            val local = ((reveal - start) / 0.58f).coerceIn(0f, 1f)
            if (local <= 0f) return@forEachIndexed
            val eased = 1f - (1f - local) * (1f - local)
            shape.fill?.let { fill -> drawPath(shape.path, fill.copy(alpha = fill.alpha * eased)) }
            val stroke = shape.stroke ?: return@forEachIndexed
            val measure = PathMeasure()
            measure.setPath(shape.path, false)
            val length = measure.length
            val partial = Path()
            val traced = length > 0.5f && measure.getSegment(0f, length * eased, partial, true)
            drawPath(
                if (traced) partial else shape.path,
                stroke.copy(alpha = stroke.alpha * if (traced) 1f else eased),
                style = Stroke(width = shape.strokeWidth, cap = shape.cap, join = shape.join),
            )
        }
    }
}

private fun plate(body: String): String = """
    <svg viewBox="0 0 80 80">
      <circle cx="40" cy="40" r="31" fill="#141E2B" stroke="#2A3A50" stroke-width="1.4"/>
      $body
    </svg>
""".trimIndent()

private fun illustrationBody(kind: IllustrationKind): String = when (kind) {
    IllustrationKind.STORY -> """
        <path d="M27 30 H38 C40 36 40 46 38 54 H27 Z" fill="none" stroke="#6EB6F0" stroke-width="1.6" stroke-linejoin="round"/>
        <path d="M53 30 H42 C40 36 40 46 42 54 H53 Z" fill="none" stroke="#6EB6F0" stroke-width="1.6" stroke-linejoin="round"/>
        <path d="M31 37 H36 M31 42 H36 M31 47 H35" fill="none" stroke="#8A9AB0" stroke-width="1.4" stroke-linecap="round"/>
        <path d="M44 37 H49 M44 42 H49 M45 47 H49" fill="none" stroke="#7FE6D8" stroke-width="1.4" stroke-linecap="round"/>
    """
    IllustrationKind.MESSAGE -> """
        <rect x="27" y="23" width="26" height="34" rx="3" fill="#0B1118" stroke="#6EB6F0" stroke-width="1.6"/>
        <path d="M33 33 V49" fill="none" stroke="#6EB6F0" stroke-width="1.8" stroke-linecap="round"/>
        <path d="M37 33 H48 M37 39 H46 M37 45 H43" fill="none" stroke="#D8E1EC" stroke-width="1.5" stroke-linecap="round"/>
    """
    IllustrationKind.MESSAGES -> """
        <rect x="23" y="24" width="34" height="10" rx="2.5" fill="none" stroke="#F26D85" stroke-width="1.5"/>
        <rect x="23" y="36" width="34" height="10" rx="2.5" fill="none" stroke="#7FE6D8" stroke-width="1.5"/>
        <rect x="23" y="48" width="34" height="10" rx="2.5" fill="none" stroke="#8FA8F2" stroke-width="1.5"/>
        <path d="M28 29 H48" fill="none" stroke="#F26D85" stroke-width="1.3" stroke-linecap="round"/>
        <path d="M28 41 H44" fill="none" stroke="#7FE6D8" stroke-width="1.3" stroke-linecap="round"/>
        <path d="M28 53 H46" fill="none" stroke="#8FA8F2" stroke-width="1.3" stroke-linecap="round"/>
    """
    IllustrationKind.SENDER -> """
        <circle cx="28" cy="40" r="7" fill="none" stroke="#6EB6F0" stroke-width="1.6"/>
        <path d="M35 40 H46" fill="none" stroke="#8A9AB0" stroke-width="1.6" stroke-linecap="round"/>
        <path d="M46 33 L57 40 L46 47" fill="none" stroke="#2F7FD1" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
    """
    IllustrationKind.RECEIVER -> """
        <path d="M25 36 H55 L50 55 H30 Z" fill="none" stroke="#6EB6F0" stroke-width="1.6" stroke-linejoin="round"/>
        <path d="M25 36 L40 46 L55 36" fill="none" stroke="#7FE6D8" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>
        <path d="M36 24 L40 31 L44 24" fill="none" stroke="#2F7FD1" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/>
    """
    IllustrationKind.INTEGRATION -> """
        <circle cx="40" cy="27" r="5.5" fill="none" stroke="#6EB6F0" stroke-width="1.6"/>
        <circle cx="26" cy="52" r="5.5" fill="none" stroke="#7FE6D8" stroke-width="1.6"/>
        <circle cx="54" cy="52" r="5.5" fill="none" stroke="#F26D85" stroke-width="1.6"/>
        <path d="M37 32 L30 47 M43 32 L50 47 M32 52 H48" fill="none" stroke="#8A9AB0" stroke-width="1.4" stroke-linecap="round"/>
    """
    IllustrationKind.ACK -> """
        <rect x="22" y="24" width="36" height="24" rx="6" fill="none" stroke="#6EB6F0" stroke-width="1.6"/>
        <path d="M34 48 V56 L44 48" fill="none" stroke="#6EB6F0" stroke-width="1.6" stroke-linejoin="round"/>
        <path d="M32 35 L38 41 L49 29" fill="none" stroke="#3FB950" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"/>
    """
    IllustrationKind.FIELDS -> """
        <rect x="24" y="24" width="32" height="32" rx="3" fill="none" stroke="#6EB6F0" stroke-width="1.5"/>
        <path d="M24 34.7 H56 M24 45.3 H56 M34.7 24 V56 M45.3 24 V56" fill="none" stroke="#8A9AB0" stroke-width="1.15"/>
        <rect x="34.7" y="34.7" width="10.6" height="10.6" fill="#1B7BBC"/>
    """
    IllustrationKind.STATISTICS -> """
        <path d="M22 58 H58" fill="none" stroke="#2A3A50" stroke-width="1.5" stroke-linecap="round"/>
        <path d="M30 56 V40" fill="none" stroke="#8FA8F2" stroke-width="5.5" stroke-linecap="round"/>
        <path d="M40 56 V28" fill="none" stroke="#6EB6F0" stroke-width="5.5" stroke-linecap="round"/>
        <path d="M50 56 V36" fill="none" stroke="#7FE6D8" stroke-width="5.5" stroke-linecap="round"/>
    """
    IllustrationKind.VALIDATION -> """
        <path d="M40 20 L56 27 V42 C56 51 40 59 40 59 C40 59 24 51 24 42 V27 Z" fill="none" stroke="#6EB6F0" stroke-width="1.6" stroke-linejoin="round"/>
        <path d="M33 40 L38 46 L49 33" fill="none" stroke="#3FB950" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"/>
    """
}

private val splashSvg = """
    <svg viewBox="0 0 120 120">
      <circle cx="60" cy="60" r="52" fill="#141E2B" stroke="#2A3A50" stroke-width="1.5"/>
      <rect x="38" y="30" width="44" height="56" rx="6" fill="#0B1118" stroke="#6EB6F0" stroke-width="1.7"/>
      <path d="M48 46 V74" fill="none" stroke="#6EB6F0" stroke-width="2.2" stroke-linecap="round"/>
      <path d="M54 48 H74" fill="none" stroke="#F26D85" stroke-width="3" stroke-linecap="round"/>
      <path d="M54 58 H70" fill="none" stroke="#7FE6D8" stroke-width="3" stroke-linecap="round"/>
      <path d="M54 68 H66" fill="none" stroke="#8FA8F2" stroke-width="3" stroke-linecap="round"/>
      <circle cx="78" cy="78" r="12" fill="#0E1520" stroke="#2F7FD1" stroke-width="2"/>
      <path d="M86 86 L96 96" fill="none" stroke="#2F7FD1" stroke-width="2.2" stroke-linecap="round"/>
    </svg>
""".trimIndent()

private class Contour(val path: Path) {
    var ink: Boolean = false
}

private fun attributes(raw: String): Map<String, String> {
    val map = LinkedHashMap<String, String>()
    attributePattern.findAll(raw).forEach { match ->
        val quoted = match.groups[2]
        map[match.groupValues[1].lowercase()] = if (quoted != null) match.groupValues[2] else match.groupValues[3]
    }
    return map
}

private fun styled(tag: String, attr: Map<String, String>): List<VectorShape> {
    val paths = geometry(tag, attr)
    if (paths.isEmpty()) return emptyList()
    val opacity = attr["opacity"]?.toFloatOrNull() ?: 1f
    val fillOpacity = attr["fill-opacity"]?.toFloatOrNull() ?: 1f
    val strokeOpacity = attr["stroke-opacity"]?.toFloatOrNull() ?: 1f
    val fill = parseColor(attr["fill"])?.let { it.copy(alpha = it.alpha * opacity * fillOpacity) }
    val stroke = parseColor(attr["stroke"])?.let { it.copy(alpha = it.alpha * opacity * strokeOpacity) }
    if (fill == null && stroke == null) return emptyList()
    val width = dimension(attr["stroke-width"]) ?: 1.6f
    val cap = when (attr["stroke-linecap"]?.lowercase()) {
        "round" -> StrokeCap.Round
        "square" -> StrokeCap.Square
        else -> StrokeCap.Butt
    }
    val join = when (attr["stroke-linejoin"]?.lowercase()) {
        "round" -> StrokeJoin.Round
        "bevel" -> StrokeJoin.Bevel
        else -> StrokeJoin.Miter
    }
    return paths.map { VectorShape(it, stroke, fill, width, cap, join) }
}

private fun geometry(tag: String, attr: Map<String, String>): List<Path> = when (tag) {
    "path" -> vectorContours(attr["d"].orEmpty())
    "circle" -> {
        val r = dimension(attr["r"]) ?: 0f
        if (r <= 0f) emptyList() else listOf(oval(dimension(attr["cx"]) ?: 0f, dimension(attr["cy"]) ?: 0f, r, r))
    }
    "ellipse" -> {
        val rx = dimension(attr["rx"]) ?: 0f
        val ry = dimension(attr["ry"]) ?: 0f
        if (rx <= 0f || ry <= 0f) emptyList() else listOf(oval(dimension(attr["cx"]) ?: 0f, dimension(attr["cy"]) ?: 0f, rx, ry))
    }
    "rect" -> {
        val w = dimension(attr["width"]) ?: 0f
        val h = dimension(attr["height"]) ?: 0f
        if (w <= 0f || h <= 0f) emptyList()
        else listOf(roundedRect(dimension(attr["x"]) ?: 0f, dimension(attr["y"]) ?: 0f, w, h, dimension(attr["rx"]) ?: 0f, dimension(attr["ry"]) ?: dimension(attr["rx"]) ?: 0f))
    }
    "line" -> listOf(Path().apply {
        moveTo(dimension(attr["x1"]) ?: 0f, dimension(attr["y1"]) ?: 0f)
        lineTo(dimension(attr["x2"]) ?: 0f, dimension(attr["y2"]) ?: 0f)
    })
    "polyline", "polygon" -> {
        val points = numberPattern.findAll(attr["points"].orEmpty()).map { it.value.toFloat() }.toList()
        if (points.size < 4) emptyList()
        else listOf(Path().apply {
            moveTo(points[0], points[1])
            var index = 2
            while (index + 1 < points.size) {
                lineTo(points[index], points[index + 1])
                index += 2
            }
            if (tag == "polygon") close()
        })
    }
    else -> emptyList()
}

private fun oval(cx: Float, cy: Float, rx: Float, ry: Float): Path = Path().apply {
    addOval(Rect(cx - rx, cy - ry, cx + rx, cy + ry))
}

private fun roundedRect(x: Float, y: Float, w: Float, h: Float, rx: Float, ry: Float): Path {
    val path = Path()
    val rrx = rx.coerceIn(0f, w / 2f)
    val rry = ry.coerceIn(0f, h / 2f)
    if (rrx <= 0.01f || rry <= 0.01f) {
        path.addRect(Rect(x, y, x + w, y + h))
        return path
    }
    val kx = rrx * 0.55228475f
    val ky = rry * 0.55228475f
    path.moveTo(x + rrx, y)
    path.lineTo(x + w - rrx, y)
    path.cubicTo(x + w - rrx + kx, y, x + w, y + rry - ky, x + w, y + rry)
    path.lineTo(x + w, y + h - rry)
    path.cubicTo(x + w, y + h - rry + ky, x + w - rrx + kx, y + h, x + w - rrx, y + h)
    path.lineTo(x + rrx, y + h)
    path.cubicTo(x + rrx - kx, y + h, x, y + h - rry + ky, x, y + h - rry)
    path.lineTo(x, y + rry)
    path.cubicTo(x, y + rry - ky, x + rrx - kx, y, x + rrx, y)
    path.close()
    return path
}

private fun vectorContours(description: String): List<Path> {
    val tokens = pathTokenPattern.findAll(description).map { it.value }.toList()
    if (tokens.isEmpty()) return emptyList()
    val contours = ArrayList<Contour>()
    var contour: Contour? = null
    var index = 0
    var command = 'L'
    var cx = 0f
    var cy = 0f
    var sx = 0f
    var sy = 0f
    var ctrlX = 0f
    var ctrlY = 0f
    var cubic = false
    var quad = false

    fun numberAt(at: Int): Boolean {
        if (at >= tokens.size) return false
        val char = tokens[at][0]
        return char.isDigit() || char == '-' || char == '+' || char == '.'
    }
    fun take(count: Int): List<Float>? {
        if ((0 until count).any { !numberAt(index + it) }) return null
        return List(count) { tokens[index++].toFloat() }
    }
    fun begin(x: Float, y: Float) {
        val current = contour
        val next = if (current == null || current.ink) Contour(Path()).also { contours += it } else current
        contour = next
        next.path.moveTo(x, y)
        cx = x
        cy = y
        sx = x
        sy = y
        cubic = false
        quad = false
    }
    fun mark() {
        contour?.ink = true
    }
    fun line(x: Float, y: Float) {
        if (contour == null) begin(cx, cy)
        contour!!.path.lineTo(x, y)
        mark()
        cx = x
        cy = y
        cubic = false
        quad = false
    }

    while (index < tokens.size) {
        if (!numberAt(index)) {
            command = tokens[index][0]
            index++
            if (command == 'Z' || command == 'z') {
                contour?.path?.close()
                mark()
                cx = sx
                cy = sy
                cubic = false
                quad = false
                continue
            }
        }
        val absolute = command.isUpperCase()
        when (command.uppercaseChar()) {
            'M' -> {
                val pair = take(2) ?: break
                val x = if (absolute) pair[0] else cx + pair[0]
                val y = if (absolute) pair[1] else cy + pair[1]
                begin(x, y)
                command = if (absolute) 'L' else 'l'
            }
            'L' -> {
                val pair = take(2) ?: break
                line(if (absolute) pair[0] else cx + pair[0], if (absolute) pair[1] else cy + pair[1])
            }
            'H' -> {
                val value = take(1) ?: break
                line(if (absolute) value[0] else cx + value[0], cy)
            }
            'V' -> {
                val value = take(1) ?: break
                line(cx, if (absolute) value[0] else cy + value[0])
            }
            'C' -> {
                val values = take(6) ?: break
                val x1 = if (absolute) values[0] else cx + values[0]
                val y1 = if (absolute) values[1] else cy + values[1]
                val x2 = if (absolute) values[2] else cx + values[2]
                val y2 = if (absolute) values[3] else cy + values[3]
                val x = if (absolute) values[4] else cx + values[4]
                val y = if (absolute) values[5] else cy + values[5]
                if (contour == null) begin(cx, cy)
                contour!!.path.cubicTo(x1, y1, x2, y2, x, y)
                mark()
                ctrlX = x2
                ctrlY = y2
                cx = x
                cy = y
                cubic = true
                quad = false
            }
            'S' -> {
                val values = take(4) ?: break
                val x1 = if (cubic) 2f * cx - ctrlX else cx
                val y1 = if (cubic) 2f * cy - ctrlY else cy
                val x2 = if (absolute) values[0] else cx + values[0]
                val y2 = if (absolute) values[1] else cy + values[1]
                val x = if (absolute) values[2] else cx + values[2]
                val y = if (absolute) values[3] else cy + values[3]
                if (contour == null) begin(cx, cy)
                contour!!.path.cubicTo(x1, y1, x2, y2, x, y)
                mark()
                ctrlX = x2
                ctrlY = y2
                cx = x
                cy = y
                cubic = true
                quad = false
            }
            'Q' -> {
                val values = take(4) ?: break
                val x1 = if (absolute) values[0] else cx + values[0]
                val y1 = if (absolute) values[1] else cy + values[1]
                val x = if (absolute) values[2] else cx + values[2]
                val y = if (absolute) values[3] else cy + values[3]
                if (contour == null) begin(cx, cy)
                contour!!.path.quadraticTo(x1, y1, x, y)
                mark()
                ctrlX = x1
                ctrlY = y1
                cx = x
                cy = y
                quad = true
                cubic = false
            }
            'T' -> {
                val values = take(2) ?: break
                val x1 = if (quad) 2f * cx - ctrlX else cx
                val y1 = if (quad) 2f * cy - ctrlY else cy
                val x = if (absolute) values[0] else cx + values[0]
                val y = if (absolute) values[1] else cy + values[1]
                if (contour == null) begin(cx, cy)
                contour!!.path.quadraticTo(x1, y1, x, y)
                mark()
                ctrlX = x1
                ctrlY = y1
                cx = x
                cy = y
                quad = true
                cubic = false
            }
            'A' -> {
                val values = take(7) ?: break
                line(if (absolute) values[5] else cx + values[5], if (absolute) values[6] else cy + values[6])
            }
            else -> break
        }
    }
    return contours.filter { it.ink }.map { it.path }
}

private fun dimension(value: String?): Float? = value?.trim()?.removeSuffix("px")?.toFloatOrNull()

private fun parseColor(value: String?): Color? {
    if (value == null || value.equals("none", true) || value.equals("transparent", true)) return null
    val hex = value.removePrefix("#")
    val packed = hex.toLongOrNull(16) ?: return null
    return when (hex.length) {
        3 -> Color(
            red = ((packed shr 8) and 0xF).toInt() * 17,
            green = ((packed shr 4) and 0xF).toInt() * 17,
            blue = (packed and 0xF).toInt() * 17,
        )
        6 -> Color(
            red = ((packed shr 16) and 0xFF).toInt(),
            green = ((packed shr 8) and 0xFF).toInt(),
            blue = (packed and 0xFF).toInt(),
        )
        8 -> Color(
            red = ((packed shr 24) and 0xFF).toInt(),
            green = ((packed shr 16) and 0xFF).toInt(),
            blue = ((packed shr 8) and 0xFF).toInt(),
            alpha = (packed and 0xFF).toInt(),
        )
        else -> null
    }
}
