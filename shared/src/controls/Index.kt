package hl7lookup.controls

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import hl7lookup.i18n.tr
import hl7lookup.motion.Illustration
import hl7lookup.motion.IllustrationKind
import hl7lookup.theme.Icon
import hl7lookup.theme.IconShape
import hl7lookup.theme.LocalMonoFont
import hl7lookup.theme.LocalPalette

object Controls {
    fun percent(part: Int, total: Int): String = hl7lookup.controls.percent(part, total)
    fun ellipsize(text: String, max: Int): String = hl7lookup.controls.ellipsize(text, max)
    fun oneLine(text: String): String = hl7lookup.controls.oneLine(text)
    fun texts() = ControlTexts
}

@Composable
fun Label(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalPalette.current.text,
    size: TextUnit = 13.sp,
    weight: FontWeight = FontWeight.Normal,
    mono: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    align: TextAlign = TextAlign.Start,
) {
    Text(
        text,
        modifier = modifier,
        color = color,
        fontSize = size,
        fontWeight = weight,
        fontFamily = if (mono) LocalMonoFont.current else null,
        maxLines = maxLines,
        overflow = if (maxLines == Int.MAX_VALUE) TextOverflow.Clip else TextOverflow.Ellipsis,
        textAlign = align,
    )
}

@Composable
fun SplitPane(
    vertical: Boolean,
    fraction: Float,
    onFraction: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onCommit: () -> Unit = {},
    first: @Composable () -> Unit,
    second: @Composable () -> Unit,
) {
    val palette = LocalPalette.current
    val clamped = clampFraction(fraction, 0.16f, 0.84f)
    BoxWithConstraints(modifier) {
        val total = with(LocalDensity.current) { (if (vertical) maxHeight else maxWidth).toPx() }
        var dragging by remember { mutableStateOf(false) }
        val live = remember { floatArrayOf(clamped) }
        val span = remember { floatArrayOf(total) }
        span[0] = total
        if (!dragging) live[0] = clamped
        val state = rememberDraggableState { delta ->
            live[0] = dragFraction(live[0], delta, span[0])
            onFraction(live[0])
        }
        val handle = Modifier
            .background(palette.border)
            .pointerHoverIcon(resizeCursor(vertical))
            .draggable(
                state,
                if (vertical) Orientation.Vertical else Orientation.Horizontal,
                onDragStarted = { dragging = true },
                onDragStopped = {
                    dragging = false
                    onCommit()
                },
            )
        val grip = @Composable {
            Canvas(Modifier.size(if (vertical) 28.dp else 10.dp, if (vertical) 10.dp else 28.dp)) {
                val color = palette.textDim
                val count = 3
                repeat(count) { index ->
                    val t = (index + 1).toFloat() / (count + 1)
                    val center = if (vertical) Offset(size.width * t, size.height / 2f) else Offset(size.width / 2f, size.height * t)
                    drawCircle(color, radius = 1.6.dp.toPx(), center = center)
                }
            }
        }
        if (vertical) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().weight(clamped)) { first() }
                Box(handle.fillMaxWidth().height(10.dp), contentAlignment = Alignment.Center) { grip() }
                Box(Modifier.fillMaxWidth().weight(1f - clamped)) { second() }
            }
        } else {
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxHeight().weight(clamped)) { first() }
                Box(handle.fillMaxHeight().width(10.dp), contentAlignment = Alignment.Center) { grip() }
                Box(Modifier.fillMaxHeight().weight(1f - clamped)) { second() }
            }
        }
    }
}

@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.background(LocalPalette.current.surface), content = content)
}

class PaneMove(
    val onDragStart: () -> Unit,
    val onDrag: (Offset) -> Unit,
    val onDrop: () -> Unit,
    val onCancel: () -> Unit,
)

val LocalPaneMove: androidx.compose.runtime.ProvidableCompositionLocal<PaneMove?> = staticCompositionLocalOf { null }

