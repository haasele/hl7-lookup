package hl7lookup.platform

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

val LocalPlatform: ProvidableCompositionLocal<Platform> = staticCompositionLocalOf { error("No platform provided") }

object Platforms {
    fun key(scope: String, name: String): String = storageKey(scope, name)
    fun messageExtensions(): List<String> = messageFileExtensions
    fun interfaceExtensions(): List<String> = interfaceFileExtensions
    fun texts() = PlatformTexts
}
