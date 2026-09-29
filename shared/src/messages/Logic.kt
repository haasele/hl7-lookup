// Titles and order of those messages. messages/Index calls it.
package hl7lookup.messages

import hl7lookup.datetime.DateStyle
import hl7lookup.datetime.Hl7Dates
import hl7lookup.dictionary.Dictionaries
import hl7lookup.dictionary.Hl7Dictionary
import hl7lookup.document.Er7

// One display row for the message table. MessageList.row and the panel build these via rowOf.
data class MessageRow(val index: Int, val time: String, val type: String, val description: String, val controlId: String)

// Reads MSH version from ER7 text. MessageListPanel and row builders call it.
internal fun versionOf(text: String): String = Er7.header(Er7.parse(text)).version

// Parses ER7 into time, type and description columns. MessageList.row and the panel call it.
internal fun rowOf(index: Int, text: String, style: DateStyle, dict: Hl7Dictionary?): MessageRow {
    val message = Er7.parse(text)
    val header = Er7.header(message)
    val time = Hl7Dates.parse(header.timestamp)?.let { Hl7Dates.format(it, style) } ?: header.timestamp
    val type = listOf(header.type, header.event).filter { it.isNotEmpty() }.joinToString("^")
    val patient = listOf(Er7.value(message, "PID-5.1"), Er7.value(message, "PID-5.2")).filter { it.isNotBlank() }.joinToString(", ")
    val event = Dictionaries.event(dict, header.type, header.event)
    val ack = Er7.value(message, "MSA-1").takeIf { it.isNotBlank() }?.let { code ->
        listOf(code, Er7.value(message, "MSA-2")).filter { it.isNotBlank() }.joinToString(" ")
    }
    val description = listOfNotNull(ack ?: event, patient.takeIf { it.isNotBlank() })
        .joinToString(" – ").ifEmpty { if (message.isEmpty) "" else message.segments.joinToString(" ") { it.name }.take(60) }
    return MessageRow(index, time, type, description, header.controlId)
}
