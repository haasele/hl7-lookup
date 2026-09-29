// Name tables used while the dictionary is built. engine/catalog/Index exposes them.
package hl7lookup.desktop.engine.catalog

// Looks up a segment description map entry, with a Z-segment fallback. Catalog.segment calls it.
internal fun segmentDescription(name: String): String = segmentDescriptions[name] ?: if (name.startsWith("Z")) "Custom segment $name" else name

// Looks up a datatype description map entry. Catalog.datatype calls it.
internal fun datatypeDescription(name: String): String = datatypeDescriptions[name] ?: name

private val acronymBoundary = Regex("(?<=[A-Z])(?=[A-Z][a-z])|(?<=[a-z0-9])(?=[A-Z])")

// Turns camelCase HAPI names into spaced words. Catalog.humanize calls it.
internal fun humanize(camel: String): String =
    camel.replace('_', ' ').split(acronymBoundary).joinToString(" ").replace(Regex("\\s+"), " ").trim()
