package hl7lookup.desktop.engine

import ca.uhn.hl7v2.AcknowledgmentCode
import ca.uhn.hl7v2.DefaultHapiContext
import ca.uhn.hl7v2.HL7Exception
import ca.uhn.hl7v2.HapiContext
import ca.uhn.hl7v2.Location
import ca.uhn.hl7v2.Version
import ca.uhn.hl7v2.model.AbstractGroup
import ca.uhn.hl7v2.model.AbstractSegment
import ca.uhn.hl7v2.model.Composite
import ca.uhn.hl7v2.model.Group
import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.model.Segment
import ca.uhn.hl7v2.model.Structure
import ca.uhn.hl7v2.model.Type
import ca.uhn.hl7v2.parser.ModelClassFactory
import ca.uhn.hl7v2.parser.PipeParser
import ca.uhn.hl7v2.validation.CollectingValidationExceptionHandler
import ca.uhn.hl7v2.validation.DefaultValidator
import ca.uhn.hl7v2.validation.impl.ValidationContextFactory
import hl7lookup.desktop.engine.catalog.Catalog
import hl7lookup.desktop.engine.tables.Tables
import hl7lookup.dictionary.ComponentDef
import hl7lookup.dictionary.DatatypeDef
import hl7lookup.dictionary.FieldDef
import hl7lookup.dictionary.Hl7Dictionary
import hl7lookup.dictionary.SegmentDef
import hl7lookup.dictionary.StructureDef
import hl7lookup.dictionary.StructureElement
import hl7lookup.document.Er7
import hl7lookup.document.FieldPath
import hl7lookup.engine.AckRequest
import hl7lookup.engine.CreateRequest
import hl7lookup.engine.EngineFinding
import hl7lookup.engine.Engines
import hl7lookup.engine.InspectReport
import hl7lookup.engine.InspectRequest
import hl7lookup.engine.Severity
import java.lang.reflect.Modifier
import java.net.JarURLConnection
import java.util.IdentityHashMap
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap

internal class HapiRuntime {
    val parsing: HapiContext = DefaultHapiContext().apply {
        validationContext = ValidationContextFactory.noValidation()
        parserConfiguration.isAllowUnknownVersions = true
        parserConfiguration.idGenerator = ca.uhn.hl7v2.util.idgenerator.InMemoryIDGenerator()
    }
    val validating: HapiContext = DefaultHapiContext().apply {
        parserConfiguration.idGenerator = ca.uhn.hl7v2.util.idgenerator.InMemoryIDGenerator()
    }
    val parser: PipeParser = parsing.pipeParser
    val dictionaries = ConcurrentHashMap<String, Hl7Dictionary>()
}

private val componentGetter = Regex("^get([A-Z][A-Za-z]*?)(\\d+)_(\\w+)$")

internal fun availableVersions(): List<String> = Version.availableVersions().map { it.version }

internal fun versionOf(raw: String): Version =
    Version.versionOf(raw) ?: Version.versionOf(Engines.matchVersion(raw, availableVersions()) ?: Engines.defaultVersion())
    ?: error("HL7 version $raw is not available")

internal fun classesIn(packageName: String): List<Class<*>> {
    val path = packageName.replace('.', '/')
    val loader = HapiRuntime::class.java.classLoader
    val url = loader.getResource(path) ?: return emptyList()
    val names = when (url.protocol) {
        "jar" -> (url.openConnection() as JarURLConnection).jarFile.use { jar ->
            jar.entries().asSequence().map { it.name }
                .filter { it.startsWith("$path/") && it.endsWith(".class") && !it.contains('$') && it.count { c -> c == '/' } == path.count { c -> c == '/' } + 1 }
                .map { it.removeSuffix(".class").replace('/', '.') }.toList()
        }
        "file" -> java.io.File(url.toURI()).listFiles().orEmpty()
            .filter { it.name.endsWith(".class") && !it.name.contains('$') }
            .map { "$packageName.${it.name.removeSuffix(".class")}" }
        else -> emptyList()
    }
    return names.sorted().mapNotNull { runCatching { Class.forName(it, false, loader) }.getOrNull() }
        .filter { !Modifier.isAbstract(it.modifiers) && Modifier.isPublic(it.modifiers) }
}

