// Locks tooltip placement beside the pointer. Calls editor/Index.
package hl7lookup.editor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Checks tooltip placement beside the pointer. Calls Editor.tipSpot via Index.
class TooltipSpotTest {
    // Asserts the tip opens offset from the pointer. Calls Editor.tipSpot.
    @Test
    fun tooltipSitsBesideThePointer() {
        val spot = Editor.tipSpot(40f, 30f, 100, 80, 400, 300)
        assertEquals(56, spot.x)
        assertEquals(46, spot.y)
    }

    // Asserts the tip flips near the viewport edge. Calls Editor.tipSpot.
    @Test
    fun tooltipFlipsWhenItWouldLeaveTheView() {
        val spot = Editor.tipSpot(360f, 250f, 100, 80, 400, 300)
        assertTrue(spot.x < 360)
        assertTrue(spot.y < 250)
        assertTrue(spot.x >= 8)
        assertTrue(spot.y >= 8)
    }
}
