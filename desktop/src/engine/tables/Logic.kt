// Selects which authored tables a version needs. engine/tables/Index exposes them.
package hl7lookup.desktop.engine.tables

import hl7lookup.dictionary.TableDef
import hl7lookup.dictionary.TableEntry

// Pads a table number to four digits. Tables.id calls it.
internal fun normalizeTableId(number: Int): String = number.toString().padStart(4, '0')

// Builds a TableDef from authoredTables. tablesFor maps each id through it.
internal fun tableDef(id: String): TableDef {
    val authored = authoredTables[id]
    return TableDef(
        id = id,
        name = authored?.name ?: "",
        entries = authored?.entries?.map { (code, description) -> TableEntry(code, description) }.orEmpty(),
    )
}

// Unions referenced ids with all authored tables. Tables.all calls it for the dictionary.
internal fun tablesFor(ids: Set<String>): Map<String, TableDef> = (ids + authoredTables.keys).associateWith(::tableDef)

// Looks up codedFieldTables for a segment-field key. Tables.forField calls it.
internal fun fieldTable(segment: String, field: Int): String? = codedFieldTables["$segment-$field"]

// Looks up codedComponentTables for a datatype.component key. Tables.forComponent calls it.
internal fun componentTable(datatype: String, component: Int): String? = codedComponentTables["$datatype.$component"]
