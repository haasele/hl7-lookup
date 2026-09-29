// Resize and move cursors for the desktop window. controls/Logic expects them.
package hl7lookup.controls

import androidx.compose.ui.input.pointer.PointerIcon
import java.awt.Cursor

// AWT resize cursor for vertical or horizontal splits. SplitPane gets it through resizeCursor.
internal actual fun resizeCursor(vertical: Boolean): PointerIcon =
    PointerIcon(Cursor.getPredefinedCursor(if (vertical) Cursor.N_RESIZE_CURSOR else Cursor.E_RESIZE_CURSOR))

// AWT move cursor for pane grips. paneDrag gets it through moveCursor.
internal actual fun moveCursor(): PointerIcon =
    PointerIcon(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR))
