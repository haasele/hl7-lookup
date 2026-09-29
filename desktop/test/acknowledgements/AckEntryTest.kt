// Locks which acknowledgement code wins. Calls acknowledgements/Index.
package hl7lookup.acknowledgements

import hl7lookup.engine.FailureCause
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// Checks how acknowledgement codes and outcomes are chosen. Calls Acknowledgements via Index.
class AckEntryTest {
    private val order = "MSH|^~\\&|CPOE|NORTH|LAB|CENTRAL|20260301091500||ORM^O01|T1|P|2.4\rPID|1||4711"
    private val ack = "MSH|^~\\&|LAB|CENTRAL|CPOE|NORTH|20260301091501||ACK^O01|T2|P|2.4\rMSA|AA|T1"

    // Asserts a reply ACK code beats the outbound message. Calls Acknowledgements.entry.
    @Test
    fun replyCodeWinsOverTheMessageItself() {
        val entry = Acknowledgements.entry("a", 0, Direction.SENT, "peer", order, ack, null, null, 12)
        assertEquals("AA", entry.code)
        assertEquals("T1", entry.controlId)
    }

    // Asserts a received ACK keeps its MSA code. Calls Acknowledgements.entry.
    @Test
    fun acknowledgementWithoutAReplyKeepsItsOwnCode() {
        val entry = Acknowledgements.entry("a", 0, Direction.RECEIVED, "peer", ack, null, null, null, 0)
        assertEquals("AA", entry.code)
        assertEquals("T2", entry.controlId)
    }

    // Asserts a failed send hides the message code. Calls Acknowledgements.entry and outcome.
    @Test
    fun failureHidesTheMessagesOwnCode() {
        val entry = Acknowledgements.entry("a", 0, Direction.SENT, "peer", ack, null, FailureCause.TIMEOUT, null, 1000)
        assertNull(entry.code)
        assertEquals(AckOutcome.FAILED, Acknowledgements.outcome(entry))
    }
}
