// Message-filter dialog. Workspace opens it from Tools.
package hl7lookup.filters

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import hl7lookup.controls.ActionButton
import hl7lookup.controls.FormRow
import hl7lookup.controls.Label
import hl7lookup.controls.Modal
import hl7lookup.controls.TextInput
import hl7lookup.datetime.DateStyle
import hl7lookup.datetime.Hl7Dates
import hl7lookup.i18n.tr
import hl7lookup.theme.LocalPalette

// Facade for message-list filtering. Workspace keeps visible indices through it.
object Filters {
    // True when a message passes the filter. Workspace and the message list call it.
    fun accepts(filter: ListFilter, text: String, style: DateStyle): Boolean = hl7lookup.filters.accepts(filter, text, style)
    // Exposes filter wording. Workspace reads it via tr().
    fun texts() = FilterTexts
}

// Dialog to edit type, date and text filters. Workspace opens it from Tools.
@Composable
fun FilterDialog(state: FilterState, style: DateStyle, onDismiss: () -> Unit) {
    val filter = state.filter
    val palette = LocalPalette.current
    Modal(tr(FilterTexts.title), onDismiss, actions = {
        ActionButton(tr(FilterTexts.clear), state::clear, enabled = filter.active)
    }) {
        FormRow(tr(FilterTexts.type)) {
            TextInput(filter.type, { state.update(filter.copy(type = it)) }, Modifier.fillMaxWidth(), placeholder = tr(FilterTexts.typeHint), mono = true)
        }
        FormRow(tr(FilterTexts.from)) {
            TextInput(filter.from, { state.update(filter.copy(from = it)) }, Modifier.fillMaxWidth(), placeholder = Hl7Dates.inputHint(style), error = !isValidBoundary(filter.from, style))
        }
        FormRow(tr(FilterTexts.to)) {
            TextInput(filter.to, { state.update(filter.copy(to = it)) }, Modifier.fillMaxWidth(), placeholder = Hl7Dates.inputHint(style), error = !isValidBoundary(filter.to, style))
        }
        if (!isValidBoundary(filter.from, style) || !isValidBoundary(filter.to, style)) {
            Label(tr(FilterTexts.invalidDate, Hl7Dates.inputHint(style)), color = palette.warning, size = 11.sp)
        }
        FormRow(tr(FilterTexts.text)) {
            TextInput(filter.text, { state.update(filter.copy(text = it)) }, Modifier.fillMaxWidth())
        }
    }
}