internal fun tableNumberOf(type: Type?): String? {
    if (type == null) return null
    val number = runCatching { type.javaClass.getMethod("getTable").invoke(type) as? Int }.getOrNull() ?: return null
    return if (number > 0) Tables.id(number) else null
}

internal fun componentNames(composite: Composite): Map<Int, String> =
    composite.javaClass.methods.asSequence()
        .filter { it.parameterCount == 0 }
        .mapNotNull { componentGetter.matchEntire(it.name) }
        .associate { it.groupValues[2].toInt() to Catalog.humanize(it.groupValues[3]) }

private val hapiDatatypeAliases = mapOf("TSComponentOne" to "DTM")

internal fun datatypeName(hapiName: String): String = hapiDatatypeAliases[hapiName] ?: hapiName

internal fun datatypeDefOf(type: Type): DatatypeDef {
    val components = if (type is Composite) {
        val names = componentNames(type)
        type.components.mapIndexed { i, component ->
            ComponentDef(i + 1, names[i + 1] ?: component.name, datatypeName(component.name), tableNumberOf(component) ?: Tables.forComponent(type.name, i + 1))
        }
    } else emptyList()
    return DatatypeDef(type.name, Catalog.datatype(type.name), components)
}

internal fun segmentDefOf(segment: AbstractSegment, datatypes: MutableMap<String, DatatypeDef>): SegmentDef {
    val fields = segment.names.mapIndexed { i, name ->
        val number = i + 1
        val type = runCatching { segment.getField(number, 0) }.getOrNull()
        if (type != null && type.name !in datatypes) datatypes[type.name] = datatypeDefOf(type)
        FieldDef(
            number = number,
            name = name,
            datatype = type?.name ?: "ST",
            required = runCatching { segment.isRequired(number) }.getOrDefault(false),
            maxRepetitions = runCatching { segment.getMaxCardinality(number) }.getOrDefault(1),
            length = runCatching { segment.getLength(number) }.getOrDefault(0),
            table = tableNumberOf(type) ?: Tables.forField(segment.name, number),
        )
    }
    return SegmentDef(segment.name, Catalog.segment(segment.name), fields)
}

internal fun structureElements(group: Group): List<StructureElement> = group.names.mapNotNull { name ->
    runCatching {
        val isGroup = group.isGroup(name)
        val children = if (isGroup) structureElements(group.get(name) as Group) else emptyList()
        val shown = if (isGroup) name else group.getClass(name).simpleName
        StructureElement(shown, group.isRequired(name), group.isRepeating(name), isGroup, children)
    }.getOrNull()
}

internal fun eventMap(version: String): Map<String, String> {
    val stream = HapiRuntime::class.java.classLoader.getResourceAsStream("ca/uhn/hl7v2/parser/eventmap/$version.properties") ?: return emptyMap()
    val properties = Properties().apply { stream.use { load(it) } }
    return properties.stringPropertyNames().associateWith { properties.getProperty(it).trim() }
}

