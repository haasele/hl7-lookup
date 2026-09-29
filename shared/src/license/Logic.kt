package hl7lookup.license

import hl7lookup.i18n.Text

data class Component(val name: String, val version: String, val license: String, val url: String, val note: Text? = null)

internal fun componentsFor(components: List<Component>, desktop: Boolean): List<Component> =
    if (desktop) components else components.filter { it.name != "SLF4J" }
