// Args, storage, Droid Sans Mono, the window icon and Wayland sizing. window/Index calls them.
package hl7lookup.desktop.window

import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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

// Captures CLI flags for headless mode, server port, web dir and files to open. parseArgs builds it.
data class LaunchOptions(val headless: Boolean = false, val server: Boolean = true, val port: Int = Engines.defaultPort(), val webDir: File? = null, val files: List<File> = emptyList())

// Turns argv into LaunchOptions. main calls it before starting the engine and server.
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

// Relaunches the process with Wayland non-reparenting set. main calls it first on Linux.
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
        // jpackage skips the cfg classpath when this token is inherited, so the relaunch would exit at once.
        environment().remove("_JPACKAGE_LAUNCHER")
    }.start()
    kotlin.system.exitProcess(child.waitFor())
}

// Clears AWT min/max size clamps so the Compose window can grow. main's Window LaunchedEffect calls it.
internal fun releaseWindowSizeLimits(window: Window) {
    window.minimumSize = Dimension(480, 320)
    window.maximumSize = Dimension(16384, 16384)
    if (window is Frame) window.maximizedBounds = null
}

// Picks the OS config folder for HL7 Lookup. main passes it into DesktopPlatform.
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

// Maps a storage key to a safe JSON filename. DesktopPlatform load/store call it.
internal fun keyFile(directory: File, key: String): File = directory.resolve(key.replace(Regex("[^A-Za-z0-9._-]"), "_") + ".json")

// Loads Droid Sans Mono from resources. main uses it for the Workspace mono font.
internal fun loadFont(): FontFamily? = runCatching {
    val bytes = Thread.currentThread().contextClassLoader.getResourceAsStream("fonts/DroidSansMono.ttf")!!.use { it.readBytes() }
    FontFamily(Font("DroidSansMono", bytes))
}.getOrNull()

// Loads Droid Sans for the interface. main passes it to Workspace so Windows does not fall back to Times New Roman.
internal fun loadUiFont(): FontFamily? = runCatching {
    val regular = Thread.currentThread().contextClassLoader.getResourceAsStream("fonts/DroidSans.ttf")!!.use { it.readBytes() }
    val bold = Thread.currentThread().contextClassLoader.getResourceAsStream("fonts/DroidSans-Bold.ttf")!!.use { it.readBytes() }
    FontFamily(
        Font("DroidSans", regular, FontWeight.Normal),
        Font("DroidSans", regular, FontWeight.Medium),
        Font("DroidSansBold", bold, FontWeight.SemiBold),
        Font("DroidSansBold", bold, FontWeight.Bold),
    )
}.getOrNull()

// Reads the PNG icon bytes from resources. applyWindowIcon and logoBitmap call it.
internal fun iconBytes(): ByteArray? = Thread.currentThread().contextClassLoader.getResourceAsStream("icons/hl7.png")?.use { it.readBytes() }

// Sets the AWT window icon from the bundled PNG. main's Window LaunchedEffect calls it.
internal fun applyWindowIcon(window: java.awt.Window) {
    val bytes = iconBytes() ?: return
    val image = javax.imageio.ImageIO.read(java.io.ByteArrayInputStream(bytes)) ?: return
    window.iconImages = listOf(image)
}

// Decodes the logo into a Compose ImageBitmap. main passes it to Workspace.
internal fun logoBitmap(): androidx.compose.ui.graphics.ImageBitmap? = iconBytes()?.let {
    org.jetbrains.skia.Image.makeFromEncoded(it).toComposeImageBitmap()
}

// Reads CLI file paths into OpenedFile values. main opens each into the workspace.
internal fun readFiles(files: List<File>): List<OpenedFile> = files.filter { it.isFile }.mapNotNull { file ->
    runCatching { OpenedFile(file.absolutePath, file.readText()) }.getOrNull()
}

// JVM Platform: file dialogs, clipboard and JSON storage. Workspace uses it through Workspaces.create.
class DesktopPlatform(private val storage: File) : Platform {
    override val kind: PlatformKind = PlatformKind.DESKTOP
    override var serverAddress: String? = null
    private var lastDirectory: String? = null

    // Shows a native open dialog and returns the chosen file text. Workspace open actions call it.
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

    // Shows a native save dialog and writes content to disk. Workspace save actions call it.
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

    // Reads the system clipboard as text. Workspace paste actions call it.
    override suspend fun readClipboard(): String? = withContext(Dispatchers.Main) {
        runCatching { Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor) as? String }.getOrNull()
    }

    // Writes text to the system clipboard. Workspace copy actions call it.
    override suspend fun writeClipboard(text: String) = withContext(Dispatchers.Main) {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
    }

    // Loads a persisted JSON value from the config folder. Session and settings call it through Platform.
    override fun loadValue(key: String): String? = keyFile(storage, key).takeIf { it.isFile }?.let { runCatching { it.readText() }.getOrNull() }

    // Atomically writes a JSON value into the config folder. Session and settings call it through Platform.
    override fun storeValue(key: String, value: String) {
        val target = keyFile(storage, key)
        val temp = File(storage, target.name + ".tmp")
        temp.writeText(value)
        Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    // Returns wall-clock millis. Story relative dates and timestamps call it through Platform.
    override fun nowMillis(): Long = System.currentTimeMillis()
}
