// How often a field value appears in the tab. Workspace shows it under the grid.
package hl7lookup.statistics

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hl7lookup.controls.Controls
import hl7lookup.controls.EmptyState
import hl7lookup.motion.IllustrationKind
import hl7lookup.controls.Label
import hl7lookup.controls.TableColumn
import hl7lookup.controls.TableHeader
import hl7lookup.controls.TableRow
import hl7lookup.document.PathSpec
import hl7lookup.i18n.tr
import hl7lookup.theme.LocalPalette

// Facade for field-value counts. Workspace asks it to compute; panels show results.
object Statistics {
    // Counts field values across messages. Workspace calls this when a field is picked.
    fun compute(messages: List<String>, spec: PathSpec): FieldStatistics = computeStatistics(messages, spec)
    // Hands out StatisticsTexts. Feature screens that need labels use this.
    fun texts() = StatisticsTexts
}

// Table of value frequencies for one field. Workspace shows it under the grid.
@Composable
fun StatisticsPanel(
    statistics: FieldStatistics?,
    fieldName: String?,
    modifier: Modifier = Modifier,
    describe: (String) -> String? = { null },
    highlight: (String) -> Color? = { null },
    onPick: (StatRow) -> Unit,
) {
    val palette = LocalPalette.current
    if (statistics == null) {
        EmptyState(tr(StatisticsTexts.pick), modifier, illustration = IllustrationKind.STATISTICS)
        return
    }
    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp)) {
            Label(statistics.label, mono = true, color = palette.link, weight = FontWeight.SemiBold)
            if (fieldName != null) Label("  $fieldName", color = palette.textDim, size = 12.sp, maxLines = 1)
        }
        TableHeader(listOf(TableColumn(tr(StatisticsTexts.value), 0.6f), TableColumn(tr(StatisticsTexts.count), 0.2f), TableColumn(tr(StatisticsTexts.share), 0.2f)))
        if (statistics.rows.isEmpty()) {
            Label(tr(StatisticsTexts.noValues), Modifier.padding(10.dp), color = palette.textDim, size = 12.sp)
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            items(statistics.rows) { row ->
                TableRow(onClick = { onPick(row) }) {
                    Column(Modifier.weight(0.6f)) {
                        Label(Controls.oneLine(row.value), mono = true, size = 12.sp, color = highlight(row.value) ?: palette.text, maxLines = 1)
                        describe(row.value)?.let { Label(it, color = palette.textDim, size = 10.sp, maxLines = 1) }
                    }
                    Label(row.count.toString(), Modifier.weight(0.2f), size = 12.sp)
                    Label(Controls.percent(row.count, statistics.total), Modifier.weight(0.2f), size = 12.sp, color = palette.textDim)
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(8.dp)) {
            Label(tr(StatisticsTexts.examined, statistics.examined), color = palette.textDim, size = 11.sp)
            Label(tr(StatisticsTexts.missing, statistics.missing), color = if (statistics.missing > 0) palette.warning else palette.textDim, size = 11.sp)
        }
    }
}
