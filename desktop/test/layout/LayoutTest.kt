package hl7lookup.layout

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LayoutTest {
    @Test
    fun standardLayoutPutsInterpretationAboveTheMessage() {
        val root = Layouts.standard()
        val left = root.first?.first
        assertEquals("story", left?.first?.pane)
        assertEquals("editor", left?.second?.pane)
        assertEquals(true, left?.vertical)
        assertEquals("grid", root.first?.second?.pane)
        assertEquals(setOf("story", "editor", "grid", "session", "side"), Layouts.panes(root).toSet())
    }

    @Test
    fun draggingSwapsTwoPanes() {
        val swapped = Layouts.swap(Layouts.standard(), "story", "grid")
        assertEquals("grid", swapped.first?.first?.first?.pane)
        assertEquals("story", swapped.first?.second?.pane)
        assertEquals(setOf("story", "editor", "grid", "session", "side"), Layouts.panes(swapped).toSet())
        assertTrue(Layouts.panes(swapped).size == 5)
    }
}
