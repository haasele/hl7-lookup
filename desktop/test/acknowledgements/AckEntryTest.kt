package hl7lookup.acknowledgements

import hl7lookup.engine.FailureCause
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AckEntryTest {
    private val order = "MSH|^~\\&|CPOE|NORTH|LAB|CENTRAL|20260301091500||ORM^O01|T1|P|2.4\rPID|1||4711"
    private val ack = "MSH|^~\\&|LAB|CENTRAL|CPOE|NORTH|20260301091501||ACK^O01|T2|P|2.4\rMSA|AA|T1"

    @Test
    fun replyCodeWinsOverTheMessageItself() {
        val entry = Acknowledgements.entry("a", 0, Direction.SENT, "peer", order, ack, null, null, 12)
        assertEquals("AA", entry.code)
        assertEquals("T1", entry.controlId)
    }

    @Test
    fun acknowledgementWithoutAReplyKeepsItsOwnCode() {
        val entry = Acknowledgements.entry("a", 0, Direction.RECEIVED, "peer", ack, null, null, null, 0)
        assertEquals("AA", entry.code)
        assertEquals("T2", entry.controlId)
    }

    @Test
    fun failureHidesTheMessagesOwnCode() {
        val entry = Acknowledgements.entry("a", 0, Direction.SENT, "peer", ack, null, FailureCause.TIMEOUT, null, 1000)
        assertNull(entry.code)
        assertEquals(AckOutcome.FAILED, Acknowledgements.outcome(entry))
    }
}
