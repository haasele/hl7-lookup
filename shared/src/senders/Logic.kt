// Endpoint list state. senders/Index calls it.
package hl7lookup.senders

import androidx.compose.runtime.mutableStateListOf
import hl7lookup.engine.Endpoint
import hl7lookup.engine.Protocol
import hl7lookup.platform.Platform
import hl7lookup.platform.Platforms
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

// Outgoing endpoint definition. SenderStore persists these; Index panels edit them.
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

// Builds an engine Endpoint from a Sender. Senders.endpoint and send flows call it.
internal fun endpointOf(sender: Sender): Endpoint = Endpoint(sender.protocol, sender.host.trim(), sender.port, sender.path.ifBlank { "/" }, sender.timeoutMillis)

// Checks name and host/port or target tab. SenderDialog and Senders.complete call it.
internal fun isComplete(sender: Sender): Boolean =
    sender.name.isNotBlank() && (sender.protocol == Protocol.TAB && sender.targetTab != null || sender.protocol != Protocol.TAB && sender.host.isNotBlank() && sender.port in 1..65535)

// Formats host:port, URL or tab target. SendersPanel and Senders.address call it.
internal fun addressOf(sender: Sender, tabTitle: (String) -> String?): String = when (sender.protocol) {
    Protocol.TAB -> "→ " + (sender.targetTab?.let(tabTitle) ?: "?")
    Protocol.HTTP -> "http://${sender.host}:${sender.port}${sender.path.ifBlank { "/" }}"
    Protocol.MLLP -> "${sender.host}:${sender.port}"
}

// Persisted list of senders. Workspace holds it; SendersPanel mutates via upsert/remove.
class SenderStore(private val platform: Platform) {
    val items = mutableStateListOf<Sender>().apply {
        addAll(platform.loadValue(sendersKey)?.let { runCatching { sendersJson.decodeFromString(ListSerializer(Sender.serializer()), it) }.getOrNull() }.orEmpty())
    }

    // Writes the list to platform storage. upsert and remove call it.
    private fun save() = platform.storeValue(sendersKey, sendersJson.encodeToString(ListSerializer(Sender.serializer()), items.toList()))

    // Inserts or replaces a sender then saves. SenderDialog onSave calls it.
    fun upsert(sender: Sender) {
        val index = items.indexOfFirst { it.id == sender.id }
        if (index >= 0) items[index] = sender else items += sender
        save()
    }

    // Deletes a sender by id then saves. SendersPanel trash action calls it.
    fun remove(id: String) {
        items.removeAll { it.id == id }
        save()
    }

    // Looks up a sender by id. Workspace send/test flows call it.
    fun get(id: String?): Sender? = items.firstOrNull { it.id == id }
}
