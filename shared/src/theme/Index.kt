// Palette, mono font and icons. AppTheme wraps the workspace; controls and the editor read LocalPalette.
package hl7lookup.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
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
val LocalUiFont: ProvidableCompositionLocal<FontFamily> = staticCompositionLocalOf { FontFamily.Default }

// Entry point other features use for segment and highlight colors; forwards to Logic.
object Theme {
    // Maps a segment name to its tint; other features call this, it forwards to colorForSegment.
    fun segmentColor(name: String): Color = colorForSegment(name)
    // Picks a highlight tint by index; other features call this, it forwards to highlightColor.
    fun highlight(index: Int): Color = highlightColor(index)
    // Reports how many highlight tints exist; other features call this, it reads highlightColors.
    fun highlightCount(): Int = highlightColors.size
    // Exposes the theme name wording; other features call this, it returns ThemeTexts.dark.
    fun themeName() = ThemeTexts.dark
}

// Installs the dark Material palette and composition locals; the workspace root calls this.
@Composable
fun AppTheme(monoFont: FontFamily, uiFont: FontFamily = FontFamily.Default, content: @Composable () -> Unit) {
    val palette = Palette()
    CompositionLocalProvider(LocalPalette provides palette, LocalMonoFont provides monoFont, LocalUiFont provides uiFont) {
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
            typography = Typography().withFamily(uiFont),
            content = content,
        )
    }
}

// Points every Material text style at the bundled interface font. AppTheme passes it to MaterialTheme.
private fun Typography.withFamily(font: FontFamily): Typography = copy(
    displayLarge = displayLarge.copy(fontFamily = font),
    displayMedium = displayMedium.copy(fontFamily = font),
    displaySmall = displaySmall.copy(fontFamily = font),
    headlineLarge = headlineLarge.copy(fontFamily = font),
    headlineMedium = headlineMedium.copy(fontFamily = font),
    headlineSmall = headlineSmall.copy(fontFamily = font),
    titleLarge = titleLarge.copy(fontFamily = font),
    titleMedium = titleMedium.copy(fontFamily = font),
    titleSmall = titleSmall.copy(fontFamily = font),
    bodyLarge = bodyLarge.copy(fontFamily = font),
    bodyMedium = bodyMedium.copy(fontFamily = font),
    bodySmall = bodySmall.copy(fontFamily = font),
    labelLarge = labelLarge.copy(fontFamily = font),
    labelMedium = labelMedium.copy(fontFamily = font),
    labelSmall = labelSmall.copy(fontFamily = font),
)

// Draws a stroked icon glyph on a canvas; controls and menus call this, it uses drawIconShape.
@Composable
fun Icon(shape: IconShape, color: Color, modifier: Modifier = Modifier, size: Dp = 14.dp) {
    Canvas(modifier.size(size)) { drawIconShape(this, shape, color) }
}
