package hl7lookup.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val LocalPalette: ProvidableCompositionLocal<Palette> = staticCompositionLocalOf { Palette() }
val LocalMonoFont: ProvidableCompositionLocal<FontFamily> = staticCompositionLocalOf { FontFamily.Monospace }

object Theme {
    fun segmentColor(name: String): Color = colorForSegment(name)
    fun highlight(index: Int): Color = highlightColor(index)
    fun highlightCount(): Int = highlightColors.size
    fun themeName() = ThemeTexts.dark
}

@Composable
fun AppTheme(monoFont: FontFamily, content: @Composable () -> Unit) {
    val palette = Palette()
    CompositionLocalProvider(LocalPalette provides palette, LocalMonoFont provides monoFont) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = palette.accent,
                onPrimary = Color.White,
                secondary = palette.accentStrong,
                background = palette.background,
                onBackground = palette.text,
                surface = palette.surfaceRaised,
                onSurface = palette.text,
                surfaceVariant = palette.surface,
                onSurfaceVariant = palette.textDim,
                outline = palette.border,
                error = palette.error,
            ),
            content = content,
        )
    }
}

@Composable
fun Icon(shape: IconShape, color: Color, modifier: Modifier = Modifier, size: Dp = 14.dp) {
    Canvas(modifier.size(size)) { drawIconShape(this, shape, color) }
}
