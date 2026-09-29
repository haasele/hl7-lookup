// The same cursors as CSS keywords in the browser. controls/Logic expects them.
package hl7lookup.controls

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.fromKeyword

// CSS ns/ew-resize cursor for splits in the browser. SplitPane gets it through resizeCursor.
@OptIn(ExperimentalComposeUiApi::class)
internal actual fun resizeCursor(vertical: Boolean): PointerIcon =
    PointerIcon.fromKeyword(if (vertical) "ns-resize" else "ew-resize")

// CSS move cursor for pane grips in the browser. paneDrag gets it through moveCursor.
@OptIn(ExperimentalComposeUiApi::class)
internal actual fun moveCursor(): PointerIcon = PointerIcon.fromKeyword("move")
