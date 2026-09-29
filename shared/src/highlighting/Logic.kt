package hl7lookup.highlighting

import androidx.compose.runtime.mutableStateListOf
import hl7lookup.document.Er7
import hl7lookup.document.FieldPath
import hl7lookup.document.ParsedMessage
import hl7lookup.platform.Platform
import hl7lookup.platform.Platforms
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Serializable
data class HighlightRule(
    val id: String,
    val spec: String,
    val value: String = "",
    val color: Int = 0,
    val enabled: Boolean = true,
    val source: String? = null,
)

data class HighlightHit(val path: FieldPath, val color: Int)

private val rulesJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

private val rulesKey get() = Platforms.key("highlights", "v1")

internal fun loadRules(platform: Platform): List<HighlightRule> =
    platform.loadValue(rulesKey)?.let { runCatching { rulesJson.decodeFromString(ListSerializer(HighlightRule.serializer()), it) }.getOrNull() }.orEmpty()

internal fun saveRules(platform: Platform, rules: List<HighlightRule>) =
    platform.storeValue(rulesKey, rulesJson.encodeToString(ListSerializer(HighlightRule.serializer()), rules))

internal fun isValidSpec(spec: String): Boolean = Er7.spec(spec.trim().uppercase()) != null

internal fun hitsFor(rules: List<HighlightRule>, message: ParsedMessage): List<HighlightHit> {
    val hits = mutableListOf<HighlightHit>()
    for (rule in rules) {
        if (!rule.enabled) continue
        val spec = Er7.spec(rule.spec.trim().uppercase()) ?: continue
        for (segment in message.segments) {
            if (segment.name != spec.segment) continue
            val occurrence = Er7.occurrence(message, segment.index)
            val path = Er7.resolve(message, spec, occurrence) ?: continue
            val repetitions = if (spec.repetition != null || spec.field == 0) listOf(path.repetition)
            else (segment.field(spec.field)?.repetitions?.indices?.map { it + 1 } ?: listOf(1))
            for (rep in repetitions) {
                val target = path.copy(repetition = rep)
                val value = Er7.value(message, target)
                if (rule.value.isBlank() && value.isBlank() && spec.field != 0) continue
                if (rule.value.isNotBlank() && !value.contains(rule.value.trim(), ignoreCase = true)) continue
                hits += HighlightHit(target, rule.color)
            }
        }
    }
    return hits
}

internal fun colorAt(hits: List<HighlightHit>, path: FieldPath): Int? =
    hits.lastOrNull { hit ->
        hit.path.segment == path.segment &&
            (hit.path.field == 0 || hit.path.field == path.field) &&
            (hit.path.field == 0 || path.field == 0 || hit.path.repetition == path.repetition || path.component == 0 && hit.path.component == 0) &&
            (hit.path.component == 0 || hit.path.component == path.component) &&
            (hit.path.subcomponent == 0 || hit.path.subcomponent == path.subcomponent)
    }?.color

internal fun messageColor(rules: List<HighlightRule>, text: String): Int? {
    if (rules.none { it.enabled }) return null
    return hitsFor(rules, Er7.parse(text)).firstOrNull()?.color
}

class HighlightState(private val platform: Platform) {
    val rules = mutableStateListOf<HighlightRule>().apply { addAll(loadRules(platform)) }

    private fun save() = saveRules(platform, rules.filter { it.source == null })

    fun add(rule: HighlightRule) {
        rules += rule
        save()
    }

    fun remove(id: String) {
        rules.removeAll { it.id == id }
        save()
    }

    fun toggle(id: String) {
        val index = rules.indexOfFirst { it.id == id }
        if (index >= 0) {
            rules[index] = rules[index].copy(enabled = !rules[index].enabled)
            save()
        }
    }

    fun replaceFromSource(source: String, next: List<HighlightRule>) {
        rules.removeAll { it.source == source }
        rules.addAll(next.map { it.copy(source = source) })
    }

    fun clear() {
        rules.removeAll { it.source == null }
        save()
    }
}
