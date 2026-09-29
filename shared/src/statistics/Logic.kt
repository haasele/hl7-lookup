package hl7lookup.statistics

import hl7lookup.document.Er7
import hl7lookup.document.PathSpec

data class StatRow(val value: String, val count: Int, val firstMessage: Int)

data class FieldStatistics(
    val spec: PathSpec,
    val label: String,
    val rows: List<StatRow>,
    val examined: Int,
    val missing: Int,
    val total: Int,
)

internal fun computeStatistics(messages: List<String>, spec: PathSpec): FieldStatistics {
    val counts = linkedMapOf<String, Int>()
    val first = mutableMapOf<String, Int>()
    var missing = 0
    messages.forEachIndexed { index, text ->
        val message = Er7.parse(text)
        var found = false
        val segments = message.segments.filter { it.name == spec.segment }
        for (segment in segments) {
            val occurrence = Er7.occurrence(message, segment.index)
            val base = Er7.resolve(message, spec, occurrence) ?: continue
            val reps = when {
                spec.field == 0 -> listOf(1)
                spec.repetition != null -> listOf(spec.repetition)
                else -> segment.field(spec.field)?.repetitions?.indices?.map { it + 1 } ?: emptyList()
            }
            for (rep in reps) {
                val value = if (spec.field == 0) segment.name else Er7.value(message, base.copy(repetition = rep))
                if (value.isBlank()) continue
                found = true
                counts[value] = (counts[value] ?: 0) + 1
                first.getOrPut(value) { index }
            }
        }
        if (!found) missing++
    }
    val rows = counts.entries.map { StatRow(it.key, it.value, first.getValue(it.key)) }.sortedWith(compareByDescending<StatRow> { it.count }.thenBy { it.value })
    return FieldStatistics(spec, Er7.label(spec), rows, messages.size, missing, rows.sumOf { it.count })
}
