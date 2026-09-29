// RemoteEngine and RemoteTransport over /api, plus file and clipboard shims. host/Index wires them to Workspace.
package hl7lookup.web.host

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import org.jetbrains.skia.Image
import hl7lookup.dictionary.Hl7Dictionary
import hl7lookup.engine.AckRequest
import hl7lookup.engine.ApiRoute
import hl7lookup.engine.CreateRequest
import hl7lookup.engine.Diagnosis
import hl7lookup.engine.Endpoint
import hl7lookup.engine.Engines
import hl7lookup.engine.Hl7Engine
import hl7lookup.engine.Hl7Transport
import hl7lookup.engine.IdQuery
import hl7lookup.engine.InspectReport
import hl7lookup.engine.InspectRequest
import hl7lookup.engine.PollBatch
import hl7lookup.engine.PollQuery
import hl7lookup.engine.ReceiverConfig
import hl7lookup.engine.ReceiverStatus
import hl7lookup.engine.SendRequest
import hl7lookup.engine.SendResult
import hl7lookup.engine.TextReply
import hl7lookup.engine.VersionQuery
import hl7lookup.platform.OpenedFile
import hl7lookup.platform.Platform
import hl7lookup.platform.PlatformKind
import kotlinx.coroutines.await
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlin.js.Promise

internal const val APP_VERSION = "2.0.0"

// POSTs JSON to a desktop /api route. Remote.call awaits it.
private fun postJson(url: String, body: String): Promise<JsString> = js(
    """fetch(url, { method: 'POST', headers: { 'Content-Type': 'application/json', 'X-HL7-Lookup': '1' }, body: body })
        .then(r => r.text().then(t => {
            if (!r.ok) { let m = t; try { m = JSON.parse(t).message || t } catch (e) {} throw new Error(m || ('HTTP ' + r.status)) }
            return t
        }))""",
)

// Reads localStorage for a key. BrowserPlatform.loadValue calls it.
private fun storageGet(key: String): String? = js("(() => { try { return window.localStorage.getItem(key) } catch (e) { return null } })()")

// Writes localStorage for a key. BrowserPlatform.storeValue calls it.
private fun storageSet(key: String, value: String): Boolean = js("(() => { try { window.localStorage.setItem(key, value); return true } catch (e) { return false } })()")

// Opens a hidden file input and returns the chosen file. BrowserPlatform.openTextFile awaits it.
private fun pickFile(accept: String): Promise<JsAny?> = js(
    """new Promise((resolve) => {
        const input = document.createElement('input');
        input.type = 'file';
        input.accept = accept;
        input.style.display = 'none';
        document.body.appendChild(input);
        const done = (value) => { input.remove(); resolve(value) };
        input.onchange = () => {
            const f = input.files && input.files[0];
            if (!f) { done(null); return }
            f.text().then(t => done({ name: f.name, text: t }), () => done(null));
        };
        input.oncancel = () => done(null);
        input.click();
    })""",
)

// Reads the name from a JS file pick result. BrowserPlatform.openTextFile calls it.
private fun pickedName(file: JsAny): String = js("file.name")

// Reads the text from a JS file pick result. BrowserPlatform.openTextFile calls it.
private fun pickedText(file: JsAny): String = js("file.text")

// Triggers a browser download of text content. BrowserPlatform.saveTextFile calls it.
private fun download(name: String, content: String): Unit = js(
    """(() => {
        const blob = new Blob([content], { type: 'text/plain;charset=utf-8' });
        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = name;
        document.body.appendChild(a);
        a.click();
        setTimeout(() => { URL.revokeObjectURL(a.href); a.remove() }, 1000);
    })()""",
)

// Reads the browser clipboard. BrowserPlatform.readClipboard awaits it.
private fun clipboardRead(): Promise<JsString?> = js("(navigator.clipboard && navigator.clipboard.readText) ? navigator.clipboard.readText().catch(() => null) : Promise.resolve(null)")

