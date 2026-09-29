// Template fill and language codes. i18n/Index exposes them.
package hl7lookup.i18n

import kotlinx.serialization.Serializable

// Supported UI languages; Index and LocalLanguage use these codes.
@Serializable
enum class Language { EN, DE }

// English and German wording pair; Index resolve/format and Translations maps hold these.
data class Text(val en: String, val de: String)

// One piece of a filled template; splitTemplate builds these for Index.parts callers.
sealed interface TemplatePart {
    // Fixed text between slots; splitTemplate emits these for Index.parts.
    data class Literal(val text: String) : TemplatePart
    // Placeholder index for an argument; splitTemplate emits these for Index.parts.
    data class Slot(val index: Int) : TemplatePart
}

// Picks en or de from a Text; Index.resolve and tr call this.
internal fun resolveText(text: Text, language: Language): String = when (language) {
    Language.EN -> text.en
    Language.DE -> text.de
}

// Replaces {n} markers with argument strings; Index.format and tr call this.
internal fun fillTemplate(template: String, args: List<String>): String =
    args.foldIndexed(template) { index, acc, arg -> acc.replace("{$index}", arg) }

// Parses a template into Literal and Slot parts; Index.parts calls this.
internal fun splitTemplate(template: String): List<TemplatePart> {
    val parts = mutableListOf<TemplatePart>()
    val pattern = Regex("\\{(\\d+)}")
    var cursor = 0
    for (match in pattern.findAll(template)) {
        if (match.range.first > cursor) parts += TemplatePart.Literal(template.substring(cursor, match.range.first))
        parts += TemplatePart.Slot(match.groupValues[1].toInt())
        cursor = match.range.last + 1
    }
    if (cursor < template.length) parts += TemplatePart.Literal(template.substring(cursor))
    return parts
}

// Selects singular or plural Text by count; Index.plural calls this.
internal fun pluralText(count: Long, one: Text, many: Text): Text = if (count == 1L) one else many
