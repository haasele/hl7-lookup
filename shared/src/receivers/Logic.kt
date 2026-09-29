package hl7lookup.receivers

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import hl7lookup.engine.Protocol
import hl7lookup.engine.ReceiverConfig
import hl7lookup.engine.ReceiverStatus
import hl7lookup.platform.Platform
import hl7lookup.platform.Platforms
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Serializable
data class Receiver(
    val id: String,
    val name: String,
    val protocol: Protocol = Protocol.MLLP,
    val port: Int = 2576,
    val path: String = "/",
    val autoAck: Boolean = true,
    val targetTab: String? = null,
)

private val receiversJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

private val receiversKey get() = Platforms.key("receivers", "v1")

internal fun configOf(receiver: Receiver): ReceiverConfig =
    ReceiverConfig(receiver.id, receiver.name, receiver.protocol, receiver.port, receiver.path.ifBlank { "/" }, receiver.autoAck)

internal fun isComplete(receiver: Receiver): Boolean = receiver.name.isNotBlank() && receiver.port in 1..65535 && receiver.protocol != Protocol.TAB

internal fun addressOf(receiver: Receiver): String = when (receiver.protocol) {
    Protocol.HTTP -> "http://0.0.0.0:${receiver.port}${receiver.path.ifBlank { "/" }}"
    else -> "0.0.0.0:${receiver.port}"
}

class ReceiverStore(private val platform: Platform) {
    val items = mutableStateListOf<Receiver>().apply {
        addAll(platform.loadValue(receiversKey)?.let { runCatching { receiversJson.decodeFromString(ListSerializer(Receiver.serializer()), it) }.getOrNull() }.orEmpty())
    }
    val statuses = mutableStateMapOf<String, ReceiverStatus>()

    private fun save() = platform.storeValue(receiversKey, receiversJson.encodeToString(ListSerializer(Receiver.serializer()), items.toList()))

    fun upsert(receiver: Receiver) {
        val index = items.indexOfFirst { it.id == receiver.id }
        if (index >= 0) items[index] = receiver else items += receiver
        save()
    }

    fun remove(id: String) {
        items.removeAll { it.id == id }
        statuses.remove(id)
        save()
    }

    fun get(id: String?): Receiver? = items.firstOrNull { it.id == id }

    fun update(status: ReceiverStatus) {
        statuses[status.id] = status
    }

    fun running(id: String): Boolean = statuses[id]?.running == true
}