// Writes the browser clipboard. BrowserPlatform.writeClipboard awaits it.
private fun clipboardWrite(text: String): Promise<JsAny?> = js("(navigator.clipboard && navigator.clipboard.writeText) ? navigator.clipboard.writeText(text).catch(() => null) : Promise.resolve(null)")

// Returns window.location.origin. BrowserPlatform uses it as the server base URL.
private fun origin(): String = js("window.location.origin")

// Returns Date.now as a double. BrowserPlatform.nowMillis converts it.
private fun now(): Double = js("Date.now()")

// Fetches a URL as a Uint8Array. loadLogo and loadMonoFont await it.
private fun fetchBytes(url: String): Promise<JsAny?> = js("fetch(url).then(r => r.ok ? r.arrayBuffer().then(b => new Uint8Array(b)) : null).catch(() => null)")

// Reads JS array length. loadLogo and loadMonoFont size the ByteArray with it.
private fun lengthOf(array: JsAny): Int = js("array.length")

// Reads one byte from a JS array. loadLogo and loadMonoFont copy bytes with it.
private fun byteAt(array: JsAny, index: Int): Byte = js("array[index]")

// Builds an HTML accept string from extensions. BrowserPlatform.openTextFile calls it.
internal fun accept(extensions: List<String>): String = extensions.joinToString(",") { ".$it" }

