package hl7lookup.desktop.window

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import hl7lookup.engine.Engines
import hl7lookup.platform.OpenedFile
import hl7lookup.platform.Platform
import hl7lookup.platform.PlatformKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Dimension
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Toolkit
import java.awt.Window
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

internal const val APP_VERSION = "2.0.0"

data class LaunchOptions(val headless: Boolean = false, val server: Boolean = true, val port: Int = Engines.defaultPort(), val webDir: File? = null, val files: List<File> = emptyList())

internal fun parseArgs(args: Array<String>): LaunchOptions {
    var options = LaunchOptions()
    var index = 0
    while (index < args.size) {
        when (val arg = args[index]) {
            "--server", "--headless" -> options = options.copy(headless = true, server = true)
            "--no-server" -> options = options.copy(server = false)
            "--port" -> options = options.copy(port = args.getOrNull(++index)?.toIntOrNull() ?: options.port)
            "--web" -> options = options.copy(webDir = args.getOrNull(++index)?.let(::File))
            else -> if (!arg.startsWith("--")) options = options.copy(files = options.files + File(arg))
        }
        index += 1
    }
    return options
}

internal fun relaunchForWayland() {
    if (System.getenv("WAYLAND_DISPLAY").isNullOrBlank()) return
    if (System.getenv("_JAVA_AWT_WM_NONREPARENTING") == "1") return
    val raw = File("/proc/self/cmdline").readBytes()
    val command = mutableListOf<String>()
    var start = 0
    for (index in raw.indices) {
        if (raw[index] != 0.toByte()) continue
        if (index > start) command += String(raw, start, index - start, Charsets.UTF_8)
        start = index + 1
    }
    if (start < raw.size) command += String(raw, start, raw.size - start, Charsets.UTF_8)
    if (command.isEmpty()) return
    val child = ProcessBuilder(command).inheritIO().apply {
        environment()["_JAVA_AWT_WM_NONREPARENTING"] = "1"
    }.start()
    kotlin.system.exitProcess(child.waitFor())
}

internal fun releaseWindowSizeLimits(window: Window) {
    window.minimumSize = Dimension(480, 320)
    window.maximumSize = Dimension(16384, 16384)
    if (window is Frame) window.maximizedBounds = null
}

internal fun storageDirectory(): File {
    val os = System.getProperty("os.name").lowercase()
    val home = File(System.getProperty("user.home"))
    val base = when {
        os.contains("win") -> System.getenv("APPDATA")?.let(::File)?.resolve("HL7Lookup") ?: home.resolve("AppData/Roaming/HL7Lookup")
        os.contains("mac") -> home.resolve("Library/Application Support/HL7Lookup")
        else -> (System.getenv("XDG_CONFIG_HOME")?.let(::File) ?: home.resolve(".config")).resolve("hl7lookup")
    }
    return base.also { it.mkdirs() }
}

internal fun keyFile(directory: File, key: String): File = directory.resolve(key.replace(Regex("[^A-Za-z0-9._-]"), "_") + ".json")

internal fun loadFont(): FontFamily? = runCatching {
    val bytes = Thread.currentThread().contextClassLoader.getResourceAsStream("fonts/JetBrainsMono-Regular.ttf")!!.use { it.readBytes() }
    FontFamily(Font("JetBrainsMono-Regular", bytes))
}.getOrNull()

internal fun iconBytes(): ByteArray? = Thread.currentThread().contextClassLoader.getResourceAsStream("icons/hl7.png")?.use { it.readBytes() }

internal fun readFiles(files: List<File>): List<OpenedFile> = files.filter { it.isFile }.mapNotNull { file ->
    runCatching { OpenedFile(file.absolutePath, file.readText()) }.getOrNull()
}

class DesktopPlatform(private val storage: File) : Platform {
    override val kind: PlatformKind = PlatformKind.DESKTOP
    override var serverAddress: String? = null
    private var lastDirectory: String? = null

    override suspend fun openTextFile(title: String, extensions: List<String>): OpenedFile? = withContext(Dispatchers.Main) {
        val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
        dialog.directory = lastDirectory
        dialog.setFilenameFilter { _, name -> extensions.isEmpty() || extensions.any { name.endsWith(".$it", ignoreCase = true) } }
        dialog.isVisible = true
        val name = dialog.file ?: return@withContext null
        val file = File(dialog.directory, name)
        lastDirectory = dialog.directory
        withContext(Dispatchers.IO) { runCatching { OpenedFile(file.absolutePath, file.readText()) }.getOrNull() }
    }

    override suspend fun saveTextFile(title: String, suggestedName: String, content: String): String? = withContext(Dispatchers.Main) {
        val existing = File(suggestedName).takeIf { it.isAbsolute }
        val dialog = FileDialog(null as Frame?, title, FileDialog.SAVE)
        dialog.directory = existing?.parent ?: lastDirectory
        dialog.file = existing?.name ?: suggestedName
        dialog.isVisible = true
        val name = dialog.file ?: return@withContext null
        val file = File(dialog.directory, name)
        lastDirectory = dialog.directory
        withContext(Dispatchers.IO) {
            file.writeText(content)
            file.absolutePath
        }
    }

    override suspend fun readClipboard(): String? = withContext(Dispatchers.Main) {
        runCatching { Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor) as? String }.getOrNull()
    }

    override suspend fun writeClipboard(text: String) = withContext(Dispatchers.Main) {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
    }

    override fun loadValue(key: String): String? = keyFile(storage, key).takeIf { it.isFile }?.let { runCatching { it.readText() }.getOrNull() }

    override fun storeValue(key: String, value: String) {
        val target = keyFile(storage, key)
        val temp = File(storage, target.name + ".tmp")
        temp.writeText(value)
        Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    override fun nowMillis(): Long = System.currentTimeMillis()
}
