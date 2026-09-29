// Which segment the grid follows and which cells are empty. grid/Index calls it.
package hl7lookup.grid

import hl7lookup.dictionary.Dictionaries
import hl7lookup.dictionary.Hl7Dictionary
import hl7lookup.dictionary.NodeLevel
import hl7lookup.document.Er7
import hl7lookup.document.FieldPath
import hl7lookup.document.ParsedMessage

// One grid cell: path, label, value and editability. gridRows builds these for FieldGrid.
data class GridRow(
    val path: FieldPath,
    val level: NodeLevel,
    val label: String,
    val name: String,
    val datatype: String,
    val table: String?,
    val value: String,
    val raw: String,
    val editable: Boolean,
    val required: Boolean,
)

// Segment option for the dropdown. segmentChoices builds the list FieldGrid picks from.
data class SegmentChoice(val index: Int, val name: String, val occurrence: Int, val description: String?)

// Maps each segment to a dropdown choice. Grid.segments and FieldGrid call it.
internal fun segmentChoices(message: ParsedMessage, dictionary: Hl7Dictionary?): List<SegmentChoice> =
    message.segments.map { SegmentChoice(it.index, it.name, Er7.occurrence(message, it.index), Dictionaries.segment(dictionary, it.name)?.description) }

private val primitive = setOf("ST", "ID", "IS", "NM", "SI", "TX", "FT", "DT", "DTM", "TM", "GTS", "varies")

// Walks fields, components and subs into rows. Grid.rows and FieldGrid call it.
internal fun gridRows(message: ParsedMessage, dictionary: Hl7Dictionary?, segmentIndex: Int, showEmpty: Boolean, keep: FieldPath? = null): List<GridRow> {
    val segment = message.segments.getOrNull(segmentIndex) ?: return emptyList()
    // Keeps an empty row when it contains the selection. gridRows uses it while filtering empties.
    fun kept(path: FieldPath) = keep != null && Er7.contains(path, keep)
    val definition = Dictionaries.segment(dictionary, segment.name)
    val defined = definition?.fields?.map { it.number }.orEmpty()
    val numbers = (defined + segment.fields.map { it.number }).distinct().sorted()
    val rows = mutableListOf<GridRow>()
    for (number in numbers) {
        val fieldDef = Dictionaries.field(dictionary, segment.name, number)
        val node = segment.field(number)
        val header = segment.name in setOf("MSH", "FHS", "BHS") && number <= 2
        val reps = node?.repetitions?.size?.coerceAtLeast(1) ?: 1
        val hasValue = node != null && node.raw.isNotEmpty()
        if (!hasValue && !showEmpty && !kept(FieldPath(segmentIndex, number))) continue
        for (rep in 1..reps) {
            val path = FieldPath(segmentIndex, number, rep)
            val raw = Er7.raw(message, path)
            val datatype = fieldDef?.datatype ?: ""
            val components = Dictionaries.components(dictionary, datatype)
            val rawParts = if (header) 1 else raw.split(message.delimiters.component).size
            val composite = !header && (components.size > 1 && datatype !in primitive || rawParts > 1)
            rows += GridRow(
                path, NodeLevel.FIELD, Er7.label(message, path), fieldDef?.name ?: "", datatype, fieldDef?.table,
                Er7.value(message, path), raw, editable = !composite, required = fieldDef?.required == true,
            )
            if (!composite) continue
            val count = maxOf(if (datatype in primitive) 0 else components.size, rawParts)
            for (c in 1..count) {
                val compPath = path.copy(component = c)
                val compRaw = Er7.raw(message, compPath)
                if (compRaw.isEmpty() && !showEmpty && !kept(compPath)) continue
                val compDef = components.getOrNull(c - 1)
                val subDefs = compDef?.let { Dictionaries.components(dictionary, it.datatype) }.orEmpty()
                val subParts = compRaw.split(message.delimiters.subcomponent).size
                val compComposite = subDefs.size > 1 && compDef?.datatype !in primitive || subParts > 1
                rows += GridRow(
                    compPath, NodeLevel.COMPONENT, Er7.label(message, compPath), compDef?.name ?: "", compDef?.datatype ?: "", compDef?.table,
                    Er7.value(message, compPath), compRaw, editable = !compComposite, required = false,
                )
                if (!compComposite) continue
                val subCount = maxOf(if (compDef?.datatype in primitive) 0 else subDefs.size, subParts)
                for (s in 1..subCount) {
                    val subPath = compPath.copy(subcomponent = s)
                    val subRaw = Er7.raw(message, subPath)
                    if (subRaw.isEmpty() && !showEmpty && !kept(subPath)) continue
                    val subDef = subDefs.getOrNull(s - 1)
                    rows += GridRow(
                        subPath, NodeLevel.SUBCOMPONENT, Er7.label(message, subPath), subDef?.name ?: "", subDef?.datatype ?: "", subDef?.table,
                        Er7.value(message, subPath), subRaw, editable = true, required = false,
                    )
                }
            }
        }
    }
    return rows
}

// Picks the segment index from selection or a fallback. FieldGrid calls it after the picker updates.
internal fun segmentOfSelection(selection: FieldPath?, fallback: Int): Int = selection?.segment ?: fallback

// True for DT, DTM or TS. ValueEditor shows the calendar when it matches.
internal fun isDateType(datatype: String): Boolean = datatype in setOf("DT", "DTM", "TS")
