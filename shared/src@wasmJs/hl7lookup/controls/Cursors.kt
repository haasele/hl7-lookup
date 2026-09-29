package hl7lookup.controls

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.fromKeyword

@OptIn(ExperimentalComposeUiApi::class)
internal actual fun resizeCursor(vertical: Boolean): PointerIcon =
    PointerIcon.fromKeyword(if (vertical) "ns-resize" else "ew-resize")

@OptIn(ExperimentalComposeUiApi::class)
internal actual fun moveCursor(): PointerIcon = PointerIcon.fromKeyword("move")
