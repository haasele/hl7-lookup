package hl7lookup.editor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TooltipSpotTest {
    @Test
    fun tooltipSitsBesideThePointer() {
        val spot = Editor.tipSpot(40f, 30f, 100, 80, 400, 300)
        assertEquals(56, spot.x)
        assertEquals(46, spot.y)
    }

    @Test
    fun tooltipFlipsWhenItWouldLeaveTheView() {
        val spot = Editor.tipSpot(360f, 250f, 100, 80, 400, 300)
        assertTrue(spot.x < 360)
        assertTrue(spot.y < 250)
        assertTrue(spot.x >= 8)
        assertTrue(spot.y >= 8)
    }
}
