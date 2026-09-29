// Storage keys and file extensions. platform/Index exposes them.
package hl7lookup.platform

// Which host the app is running on; Platform implementations expose this via kind.
enum class PlatformKind { DESKTOP, BROWSER }

// Name and content from a file pick; openTextFile returns this to callers.
data class OpenedFile(val name: String, val content: String)

// Host contract for files, clipboard and storage; desktop and web implement it, Index routes through Platforms.
interface Platform {
    val kind: PlatformKind
    val serverAddress: String?
    // Opens a text file picker and returns the chosen file; UI flows call this on the provided Platform.
    suspend fun openTextFile(title: String, extensions: List<String>): OpenedFile?
    // Saves text through a file dialog and returns the path or name; UI flows call this on the provided Platform.
    suspend fun saveTextFile(title: String, suggestedName: String, content: String): String?
    // Reads the system clipboard text; UI flows call this on the provided Platform.
    suspend fun readClipboard(): String?
    // Writes text to the system clipboard; UI flows call this on the provided Platform.
    suspend fun writeClipboard(text: String)
    // Loads a persisted string by key; settings and workspace call this on the provided Platform.
    fun loadValue(key: String): String?
    // Stores a string under a key; settings and workspace call this on the provided Platform.
    fun storeValue(key: String, value: String)
    // Returns the current time in milliseconds; callers needing timestamps use this on the provided Platform.
    fun nowMillis(): Long
}

// Builds a dotted preference key; Platforms.key forwards here.
internal fun storageKey(scope: String, name: String): String = "hl7lookup.$scope.$name"

internal val messageFileExtensions = listOf("hl7", "txt", "er7", "msg")

internal val interfaceFileExtensions = listOf("json", "hl7if")
