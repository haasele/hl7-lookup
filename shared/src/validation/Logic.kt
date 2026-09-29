package hl7lookup.validation

import hl7lookup.datetime.Hl7Dates
import hl7lookup.dictionary.Dictionaries
import hl7lookup.dictionary.Hl7Dictionary
import hl7lookup.dictionary.TableDef
import hl7lookup.document.Er7
import hl7lookup.document.FieldPath
import hl7lookup.document.ParsedMessage
import hl7lookup.engine.InspectReport
import hl7lookup.engine.Severity

enum class FindingKind {
    REQUIRED, EMPTY, DATE, TABLE, LENGTH, REPETITION, NUMERIC, UNKNOWN_SEGMENT, UNKNOWN_FIELD, ENGINE,
    RULE_REQUIRED, RULE_EXPECTED, RULE_DATE, RULE_TABLE, RULE_TYPE,
}

data class Finding(
    val severity: Severity,
    val kind: FindingKind,
    val path: FieldPath?,
    val label: String,
    val args: List<String> = emptyList(),
    val source: String? = null,
)

private val numeric = Regex("^[+-]?(\\d+\\.?\\d*|\\.\\d+)$")

private val primitiveTypes = setOf("ST", "ID", "IS", "NM", "SI", "TX", "FT", "DT", "DTM", "TM", "GTS", "SNM", "NUL", "varies")

internal fun checkLeaf(
    message: ParsedMessage,
    path: FieldPath,
    datatype: String,
    table: TableDef?,
    into: MutableList<Finding>,
) {
    val raw = Er7.raw(message, path)
    if (raw.isEmpty() || raw == "\"\"") return
    val value = Er7.value(message, path)
    val label = Er7.label(message, path)
    when (datatype) {
        "DT", "DTM" -> if (!Hl7Dates.isValid(value)) into += Finding(Severity.ERROR, FindingKind.DATE, path, label, listOf(value))
        "TM" -> if (!Regex("^\\d{2}(\\d{2}(\\d{2}(\\.\\d{1,4})?)?)?([+-]\\d{4})?$").matches(value)) into += Finding(Severity.ERROR, FindingKind.DATE, path, label, listOf(value))
        "NM", "SI" -> if (!numeric.matches(value.trim())) into += Finding(Severity.ERROR, FindingKind.NUMERIC, path, label, listOf(value))
    }
    if (table != null && table.entries.isNotEmpty() && (datatype == "ID" || datatype == "IS")) {
        if (Dictionaries.entry(table, value) == null) {
            into += Finding(Severity.WARNING, FindingKind.TABLE, path, label, listOf(value, table.id))
        }
    }
}

internal fun checkComposite(
    message: ParsedMessage,
    dictionary: Hl7Dictionary,
    base: FieldPath,
    datatype: String,
    table: String?,
    depth: Int,
    into: MutableList<Finding>,
) {
    val components = Dictionaries.components(dictionary, datatype)
    if (components.isEmpty() || datatype in primitiveTypes) {
        checkLeaf(message, base, datatype, Dictionaries.table(dictionary, table), into)
        return
    }
    if (datatype == "TS" && depth == 0) {
        checkLeaf(message, base.copy(component = 1), "DTM", null, into)
        return
    }
    for (component in components) {
        val path = if (depth == 0) base.copy(component = component.number) else base.copy(subcomponent = component.number)
        if (Er7.raw(message, path).isEmpty()) continue
        if (depth == 0) {
            checkComposite(message, dictionary, path, component.datatype, component.table ?: if (component.number == 1) table else null, 1, into)
        } else {
            val leafType = if (component.datatype == "TS") "DTM" else component.datatype
            checkLeaf(message, path, leafType, Dictionaries.table(dictionary, component.table), into)
        }
    }
}

