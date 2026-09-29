package hl7lookup.validation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hl7lookup.controls.CheckOption
import hl7lookup.controls.EmptyState
import hl7lookup.motion.IllustrationKind
import hl7lookup.controls.Label
import hl7lookup.controls.Notice
import hl7lookup.controls.TableRow
import hl7lookup.dictionary.Hl7Dictionary
import hl7lookup.document.FieldPath
import hl7lookup.document.ParsedMessage
import hl7lookup.engine.InspectReport
import hl7lookup.engine.Severity
import hl7lookup.i18n.Text
import hl7lookup.i18n.tr
import hl7lookup.theme.Icon
import hl7lookup.theme.IconShape
import hl7lookup.theme.LocalPalette
import hl7lookup.theme.Palette

object Validation {
    fun check(message: ParsedMessage, dictionary: Hl7Dictionary?): List<Finding> = validateMessage(message, dictionary)
    fun fromEngine(message: ParsedMessage, report: InspectReport?): List<Finding> = engineFindings(message, report)
    fun merge(vararg groups: List<Finding>): List<Finding> = mergeFindings(*groups)
    fun severityAt(findings: List<Finding>, path: FieldPath): Severity? = hl7lookup.validation.severityAt(findings, path)
    fun counts(findings: List<Finding>): Map<Severity, Int> = countBySeverity(findings)
    fun text(kind: FindingKind): Text = findingTexts.getValue(kind)
    fun color(severity: Severity, palette: Palette): Color = when (severity) {
        Severity.ERROR -> palette.error
        Severity.WARNING -> palette.warning
        Severity.INFO -> palette.link
    }
    fun texts() = ValidationTexts
}

@Composable
fun findingText(finding: Finding): String = tr(findingTexts.getValue(finding.kind), *finding.args.toTypedArray())

@Composable
fun ValidationPanel(
    findings: List<Finding>,
    definitionsReady: Boolean,
    engineOnline: Boolean,
    selected: FieldPath?,
    onPick: (FieldPath) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    var showNotes by remember { mutableStateOf(false) }
    val counts = countBySeverity(findings)
    val shown = findings.filter { showNotes || it.severity != Severity.INFO }
    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Label(
                tr(ValidationTexts.summary, counts[Severity.ERROR] ?: 0, counts[Severity.WARNING] ?: 0, counts[Severity.INFO] ?: 0),
                Modifier.weight(1f), color = palette.textDim, size = 11.sp,
            )
            CheckOption(tr(ValidationTexts.showNotes), showNotes, { showNotes = it })
        }
        if (!definitionsReady) Notice(tr(ValidationTexts.noDefinitions), palette.warning, Modifier.padding(horizontal = 8.dp))
        if (!engineOnline) Notice(tr(ValidationTexts.engineOffline), palette.textDim, Modifier.padding(horizontal = 8.dp))
        if (shown.isEmpty()) {
            EmptyState(tr(ValidationTexts.clean), Modifier.weight(1f), illustration = if (findings.isEmpty()) IllustrationKind.VALIDATION else null)
            return@Column
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(shown) { finding ->
                val color = Validation.color(finding.severity, palette)
                val path = finding.path
                TableRow(
                    selected = path != null && selected != null && hl7lookup.document.Er7.same(path, selected),
                    onClick = path?.let { { onPick(it) } },
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (finding.severity == Severity.INFO) IconShape.Check else IconShape.Warning, color, size = 12.dp)
                        Label(finding.label, Modifier.width(92.dp), mono = true, size = 12.sp, color = color, maxLines = 1)
                        Label(findingText(finding), Modifier.weight(1f), size = 12.sp)
                        if (finding.source != null) Label(finding.source, color = palette.textDim, size = 10.sp)
                    }
                }
            }
        }
    }
}
