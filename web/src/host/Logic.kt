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

private fun postJson(url: String, body: String): Promise<JsString> = js(
    """fetch(url, { method: 'POST', headers: { 'Content-Type': 'application/json', 'X-HL7-Lookup': '1' }, body: body })
        .then(r => r.text().then(t => {
            if (!r.ok) { let m = t; try { m = JSON.parse(t).message || t } catch (e) {} throw new Error(m || ('HTTP ' + r.status)) }
            return t
        }))""",
)

private fun storageGet(key: String): String? = js("(() => { try { return window.localStorage.getItem(key) } catch (e) { return null } })()")

private fun storageSet(key: String, value: String): Boolean = js("(() => { try { window.localStorage.setItem(key, value); return true } catch (e) { return false } })()")

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

private fun pickedName(file: JsAny): String = js("file.name")

private fun pickedText(file: JsAny): String = js("file.text")

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

private fun clipboardRead(): Promise<JsString?> = js("(navigator.clipboard && navigator.clipboard.readText) ? navigator.clipboard.readText().catch(() => null) : Promise.resolve(null)")

private fun clipboardWrite(text: String): Promise<JsAny?> = js("(navigator.clipboard && navigator.clipboard.writeText) ? navigator.clipboard.writeText(text).catch(() => null) : Promise.resolve(null)")

private fun origin(): String = js("window.location.origin")

private fun now(): Double = js("Date.now()")

private fun fetchBytes(url: String): Promise<JsAny?> = js("fetch(url).then(r => r.ok ? r.arrayBuffer().then(b => new Uint8Array(b)) : null).catch(() => null)")

private fun lengthOf(array: JsAny): Int = js("array.length")

private fun byteAt(array: JsAny, index: Int): Byte = js("array[index]")

internal fun accept(extensions: List<String>): String = extensions.joinToString(",") { ".$it" }

internal suspend fun loadLogo(url: String): ImageBitmap? {
    val array = runCatching { fetchBytes(url).await<JsAny?>() }.getOrNull() ?: return null
    val size = lengthOf(array)
    val bytes = ByteArray(size) { byteAt(array, it) }
    return runCatching { Image.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull()
}

internal suspend fun loadMonoFont(url: String): FontFamily? {
    val array = runCatching { fetchBytes(url).await<JsAny?>() }.getOrNull() ?: return null
    val size = lengthOf(array)
    val bytes = ByteArray(size) { byteAt(array, it) }
    return runCatching { FontFamily(Font("DroidSansMono", bytes)) }.getOrNull()
}

class BrowserPlatform : Platform {
    override val kind: PlatformKind = PlatformKind.BROWSER
    override val serverAddress: String? = origin()

    override suspend fun openTextFile(title: String, extensions: List<String>): OpenedFile? {
        val picked = pickFile(accept(extensions)).await<JsAny?>() ?: return null
        return OpenedFile(pickedName(picked), pickedText(picked))
    }

    override suspend fun saveTextFile(title: String, suggestedName: String, content: String): String? {
        val name = suggestedName.substringAfterLast('/').substringAfterLast('\\')
        download(name, content)
        return name
    }

    override suspend fun readClipboard(): String? = clipboardRead().await<JsString?>()?.toString()

    override suspend fun writeClipboard(text: String) {
        clipboardWrite(text).await<JsAny?>()
    }

    override fun loadValue(key: String): String? = storageGet(key)

    override fun storeValue(key: String, value: String) {
        storageSet(key, value)
    }

    override fun nowMillis(): Long = now().toLong()
}

internal class Remote(private val base: String) {
    suspend fun <R> call(route: ApiRoute, body: String, reply: KSerializer<R>): R {
        val text = postJson(base.trimEnd('/') + route.path, body).await<JsString>().toString()
        return Engines.json().decodeFromString(reply, text)
    }

    suspend fun <Q, R> call(route: ApiRoute, query: Q, request: KSerializer<Q>, reply: KSerializer<R>): R =
        call(route, Engines.json().encodeToString(request, query), reply)
}

class RemoteEngine(base: String) : Hl7Engine {
    private val remote = Remote(base)

    override suspend fun versions(): List<String> = remote.call(ApiRoute.VERSIONS, "{}", ListSerializer(String.serializer()))

    override suspend fun dictionary(version: String): Hl7Dictionary =
        remote.call(ApiRoute.DICTIONARY, VersionQuery(version), VersionQuery.serializer(), Hl7Dictionary.serializer())

    override suspend fun inspect(request: InspectRequest): InspectReport =
        remote.call(ApiRoute.INSPECT, request, InspectRequest.serializer(), InspectReport.serializer())

    override suspend fun acknowledge(request: AckRequest): String =
        remote.call(ApiRoute.ACK, request, AckRequest.serializer(), TextReply.serializer()).text

    override suspend fun create(request: CreateRequest): String =
        remote.call(ApiRoute.CREATE, request, CreateRequest.serializer(), TextReply.serializer()).text
}

class RemoteTransport(base: String) : Hl7Transport {
    private val remote = Remote(base)

    override suspend fun send(request: SendRequest): SendResult =
        remote.call(ApiRoute.SEND, request, SendRequest.serializer(), SendResult.serializer())

    override suspend fun diagnose(endpoint: Endpoint): Diagnosis =
        remote.call(ApiRoute.DIAGNOSE, endpoint, Endpoint.serializer(), Diagnosis.serializer())

    override suspend fun startReceiver(config: ReceiverConfig): ReceiverStatus =
        remote.call(ApiRoute.START_RECEIVER, config, ReceiverConfig.serializer(), ReceiverStatus.serializer())

    override suspend fun stopReceiver(id: String): ReceiverStatus =
        remote.call(ApiRoute.STOP_RECEIVER, IdQuery(id), IdQuery.serializer(), ReceiverStatus.serializer())

    override suspend fun receivers(): List<ReceiverStatus> =
        remote.call(ApiRoute.RECEIVERS, "{}", ListSerializer(ReceiverStatus.serializer()))

    override suspend fun poll(after: Long): PollBatch =
        remote.call(ApiRoute.POLL, PollQuery(after), PollQuery.serializer(), PollBatch.serializer())
}

internal fun dismissLoadingScreen() {
    js("window.__hl7Ready && window.__hl7Ready()")
}
