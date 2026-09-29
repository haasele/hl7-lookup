// Version, language and validation preferences. Workspace opens the dialog from Tools.
package hl7lookup.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import hl7lookup.controls.CheckOption
import hl7lookup.controls.Dropdown
import hl7lookup.controls.FormRow
import hl7lookup.controls.Label
import hl7lookup.controls.Modal
import hl7lookup.datetime.Hl7Dates
import hl7lookup.i18n.I18n
import hl7lookup.i18n.tr
import hl7lookup.theme.LocalPalette

// Modal for language, dates and validation prefs. Workspace opens it from Tools; writes via SettingsState.
@Composable
fun SettingsDialog(state: SettingsState, versions: List<String>, onDismiss: () -> Unit) {
    val settings = state.current
    Modal(tr(SettingsTexts.title), onDismiss) {
        FormRow(tr(SettingsTexts.language)) {
            Dropdown(settings.language, I18n.languages(), { tr(I18n.languageName(it)) }, { lang -> state.update { it.copy(language = lang) } })
        }
        FormRow(tr(SettingsTexts.dateFormat)) {
            Dropdown(settings.dateStyle, Hl7Dates.styles(), { tr(Hl7Dates.styleName(it)) }, { style -> state.update { it.copy(dateStyle = style) } })
        }
        FormRow(tr(SettingsTexts.version)) {
            Dropdown(settings.version, versions, { it }, { v -> state.update { it.copy(version = v) } })
        }
        Label(tr(SettingsTexts.versionHint), color = LocalPalette.current.textDim, size = 11.sp)
        CheckOption(tr(SettingsTexts.emptyFields), settings.showEmptyFields, { v -> state.update { it.copy(showEmptyFields = v) } })
        CheckOption(tr(SettingsTexts.relativeDates), settings.relativeDates, { v -> state.update { it.copy(relativeDates = v) } })
        CheckOption(tr(SettingsTexts.autoValidate), settings.autoValidate, { v -> state.update { it.copy(autoValidate = v) } }, Modifier)
    }
}
