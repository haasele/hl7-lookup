// Lookup of segments, fields and tables. The grid, editor and validation call Dictionaries.
package hl7lookup.dictionary

// Public entry for segment, field, table and structure lookup. Other features call it; it forwards to Logic.
object Dictionaries {
    // Looks up a segment definition by name. Callers use Dictionaries; forwards to segmentOf.
    fun segment(dictionary: Hl7Dictionary?, name: String): SegmentDef? = segmentOf(dictionary, name)
    // Looks up a field definition on a segment. Callers use Dictionaries; forwards to fieldOf.
    fun field(dictionary: Hl7Dictionary?, segment: String, field: Int): FieldDef? = fieldOf(dictionary, segment, field)
    // Lists components for a datatype. Callers use Dictionaries; forwards to componentsOf.
    fun components(dictionary: Hl7Dictionary?, datatype: String): List<ComponentDef> = componentsOf(dictionary, datatype)
    // Describes the node at segment/field/component depth. Callers use Dictionaries; forwards to describe.
    fun node(dictionary: Hl7Dictionary?, segment: String, field: Int, component: Int = 0, subcomponent: Int = 0): NodeInfo? =
        describe(dictionary, segment, field, component, subcomponent)
    // Looks up a table by id. Callers use Dictionaries; forwards to tableOf.
    fun table(dictionary: Hl7Dictionary?, id: String?): TableDef? = tableOf(dictionary, id)
    // Resolves a table code to its description. Callers use Dictionaries; forwards to describeCode.
    fun code(dictionary: Hl7Dictionary?, table: String?, code: String): String? = describeCode(dictionary, table, code)
    // Finds one table entry by code or pattern. Callers use Dictionaries; forwards to entryFor.
    fun entry(table: TableDef?, code: String): TableEntry? = entryFor(table, code)
    // True when the datatype is a date/time type. Callers use Dictionaries; forwards to isDateType.
    fun isDate(datatype: String): Boolean = isDateType(datatype)
    // True when the datatype has more than one component. Callers use Dictionaries; forwards to isComposite.
    fun isComposite(dictionary: Hl7Dictionary?, datatype: String): Boolean = hl7lookup.dictionary.isComposite(dictionary, datatype)
    // Looks up an event display name. Callers use Dictionaries; forwards to eventName.
    fun event(dictionary: Hl7Dictionary?, type: String, event: String): String? = eventName(dictionary, type, event)
    // Looks up a message type display name. Callers use Dictionaries; forwards to messageTypeName.
    fun messageType(dictionary: Hl7Dictionary?, type: String): String? = messageTypeName(dictionary, type)
    // Resolves the structure for a type/event pair. Callers use Dictionaries; forwards to structureFor.
    fun structure(dictionary: Hl7Dictionary?, type: String, event: String, declared: String): StructureDef? =
        structureFor(dictionary, type, event, declared)
    // Filters table entries by a query string. Callers use Dictionaries; forwards to searchTable.
    fun search(table: TableDef?, query: String): List<TableEntry> = searchTable(table, query)
    // Exposes dictionary wording. Callers use Dictionaries; returns DictionaryTexts.
    fun texts() = DictionaryTexts
}
