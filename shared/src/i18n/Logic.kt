package hl7lookup.i18n

import kotlinx.serialization.Serializable

@Serializable
enum class Language { EN, DE }

data class Text(val en: String, val de: String)

sealed interface TemplatePart {
    data class Literal(val text: String) : TemplatePart
    data class Slot(val index: Int) : TemplatePart
}

internal fun resolveText(text: Text, language: Language): String = when (language) {
    Language.EN -> text.en
    Language.DE -> text.de
}

internal fun fillTemplate(template: String, args: List<String>): String =
    args.foldIndexed(template) { index, acc, arg -> acc.replace("{$index}", arg) }

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

internal fun pluralText(count: Long, one: Text, many: Text): Text = if (count == 1L) one else many