internal fun buildDictionary(runtime: HapiRuntime, raw: String): Hl7Dictionary {
    val version = versionOf(raw)
    val base = version.modelPackageName().trimEnd('.')
    val factory: ModelClassFactory = runtime.parsing.modelClassFactory
    val parent = version.newGenericMessage(factory)
    val datatypes = linkedMapOf<String, DatatypeDef>()
    val segments = linkedMapOf<String, SegmentDef>()
    for (cls in classesIn("$base.segment")) {
        if (!AbstractSegment::class.java.isAssignableFrom(cls)) continue
        val segment = runCatching {
            cls.getConstructor(Group::class.java, ModelClassFactory::class.java).newInstance(parent, factory) as AbstractSegment
        }.getOrNull() ?: continue
        segments[segment.name] = segmentDefOf(segment, datatypes)
    }
    for (cls in classesIn("$base.datatype")) {
        if (!Type::class.java.isAssignableFrom(cls)) continue
        val type = runCatching { cls.getConstructor(Message::class.java).newInstance(parent) as Type }.getOrNull() ?: continue
        if (type.name != cls.simpleName && type.name in datatypes) continue
        datatypes.putIfAbsent(type.name, datatypeDefOf(type))
    }
    for (definition in datatypes.values.toList()) {
        for (component in definition.components) {
            if (component.datatype !in datatypes) {
                val cls = runCatching { Class.forName("$base.datatype.${component.datatype}") }.getOrNull() ?: continue
                val type = runCatching { cls.getConstructor(Message::class.java).newInstance(parent) as Type }.getOrNull() ?: continue
                datatypes[type.name] = datatypeDefOf(type)
            }
        }
    }
    val structures = linkedMapOf<String, StructureDef>()
    for (cls in classesIn("$base.message")) {
        if (!AbstractGroup::class.java.isAssignableFrom(cls)) continue
        val message = runCatching { cls.getConstructor(ModelClassFactory::class.java).newInstance(factory) as AbstractGroup }.getOrNull() ?: continue
        structures[cls.simpleName] = StructureDef(cls.simpleName, structureElements(message))
    }
    val referenced = buildSet {
        segments.values.forEach { s -> s.fields.forEach { f -> f.table?.let(::add) } }
        datatypes.values.forEach { d -> d.components.forEach { c -> c.table?.let(::add) } }
    }
    val tables = Tables.all(referenced)
    return Hl7Dictionary(
        version = version.version,
        segments = segments,
        datatypes = datatypes,
        tables = tables,
        messageTypes = tables["0076"]?.entries?.associate { it.code to it.description }.orEmpty(),
        events = tables["0003"]?.entries?.associate { it.code to it.description }.orEmpty(),
        eventStructures = eventMap(version.version),
        structures = structures,
    )
}

internal fun dictionaryFor(runtime: HapiRuntime, raw: String): Hl7Dictionary {
    val key = versionOf(raw).version
    return runtime.dictionaries.getOrPut(key) { buildDictionary(runtime, key) }
}

internal fun prepareWire(text: String, version: String?): String {
    val parsed = Er7.parse(Er7.normalize(text))
    if (parsed.isEmpty) return ""
    val declared = Er7.header(parsed).version
    val supported = availableVersions()
    val needsVersion = version != null && (declared.isBlank() || Engines.matchVersion(declared, supported) == null)
    val adjusted = if (needsVersion && parsed.segments.first().name == "MSH") {
        Er7.write(parsed, FieldPath(0, 12, 1, 1, 0), version)
    } else if (declared.isNotBlank() && declared !in supported) {
        val match = Engines.matchVersion(declared, supported)
        if (match != null && parsed.segments.first().name == "MSH") Er7.write(parsed, FieldPath(0, 12, 1, 1, 0), match) else parsed.text
    } else parsed.text
    return Er7.toWire(adjusted)
}

internal fun severityOf(severity: ca.uhn.hl7v2.Severity?): Severity = when (severity) {
    ca.uhn.hl7v2.Severity.WARNING -> Severity.WARNING
    ca.uhn.hl7v2.Severity.INFO -> Severity.INFO
    else -> Severity.ERROR
}

internal fun orderedSegments(group: Group, into: MutableList<Segment> = mutableListOf()): MutableList<Segment> {
    for (name in group.names) {
        val reps = runCatching { group.getAll(name) }.getOrNull() ?: continue
        for (structure in reps) {
            when (structure) {
                is Segment -> if (!structure.isEmpty) into += structure
                is Group -> orderedSegments(structure, into)
            }
        }
    }
    return into
}