internal fun validateMessage(message: ParsedMessage, dictionary: Hl7Dictionary?): List<Finding> {
    if (dictionary == null) return emptyList()
    val findings = mutableListOf<Finding>()
    for (segment in message.segments) {
        val definition = Dictionaries.segment(dictionary, segment.name)
        if (definition == null) {
            if (!segment.name.startsWith("Z")) findings += Finding(Severity.WARNING, FindingKind.UNKNOWN_SEGMENT, FieldPath(segment.index, 0), segment.name, listOf(dictionary.version))
            continue
        }
        for (field in definition.fields) {
            if (segment.name == "MSH" && field.number <= 2) continue
            val node = segment.field(field.number)
            val path = FieldPath(segment.index, field.number)
            val label = Er7.label(message, path)
            val present = node != null && node.raw.isNotEmpty()
            if (!present) {
                if (field.required) findings += Finding(Severity.ERROR, FindingKind.REQUIRED, path, label, listOf(field.name))
                continue
            }
            if (node.repetitions.all { rep -> rep.components.all { c -> c.subcomponents.all { it.raw.isEmpty() } } }) {
                findings += Finding(if (field.required) Severity.ERROR else Severity.INFO, FindingKind.EMPTY, path, label, listOf(field.name))
                continue
            }
            if (field.maxRepetitions > 0 && node.repetitions.size > field.maxRepetitions) {
                findings += Finding(Severity.WARNING, FindingKind.REPETITION, path, label, listOf(node.repetitions.size.toString(), field.maxRepetitions.toString()))
            }
            for (rep in node.repetitions) {
                val repPath = path.copy(repetition = rep.index)
                if (field.length > 0 && rep.raw.length > field.length && field.datatype in primitiveTypes) {
                    findings += Finding(Severity.INFO, FindingKind.LENGTH, repPath, Er7.label(message, repPath), listOf(rep.raw.length.toString(), field.length.toString()))
                }
                checkComposite(message, dictionary, repPath, field.datatype, field.table, 0, findings)
            }
        }
        val lastDefined = definition.fields.maxOfOrNull { it.number } ?: 0
        segment.fields.filter { it.number > lastDefined && it.raw.isNotEmpty() }.forEach {
            findings += Finding(Severity.WARNING, FindingKind.UNKNOWN_FIELD, FieldPath(segment.index, it.number), Er7.label(message, FieldPath(segment.index, it.number)), listOf(dictionary.version))
        }
    }
    return findings
}

internal fun engineFindings(message: ParsedMessage, report: InspectReport?): List<Finding> {
    if (report == null) return emptyList()
    return report.findings.map { finding ->
        val index = finding.segment?.let { Er7.segmentIndex(message, it, finding.segmentRepetition) }
        val path = index?.let {
            FieldPath(it, finding.field, finding.fieldRepetition.coerceAtLeast(1), finding.component, finding.subcomponent)
        }
        Finding(finding.severity, FindingKind.ENGINE, path, path?.let { Er7.label(message, it) } ?: (report.structure ?: ""), listOf(finding.message), "HAPI")
    }
}

internal fun mergeFindings(vararg groups: List<Finding>): List<Finding> {
    val merged = mutableListOf<Finding>()
    for (group in groups) {
        for (finding in group) {
            val duplicate = finding.kind == FindingKind.ENGINE && finding.path != null && merged.any { it.path != null && Er7.same(it.path, finding.path) }
            if (!duplicate) merged += finding
        }
    }
    return merged.sortedWith(compareBy<Finding>({ it.severity.ordinal }, { it.path?.segment ?: -1 }, { it.path?.field ?: 0 }, { it.path?.component ?: 0 }))
}

internal fun severityAt(findings: List<Finding>, path: FieldPath): Severity? =
    findings.filter { f -> f.path != null && (Er7.contains(f.path, path) || Er7.contains(path, f.path)) }
        .minByOrNull { it.severity.ordinal }?.severity

internal fun countBySeverity(findings: List<Finding>): Map<Severity, Int> = findings.groupingBy { it.severity }.eachCount()
