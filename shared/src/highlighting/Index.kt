package hl7lookup.highlighting

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hl7lookup.controls.ActionButton
import hl7lookup.controls.CheckOption
import hl7lookup.controls.IconAction
import hl7lookup.controls.Label
import hl7lookup.controls.Modal
import hl7lookup.controls.TextInput
import hl7lookup.document.FieldPath
import hl7lookup.document.ParsedMessage
import hl7lookup.i18n.tr
import hl7lookup.theme.IconShape
import hl7lookup.theme.LocalPalette
import hl7lookup.theme.Theme

object Highlights {
    fun hits(rules: List<HighlightRule>, message: ParsedMessage): List<HighlightHit> = hitsFor(rules, message)
    fun color(hits: List<HighlightHit>, path: FieldPath): Color? = colorAt(hits, path)?.let(Theme::highlight)
    fun messageColor(rules: List<HighlightRule>, text: String): Color? = hl7lookup.highlighting.messageColor(rules, text)?.let(Theme::highlight)
    fun valid(spec: String): Boolean = isValidSpec(spec)
    fun texts() = HighlightTexts
}

@Composable
fun HighlightDialog(state: HighlightState, newId: () -> String, suggestion: String?, onDismiss: () -> Unit) {
    val palette = LocalPalette.current
    var spec by remember { mutableStateOf(suggestion ?: "") }
    var value by remember { mutableStateOf("") }
    var color by remember { mutableIntStateOf(state.rules.size % Theme.highlightCount()) }
    val valid = isValidSpec(spec)
    Modal(tr(HighlightTexts.title), onDismiss, width = 620.dp, actions = {
        if (state.rules.any { it.source == null }) ActionButton(tr(HighlightTexts.clear), state::clear, danger = true)
    }) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextInput(spec, { spec = it.uppercase() }, Modifier.width(170.dp), placeholder = "PID-5", mono = true, error = spec.isNotBlank() && !valid)
            TextInput(value, { value = it }, Modifier.weight(1f), placeholder = tr(HighlightTexts.value))
            ColorChoice(color) { color = it }
            ActionButton(tr(HighlightTexts.add), {
                state.add(HighlightRule(newId(), spec.trim(), value.trim(), color))
                spec = ""; value = ""; color = (color + 1) % Theme.highlightCount()
            }, primary = true, enabled = valid, icon = IconShape.Plus)
        }
        if (spec.isNotBlank() && !valid) Label(tr(HighlightTexts.invalid), color = palette.warning, size = 11.sp)
        if (state.rules.isEmpty()) Label(tr(HighlightTexts.empty), color = palette.textDim, size = 12.sp)
        for (rule in state.rules) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(Theme.highlight(rule.color)))
                CheckOption("", rule.enabled, { state.toggle(rule.id) })
                Label(rule.spec, Modifier.width(120.dp), mono = true, color = Theme.highlight(rule.color))
                Label(rule.value.ifBlank { tr(HighlightTexts.anyValue) }, Modifier.weight(1f), color = palette.textDim, size = 12.sp)
                if (rule.source != null) Label(tr(HighlightTexts.fromInterface), color = palette.textDim, size = 11.sp)
                else IconAction(IconShape.Trash, { state.remove(rule.id) }, color = palette.textDim)
            }
        }
    }
}

@Composable
fun ColorChoice(selected: Int, onSelect: (Int) -> Unit) {
    val palette = LocalPalette.current
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in 0 until Theme.highlightCount()) {
            Box(
                Modifier.size(18.dp).clip(CircleShape).background(Theme.highlight(i))
                    .border(2.dp, if (i == selected) palette.text else Color.Transparent, CircleShape)
                    .clickable { onSelect(i) },
            )
        }
    }
}
