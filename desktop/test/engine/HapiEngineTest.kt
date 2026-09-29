package hl7lookup.desktop.engine

import hl7lookup.dictionary.Dictionaries
import hl7lookup.dictionary.Hl7Dictionary
import hl7lookup.engine.AckRequest
import hl7lookup.engine.CreateRequest
import hl7lookup.engine.Engines
import hl7lookup.engine.InspectRequest
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HapiEngineTest {
    private val engine = HapiEngine()

    private val admit = listOf(
        "MSH|^~\\&|WARD|NORTH|HIS|CENTRAL|20260301083000||ADT^A01^ADT_A01|MSG0001|P|2.5",
        "EVN|A01|20260301083000",
        "PID|1||4711^^^NORTH^MR||Lindqvist^Mara^J||19790412|F|||Birkenweg 3^^Kiel^^24103^DE||^PRN^PH^^^^^0431 555 01",
        "PV1|1|I|STA3^12^2||||0815^Okafor^Daniel",
    ).joinToString("\n")

    @Test
    fun versionsIncludeCommonReleases() = runBlocking {
        val versions = engine.versions()
        assertTrue("2.5" in versions && "2.3" in versions && "2.8.1" in versions, versions.toString())
    }

    @Test
    fun dictionaryDescribesFieldsComponentsAndTables() = runBlocking {
        val dictionary = engine.dictionary("2.5")
        val pid = assertNotNull(dictionary.segments["PID"])
        assertEquals("Patient Name", pid.fields[4].name)
        assertEquals("XPN", pid.fields[4].datatype)
        assertEquals("0001", pid.fields[7].table)
        val xpn = assertNotNull(dictionary.datatypes["XPN"])
        assertEquals("Family Name", xpn.components[0].name)
        val cx = assertNotNull(dictionary.datatypes["CX"])
        assertEquals("0203", cx.components[4].table)
        assertEquals("Female", Dictionaries.code(dictionary, "0001", "F"))
        assertEquals("ADT_A01", dictionary.eventStructures["ADT_A04"])
        assertTrue(dictionary.structures.containsKey("ORU_R01"))
        val size = Json.encodeToString(Hl7Dictionary.serializer(), dictionary).length
        println("dictionary 2.5 json size: $size, segments ${dictionary.segments.size}, datatypes ${dictionary.datatypes.size}, structures ${dictionary.structures.size}")
    }

    @Test
    fun inspectReportsStructureAndPositions() = runBlocking {
        val report = engine.inspect(InspectRequest(admit))
        assertEquals("ADT_A01", report.structure)
        val broken = engine.inspect(InspectRequest(admit.replace("\nPV1|1|I|STA3^12^2||||0815^Okafor^Daniel", "\nZZZ|x")))
        println(broken.findings)
        assertTrue(broken.findings.any { it.message.contains("PV1") })
    }

    @Test
    fun acknowledgesWithCode() = runBlocking {
        val ack = engine.acknowledge(AckRequest(admit, "AE", "Bed not free"))
        assertEquals("AE", Engines.ackCode(ack))
        assertTrue(ack.contains("MSG0001"))
    }

    @Test
    fun createsSkeletonMessages() = runBlocking {
        val text = engine.create(CreateRequest("ORU", "R01", "2.5.1"))
        assertTrue(text.startsWith("MSH|^~\\&|"), text)
        assertTrue(text.contains("ORU^R01"), text)
    }
}