@Composable
fun PanelHeader(title: String, modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit = {}) {
    val palette = LocalPalette.current
    val move = LocalPaneMove.current
    Row(
        modifier.fillMaxWidth().background(palette.surfaceRaised).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (move != null) {
            Row(
                Modifier.weight(1f).paneDrag(move),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                MoveGrip(palette.textDim)
                Label(title, Modifier.weight(1f), weight = FontWeight.SemiBold, maxLines = 1)
            }
        } else {
            Label(title, Modifier.weight(1f), weight = FontWeight.SemiBold, maxLines = 1)
        }
        actions()
    }
}

@Composable
private fun Modifier.paneDrag(move: PaneMove): Modifier {
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    return onGloballyPositioned { coordinates = it }
        .pointerHoverIcon(moveCursor())
        .pointerInput(move) {
            var at = Offset.Zero
            detectDragGestures(
                onDragStart = { local ->
                    at = coordinates?.localToRoot(local) ?: local
                    move.onDragStart()
                    move.onDrag(at)
                },
                onDrag = { _, amount ->
                    at += amount
                    move.onDrag(at)
                },
                onDragEnd = { move.onDrop() },
                onDragCancel = { move.onCancel() },
            )
        }
}

@Composable
private fun MoveGrip(color: Color) {
    Canvas(Modifier.size(18.dp, 16.dp)) {
        repeat(2) { column ->
            repeat(3) { row ->
                drawCircle(color, radius = 1.3.dp.toPx(), center = Offset(size.width * (0.3f + column * 0.4f), size.height * (0.2f + row * 0.3f)))
            }
        }
    }
}

@Composable
fun ActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: IconShape? = null,
    primary: Boolean = false,
    enabled: Boolean = true,
    danger: Boolean = false,
) {
    val palette = LocalPalette.current
    val background = when {
        !enabled -> palette.surfaceRaised
        danger -> palette.error.copy(alpha = 0.8f)
        primary -> palette.accent
        else -> palette.surfaceRaised
    }
    val content = if (enabled) (if (primary || danger) Color.White else palette.text) else palette.textDim
    Row(
        modifier
            .clip(RoundedCornerShape(4.dp))
            .background(background)
            .border(1.dp, if (primary) palette.accentStrong else palette.border, RoundedCornerShape(4.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) Icon(icon, content, size = 12.dp)
        Label(label, color = content, size = 12.sp, maxLines = 1)
    }
}

@Composable
fun IconAction(shape: IconShape, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color? = null, enabled: Boolean = true, size: Dp = 14.dp) {
    val palette = LocalPalette.current
    Box(
        modifier
            .clip(RoundedCornerShape(3.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(shape, if (enabled) color ?: palette.text else palette.textDim, size = size)
    }
}

@Composable
fun LinkText(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, size: TextUnit = 13.sp, mono: Boolean = false) {
    Label(text, modifier.clickable(onClick = onClick).pointerHoverIcon(PointerIcon.Hand), color = LocalPalette.current.link, size = size, mono = mono)
}

@Composable
fun TextInput(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    mono: Boolean = false,
    enabled: Boolean = true,
    error: Boolean = false,
    onSubmit: (() -> Unit)? = null,
) {
    val palette = LocalPalette.current
    val style = TextStyle(color = palette.text, fontSize = 13.sp, fontFamily = if (mono) LocalMonoFont.current else null)
    BasicTextField(
        value = value,
        onValueChange = onChange,
        enabled = enabled,
        singleLine = singleLine,
        textStyle = style,
        cursorBrush = SolidColor(palette.link),
        keyboardOptions = KeyboardOptions(imeAction = if (onSubmit != null) ImeAction.Done else ImeAction.Default),
        keyboardActions = KeyboardActions(onDone = { onSubmit?.invoke() }),
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(palette.input)
            .border(1.dp, if (error) palette.error else palette.border, RoundedCornerShape(3.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty() && placeholder.isNotEmpty()) Label(placeholder, color = palette.textDim, mono = mono, maxLines = 1)
                inner()
            }
        },
    )
}

@Composable
fun <T> Dropdown(
    selected: T,
    options: List<T>,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val palette = LocalPalette.current
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        Row(
            Modifier
                .clip(RoundedCornerShape(3.dp))
                .background(palette.input)
                .border(1.dp, palette.border, RoundedCornerShape(3.dp))
                .clickable(enabled = enabled) { open = true }
                .pointerHoverIcon(PointerIcon.Hand)
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Label(label(selected), Modifier.weight(1f, fill = false), size = 12.sp, maxLines = 1)
            Icon(IconShape.ChevronDown, palette.textDim, size = 10.dp)
        }
        DropdownMenu(open, onDismissRequest = { open = false }, modifier = Modifier.heightIn(max = 420.dp).background(palette.surfaceRaised)) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Label(label(option), size = 12.sp, color = if (option == selected) palette.link else palette.text) },
                    onClick = { open = false; onSelect(option) },
                )
            }
        }
    }
}

