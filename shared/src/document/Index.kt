package hl7lookup.document

object Er7 {
    fun parse(text: String): ParsedMessage = parseMessage(text)
    fun locate(message: ParsedMessage, offset: Int): FieldPath? = locateOffset(message, offset)
    fun span(message: ParsedMessage, path: FieldPath): Span? = spanOfPath(message, path)
    fun raw(message: ParsedMessage, path: FieldPath): String = rawAt(message, path)
    fun value(message: ParsedMessage, path: FieldPath): String = unescapeValue(rawAt(message, path), message.delimiters)
    fun value(message: ParsedMessage, spec: String, occurrence: Int = 1): String = valueOfSpec(message, spec, occurrence)
    fun writeRaw(message: ParsedMessage, path: FieldPath, raw: String): String = hl7lookup.document.writeRaw(message, path, raw)
    fun write(message: ParsedMessage, path: FieldPath, value: String): String =
        hl7lookup.document.writeRaw(message, path, escapeValue(value, message.delimiters))
    fun escape(value: String, delimiters: Delimiters): String = escapeValue(value, delimiters)
    fun unescape(raw: String, delimiters: Delimiters): String = unescapeValue(raw, delimiters)
    fun withDelimiters(message: ParsedMessage, delimiters: Delimiters): String = reencode(message, delimiters)
    fun splitFile(content: String): List<String> = splitMessageFile(content)
    fun toWire(text: String): String = toWireFormat(text)
    fun toFile(messages: List<String>): String = toFileFormat(messages)
    fun normalize(text: String): String = normalizeEditorText(text)
    fun header(message: ParsedMessage): MessageHeader = headerOf(message)
    fun segmentIndex(message: ParsedMessage, name: String, occurrence: Int = 1): Int? = firstSegmentIndex(message, name, occurrence)
    fun occurrence(message: ParsedMessage, segmentIndex: Int): Int = occurrenceOf(message, segmentIndex)
    fun spec(text: String): PathSpec? = parsePathSpec(text)
    fun spec(message: ParsedMessage, path: FieldPath): PathSpec? = specOf(message, path)
    fun resolve(message: ParsedMessage, spec: PathSpec, occurrence: Int = 1): FieldPath? = resolvePathSpec(message, spec, occurrence)
    fun label(spec: PathSpec): String = labelOf(spec)
    fun label(message: ParsedMessage, path: FieldPath): String = labelOfPath(message, path)
    fun flatten(message: ParsedMessage): List<FlatNode> = flattenMessage(message)
    fun find(text: String, query: String): List<Span> = findOccurrences(text, query)
    fun same(a: FieldPath?, b: FieldPath?): Boolean = samePosition(a, b)
    fun contains(outer: FieldPath, inner: FieldPath): Boolean = hl7lookup.document.contains(outer, inner)
    fun texts() = DocumentTexts
}
