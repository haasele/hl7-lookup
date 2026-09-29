// Field-by-field diff. compare/Index calls it.
package hl7lookup.compare

import hl7lookup.document.Er7
import hl7lookup.document.FieldPath
import hl7lookup.document.ParsedMessage
import hl7lookup.document.PathSpec

// Whether a leaf is same, changed, added or removed. DiffRow and the dialog color by it.
enum class DiffKind { SAME, CHANGED, ADDED, REMOVED }

// One compared field row. compareMessages builds these for the dialog table.
data class DiffRow(
    val key: String,
    val segmentName: String,
    val leftPath: FieldPath?,
    val rightPath: FieldPath?,
    val left: String,
    val right: String,
    val kind: DiffKind,
)

// Ignore list and compare toggles. CompareDialog edits it; compareMessages applies it.
data class CompareOptions(val ignore: String = "MSH-7, MSH-10", val onlyDifferences: Boolean = true, val ignoreCase: Boolean = false)

// Diff rows plus change counts. compareMessages returns it to the dialog.
data class CompareResult(val rows: List<DiffRow>, val changed: Int, val added: Int, val removed: Int) {
    val identical: Boolean get() = changed + added + removed == 0
}

// Tab and message index for a side. CompareDialog choices carry these sources.
data class CompareSource(val tabId: String, val index: Int)

// Dropdown entry for a compare side. CompareDialog lists tabs plus pasted text.
data class CompareChoice(val source: CompareSource?, val label: String)

// Internal leaf value with sort order. leaves() builds these before matching keys.
private data class Leaf(val key: String, val segmentName: String, val order: Int, val path: FieldPath, val value: String)

// Stable key for a field leaf. leaves() and compareMessages match sides by it.
private fun keyOf(segmentName: String, occurrence: Int, path: FieldPath): String {
    val base = "$segmentName${if (occurrence > 1) "[$occurrence]" else ""}-${path.field}"
    val rep = if (path.repetition > 1) "(${path.repetition})" else ""
    return base + rep + if (path.component > 0) ".${path.component}" else ""
}

// Flattens a message into valued leaves. compareMessages diffs left and right lists.
private fun leaves(message: ParsedMessage): List<Leaf> =
    Er7.flatten(message).mapIndexedNotNull { order, node ->
        if (node.raw.isEmpty()) null
        else Leaf(keyOf(node.segmentName, node.occurrence, node.path), node.segmentName, order, node.path, Er7.unescape(node.raw, message.delimiters))
    }

// Parses the ignore-field box into path specs. compareMessages skips those specs.
internal fun parseIgnore(text: String): List<PathSpec> =
    text.split(',', ';', ' ', '\n').map { it.trim().uppercase() }.filter { it.isNotEmpty() }.mapNotNull(Er7::spec)

// True when a leaf matches an ignore spec. compareMessages filters leaves with it.
private fun ignored(specs: List<PathSpec>, segmentName: String, path: FieldPath): Boolean = specs.any { spec ->
    spec.segment == segmentName && (spec.field == 0 || spec.field == path.field) &&
        (spec.component == 0 || spec.component == path.component) &&
        (spec.repetition == null || spec.repetition == path.repetition)
}

// Builds the visible diff result. Compare.run and CompareDialog call it.
internal fun compareMessages(left: ParsedMessage, right: ParsedMessage, options: CompareOptions): CompareResult {
    val ignore = parseIgnore(options.ignore)
    val leftLeaves = leaves(left).filterNot { ignored(ignore, it.segmentName, it.path) }
    val rightLeaves = leaves(right).filterNot { ignored(ignore, it.segmentName, it.path) }
    val rightByKey = rightLeaves.associateBy { it.key }
    val leftKeys = leftLeaves.map { it.key }.toSet()
    val rows = mutableListOf<DiffRow>()
    for (leaf in leftLeaves) {
        val other = rightByKey[leaf.key]
        val kind = when {
            other == null -> DiffKind.REMOVED
            other.value.equals(leaf.value, ignoreCase = options.ignoreCase) -> DiffKind.SAME
            else -> DiffKind.CHANGED
        }
        rows += DiffRow(leaf.key, leaf.segmentName, leaf.path, other?.path, leaf.value, other?.value.orEmpty(), kind)
    }
    for (leaf in rightLeaves) {
        if (leaf.key in leftKeys) continue
        val insertAt = rows.indexOfLast { it.rightPath != null && it.rightPath.segment <= leaf.path.segment }.let { if (it < 0) rows.size else it + 1 }
        rows.add(insertAt, DiffRow(leaf.key, leaf.segmentName, null, leaf.path, "", leaf.value, DiffKind.ADDED))
    }
    val visible = if (options.onlyDifferences) rows.filter { it.kind != DiffKind.SAME } else rows
    return CompareResult(visible, rows.count { it.kind == DiffKind.CHANGED }, rows.count { it.kind == DiffKind.ADDED }, rows.count { it.kind == DiffKind.REMOVED })
}
