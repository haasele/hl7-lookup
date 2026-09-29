// Human names for HAPI segments and datatypes. engine/Logic calls Catalog while building the dictionary.
package hl7lookup.desktop.engine.catalog

// Looks up friendly segment and datatype names. buildDictionary and component naming call through it.
object Catalog {
    // Returns a human segment description. segmentDefOf calls it while reflecting HAPI segments.
    fun segment(name: String): String = segmentDescription(name)
    // Returns a human datatype description. datatypeDefOf calls it while reflecting HAPI types.
    fun datatype(name: String): String = datatypeDescription(name)
    // Splits a HAPI camelCase getter into words. componentNames calls it for field labels.
    fun humanize(camel: String): String = hl7lookup.desktop.engine.catalog.humanize(camel)
}
