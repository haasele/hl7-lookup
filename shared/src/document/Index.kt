// Public ER7 parse, write and header. The editor, grid, story, catalog and workspace call Er7.
package hl7lookup.document

// Public entry for parse, locate, write and labels. Other features call it; it forwards to Logic.
object Er7 {
    // Turns raw ER7 text into a ParsedMessage. Callers use Er7; forwards to parseMessage.
    fun parse(text: String): ParsedMessage = parseMessage(text)
    // Finds which field path owns a caret offset. Callers use Er7; forwards to locateOffset.
    fun locate(message: ParsedMessage, offset: Int): FieldPath? = locateOffset(message, offset)
    // Returns the character span for a field path. Callers use Er7; forwards to spanOfPath.
    fun span(message: ParsedMessage, path: FieldPath): Span? = spanOfPath(message, path)
    // Reads the raw escaped text at a path. Callers use Er7; forwards to rawAt.
    fun raw(message: ParsedMessage, path: FieldPath): String = rawAt(message, path)
    // Unescapes the value at a path. Callers use Er7; calls rawAt and unescapeValue.
    fun value(message: ParsedMessage, path: FieldPath): String = unescapeValue(rawAt(message, path), message.delimiters)
    // Reads an unescaped value by path spec string. Callers use Er7; forwards to valueOfSpec.
    fun value(message: ParsedMessage, spec: String, occurrence: Int = 1): String = valueOfSpec(message, spec, occurrence)
    // Writes raw escaped text at a path and returns new message text. Callers use Er7; forwards to writeRaw.
    fun writeRaw(message: ParsedMessage, path: FieldPath, raw: String): String = hl7lookup.document.writeRaw(message, path, raw)
    // Escapes a value then writes it at a path. Callers use Er7; calls escapeValue and writeRaw.
    fun write(message: ParsedMessage, path: FieldPath, value: String): String =
        hl7lookup.document.writeRaw(message, path, escapeValue(value, message.delimiters))
    // Escapes delimiter characters in a plain value. Callers use Er7; forwards to escapeValue.
    fun escape(value: String, delimiters: Delimiters): String = escapeValue(value, delimiters)
    // Turns escape sequences back into plain text. Callers use Er7; forwards to unescapeValue.
    fun unescape(raw: String, delimiters: Delimiters): String = unescapeValue(raw, delimiters)
    // Rebuilds the message with new delimiters. Callers use Er7; forwards to reencode.
    fun withDelimiters(message: ParsedMessage, delimiters: Delimiters): String = reencode(message, delimiters)
    // Splits a multi-message file into separate ER7 texts. Callers use Er7; forwards to splitMessageFile.
    fun splitFile(content: String): List<String> = splitMessageFile(content)
    // Converts editor newlines to wire CR separators. Callers use Er7; forwards to toWireFormat.
    fun toWire(text: String): String = toWireFormat(text)
    // Joins messages into CRLF file format. Callers use Er7; forwards to toFileFormat.
    fun toFile(messages: List<String>): String = toFileFormat(messages)
    // Normalizes CR/LF for the editor. Callers use Er7; forwards to normalizeEditorText.
    fun normalize(text: String): String = normalizeEditorText(text)
    // Pulls common MSH header fields. Callers use Er7; forwards to headerOf.
    fun header(message: ParsedMessage): MessageHeader = headerOf(message)
    // Finds the segment list index by name and occurrence. Callers use Er7; forwards to firstSegmentIndex.
    fun segmentIndex(message: ParsedMessage, name: String, occurrence: Int = 1): Int? = firstSegmentIndex(message, name, occurrence)
    // Counts how many times this segment name has appeared so far. Callers use Er7; forwards to occurrenceOf.
    fun occurrence(message: ParsedMessage, segmentIndex: Int): Int = occurrenceOf(message, segmentIndex)
    // Parses a path label like PID-5.1 into a PathSpec. Callers use Er7; forwards to parsePathSpec.
    fun spec(text: String): PathSpec? = parsePathSpec(text)
    // Builds a PathSpec from a FieldPath. Callers use Er7; forwards to specOf.
    fun spec(message: ParsedMessage, path: FieldPath): PathSpec? = specOf(message, path)
    // Resolves a PathSpec to a FieldPath in the message. Callers use Er7; forwards to resolvePathSpec.
    fun resolve(message: ParsedMessage, spec: PathSpec, occurrence: Int = 1): FieldPath? = resolvePathSpec(message, spec, occurrence)
    // Formats a PathSpec as a short label. Callers use Er7; forwards to labelOf.
    fun label(spec: PathSpec): String = labelOf(spec)
    // Formats a FieldPath as a short label. Callers use Er7; forwards to labelOfPath.
    fun label(message: ParsedMessage, path: FieldPath): String = labelOfPath(message, path)
    // Flattens fields and components for grids. Callers use Er7; forwards to flattenMessage.
    fun flatten(message: ParsedMessage): List<FlatNode> = flattenMessage(message)
    // Finds case-insensitive query spans in text. Callers use Er7; forwards to findOccurrences.
    fun find(text: String, query: String): List<Span> = findOccurrences(text, query)
    // True when two paths point at the same position. Callers use Er7; forwards to samePosition.
    fun same(a: FieldPath?, b: FieldPath?): Boolean = samePosition(a, b)
    // True when outer path covers or equals inner. Callers use Er7; forwards to contains.
    fun contains(outer: FieldPath, inner: FieldPath): Boolean = hl7lookup.document.contains(outer, inner)
    // Exposes document wording. Callers use Er7; returns DocumentTexts.
    fun texts() = DocumentTexts
}
