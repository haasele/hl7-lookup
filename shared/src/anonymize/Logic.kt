package hl7lookup.anonymize

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import hl7lookup.datetime.Hl7Dates
import hl7lookup.dictionary.Dictionaries
import hl7lookup.dictionary.Hl7Dictionary
import hl7lookup.document.Er7
import hl7lookup.document.FieldPath
import hl7lookup.document.ParsedMessage
import hl7lookup.platform.Platform
import hl7lookup.platform.Platforms
import kotlinx.coroutines.yield
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

enum class Kind { FAMILY, GIVEN, IDENTIFIER, STREET, CITY, POSTAL, PHONE, EMAIL, TEXT, ORGANIZATION }

enum class AnonymizeScope { CURRENT, TAB }

data class AnonymizeOptions(
    val scope: AnonymizeScope = AnonymizeScope.CURRENT,
    val newTab: Boolean = true,
    val names: Boolean = true,
    val identifiers: Boolean = true,
    val addresses: Boolean = true,
    val phones: Boolean = true,
    val freeText: Boolean = true,
    val shiftDates: Boolean = true,
)

internal data class Target(val field: Int, val components: Map<Int, Kind>)

@Serializable
data class AnonymizeCache(val seed: Long, val shiftDays: Int, val values: Map<String, String> = emptyMap())

internal data class Wordbook(
    val families: List<String>,
    val givens: List<String>,
    val streets: List<String>,
    val cities: List<String>,
    val organizations: List<String>,
    val words: List<String>,
)

private val cacheJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

private val cacheKey get() = Platforms.key("anonymize", "cache")

private val name = mapOf(1 to Kind.FAMILY, 2 to Kind.GIVEN, 3 to Kind.GIVEN)
private val xcn = mapOf(1 to Kind.IDENTIFIER, 2 to Kind.FAMILY, 3 to Kind.GIVEN, 4 to Kind.GIVEN)
private val identifier = mapOf(1 to Kind.IDENTIFIER)
private val address = mapOf(1 to Kind.STREET, 2 to Kind.STREET, 3 to Kind.CITY, 5 to Kind.POSTAL)
private val phone = mapOf(1 to Kind.PHONE, 4 to Kind.EMAIL, 6 to Kind.PHONE, 7 to Kind.PHONE)
private val organization = mapOf(1 to Kind.ORGANIZATION)

internal val targets: Map<String, List<Target>> = mapOf(
    "PID" to listOf(
        Target(2, identifier), Target(3, identifier), Target(4, identifier), Target(5, name), Target(6, name), Target(9, name),
        Target(11, address), Target(13, phone), Target(14, phone), Target(18, identifier), Target(19, identifier),
        Target(20, identifier), Target(21, identifier), Target(23, mapOf(1 to Kind.CITY)),
    ),
    "NK1" to listOf(
        Target(2, name), Target(4, address), Target(5, phone), Target(6, phone), Target(12, identifier), Target(13, organization),
        Target(30, name), Target(31, phone), Target(32, address), Target(33, identifier), Target(37, identifier),
    ),
    "GT1" to listOf(
        Target(2, identifier), Target(3, name), Target(4, name), Target(5, address), Target(6, phone), Target(7, phone),
        Target(12, identifier), Target(16, name), Target(17, address), Target(18, phone), Target(19, identifier), Target(21, organization),
    ),
    "IN1" to listOf(
        Target(16, name), Target(19, address), Target(36, identifier), Target(49, identifier), Target(46, identifier),
    ),
    "MRG" to listOf(Target(1, identifier), Target(2, identifier), Target(3, identifier), Target(4, identifier), Target(7, name)),
)

private val freeTextTypes = setOf("TX", "FT", "ST", "CF")

internal fun loadCache(platform: Platform, fresh: () -> AnonymizeCache): AnonymizeCache =
    platform.loadValue(cacheKey)?.let { runCatching { cacheJson.decodeFromString(AnonymizeCache.serializer(), it) }.getOrNull() } ?: fresh()

