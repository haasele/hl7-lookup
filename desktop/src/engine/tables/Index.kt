// Authored HL7 table texts. engine/Logic merges Tables into the dictionary.
package hl7lookup.desktop.engine.tables

import hl7lookup.dictionary.TableDef

// Builds table ids and authored table maps for the dictionary. buildDictionary and field/table helpers call it.
object Tables {
    // Pads a numeric table id to four digits. tableNumberOf and dictionary builders call it.
    fun id(number: Int): String = normalizeTableId(number)
    // Returns every authored table plus any referenced ids. buildDictionary merges the map into Hl7Dictionary.
    fun all(referenced: Set<String>): Map<String, TableDef> = tablesFor(referenced)
    // Looks up the coded table for a segment field. segmentDefOf fills FieldDef.table from it.
    fun forField(segment: String, field: Int): String? = fieldTable(segment, field)
    // Looks up the coded table for a datatype component. datatypeDefOf fills ComponentDef.table from it.
    fun forComponent(datatype: String, component: Int): String? = componentTable(datatype, component)
}
