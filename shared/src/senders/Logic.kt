package hl7lookup.senders

import androidx.compose.runtime.mutableStateListOf
import hl7lookup.engine.Endpoint
import hl7lookup.engine.Protocol
import hl7lookup.platform.Platform
import hl7lookup.platform.Platforms
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Serializable
data class Sender(
    val id: String,
    val name: String,
    val protocol: Protocol = Protocol.MLLP,
    val host: String = "localhost",
    val port: Int = 2575,
    val path: String = "/",
    val timeoutMillis: Int = 10_000,
    val targetTab: String? = null,
)

private val sendersJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

private val sendersKey get() = Platforms.key("senders", "v1")

internal fun endpointOf(sender: Sender): Endpoint = Endpoint(sender.protocol, sender.host.trim(), sender.port, sender.path.ifBlank { "/" }, sender.timeoutMillis)

internal fun isComplete(sender: Sender): Boolean =
    sender.name.isNotBlank() && (sender.protocol == Protocol.TAB && sender.targetTab != null || sender.protocol != Protocol.TAB && sender.host.isNotBlank() && sender.port in 1..65535)

internal fun addressOf(sender: Sender, tabTitle: (String) -> String?): String = when (sender.protocol) {
    Protocol.TAB -> "→ " + (sender.targetTab?.let(tabTitle) ?: "?")
    Protocol.HTTP -> "http://${sender.host}:${sender.port}${sender.path.ifBlank { "/" }}"
    Protocol.MLLP -> "${sender.host}:${sender.port}"
}

class SenderStore(private val platform: Platform) {
    val items = mutableStateListOf<Sender>().apply {
        addAll(platform.loadValue(sendersKey)?.let { runCatching { sendersJson.decodeFromString(ListSerializer(Sender.serializer()), it) }.getOrNull() }.orEmpty())
    }

    private fun save() = platform.storeValue(sendersKey, sendersJson.encodeToString(ListSerializer(Sender.serializer()), items.toList()))

    fun upsert(sender: Sender) {
        val index = items.indexOfFirst { it.id == sender.id }
        if (index >= 0) items[index] = sender else items += sender
        save()
    }

    fun remove(id: String) {
        items.removeAll { it.id == id }
        save()
    }

    fun get(id: String?): Sender? = items.firstOrNull { it.id == id }
}