data class MenuEntry(val label: String, val enabled: Boolean = true, val children: List<MenuEntry> = emptyList(), val onClick: () -> Unit = {})

@Composable
fun MenuButton(title: String, entries: List<MenuEntry>, modifier: Modifier = Modifier, icon: IconShape? = null) {
    val palette = LocalPalette.current
    var open by remember { mutableStateOf(false) }
    var stack by remember { mutableStateOf<List<MenuEntry>>(emptyList()) }
    val shown = stack.lastOrNull()?.children ?: entries
    Box(modifier) {
        Row(
            Modifier.clip(RoundedCornerShape(3.dp)).clickable { stack = emptyList(); open = true }.pointerHoverIcon(PointerIcon.Hand).padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (icon != null) Icon(icon, Color.White, size = 13.dp)
            Label(title, color = Color.White, size = 13.sp, maxLines = 1)
            Icon(IconShape.ChevronDown, Color.White.copy(alpha = 0.7f), size = 9.dp)
        }
        DropdownMenu(open, onDismissRequest = { open = false; stack = emptyList() }, modifier = Modifier.background(palette.surfaceRaised)) {
            if (stack.isNotEmpty()) {
                DropdownMenuItem(
                    text = { Label("‹  " + stack.last().label, size = 13.sp, color = palette.link) },
                    onClick = { stack = stack.dropLast(1) },
                )
            }
            shown.forEach { entry ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Label(entry.label, size = 13.sp, color = if (entry.enabled) palette.text else palette.textDim)
                            if (entry.children.isNotEmpty()) Icon(IconShape.ChevronRight, palette.textDim, size = 11.dp)
                        }
                    },
                    enabled = entry.enabled,
                    onClick = {
                        if (entry.children.isNotEmpty()) stack = stack + entry
                        else {
                            open = false
                            stack = emptyList()
                            entry.onClick()
                        }
                    },
                )
            }
        }
    }
}

@Composable
fun CheckOption(label: String, checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val knob by animateDpAsState(if (checked) 16.dp else 2.dp, tween(160), label = "switch")
    Row(
        modifier.clip(RoundedCornerShape(4.dp)).clickable { onChange(!checked) }.pointerHoverIcon(PointerIcon.Hand).padding(vertical = 2.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier.width(32.dp).height(18.dp).clip(RoundedCornerShape(9.dp))
                .background(if (checked) palette.accent else palette.input)
                .border(1.dp, if (checked) palette.accentStrong else palette.border, RoundedCornerShape(9.dp)),
        ) {
            Box(
                Modifier.align(Alignment.CenterStart).offset(x = knob).size(14.dp).clip(CircleShape)
                    .background(if (checked) Color.White else palette.textDim),
            )
        }
        if (label.isNotEmpty()) Label(label, size = 12.sp)
    }
}

