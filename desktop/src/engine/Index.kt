package hl7lookup.desktop.engine

import hl7lookup.dictionary.Hl7Dictionary
import hl7lookup.engine.AckRequest
import hl7lookup.engine.CreateRequest
import hl7lookup.engine.Hl7Engine
import hl7lookup.engine.InspectReport
import hl7lookup.engine.InspectRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HapiEngine : Hl7Engine {
    private val runtime = HapiRuntime()

    override suspend fun versions(): List<String> = availableVersions()

    override suspend fun dictionary(version: String): Hl7Dictionary = withContext(Dispatchers.IO) { dictionaryFor(runtime, version) }

    override suspend fun inspect(request: InspectRequest): InspectReport = withContext(Dispatchers.Default) { inspectMessage(runtime, request) }

    override suspend fun acknowledge(request: AckRequest): String = withContext(Dispatchers.Default) { acknowledge(runtime, request) }

    override suspend fun create(request: CreateRequest): String = withContext(Dispatchers.Default) { createMessage(runtime, request) }

    fun warmUp(version: String) {
        runCatching { dictionaryFor(runtime, version) }
    }
}
