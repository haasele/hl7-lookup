package hl7lookup.desktop.engine.catalog

object Catalog {
    fun segment(name: String): String = segmentDescription(name)
    fun datatype(name: String): String = datatypeDescription(name)
    fun humanize(camel: String): String = hl7lookup.desktop.engine.catalog.humanize(camel)
}
