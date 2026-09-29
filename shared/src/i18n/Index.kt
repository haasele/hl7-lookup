// tr() and the active language. Every feature screen calls tr.
package hl7lookup.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

val LocalLanguage: ProvidableCompositionLocal<Language> = staticCompositionLocalOf { Language.EN }

// Entry point other features use for resolve, format, parts and plurals; each method forwards to Logic.
object I18n {
    // Picks the string for a language; other features call this, it forwards to resolveText.
    fun resolve(text: Text, language: Language): String = resolveText(text, language)
    // Resolves then fills {n} slots; other features call this, it uses resolveText and fillTemplate.
    fun format(text: Text, language: Language, vararg args: Any?): String =
        fillTemplate(resolveText(text, language), args.map { it.toString() })
    // Splits a template into literals and slots; other features call this, it forwards to splitTemplate.
    fun parts(template: String): List<TemplatePart> = splitTemplate(template)
    // Chooses one vs many wording by count; other features call this, it forwards to pluralText.
    fun plural(count: Long, one: Text, many: Text): Text = pluralText(count, one, many)
    // Lists supported languages; settings and screens call this, it reads Language.entries.
    fun languages(): List<Language> = Language.entries
    // Display name for a language; settings call this, it reads languageNames.
    fun languageName(language: Language): Text = languageNames.getValue(language)
}

// Resolves a Text for the composition-local language; screens call this, it uses resolveText and LocalLanguage.
@Composable
fun tr(text: Text): String = resolveText(text, LocalLanguage.current)

// Resolves and fills a Text for the composition-local language; screens call this, it uses resolveText and fillTemplate.
@Composable
fun tr(text: Text, vararg args: Any?): String = fillTemplate(resolveText(text, LocalLanguage.current), args.map { it.toString() })
