package hl7lookup.compare

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hl7lookup.controls.ActionButton
import hl7lookup.controls.CheckOption
import hl7lookup.controls.Dropdown
import hl7lookup.controls.Label
import hl7lookup.controls.Modal
import hl7lookup.controls.Notice
import hl7lookup.controls.TableColumn
import hl7lookup.controls.TableHeader
import hl7lookup.controls.TableRow
import hl7lookup.controls.TextInput
import hl7lookup.document.Er7
import hl7lookup.document.FieldPath
import hl7lookup.document.ParsedMessage
import hl7lookup.engine.Severity
import hl7lookup.i18n.tr
import hl7lookup.theme.IconShape
import hl7lookup.theme.LocalPalette
import hl7lookup.validation.Finding

object Compare {
    fun run(left: ParsedMessage, right: ParsedMessage, options: CompareOptions = CompareOptions()): CompareResult = compareMessages(left, right, options)
    fun texts() = CompareTexts
}

@Composable
fun CompareDialog(
    choices: List<CompareChoice>,
    initialLeft: CompareSource?,
    initialRight: CompareSource?,
    textOf: (CompareSource) -> String?,
    describe: (ParsedMessage, FieldPath) -> String?,
    findings: (ParsedMessage) -> List<Finding>,
    onPickLeft: (CompareSource, FieldPath) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalPalette.current
    val pasted = CompareChoice(null, tr(CompareTexts.pasted))
    val all = choices + pasted
    var left by remember { mutableStateOf(all.firstOrNull { it.source == initialLeft } ?: all.first()) }
    var right by remember { mutableStateOf(all.firstOrNull { it.source == initialRight && initialRight != null } ?: pasted) }
    var leftPaste by remember { mutableStateOf("") }
    var rightPaste by remember { mutableStateOf("") }
    var options by remember { mutableStateOf(CompareOptions()) }
    Modal(tr(CompareTexts.title), onDismiss, width = 980.dp, actions = {
        ActionButton(tr(CompareTexts.swap), { val l = left; left = right; right = l; val p = leftPaste; leftPaste = rightPaste; rightPaste = p }, icon = IconShape.Swap)
    }) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Label(tr(CompareTexts.left), color = palette.textDim, size = 12.sp)
                Dropdown(left, all, { it.label }, { left = it }, Modifier.fillMaxWidth())
                if (left.source == null) TextInput(leftPaste, { leftPaste = it }, Modifier.fillMaxWidth().height(90.dp), singleLine = false, mono = true, placeholder = tr(CompareTexts.pasteHint))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Label(tr(CompareTexts.right), color = palette.textDim, size = 12.sp)
                Dropdown(right, all, { it.label }, { right = it }, Modifier.fillMaxWidth())
                if (right.source == null) TextInput(rightPaste, { rightPaste = it }, Modifier.fillMaxWidth().height(90.dp), singleLine = false, mono = true, placeholder = tr(CompareTexts.pasteHint))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Label(tr(CompareTexts.ignore), color = palette.textDim, size = 12.sp)
            TextInput(options.ignore, { options = options.copy(ignore = it) }, Modifier.width(260.dp), mono = true, placeholder = tr(CompareTexts.ignoreHint))
            CheckOption(tr(CompareTexts.onlyDifferences), options.onlyDifferences, { options = options.copy(onlyDifferences = it) })
            CheckOption(tr(CompareTexts.ignoreCase), options.ignoreCase, { options = options.copy(ignoreCase = it) })
        }
        val leftText = left.source?.let(textOf) ?: Er7.normalize(leftPaste)
        val rightText = right.source?.let(textOf) ?: Er7.normalize(rightPaste)
        val leftMessage = remember(leftText) { Er7.parse(leftText) }
        val rightMessage = remember(rightText) { Er7.parse(rightText) }
        if (leftMessage.isEmpty || rightMessage.isEmpty) return@Modal
        val result = remember(leftMessage, rightMessage, options) { compareMessages(leftMessage, rightMessage, options) }
        val leftErrors = findings(leftMessage).count { it.severity == Severity.ERROR }
        val rightErrors = findings(rightMessage).count { it.severity == Severity.ERROR }
        if (result.identical) Notice(tr(CompareTexts.identical), palette.success, icon = IconShape.Check)
        else Label(tr(CompareTexts.summary, result.changed, result.added, result.removed), weight = FontWeight.SemiBold, size = 12.sp)
        Label(tr(CompareTexts.interfaceCheck, leftErrors, rightErrors), color = palette.textDim, size = 12.sp)
        TableHeader(listOf(TableColumn(tr(CompareTexts.position), 0.9f, 90), TableColumn(tr(CompareTexts.left), 1.6f, 120), TableColumn(tr(CompareTexts.right), 1.6f, 120)))
        for (row in result.rows.take(1500)) {
            val color = when (row.kind) {
                DiffKind.SAME -> null
                DiffKind.CHANGED -> palette.warning
                DiffKind.ADDED -> palette.success
                DiffKind.REMOVED -> palette.error
            }
            val leftSource = left.source
            TableRow(
                background = color?.copy(alpha = 0.10f),
                onClick = if (leftSource != null && row.leftPath != null) ({ onPickLeft(leftSource, row.leftPath) }) else null,
            ) {
                Column(Modifier.weight(0.9f)) {
                    Label(row.key, mono = true, size = 12.sp, weight = FontWeight.SemiBold)
                    val name = row.leftPath?.let { describe(leftMessage, it) } ?: row.rightPath?.let { describe(rightMessage, it) }
                    if (name != null) Label(name, color = palette.textDim, size = 10.sp, maxLines = 1)
                }
                Label(row.left, Modifier.weight(1.6f), mono = true, size = 12.sp, color = if (row.kind == DiffKind.REMOVED || row.kind == DiffKind.CHANGED) palette.text else palette.textDim)
                Label(row.right, Modifier.weight(1.6f), mono = true, size = 12.sp, color = if (row.kind == DiffKind.ADDED || row.kind == DiffKind.CHANGED) palette.text else palette.textDim)
            }
        }
    }
}
