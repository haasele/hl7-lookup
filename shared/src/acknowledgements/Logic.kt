// Which code an acknowledgement reports. acknowledgements/Index calls it.
package hl7lookup.acknowledgements

import androidx.compose.runtime.mutableStateListOf
import hl7lookup.document.Er7
import hl7lookup.engine.Engines
import hl7lookup.engine.FailureCause

// Whether the ACK was sent or received. AckEntry and Index labels use it.
enum class Direction { SENT, RECEIVED }

// Result bucket for coloring and filtering. outcomeOf and Index.color use it.
enum class AckOutcome { ACCEPTED, ERROR, REJECTED, FAILED, NONE }

// One ACK list row with peer, code and timing. AckStore holds these; Index displays them.
data class AckEntry(
    val id: String,
    val time: Long,
    val direction: Direction,
    val peer: String,
    val controlId: String,
    val code: String?,
    val text: String?,
    val ack: String?,
    val failure: FailureCause? = null,
    val detail: String? = null,
    val durationMillis: Long = 0,
)

// Derives AckOutcome from code or failure. Index.outcome and the panel call it.
internal fun outcomeOf(entry: AckEntry): AckOutcome = when {
    entry.failure != null -> AckOutcome.FAILED
    entry.code == null -> AckOutcome.NONE
    entry.code in setOf("AA", "CA") -> AckOutcome.ACCEPTED
    entry.code in setOf("AE", "CE") -> AckOutcome.ERROR
    else -> AckOutcome.REJECTED
}

// Pulls MSA-3 or ERR-8 text from an ACK. entryOf fills AckEntry.text with it.
internal fun ackText(ack: String?): String? =
    ack?.let { Er7.parse(Er7.normalize(it)) }?.let { Er7.value(it, "MSA-3").ifBlank { Er7.value(it, "ERR-8").ifBlank { null } } }

// Parses message and ACK into an AckEntry. Acknowledgements.entry delegates here.
internal fun entryOf(id: String, time: Long, direction: Direction, peer: String, message: String, ack: String?, failure: FailureCause?, detail: String?, duration: Long): AckEntry {
    val normalized = Er7.normalize(message)
    val controlId = Er7.header(Er7.parse(normalized)).controlId
    val reply = ack?.let(Er7::normalize)
    val code = when {
        reply != null -> Engines.ackCode(reply)
        failure != null -> null
        else -> Engines.ackCode(normalized)
    }
    return AckEntry(id, time, direction, peer, controlId, code, ackText(reply), reply, failure, detail, duration)
}

// In-memory ACK history capped at 2000. Workspace and Index panel call add/clear.
class AckStore {
    val entries = mutableStateListOf<AckEntry>()

    // Prepends an entry and trims old ones. Transport and engine report via this.
    fun add(entry: AckEntry) {
        entries.add(0, entry)
        if (entries.size > 2000) entries.removeRange(2000, entries.size)
    }

    // Empties the list. Clear button in the panel calls it.
    fun clear() = entries.clear()
}