internal fun occurrenceIndex(message: Message): IdentityHashMap<Structure, Int> {
    val counts = mutableMapOf<String, Int>()
    val result = IdentityHashMap<Structure, Int>()
    for (segment in orderedSegments(message)) {
        val occurrence = (counts[segment.name] ?: 0) + 1
        counts[segment.name] = occurrence
        result[segment] = occurrence
    }
    return result
}

internal fun segmentAt(message: Message, location: Location): Segment? = runCatching {
    var group: Group = message
    for (step in location.groups) {
        if (step.groupName == message.name) continue
        val reps = group.getAll(step.groupName)
        group = (reps.getOrNull(step.repetition - 1) ?: reps.getOrNull(step.repetition) ?: reps.firstOrNull()) as Group
    }
    val candidates = group.getAll(location.segmentName).filterIsInstance<Segment>()
    candidates.getOrNull(location.segmentRepetition - 1) ?: candidates.getOrNull(location.segmentRepetition) ?: candidates.firstOrNull()
}.getOrNull()

internal fun findingAt(message: Message?, index: IdentityHashMap<Structure, Int>?, location: Location?, severity: Severity, text: String): EngineFinding {
    if (location == null || location.isUnknown || location.segmentName.isNullOrBlank()) return EngineFinding(severity, text)
    val occurrence = if (message != null && index != null) segmentAt(message, location)?.let { index[it] } else null
    return EngineFinding(
        severity = severity,
        message = text,
        segment = location.segmentName,
        segmentRepetition = occurrence ?: location.segmentRepetition.coerceAtLeast(1),
        field = location.field.coerceAtLeast(0),
        fieldRepetition = location.fieldRepetition.coerceAtLeast(1),
        component = location.component.coerceAtLeast(0),
        subcomponent = location.subcomponent.coerceAtLeast(0),
    )
}

internal fun structureFindings(group: Group, path: String, counts: MutableMap<String, Int>, into: MutableList<EngineFinding>) {
    val nonStandard = (group as? AbstractGroup)?.nonStandardNames.orEmpty()
    val choices = group.names.filter { runCatching { (group as? AbstractGroup)?.isChoiceElement(it) == true }.getOrDefault(false) }
    if (choices.isNotEmpty() && choices.none { name -> runCatching { group.getAll(name).any { !it.isEmpty } }.getOrDefault(false) }) {
        into += EngineFinding(Severity.ERROR, "One of ${choices.joinToString(", ")} is required in $path")
    }
    for (name in group.names) {
        val isGroup = runCatching { group.isGroup(name) }.getOrDefault(false)
        val reps = runCatching { group.getAll(name) }.getOrNull().orEmpty()
        val present = reps.filter { !it.isEmpty }
        val shown = if (isGroup) name else runCatching { group.getClass(name).simpleName }.getOrDefault(name)
        if (present.isEmpty() && name !in choices && runCatching { group.isRequired(name) }.getOrDefault(false)) {
            into += EngineFinding(Severity.ERROR, "Required ${if (isGroup) "group" else "segment"} $shown is missing in $path")
        }
        if (!isGroup && present.size > 1 && !runCatching { group.isRepeating(name) }.getOrDefault(true)) {
            into += EngineFinding(Severity.WARNING, "Segment $shown must not repeat in $path", shown, (counts[shown] ?: 0) + 2)
        }
        for (structure in present) {
            when (structure) {
                is Segment -> {
                    val occurrence = (counts[structure.name] ?: 0) + 1
                    counts[structure.name] = occurrence
                    if (name in nonStandard) {
                        val known = structure.name.startsWith("Z")
                        into += EngineFinding(
                            if (known) Severity.INFO else Severity.WARNING,
                            if (known) "Custom segment ${structure.name} is not part of $path" else "Segment ${structure.name} is not expected at this position in $path",
                            structure.name, occurrence,
                        )
                    }
                }
                is Group -> structureFindings(structure, "$path/$name", counts, into)
            }
        }
    }
}

