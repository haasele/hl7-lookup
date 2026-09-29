// Anonymise dialog. Workspace runs it on the current message or the whole tab.
package hl7lookup.anonymize

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hl7lookup.controls.ActionButton
import hl7lookup.controls.CheckOption
import hl7lookup.controls.Label
import hl7lookup.controls.Modal
import hl7lookup.controls.Notice
import hl7lookup.controls.ProgressBar
import hl7lookup.dictionary.Hl7Dictionary
import hl7lookup.i18n.I18n
import hl7lookup.i18n.tr
import hl7lookup.theme.IconShape
import hl7lookup.theme.LocalPalette
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

// Facade exposing anonymize wording. Workspace opens the dialog through it.
object Anonymize {
    // Exposes anonymize wording. Workspace reads it via tr().
    fun texts() = AnonymizeTexts
}

// Options dialog that runs the anonymizer. Workspace opens it for the current message or tab.
@Composable
fun AnonymizeDialog(
    anonymizer: Anonymizer,
    initialScope: AnonymizeScope,
    tabSize: Int,
    texts: (AnonymizeScope) -> List<String>,
    dictionary: (String) -> Hl7Dictionary?,
    onResult: (AnonymizeOptions, List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalPalette.current
    val scope = rememberCoroutineScope()
    var options by remember { mutableStateOf(AnonymizeOptions(scope = initialScope)) }
    var progress by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var finished by remember { mutableStateOf<Int?>(null) }
    val running = progress != null && finished == null
    Modal(tr(AnonymizeTexts.title), onDismiss, width = 520.dp, actions = {
        ActionButton(tr(AnonymizeTexts.start), {
            val input = texts(options.scope)
            finished = null
            progress = 0 to input.size
            scope.launch {
                val output = anonymizer.run(input, dictionary, options) { progress = it to input.size }
                finished = output.size
                onResult(options, output)
            }
        }, primary = true, enabled = !running, icon = IconShape.Play)
    }) {
        Label(tr(AnonymizeTexts.scope), color = palette.textDim, size = 12.sp)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            CheckOption(tr(AnonymizeTexts.scopeCurrent), options.scope == AnonymizeScope.CURRENT, { options = options.copy(scope = AnonymizeScope.CURRENT) })
            CheckOption(tr(I18n.plural(tabSize.toLong(), AnonymizeTexts.scopeTabOne, AnonymizeTexts.scopeTab), tabSize), options.scope == AnonymizeScope.TAB, { options = options.copy(scope = AnonymizeScope.TAB) })
        }
        CheckOption(tr(AnonymizeTexts.newTab), options.newTab, { options = options.copy(newTab = it) })
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            CheckOption(tr(AnonymizeTexts.names), options.names, { options = options.copy(names = it) })
            CheckOption(tr(AnonymizeTexts.identifiers), options.identifiers, { options = options.copy(identifiers = it) })
            CheckOption(tr(AnonymizeTexts.addresses), options.addresses, { options = options.copy(addresses = it) })
            CheckOption(tr(AnonymizeTexts.phones), options.phones, { options = options.copy(phones = it) })
            CheckOption(tr(AnonymizeTexts.freeText), options.freeText, { options = options.copy(freeText = it) })
            CheckOption(tr(AnonymizeTexts.shiftDates, anonymizer.cache.shiftDays.absoluteValue), options.shiftDates, { options = options.copy(shiftDates = it) })
        }
        Label(tr(AnonymizeTexts.coverage), color = palette.textDim, size = 12.sp)
        Label(tr(I18n.plural(anonymizer.cache.values.size.toLong(), AnonymizeTexts.cacheOne, AnonymizeTexts.cache), anonymizer.cache.values.size), color = palette.textDim, size = 12.sp)
        ActionButton(tr(AnonymizeTexts.resetCache), anonymizer::reset, enabled = !running, modifier = Modifier)
        progress?.let { (done, total) ->
            ProgressBar(if (total == 0) 1f else done.toFloat() / total)
            Label(tr(I18n.plural(total.toLong(), AnonymizeTexts.progressOne, AnonymizeTexts.progress), done, total), color = palette.textDim, size = 12.sp)
        }
        finished?.let { Notice(tr(I18n.plural(it.toLong(), AnonymizeTexts.doneOne, AnonymizeTexts.done), it), palette.success, icon = IconShape.Check) }
    }
}
