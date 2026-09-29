// Bundle list state. integrations/Index calls it.
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

// One named sender+receiver+interface bundle. IntegrationStore persists these; Index edits them.
@Serializable
data class Integration(
    val id: String,
    val name: String,
    val senderId: String? = null,
    val receiverId: String? = null,
    val interfaceId: String? = null,
)

// Which integration and peer ids are selected now. IntegrationStore keeps and saves this.
@Serializable
data class ActiveSelection(val integrationId: String? = null, val senderId: String? = null, val receiverId: String? = null)

private val integrationsJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

private val integrationsKey get() = Platforms.key("integrations", "v1")
private val activeKey get() = Platforms.key("integrations", "active")

// Name plus at least one linked peer. Integrations.complete and the save button use it.
internal fun isComplete(integration: Integration): Boolean =
    integration.name.isNotBlank() && (integration.senderId != null || integration.receiverId != null || integration.interfaceId != null)

// Loads, saves and activates integrations. Index panels and Workspace call these methods.
class IntegrationStore(private val platform: Platform) {
    val items = mutableStateListOf<Integration>().apply {
        addAll(platform.loadValue(integrationsKey)?.let { runCatching { integrationsJson.decodeFromString(ListSerializer(Integration.serializer()), it) }.getOrNull() }.orEmpty())
    }
    var active by mutableStateOf(
        platform.loadValue(activeKey)?.let { runCatching { integrationsJson.decodeFromString(ActiveSelection.serializer(), it) }.getOrNull() } ?: ActiveSelection(),
    )
        private set

    // Writes the item list to platform storage. upsert and remove call it.
    private fun save() = platform.storeValue(integrationsKey, integrationsJson.encodeToString(ListSerializer(Integration.serializer()), items.toList()))

    // Writes the active selection. activate* methods call it.
    private fun saveActive() = platform.storeValue(activeKey, integrationsJson.encodeToString(ActiveSelection.serializer(), active))

    // Inserts or replaces one integration then saves. Dialog save and panel edits call it.
    fun upsert(integration: Integration) {
        val index = items.indexOfFirst { it.id == integration.id }
        if (index >= 0) items[index] = integration else items += integration
        save()
    }

    // Deletes by id and clears active if needed. Trash action in the panel calls it.
    fun remove(id: String) {
        items.removeAll { it.id == id }
        if (active.integrationId == id) activate(null)
        save()
    }

    // Finds one item by id. activate and callers that need the current bundle use it.
    fun get(id: String?): Integration? = items.firstOrNull { it.id == id }

    // Sets the active integration and its linked peers. Panel activate and Workspace call it.
    fun activate(id: String?) {
        val integration = get(id)
        active = if (integration == null) ActiveSelection(null, active.senderId, active.receiverId)
        else ActiveSelection(integration.id, integration.senderId ?: active.senderId, integration.receiverId ?: active.receiverId)
        saveActive()
    }

    // Overrides only the active sender id. Workspace when picking a sender calls it.
    fun activateSender(id: String) {
        active = active.copy(senderId = id)
        saveActive()
    }

    // Overrides only the active receiver id. Workspace when picking a receiver calls it.
    fun activateReceiver(id: String) {
        active = active.copy(receiverId = id)
        saveActive()
    }
}
