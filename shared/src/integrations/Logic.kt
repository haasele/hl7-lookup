package hl7lookup.integrations

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import hl7lookup.platform.Platform
import hl7lookup.platform.Platforms
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Serializable
data class Integration(
    val id: String,
    val name: String,
    val senderId: String? = null,
    val receiverId: String? = null,
    val interfaceId: String? = null,
)

@Serializable
data class ActiveSelection(val integrationId: String? = null, val senderId: String? = null, val receiverId: String? = null)

private val integrationsJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

private val integrationsKey get() = Platforms.key("integrations", "v1")
private val activeKey get() = Platforms.key("integrations", "active")

internal fun isComplete(integration: Integration): Boolean =
    integration.name.isNotBlank() && (integration.senderId != null || integration.receiverId != null || integration.interfaceId != null)

class IntegrationStore(private val platform: Platform) {
    val items = mutableStateListOf<Integration>().apply {
        addAll(platform.loadValue(integrationsKey)?.let { runCatching { integrationsJson.decodeFromString(ListSerializer(Integration.serializer()), it) }.getOrNull() }.orEmpty())
    }
    var active by mutableStateOf(
        platform.loadValue(activeKey)?.let { runCatching { integrationsJson.decodeFromString(ActiveSelection.serializer(), it) }.getOrNull() } ?: ActiveSelection(),
    )
        private set

    private fun save() = platform.storeValue(integrationsKey, integrationsJson.encodeToString(ListSerializer(Integration.serializer()), items.toList()))

    private fun saveActive() = platform.storeValue(activeKey, integrationsJson.encodeToString(ActiveSelection.serializer(), active))

    fun upsert(integration: Integration) {
        val index = items.indexOfFirst { it.id == integration.id }
        if (index >= 0) items[index] = integration else items += integration
        save()
    }

    fun remove(id: String) {
        items.removeAll { it.id == id }
        if (active.integrationId == id) activate(null)
        save()
    }

    fun get(id: String?): Integration? = items.firstOrNull { it.id == id }

    fun activate(id: String?) {
        val integration = get(id)
        active = if (integration == null) ActiveSelection(null, active.senderId, active.receiverId)
        else ActiveSelection(integration.id, integration.senderId ?: active.senderId, integration.receiverId ?: active.receiverId)
        saveActive()
    }

    fun activateSender(id: String) {
        active = active.copy(senderId = id)
        saveActive()
    }

    fun activateReceiver(id: String) {
        active = active.copy(receiverId = id)
        saveActive()
    }
}
