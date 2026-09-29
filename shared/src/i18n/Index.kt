package hl7lookup.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

val LocalLanguage: ProvidableCompositionLocal<Language> = staticCompositionLocalOf { Language.EN }

object I18n {
    fun resolve(text: Text, language: Language): String = resolveText(text, language)
    fun format(text: Text, language: Language, vararg args: Any?): String =
        fillTemplate(resolveText(text, language), args.map { it.toString() })
    fun parts(template: String): List<TemplatePart> = splitTemplate(template)
    fun plural(count: Long, one: Text, many: Text): Text = pluralText(count, one, many)
    fun languages(): List<Language> = Language.entries
    fun languageName(language: Language): Text = languageNames.getValue(language)
}

@Composable
fun tr(text: Text): String = resolveText(text, LocalLanguage.current)

@Composable
fun tr(text: Text, vararg args: Any?): String = fillTemplate(resolveText(text, LocalLanguage.current), args.map { it.toString() })
