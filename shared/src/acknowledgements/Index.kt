package hl7lookup.acknowledgements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hl7lookup.controls.ActionButton
import hl7lookup.controls.EmptyState
import hl7lookup.motion.IllustrationKind
import hl7lookup.controls.IconAction
import hl7lookup.controls.Label
import hl7lookup.controls.TableRow
import hl7lookup.datetime.DateStyle
import hl7lookup.datetime.Hl7Dates
import hl7lookup.engine.Engines
import hl7lookup.engine.FailureCause
import hl7lookup.i18n.tr
import hl7lookup.theme.IconShape
import hl7lookup.theme.LocalPalette
import hl7lookup.theme.Palette

object Acknowledgements {
    fun entry(id: String, time: Long, direction: Direction, peer: String, message: String, ack: String?, failure: FailureCause?, detail: String?, duration: Long): AckEntry =
        entryOf(id, time, direction, peer, message, ack, failure, detail, duration)
    fun outcome(entry: AckEntry): AckOutcome = outcomeOf(entry)
    fun color(outcome: AckOutcome, palette: Palette): Color = when (outcome) {
        AckOutcome.ACCEPTED -> palette.success
        AckOutcome.ERROR -> palette.warning
        AckOutcome.REJECTED, AckOutcome.FAILED -> palette.error
        AckOutcome.NONE -> palette.textDim
    }
    fun texts() = AckTexts
}

@Composable
fun AcknowledgementsPanel(store: AckStore, style: DateStyle, nowIso: (Long) -> String, onOpen: (AckEntry) -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Column(modifier.fillMaxSize()) {
        if (store.entries.isEmpty()) {
            EmptyState(tr(AckTexts.empty), Modifier.weight(1f), illustration = IllustrationKind.ACK)
            return@Column
        }
        Row(Modifier.fillMaxWidth().padding(6.dp)) { ActionButton(tr(AckTexts.clear), store::clear, icon = IconShape.Trash) }
        LazyColumn(Modifier.fillMaxSize()) {
            items(store.entries, key = { it.id }) { entry ->
                val outcome = outcomeOf(entry)
                val color = Acknowledgements.color(outcome, palette)
                TableRow(background = color.copy(alpha = 0.08f), onClick = if (entry.ack != null) ({ onOpen(entry) }) else null) {
                    Box(Modifier.width(4.dp).height(30.dp).background(color))
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Label(entry.code ?: entry.failure?.name ?: tr(AckTexts.noAck), color = color, weight = FontWeight.Bold, size = 12.sp, mono = true)
                            Label(entry.controlId, mono = true, size = 12.sp, maxLines = 1)
                            Label(tr(if (entry.direction == Direction.SENT) AckTexts.sent else AckTexts.received, entry.peer), color = palette.textDim, size = 11.sp, maxLines = 1)
                        }
                        val detail = entry.failure?.let { tr(Engines.causeText(it)) + (entry.detail?.let { d -> " ($d)" } ?: "") } ?: entry.text
                        if (detail != null) Label(detail, color = palette.textDim, size = 11.sp, maxLines = 2)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Label(Hl7Dates.format(nowIso(entry.time), style) ?: "", color = palette.textDim, size = 11.sp)
                        if (entry.durationMillis > 0) Label(tr(AckTexts.took, entry.durationMillis), color = palette.textDim, size = 10.sp)
                    }
                    if (entry.ack != null) IconAction(IconShape.ChevronRight, { onOpen(entry) }, color = palette.textDim)
                }
            }
        }
    }
}
