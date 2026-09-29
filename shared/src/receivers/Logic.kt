// Listener list state. receivers/Index calls it.
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

// Incoming listener definition. ReceiverStore persists these; Index panels edit them.
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

// Builds engine ReceiverConfig from a Receiver. Receivers.config and start flows call it.
internal fun configOf(receiver: Receiver): ReceiverConfig =
    ReceiverConfig(receiver.id, receiver.name, receiver.protocol, receiver.port, receiver.path.ifBlank { "/" }, receiver.autoAck)

// Checks name, port and non-TAB protocol. ReceiverDialog and Receivers.complete call it.
internal fun isComplete(receiver: Receiver): Boolean = receiver.name.isNotBlank() && receiver.port in 1..65535 && receiver.protocol != Protocol.TAB

// Formats the listen URL or host:port. ReceiversPanel and Receivers.address call it.
internal fun addressOf(receiver: Receiver): String = when (receiver.protocol) {
    Protocol.HTTP -> "http://0.0.0.0:${receiver.port}${receiver.path.ifBlank { "/" }}"
    else -> "0.0.0.0:${receiver.port}"
}

// Persisted receivers plus live statuses. Workspace holds it; panels and poll update it.
class ReceiverStore(private val platform: Platform) {
    val items = mutableStateListOf<Receiver>().apply {
        addAll(platform.loadValue(receiversKey)?.let { runCatching { receiversJson.decodeFromString(ListSerializer(Receiver.serializer()), it) }.getOrNull() }.orEmpty())
    }
    val statuses = mutableStateMapOf<String, ReceiverStatus>()

    // Writes the list to platform storage. upsert and remove call it.
    private fun save() = platform.storeValue(receiversKey, receiversJson.encodeToString(ListSerializer(Receiver.serializer()), items.toList()))

    // Inserts or replaces a receiver then saves. ReceiverDialog onSave calls it.
    fun upsert(receiver: Receiver) {
        val index = items.indexOfFirst { it.id == receiver.id }
        if (index >= 0) items[index] = receiver else items += receiver
        save()
    }

    // Deletes a receiver and its status then saves. ReceiversPanel trash action calls it.
    fun remove(id: String) {
        items.removeAll { it.id == id }
        statuses.remove(id)
        save()
    }

    // Looks up a receiver by id. Workspace toggle/poll flows call it.
    fun get(id: String?): Receiver? = items.firstOrNull { it.id == id }

    // Stores the latest engine status for a receiver. Workspace poll updates call it.
    fun update(status: ReceiverStatus) {
        statuses[status.id] = status
    }

    // Whether a receiver is currently listening. Panel buttons and remove guard call it.
    fun running(id: String): Boolean = statuses[id]?.running == true
}
