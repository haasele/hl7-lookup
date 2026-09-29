package hl7lookup.dictionary

object Dictionaries {
    fun segment(dictionary: Hl7Dictionary?, name: String): SegmentDef? = segmentOf(dictionary, name)
    fun field(dictionary: Hl7Dictionary?, segment: String, field: Int): FieldDef? = fieldOf(dictionary, segment, field)
    fun components(dictionary: Hl7Dictionary?, datatype: String): List<ComponentDef> = componentsOf(dictionary, datatype)
    fun node(dictionary: Hl7Dictionary?, segment: String, field: Int, component: Int = 0, subcomponent: Int = 0): NodeInfo? =
        describe(dictionary, segment, field, component, subcomponent)
    fun table(dictionary: Hl7Dictionary?, id: String?): TableDef? = tableOf(dictionary, id)
    fun code(dictionary: Hl7Dictionary?, table: String?, code: String): String? = describeCode(dictionary, table, code)
    fun entry(table: TableDef?, code: String): TableEntry? = entryFor(table, code)
    fun isDate(datatype: String): Boolean = isDateType(datatype)
    fun isComposite(dictionary: Hl7Dictionary?, datatype: String): Boolean = hl7lookup.dictionary.isComposite(dictionary, datatype)
    fun event(dictionary: Hl7Dictionary?, type: String, event: String): String? = eventName(dictionary, type, event)
    fun messageType(dictionary: Hl7Dictionary?, type: String): String? = messageTypeName(dictionary, type)
    fun structure(dictionary: Hl7Dictionary?, type: String, event: String, declared: String): StructureDef? =
        structureFor(dictionary, type, event, declared)
    fun search(table: TableDef?, query: String): List<TableEntry> = searchTable(table, query)
    fun texts() = DictionaryTexts
}
