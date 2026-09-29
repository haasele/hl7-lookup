package hl7lookup.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hl7lookup.i18n.tr
import hl7lookup.theme.LocalPalette
import kotlinx.coroutines.delay

object Motion {
    fun read(svg: String): VectorScene = parseSvg(svg)
    fun illustration(kind: IllustrationKind): VectorScene = illustrationScene(kind)
    fun splash(): VectorScene = splashScene()
    fun texts() = MotionTexts
}

@Composable
fun Illustration(kind: IllustrationKind, modifier: Modifier = Modifier, size: Dp = 84.dp) {
    val scene = remember(kind) { Motion.illustration(kind) }
    val reveal = remember(kind) { Animatable(0f) }
    LaunchedEffect(kind) {
        reveal.snapTo(0f)
        reveal.animateTo(1f, tween(entranceMs, easing = FastOutSlowInEasing))
    }
    Canvas(
        modifier.size(size).graphicsLayer {
            val scale = 0.86f + 0.14f * reveal.value
            scaleX = scale
            scaleY = scale
            alpha = (reveal.value * 1.8f).coerceAtMost(1f)
        },
    ) {
        drawScene(this, scene, reveal.value)
    }
}

@Composable
fun StartupSplash(content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(true) }
    Box(Modifier.fillMaxSize()) {
        content()
        if (visible) SplashCover { visible = false }
    }
}

@Composable
private fun SplashCover(onFinished: () -> Unit) {
    val palette = LocalPalette.current
    val scene = remember { Motion.splash() }
    val reveal = remember { Animatable(0f) }
    val fade = remember { Animatable(1f) }
    var skip by remember { mutableStateOf(false) }
    LaunchedEffect(skip) {
        if (skip) reveal.snapTo(1f) else reveal.animateTo(1f, tween(splashDrawMs, easing = FastOutSlowInEasing))
        if (!skip) delay(splashHoldMs.toLong())
        fade.animateTo(0f, tween(splashFadeMs))
        onFinished()
    }
    Box(
        Modifier.fillMaxSize().graphicsLayer { alpha = fade.value }
            .background(palette.background)
            .pointerInput(Unit) { detectTapGestures { skip = true } },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(460.dp).background(
                Brush.radialGradient(listOf(palette.accent.copy(alpha = 0.20f), Color.Transparent)),
            ),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Canvas(
                Modifier.size(156.dp).graphicsLayer {
                    val scale = 0.9f + 0.1f * reveal.value
                    scaleX = scale
                    scaleY = scale
                },
            ) {
                drawScene(this, scene, reveal.value)
            }
            Spacer(Modifier.height(18.dp))
            Row(
                Modifier.graphicsLayer { alpha = ((reveal.value - 0.32f) / 0.38f).coerceIn(0f, 1f) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(tr(MotionTexts.brand), color = palette.text, fontSize = 30.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.6).sp)
                Spacer(Modifier.width(8.dp))
                Text(tr(MotionTexts.brandMark), color = palette.delimiter, fontSize = 30.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.6).sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                tr(MotionTexts.tagline),
                color = palette.textDim,
                fontSize = 14.sp,
                modifier = Modifier.graphicsLayer { alpha = ((reveal.value - 0.52f) / 0.36f).coerceIn(0f, 1f) },
            )
            Spacer(Modifier.height(22.dp))
            SweepTrack()
        }
    }
}

@Composable
private fun SweepTrack() {
    val shift by rememberInfiniteTransition().animateFloat(
        initialValue = -0.5f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing)),
    )
    Box(Modifier.width(168.dp).height(2.dp).clip(RoundedCornerShape(1.dp)).background(Color(0xFF1A2636))) {
        Box(
            Modifier.fillMaxHeight().fillMaxWidth(0.36f)
                .offset(x = 168.dp * shift)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, Color(0xFF6EB6F0), Color.Transparent))),
        )
    }
}
