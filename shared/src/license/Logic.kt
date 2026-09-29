// Assembles the component list. license/Index calls it.
package hl7lookup.license

import hl7lookup.i18n.Text

// One credited library with license and URL. thirdParty lists these; Index shows them.
data class Component(val name: String, val version: String, val license: String, val url: String, val note: Text? = null)

// Drops desktop-only entries for the browser. Licenses.components delegates here.
internal fun componentsFor(components: List<Component>, desktop: Boolean): List<Component> =
    if (desktop) components else components.filter { it.name != "SLF4J" }
