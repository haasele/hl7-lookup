// Which messages a filter keeps. filters/Index calls it.
package hl7lookup.filters

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import hl7lookup.datetime.DateStyle
import hl7lookup.datetime.Hl7Dates
import hl7lookup.document.Er7
import kotlinx.datetime.LocalDateTime

// Type, date-range and text filter. FilterState holds it; accepts() applies it.
data class ListFilter(val type: String = "", val from: String = "", val to: String = "", val text: String = "") {
    val active: Boolean get() = type.isNotBlank() || from.isNotBlank() || to.isNotBlank() || text.isNotBlank()
}

// Parses a from/to boundary, optionally end of day. accepts() compares MSH-7 against it.
internal fun boundary(input: String, style: DateStyle, endOfDay: Boolean): LocalDateTime? {
    if (input.isBlank()) return null
    val parsed = Hl7Dates.parseInput(input, style) ?: return null
    return if (!endOfDay || Hl7Dates.inputHasTime(input)) parsed else LocalDateTime(parsed.date, kotlinx.datetime.LocalTime(23, 59, 59))
}

// True when a boundary is blank or parses. FilterDialog marks invalid dates with it.
internal fun isValidBoundary(input: String, style: DateStyle): Boolean = input.isBlank() || Hl7Dates.parseInput(input, style) != null

// Applies type, date and text checks. Filters.accepts and Workspace call it.
internal fun accepts(filter: ListFilter, text: String, style: DateStyle): Boolean {
    if (!filter.active) return true
    val message = Er7.parse(text)
    val header = Er7.header(message)
    if (filter.type.isNotBlank()) {
        val wanted = filter.type.trim().uppercase().replace('_', '^')
        val actual = listOf(header.type, header.event).filter { it.isNotEmpty() }.joinToString("^").uppercase()
        if (!actual.startsWith(wanted) && header.structure.uppercase() != filter.type.trim().uppercase()) return false
    }
    val from = boundary(filter.from, style, endOfDay = false)
    val to = boundary(filter.to, style, endOfDay = true)
    if (from != null || to != null) {
        val stamp = Hl7Dates.parse(header.timestamp)?.let { Hl7Dates.local(it) } ?: return false
        if (from != null && stamp < from) return false
        if (to != null && stamp > to) return false
    }
    if (filter.text.isNotBlank() && !text.contains(filter.text.trim(), ignoreCase = true)) return false
    return true
}

// Holds the active list filter. Workspace owns it and opens FilterDialog on it.
class FilterState {
    var filter by mutableStateOf(ListFilter())
        private set

    // Replaces the current filter. FilterDialog field edits call it.
    fun update(next: ListFilter) {
        filter = next
    }

    // Resets to an empty filter. FilterDialog clear calls it.
    fun clear() {
        filter = ListFilter()
    }
}