internal fun saveCache(platform: Platform, cache: AnonymizeCache) =
    platform.storeValue(cacheKey, cacheJson.encodeToString(AnonymizeCache.serializer(), cache))

internal fun freshCache(millis: Long): AnonymizeCache {
    val seed = millis xor 0x5DEECE66DL
    val days = -(30 + (mix(seed, "shift") % 700).toInt())
    return AnonymizeCache(seed, days)
}

private fun mix(seed: Long, text: String): Long {
    var hash = seed xor -0x340d631b7bdddcdbL
    for (char in text) {
        hash = hash xor char.code.toLong()
        hash *= 0x100000001b3L
    }
    return hash ushr 1
}

private fun pick(list: List<String>, seed: Long, original: String): String = list[(mix(seed, original) % list.size).toInt()]

private fun scramble(value: String, seed: Long): String {
    var state = mix(seed, value)
    return buildString {
        for (char in value) {
            state = state * 6364136223846793005L + 1442695040888963407L
            val roll = (state ushr 33).toInt() and 0x7fffffff
            append(
                when {
                    char.isDigit() -> '0' + roll % 10
                    char.isUpperCase() -> 'A' + roll % 26
                    char.isLowerCase() -> 'a' + roll % 26
                    else -> char
                },
            )
        }
    }
}

private fun textLike(value: String, seed: Long, words: List<String>): String {
    val count = value.split(' ').count { it.isNotBlank() }.coerceIn(1, 60)
    return (0 until count).joinToString(" ") { pick(words, seed + it, "$value#$it") }.replaceFirstChar { it.uppercase() } + "."
}

internal fun replacement(kind: Kind, value: String, cache: AnonymizeCache, book: Wordbook): String = when (kind) {
    Kind.FAMILY -> pick(book.families, cache.seed, value.uppercase())
    Kind.GIVEN -> pick(book.givens, cache.seed, value.uppercase())
    Kind.STREET -> "${pick(book.streets, cache.seed, value)} ${1 + mix(cache.seed, value + "#") % 120}"
    Kind.CITY -> pick(book.cities, cache.seed, value.uppercase())
    Kind.ORGANIZATION -> pick(book.organizations, cache.seed, value.uppercase())
    Kind.EMAIL -> "${scramble(value.substringBefore('@').filter { it.isLetterOrDigit() }.ifEmpty { "user" }, cache.seed).lowercase()}@example.org"
    Kind.TEXT -> textLike(value, cache.seed, book.words)
    Kind.IDENTIFIER, Kind.POSTAL, Kind.PHONE -> scramble(value, cache.seed)
}

private fun enabled(kind: Kind, options: AnonymizeOptions): Boolean = when (kind) {
    Kind.FAMILY, Kind.GIVEN, Kind.ORGANIZATION -> options.names
    Kind.IDENTIFIER -> options.identifiers
    Kind.STREET, Kind.CITY, Kind.POSTAL -> options.addresses
    Kind.PHONE, Kind.EMAIL -> options.phones
    Kind.TEXT -> options.freeText
}

private fun looksLikeDate(raw: String): Boolean = raw.length >= 8 && raw.take(8).all { it.isDigit() } && Hl7Dates.isValid(raw)

internal class Edit(val path: FieldPath, val value: String)