// Downloads and decodes the logo PNG. host main loads it for Workspace.
internal suspend fun loadLogo(url: String): ImageBitmap? {
    val array = runCatching { fetchBytes(url).await<JsAny?>() }.getOrNull() ?: return null
    val size = lengthOf(array)
    val bytes = ByteArray(size) { byteAt(array, it) }
    return runCatching { Image.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull()
}

// Downloads and builds a monospace FontFamily. host main loads it for Workspace.
internal suspend fun loadMonoFont(url: String): FontFamily? {
    val array = runCatching { fetchBytes(url).await<JsAny?>() }.getOrNull() ?: return null
    val size = lengthOf(array)
    val bytes = ByteArray(size) { byteAt(array, it) }
    return runCatching { FontFamily(Font("DroidSansMono", bytes)) }.getOrNull()
}

// Browser Platform for files, clipboard and storage. host main passes it to Workspaces.create.
class BrowserPlatform : Platform {
    override val kind: PlatformKind = PlatformKind.BROWSER
    override val serverAddress: String? = origin()

    // Picks a local file via an input element. Workspace open actions call it.
    override suspend fun openTextFile(title: String, extensions: List<String>): OpenedFile? {
        val picked = pickFile(accept(extensions)).await<JsAny?>() ?: return null
        return OpenedFile(pickedName(picked), pickedText(picked))
    }

    // Downloads content as a file in the browser. Workspace save/download actions call it.
    override suspend fun saveTextFile(title: String, suggestedName: String, content: String): String? {
        val name = suggestedName.substringAfterLast('/').substringAfterLast('\\')
        download(name, content)
        return name
    }

    // Reads clipboard text in the browser. Workspace paste actions call it.
    override suspend fun readClipboard(): String? = clipboardRead().await<JsString?>()?.toString()

    // Writes clipboard text in the browser. Workspace copy actions call it.
    override suspend fun writeClipboard(text: String) {
        clipboardWrite(text).await<JsAny?>()
    }

    // Loads a persisted value from localStorage. Session and settings call it through Platform.
    override fun loadValue(key: String): String? = storageGet(key)

    // Stores a value in localStorage. Session and settings call it through Platform.
    override fun storeValue(key: String, value: String) {
        storageSet(key, value)
    }

    // Returns wall-clock millis from Date.now. Story timestamps call it through Platform.
    override fun nowMillis(): Long = now().toLong()
}

// Thin JSON client for desktop /api routes. RemoteEngine and RemoteTransport own one.
internal class Remote(private val base: String) {
    // Posts a query or body to an ApiRoute and decodes the reply. Engine and transport methods call it.
    suspend fun <R> call(route: ApiRoute, body: String, reply: KSerializer<R>): R {
        val text = postJson(base.trimEnd('/') + route.path, body).await<JsString>().toString()
        return Engines.json().decodeFromString(reply, text)
    }

    // Posts a query or body to an ApiRoute and decodes the reply. Engine and transport methods call it.
    suspend fun <Q, R> call(route: ApiRoute, query: Q, request: KSerializer<Q>, reply: KSerializer<R>): R =
        call(route, Engines.json().encodeToString(request, query), reply)
}

// Hl7Engine over HTTP to the desktop server. host main passes it to Workspace.
class RemoteEngine(base: String) : Hl7Engine {
    private val remote = Remote(base)

    // Fetches supported versions from /api/versions. Workspace dictionary loading calls it.
    override suspend fun versions(): List<String> = remote.call(ApiRoute.VERSIONS, "{}", ListSerializer(String.serializer()))

    // Fetches a version dictionary from /api/dictionary. Workspace loads definitions through it.
    override suspend fun dictionary(version: String): Hl7Dictionary =
        remote.call(ApiRoute.DICTIONARY, VersionQuery(version), VersionQuery.serializer(), Hl7Dictionary.serializer())

    // Posts an inspect request to /api/inspect. Workspace validation calls it.
    override suspend fun inspect(request: InspectRequest): InspectReport =
        remote.call(ApiRoute.INSPECT, request, InspectRequest.serializer(), InspectReport.serializer())

    // Posts an ack request to /api/ack. Workspace ack actions call it.
    override suspend fun acknowledge(request: AckRequest): String =
        remote.call(ApiRoute.ACK, request, AckRequest.serializer(), TextReply.serializer()).text

    // Posts a create request to /api/create. Workspace new-message actions call it.
    override suspend fun create(request: CreateRequest): String =
        remote.call(ApiRoute.CREATE, request, CreateRequest.serializer(), TextReply.serializer()).text
}

// Hl7Transport over HTTP to the desktop server. host main passes it to Workspace.
class RemoteTransport(base: String) : Hl7Transport {
    private val remote = Remote(base)

    // Posts a send request to /api/send. Workspace senders call it.
    override suspend fun send(request: SendRequest): SendResult =
        remote.call(ApiRoute.SEND, request, SendRequest.serializer(), SendResult.serializer())

    // Posts an endpoint to /api/diagnose. Workspace diagnosis dialogs call it.
    override suspend fun diagnose(endpoint: Endpoint): Diagnosis =
        remote.call(ApiRoute.DIAGNOSE, endpoint, Endpoint.serializer(), Diagnosis.serializer())

    // Posts a receiver config to /api/startReceiver. Workspace receivers call it.
    override suspend fun startReceiver(config: ReceiverConfig): ReceiverStatus =
        remote.call(ApiRoute.START_RECEIVER, config, ReceiverConfig.serializer(), ReceiverStatus.serializer())

    // Posts a receiver id to /api/stopReceiver. Workspace receivers call it.
    override suspend fun stopReceiver(id: String): ReceiverStatus =
        remote.call(ApiRoute.STOP_RECEIVER, IdQuery(id), IdQuery.serializer(), ReceiverStatus.serializer())

    // Fetches receiver statuses from /api/receivers. Workspace polling calls it.
    override suspend fun receivers(): List<ReceiverStatus> =
        remote.call(ApiRoute.RECEIVERS, "{}", ListSerializer(ReceiverStatus.serializer()))

    // Fetches new transport events from /api/poll. Workspace poll loop calls it.
    override suspend fun poll(after: Long): PollBatch =
        remote.call(ApiRoute.POLL, PollQuery(after), PollQuery.serializer(), PollBatch.serializer())
}

// Signals the HTML shell that Compose is ready. host main calls it after the font loads.
internal fun dismissLoadingScreen() {
    js("window.__hl7Ready && window.__hl7Ready()")
}
