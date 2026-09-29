package hl7lookup.desktop.engine.catalog

internal fun segmentDescription(name: String): String = segmentDescriptions[name] ?: if (name.startsWith("Z")) "Custom segment $name" else name

internal fun datatypeDescription(name: String): String = datatypeDescriptions[name] ?: name

private val acronymBoundary = Regex("(?<=[A-Z])(?=[A-Z][a-z])|(?<=[a-z0-9])(?=[A-Z])")

internal fun humanize(camel: String): String =
    camel.replace('_', ' ').split(acronymBoundary).joinToString(" ").replace(Regex("\\s+"), " ").trim()
