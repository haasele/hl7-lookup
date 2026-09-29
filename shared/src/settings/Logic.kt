// Persisted preference values. settings/Index calls it.
package hl7lookup.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import hl7lookup.datetime.DateStyle
import hl7lookup.i18n.Language
import hl7lookup.platform.Platform
import hl7lookup.platform.Platforms
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// User preference values persisted to the platform. SettingsState holds the live copy.
@Serializable
data class Settings(
    val dateStyle: DateStyle = DateStyle.EUROPEAN,
    val version: String = "2.5",
    val showEmptyFields: Boolean = false,
    val language: Language = Language.EN,
    val autoValidate: Boolean = true,
    val relativeDates: Boolean = true,
)

private val settingsJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

// Reads Settings from platform storage. SettingsState constructor calls it.
internal fun loadSettings(platform: Platform): Settings =
    platform.loadValue(Platforms.key("settings", "v1"))
        ?.let { runCatching { settingsJson.decodeFromString(Settings.serializer(), it) }.getOrNull() }
        ?: Settings()

// Writes Settings to platform storage. SettingsState.update calls it.
internal fun saveSettings(platform: Platform, settings: Settings) =
    platform.storeValue(Platforms.key("settings", "v1"), settingsJson.encodeToString(Settings.serializer(), settings))

// Live settings with load and save. SettingsDialog and Workspace read current and call update.
class SettingsState(private val platform: Platform) {
    var current by mutableStateOf(loadSettings(platform))
        private set

    // Applies a change and persists. Dialog controls call this on every edit.
    fun update(change: (Settings) -> Settings) {
        current = change(current)
        saveSettings(platform, current)
    }
}
