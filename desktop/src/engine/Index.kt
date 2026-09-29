// HapiEngine, the JVM Hl7Engine. The desktop window and the local server call it.
package hl7lookup.desktop.engine

import hl7lookup.dictionary.Hl7Dictionary
import hl7lookup.engine.AckRequest
import hl7lookup.engine.CreateRequest
import hl7lookup.engine.Hl7Engine
import hl7lookup.engine.InspectReport
import hl7lookup.engine.InspectRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// JVM Hl7Engine backed by HAPI. The window and Servers start use it; Workspace talks through Hl7Engine.
class HapiEngine : Hl7Engine {
    private val runtime = HapiRuntime()

    // Lists HAPI-supported HL7 versions. Workspace and the HTTP /api/versions route call it.
    override suspend fun versions(): List<String> = availableVersions()

    // Builds or returns a cached dictionary for a version. Workspace and /api/dictionary call it.
    override suspend fun dictionary(version: String): Hl7Dictionary = withContext(Dispatchers.IO) { dictionaryFor(runtime, version) }

    // Parses and validates a message into an InspectReport. Workspace inspect and /api/inspect call it.
    override suspend fun inspect(request: InspectRequest): InspectReport = withContext(Dispatchers.Default) { inspectMessage(runtime, request) }

    // Builds an ACK for a message. Receivers, ack dialogs and /api/ack call it.
    override suspend fun acknowledge(request: AckRequest): String = withContext(Dispatchers.Default) { acknowledge(runtime, request) }

    // Creates a skeleton message for a type/event. New-message dialogs and /api/create call it.
    override suspend fun create(request: CreateRequest): String = withContext(Dispatchers.Default) { createMessage(runtime, request) }

    // Preloads dictionary definitions on a background thread. main starts it after constructing the engine.
    fun warmUp(version: String) {
        runCatching { dictionaryFor(runtime, version) }
    }
}