internal fun validationFindings(runtime: HapiRuntime, message: Message, index: IdentityHashMap<Structure, Int>): List<EngineFinding> {
    val handler = object : CollectingValidationExceptionHandler<Unit>(runtime.validating) {
        override fun result() = Unit
    }
    runCatching { DefaultValidator<Unit>(runtime.validating).validate(message, handler) }
    return handler.exceptions.map { findingAt(message, index, it.location, severityOf(it.severity), it.messageWithoutLocation ?: it.message ?: "") }
}

internal fun inspectMessage(runtime: HapiRuntime, request: InspectRequest): InspectReport {
    val wire = prepareWire(request.text, request.version)
    if (wire.isBlank()) return InspectReport()
    val message = try {
        runtime.parser.parse(wire)
    } catch (error: HL7Exception) {
        return InspectReport(null, listOf(findingAt(null, null, error.location, Severity.ERROR, error.messageWithoutLocation ?: error.message ?: "Parse error")))
    } catch (error: Exception) {
        return InspectReport(null, listOf(EngineFinding(Severity.ERROR, error.message ?: "Parse error")))
    }
    val index = occurrenceIndex(message)
    val findings = mutableListOf<EngineFinding>()
    structureFindings(message, message.name, mutableMapOf(), findings)
    findings += validationFindings(runtime, message, index)
    return InspectReport(message.name, findings)
}

internal fun acknowledgementCode(code: String): AcknowledgmentCode =
    runCatching { AcknowledgmentCode.valueOf(code.uppercase()) }.getOrDefault(AcknowledgmentCode.AA)

internal fun acknowledge(runtime: HapiRuntime, request: AckRequest): String {
    val wire = prepareWire(request.text, null)
    val ack = runCatching {
        val message = runtime.parser.parse(wire)
        val code = acknowledgementCode(request.code)
        val error = request.errorText?.takeIf { it.isNotBlank() }?.let { HL7Exception(it) }
        runtime.parser.encode(message.generateACK(code, error))
    }.getOrElse { fallbackAck(wire, request.code, request.errorText ?: it.message) }
    return Er7.normalize(ack)
}

internal fun fallbackAck(wire: String, code: String, errorText: String?): String {
    val parsed = Er7.parse(Er7.normalize(wire))
    val header = Er7.header(parsed)
    val d = parsed.delimiters
    val f = d.field
    val stamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
    val control = "ACK" + System.currentTimeMillis()
    val version = header.version.ifBlank { Engines.defaultVersion() }
    val msh = listOf(
        "MSH", d.encodingCharacters, header.receivingApplication, header.receivingFacility, header.sendingApplication, header.sendingFacility,
        stamp, "", "ACK${if (header.event.isNotBlank()) "${d.component}${header.event}" else ""}", control, header.processingId.ifBlank { "P" }, version,
    ).let { parts -> parts[0] + f + parts.drop(1).joinToString(f.toString()) }
    val text = errorText?.let { Er7.escape(it.take(80), d) } ?: ""
    val msa = listOf("MSA", code.ifBlank { "AR" }, header.controlId, text).joinToString(f.toString()).trimEnd(f)
    return "$msh\r$msa\r"
}

internal fun createMessage(runtime: HapiRuntime, request: CreateRequest): String {
    val message = runtime.parsing.newMessage(request.messageType, request.event, versionOf(request.version))
    runCatching {
        val terser = ca.uhn.hl7v2.util.Terser(message)
        terser.set("/MSH-11-1", request.processingId)
        terser.set("/MSH-3-1", "HL7Lookup")
    }
    return Er7.normalize(runtime.parser.encode(message))
}
