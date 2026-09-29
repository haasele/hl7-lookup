package hl7lookup.platform

enum class PlatformKind { DESKTOP, BROWSER }

data class OpenedFile(val name: String, val content: String)

interface Platform {
    val kind: PlatformKind
    val serverAddress: String?
    suspend fun openTextFile(title: String, extensions: List<String>): OpenedFile?
    suspend fun saveTextFile(title: String, suggestedName: String, content: String): String?
    suspend fun readClipboard(): String?
    suspend fun writeClipboard(text: String)
    fun loadValue(key: String): String?
    fun storeValue(key: String, value: String)
    fun nowMillis(): Long
}

internal fun storageKey(scope: String, name: String): String = "hl7lookup.$scope.$name"

internal val messageFileExtensions = listOf("hl7", "txt", "er7", "msg")

internal val interfaceFileExtensions = listOf("json", "hl7if")
