package hl7lookup.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hl7lookup.controls.EmptyState
import hl7lookup.motion.IllustrationKind
import hl7lookup.controls.IconAction
import hl7lookup.controls.Label
import hl7lookup.controls.TableColumn
import hl7lookup.controls.TableHeader
import hl7lookup.controls.TableRow
import hl7lookup.datetime.DateStyle
import hl7lookup.dictionary.Hl7Dictionary
import hl7lookup.i18n.tr
import hl7lookup.session.MessageEntry
import hl7lookup.theme.IconShape
import hl7lookup.theme.LocalPalette

object MessageList {
    fun row(index: Int, text: String, style: DateStyle, dictionary: (String) -> Hl7Dictionary?): MessageRow = rowOf(index, text, style, dictionary(versionOf(text)))
    fun texts() = MessageListTexts
}

@Composable
fun MessageListPanel(
    messages: List<MessageEntry>,
    visible: List<Int>,
    current: Int,
    style: DateStyle,
    dictionary: (String) -> Hl7Dictionary?,
    marker: (MessageEntry) -> Color?,
    onSelect: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    if (messages.isEmpty()) {
        EmptyState(tr(MessageListTexts.empty), modifier, illustration = IllustrationKind.MESSAGES)
        return
    }
    val listState = rememberLazyListState()
    LaunchedEffect(current, visible.size) {
        val position = visible.indexOf(current)
        if (position >= 0) listState.animateScrollToItem(position)
    }
    Column(modifier.fillMaxSize()) {
        TableHeader(
            listOf(
                TableColumn(tr(MessageListTexts.time), 0.26f),
                TableColumn(tr(MessageListTexts.type), 0.16f),
                TableColumn(tr(MessageListTexts.description), 0.58f),
            ),
        )
        if (visible.isEmpty()) {
            EmptyState(tr(MessageListTexts.filtered), illustration = IllustrationKind.MESSAGES)
            return@Column
        }
        LazyColumn(Modifier.fillMaxSize(), state = listState) {
            items(visible, key = { messages[it].id }) { index ->
                val entry = messages[index]
                val version = remember(entry.text) { versionOf(entry.text) }
                val dict = dictionary(version)
                val row = remember(index, entry.text, style, dict) { rowOf(index, entry.text, style, dict) }
                val color = marker(entry)
                TableRow(selected = index == current, onClick = { onSelect(index) }) {
                    Box(Modifier.width(3.dp).height(16.dp).background(color ?: Color.Transparent))
                    Label(row.time, Modifier.weight(0.26f).padding(start = 5.dp), size = 12.sp, maxLines = 1)
                    Label(row.type, Modifier.weight(0.16f), size = 12.sp, mono = true, color = palette.link, maxLines = 1)
                    Label(row.description, Modifier.weight(0.58f), size = 12.sp, maxLines = 1)
                    IconAction(IconShape.Close, { onRemove(index) }, color = palette.textDim, size = 10.dp)
                }
            }
        }
    }
}
