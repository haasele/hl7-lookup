package hl7lookup.desktop.engine.tables

import hl7lookup.dictionary.TableDef
import hl7lookup.dictionary.TableEntry

internal fun normalizeTableId(number: Int): String = number.toString().padStart(4, '0')

internal fun tableDef(id: String): TableDef {
    val authored = authoredTables[id]
    return TableDef(
        id = id,
        name = authored?.name ?: "",
        entries = authored?.entries?.map { (code, description) -> TableEntry(code, description) }.orEmpty(),
    )
}

internal fun tablesFor(ids: Set<String>): Map<String, TableDef> = (ids + authoredTables.keys).associateWith(::tableDef)

internal fun fieldTable(segment: String, field: Int): String? = codedFieldTables["$segment-$field"]

internal fun componentTable(datatype: String, component: Int): String? = codedComponentTables["$datatype.$component"]
