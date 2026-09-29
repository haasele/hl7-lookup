package hl7lookup.desktop.engine.tables

import hl7lookup.dictionary.TableDef

object Tables {
    fun id(number: Int): String = normalizeTableId(number)
    fun all(referenced: Set<String>): Map<String, TableDef> = tablesFor(referenced)
    fun forField(segment: String, field: Int): String? = fieldTable(segment, field)
    fun forComponent(datatype: String, component: Int): String? = componentTable(datatype, component)
}
