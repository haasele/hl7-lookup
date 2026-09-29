// Locks MLLP and HTTP send and ack. Calls desktop transport/Index.
package hl7lookup.desktop.transport

import hl7lookup.engine.Endpoint
import hl7lookup.engine.FailureCause
import hl7lookup.engine.Protocol
import hl7lookup.engine.ReceiverConfig
import java.net.ServerSocket
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Checks MLLP/HTTP send, ack, poll and diagnosis. Calls desktop transport helpers.
class TransportTest {
    private val order = "MSH|^~\\&|CPOE|NORTH|LAB|CENTRAL|20260301091500||ORM^O01|T1|P|2.4\rPID|1||4711"
    private val ack = "MSH|^~\\&|LAB|CENTRAL|CPOE|NORTH|20260301091501||ACK^O01|T2|P|2.4\rMSA|AA|T1"

    // Picks an unused local port. Receiver tests call it before hub.start.
    private fun freePort() = ServerSocket(0).use { it.localPort }

    // Asserts ACK detection from MSH-9. Calls isAcknowledgement.
    @Test
    fun recognisesAcknowledgements() {
        assertTrue(isAcknowledgement(ack))
        assertTrue(isAcknowledgement(ack.replace('^', '#')))
        assertFalse(isAcknowledgement(order))
        assertFalse(isAcknowledgement("PID|1"))
    }

    // Sends MLLP to a local receiver and expects an ACK. Calls send and withReceiver.
    @Test
    fun mllpLoopbackReturnsTheAcknowledgement() = withReceiver(Protocol.MLLP) { port ->
        val result = send(Endpoint(Protocol.MLLP, "127.0.0.1", port, timeoutMillis = 3000), order)
        assertTrue(result.ok, result.detail)
        assertTrue(isAcknowledgement(result.response.orEmpty()), result.response)
    }

    // Asserts sending an ACK returns immediately. Calls send and withReceiver.
    @Test
    fun outgoingAcknowledgementDoesNotWaitForAReply() = withReceiver(Protocol.MLLP) { port ->
        val result = send(Endpoint(Protocol.MLLP, "127.0.0.1", port, timeoutMillis = 3000), ack)
        assertTrue(result.ok, result.detail)
        assertNull(result.response)
        assertTrue(result.durationMillis < 3000, "took ${result.durationMillis} ms")
    }

    // Sends HTTP HL7 to a local receiver and expects MSA. Calls send and withReceiver.
    @Test
    fun httpLoopbackReturnsTheAcknowledgement() = withReceiver(Protocol.HTTP) { port ->
        val result = send(Endpoint(Protocol.HTTP, "127.0.0.1", port, "/hl7", timeoutMillis = 3000), order)
        assertTrue(result.ok, result.detail)
        assertTrue(result.response.orEmpty().contains("MSA|AA|T1"), result.response)
    }

    // Asserts poll filters by sequence and epochs differ. Calls TransportHub.poll and send.
    @Test
    fun pollSkipsEventsAlreadySeen() {
        val hub = TransportHub { ack }
        val port = freePort()
        assertTrue(hub.start(ReceiverConfig("r", "Loopback", Protocol.MLLP, port)).running)
        try {
            assertTrue(send(Endpoint(Protocol.MLLP, "127.0.0.1", port, timeoutMillis = 3000), order).ok)
            val seen = hub.poll(0)
            assertEquals(1, seen.events.size)
            val again = hub.poll(seen.events.single().sequence)
            assertEquals(seen.epoch, again.epoch)
            assertTrue(again.events.isEmpty())
        } finally {
            hub.stopAll()
        }
        assertTrue(TransportHub { ack }.poll(0).epoch != hub.poll(0).epoch)
    }

    // Asserts diagnose succeeds against a live MLLP port. Calls diagnose and TransportHub.
    @Test
    fun diagnosisReachesAnOpenReceiver() {
        val hub = TransportHub { ack }
        val port = freePort()
        assertTrue(hub.start(ReceiverConfig("r", "Loopback", Protocol.MLLP, port)).running)
        try {
            val report = diagnose(Endpoint(Protocol.MLLP, "127.0.0.1", port, timeoutMillis = 3000))
            assertTrue(report.steps.all { it.ok }, report.detail)
            assertNull(report.cause)
        } finally {
            hub.stopAll()
        }
    }

    // Asserts send to a closed port yields REFUSED. Calls send.
    @Test
    fun closedPortIsReportedAsRefused() {
        val result = send(Endpoint(Protocol.MLLP, "127.0.0.1", freePort(), timeoutMillis = 1000), order)
        assertFalse(result.ok)
        assertEquals(FailureCause.REFUSED, result.failure)
    }

    // Starts a hub receiver, runs a block, then asserts one receipt. Loopback tests call it.
    private fun withReceiver(protocol: Protocol, block: (Int) -> Unit) {
        val hub = TransportHub { text -> if (isAcknowledgement(text)) null else ack }
        val port = freePort()
        val status = hub.start(ReceiverConfig("r", "Loopback", protocol, port, "/hl7"))
        assertTrue(status.running, status.detail)
        try {
            block(port)
            val deadline = System.currentTimeMillis() + 2000
            while (hub.statuses().single().received == 0 && System.currentTimeMillis() < deadline) Thread.sleep(20)
            assertEquals(1, hub.statuses().single().received)
        } finally {
            hub.stopAll()
        }
    }
}
