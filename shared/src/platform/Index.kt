// File, clipboard and storage contract. Workspace calls Platforms; the desktop and web hosts implement Platform.
package hl7lookup.platform

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

val LocalPlatform: ProvidableCompositionLocal<Platform> = staticCompositionLocalOf { error("No platform provided") }

// Entry point other features use for storage keys, extensions and dialog texts; forwards to Logic.
object Platforms {
    // Builds a namespaced preference key; other features call this, it forwards to storageKey.
    fun key(scope: String, name: String): String = storageKey(scope, name)
    // Returns allowed HL7 message file extensions; other features call this, it reads messageFileExtensions.
    fun messageExtensions(): List<String> = messageFileExtensions
    // Returns allowed interface file extensions; other features call this, it reads interfaceFileExtensions.
    fun interfaceExtensions(): List<String> = interfaceFileExtensions
    // Exposes file-dialog wording; other features call this, it returns PlatformTexts.
    fun texts() = PlatformTexts
}
