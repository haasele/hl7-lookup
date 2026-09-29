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

internal fun loadSettings(platform: Platform): Settings =
    platform.loadValue(Platforms.key("settings", "v1"))
        ?.let { runCatching { settingsJson.decodeFromString(Settings.serializer(), it) }.getOrNull() }
        ?: Settings()

internal fun saveSettings(platform: Platform, settings: Settings) =
    platform.storeValue(Platforms.key("settings", "v1"), settingsJson.encodeToString(Settings.serializer(), settings))

class SettingsState(private val platform: Platform) {
    var current by mutableStateOf(loadSettings(platform))
        private set

    fun update(change: (Settings) -> Settings) {
        current = change(current)
        saveSettings(platform, current)
    }
}
