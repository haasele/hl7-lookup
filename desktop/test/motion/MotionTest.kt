// Locks that every empty-state scene draws. Calls motion/Index.
package hl7lookup.motion

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Checks empty-state scenes and SVG parsing. Calls Motion via Index.
class MotionTest {
    // Asserts every IllustrationKind builds a scene. Calls Motion.illustration and splash.
    @Test
    fun everyPlaceholderIsADrawableScene() {
        IllustrationKind.entries.forEach { kind ->
            val scene = Motion.illustration(kind)
            assertEquals(80f, scene.width)
            assertEquals(80f, scene.height)
            assertTrue(scene.shapes.size >= 3, kind.name)
        }
        val splash = Motion.splash()
        assertEquals(120f, splash.width)
        assertTrue(splash.shapes.size >= 6)
    }

    // Asserts SVG path/rect parsing and bad paths. Calls Motion.read.
    @Test
    fun svgPathsKeepSubpathsColorsAndCurves() {
        val split = Motion.read("""<svg viewBox="0 0 20 12"><path d="M1 1 H8 V6 Z M12 2 H18" fill="none" stroke="#6EB6F0" stroke-width="2" stroke-linecap="round"/></svg>""")
        assertEquals(20f, split.width)
        assertEquals(12f, split.height)
        assertEquals(2, split.shapes.size)
        assertEquals(Color(0xFF6EB6F0), split.shapes[0].stroke)
        assertEquals(2f, split.shapes[0].strokeWidth)
        assertEquals(StrokeCap.Round, split.shapes[0].cap)
        assertNull(split.shapes[0].fill)

        val curve = Motion.read("""<svg viewBox="0 0 40 40"><path d="M10 30 C20 10 30 10 36 24" fill="#141E2B" stroke="#2F7FD1"/></svg>""")
        assertEquals(1, curve.shapes.size)
        assertNotNull(curve.shapes[0].fill)
        assertNotNull(curve.shapes[0].stroke)

        val rect = Motion.read("""<svg viewBox="0 0 40 40"><rect x="4" y="6" width="20" height="12" rx="2" fill="none" stroke="#D8E1EC"/></svg>""")
        assertEquals(1, rect.shapes.size)

        val broken = Motion.read("""<svg viewBox="0 0 10 10"><path d="M" stroke="#ffffff"/></svg>""")
        assertTrue(broken.shapes.isEmpty())
    }
}
