package hl7lookup.controls

import androidx.compose.ui.input.pointer.PointerIcon
import java.awt.Cursor

internal actual fun resizeCursor(vertical: Boolean): PointerIcon =
    PointerIcon(Cursor.getPredefinedCursor(if (vertical) Cursor.N_RESIZE_CURSOR else Cursor.E_RESIZE_CURSOR))

internal actual fun moveCursor(): PointerIcon =
    PointerIcon(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR))