internal fun editsFor(message: ParsedMessage, dictionary: Hl7Dictionary?, options: AnonymizeOptions, cache: MutableMap<String, String>, state: AnonymizeCache, book: Wordbook): List<Edit> {
    val edits = mutableListOf<Edit>()
    fun replace(path: FieldPath, kind: Kind) {
        val value = Er7.value(message, path)
        if (value.isBlank() || value == "\"\"") return
        val key = "${kind.name}:$value"
        edits += Edit(path, cache.getOrPut(key) { replacement(kind, value, state, book) })
    }
    for (segment in message.segments) {
        for (target in targets[segment.name].orEmpty()) {
            val field = segment.field(target.field) ?: continue
            for (rep in field.repetitions) {
                val multi = rep.components.size > 1
                for ((component, kind) in target.components) {
                    if (!enabled(kind, options)) continue
                    if (!multi && component != 1) continue
                    replace(FieldPath(segment.index, target.field, rep.index, if (multi) component else 0), kind)
                }
            }
        }
        if (options.freeText) {
            when (segment.name) {
                "NTE" -> segment.field(3)?.repetitions?.forEach { replace(FieldPath(segment.index, 3, it.index), Kind.TEXT) }
                "OBX" -> {
                    val type = Er7.value(message, FieldPath(segment.index, 2))
                    if (type in freeTextTypes) segment.field(5)?.repetitions?.forEach { replace(FieldPath(segment.index, 5, it.index), Kind.TEXT) }
                }
            }
        }
        if (options.shiftDates) {
            for (field in segment.fields) {
                if (segment.name == "MSH" && field.number <= 2) continue
                val def = Dictionaries.field(dictionary, segment.name, field.number)
                for (rep in field.repetitions) {
                    val multi = rep.components.size > 1
                    if (!multi) {
                        val raw = rep.raw
                        val isDate = def?.let { Dictionaries.isDate(it.datatype) || (it.datatype == "TS" || it.datatype == "DR") } ?: looksLikeDate(raw)
                        if (isDate && looksLikeDate(raw)) Hl7Dates.shift(raw, state.shiftDays)?.let { edits += Edit(FieldPath(segment.index, field.number, rep.index), it) }
                    } else {
                        for (comp in rep.components) {
                            if (comp.subcomponents.size > 1) continue
                            val node = Dictionaries.node(dictionary, segment.name, field.number, comp.index)
                            val isDate = node?.let { Dictionaries.isDate(it.datatype) || it.datatype == "TS" } ?: false
                            if (isDate && looksLikeDate(comp.raw)) Hl7Dates.shift(comp.raw, state.shiftDays)?.let { edits += Edit(FieldPath(segment.index, field.number, rep.index, comp.index), it) }
                        }
                    }
                }
            }
        }
    }
    return edits.distinctBy { it.path }
}

internal fun applyEdits(text: String, edits: List<Edit>): String {
    var current = text
    val ordered = edits.sortedWith(compareByDescending<Edit> { it.path.segment }.thenByDescending { it.path.field }.thenByDescending { it.path.repetition }.thenByDescending { it.path.component })
    for (edit in ordered) {
        current = Er7.write(Er7.parse(current), edit.path, edit.value)
    }
    return current
}

class Anonymizer(private val platform: Platform) {
    var cache by mutableStateOf(loadCache(platform) { freshCache(platform.nowMillis()) })
        private set

    suspend fun run(
        texts: List<String>,
        dictionary: (String) -> Hl7Dictionary?,
        options: AnonymizeOptions,
        onProgress: (Int) -> Unit,
    ): List<String> {
        val values = cache.values.toMutableMap()
        val result = texts.mapIndexed { index, text ->
            onProgress(index)
            yield()
            anonymizeText(text, dictionary(text), options, values, cache, englishWordbook)
        }
        onProgress(texts.size)
        cache = cache.copy(values = values)
        saveCache(platform, cache)
        return result
    }

    fun reset() {
        cache = freshCache(platform.nowMillis())
        saveCache(platform, cache)
    }
}

internal fun anonymizeText(text: String, dictionary: Hl7Dictionary?, options: AnonymizeOptions, cache: MutableMap<String, String>, state: AnonymizeCache, book: Wordbook): String {
    val message = Er7.parse(text)
    if (message.isEmpty) return text
    return applyEdits(text, editsFor(message, dictionary, options, cache, state, book))
}