@Composable
fun TabStrip(tabs: List<TabItem>, selected: String?, onSelect: (String) -> Unit, modifier: Modifier = Modifier, onClose: ((String) -> Unit)? = null, trailing: @Composable RowScope.() -> Unit = {}) {
    val palette = LocalPalette.current
    val move = LocalPaneMove.current
    Row(modifier.fillMaxWidth().background(palette.surfaceRaised), verticalAlignment = Alignment.CenterVertically) {
        if (move != null) Box(Modifier.padding(start = 8.dp).paneDrag(move)) { MoveGrip(palette.textDim) }
        Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
            tabs.forEach { tab ->
                val active = tab.id == selected
                Row(
                    Modifier
                        .background(if (active) palette.surface else Color.Transparent)
                        .clickable { onSelect(tab.id) }
                        .pointerHoverIcon(PointerIcon.Hand)
                        .padding(start = 12.dp, end = if (tab.closable) 4.dp else 12.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Label(tab.title, color = if (active) palette.text else palette.textDim, size = 12.sp, weight = if (active) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
                    if (tab.badge != null) Badge(tab.badge, palette.accent)
                    if (tab.closable && onClose != null) IconAction(IconShape.Close, { onClose(tab.id) }, size = 10.dp, color = palette.textDim)
                }
            }
        }
        trailing()
    }
}

@Composable
fun Badge(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.25f)).padding(horizontal = 6.dp, vertical = 1.dp)) {
        Label(text, color = color, size = 10.sp, weight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
fun EmptyState(
    text: String,
    modifier: Modifier = Modifier,
    illustration: IllustrationKind? = null,
    action: (@Composable () -> Unit)? = null,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val compact = this.maxHeight < 160.dp
        val mark = illustration?.takeIf { this.maxHeight > 100.dp }
        Column(
            Modifier.fillMaxSize().padding(if (compact) 12.dp else 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (mark != null) {
                Illustration(mark, size = if (compact) 56.dp else 84.dp)
                Spacer(Modifier.height(if (compact) 6.dp else 12.dp))
            }
            Label(text, Modifier.widthIn(max = 380.dp), color = LocalPalette.current.textDim, size = 13.sp, align = TextAlign.Center)
            if (action != null) {
                Spacer(Modifier.height(10.dp))
                action()
            }
        }
    }
}

@Composable
fun TableHeader(columns: List<TableColumn>, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Row(modifier.fillMaxWidth().background(palette.surfaceRaised).padding(horizontal = 8.dp, vertical = 4.dp)) {
        columns.forEach { column ->
            Label(column.title, Modifier.weight(column.weight).widthIn(min = column.minWidth.dp), color = palette.textDim, size = 11.sp, weight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
fun TableRow(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    background: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val palette = LocalPalette.current
    val base = modifier.fillMaxWidth()
        .background(if (selected) palette.selected else background ?: Color.Transparent)
        .let { if (selected) it.border(1.dp, palette.selectedBorder) else it }
        .let { if (onClick != null) it.clickable(onClick = onClick).pointerHoverIcon(PointerIcon.Hand) else it }
        .padding(horizontal = 8.dp, vertical = 3.dp)
    Row(base, verticalAlignment = Alignment.CenterVertically, content = content)
}

@Composable
fun Modal(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 560.dp,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalPalette.current
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier.width(width).heightIn(max = 720.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(palette.surface)
                .border(1.dp, palette.border, RoundedCornerShape(6.dp)),
        ) {
            Row(Modifier.fillMaxWidth().background(palette.topBar).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Label(title, Modifier.weight(1f), color = Color.White, weight = FontWeight.SemiBold, maxLines = 1)
                IconAction(IconShape.Close, onDismiss, color = Color.White)
            }
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                actions()
                ActionButton(tr(ControlTexts.close), onDismiss)
            }
        }
    }
}

@Composable
fun FormRow(label: String, modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Label(label, Modifier.width(150.dp), color = LocalPalette.current.textDim, size = 12.sp)
        content()
    }
}

@Composable
fun Notice(text: String, color: Color, modifier: Modifier = Modifier, icon: IconShape = IconShape.Warning) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(color.copy(alpha = 0.14f)).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, color, size = 14.dp)
        Label(text, color = LocalPalette.current.text, size = 12.sp)
    }
}

@Composable
fun ProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Box(modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(palette.input)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0f, 1f)).background(palette.accent))
    }
}
