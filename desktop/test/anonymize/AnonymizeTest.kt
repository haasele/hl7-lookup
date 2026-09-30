// Locks datatype-based anonymizing. Calls anonymize/Index and the HAPI dictionary.
package hl7lookup.anonymize

import hl7lookup.desktop.engine.HapiEngine
import hl7lookup.document.Er7
import hl7lookup.samples.Samples
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

// Checks that person names and identifiers are replaced while structure codes stay. Calls Anonymize.rewrite.
class AnonymizeTest {
    private val message = listOf(
        "MSH|^~\\&|WARD|NORTH|HIS|CENTRAL|20260301083000||ADT^A01^ADT_A01|MSG0001|P|2.5",
        "EVN|A01|20260301083000",
        "PID|1||4711^^^NORTH^MR||Lindqvist^Mara^J||19790412|F|||Birkenweg 3^^Kiel^^24103^DE||^PRN^PH^^^^^0431 555 01",
        "PV1|1|I|STA3^12^2||||0815^Okafor^Daniel",
        "NTE|1||Call the ward about Mara Lindqvist",
        "OBR|1|||Troponin|||202603010915|||||||||0815^Okafor^Daniel",
    ).joinToString("\n")

    // Asserts attending doctor, patient and free text change, and codes stay. Calls HapiEngine.dictionary and Anonymize.rewrite.
    @Test
    fun replacesNamesIdentifiersAndFreeText() = runBlocking {
        val dictionary = HapiEngine().dictionary("2.5")
        val result = Anonymize.rewrite(message, dictionary)
        val parsed = Er7.parse(result)
        assertNotEquals("Okafor", Er7.value(parsed, "PV1-7.2"))
        assertNotEquals("Daniel", Er7.value(parsed, "PV1-7.3"))
        assertNotEquals("0815", Er7.value(parsed, "PV1-7.1"))
        assertNotEquals("Lindqvist", Er7.value(parsed, "PID-5.1"))
        assertNotEquals("Mara", Er7.value(parsed, "PID-5.2"))
        assertNotEquals("4711", Er7.value(parsed, "PID-3.1"))
        assertNotEquals("Birkenweg 3", Er7.value(parsed, "PID-11.1"))
        assertNotEquals("Kiel", Er7.value(parsed, "PID-11.3"))
        assertNotEquals("Okafor", Er7.value(parsed, "OBR-16.2"))
        assertFalse(result.contains("Okafor"), result)
        assertFalse(result.contains("Lindqvist"), result)
        assertFalse(result.contains("Mara"), result)
        assertFalse(result.contains("Daniel"), result)
        assertFalse(result.contains("4711"), result)
        assertFalse(result.contains("Birkenweg"), result)
        assertEquals("ADT", Er7.header(parsed).type)
        assertEquals("A01", Er7.header(parsed).event)
        assertEquals("I", Er7.value(parsed, "PV1-2"))
        assertEquals("STA3", Er7.value(parsed, "PV1-3.1"))
        assertEquals("F", Er7.value(parsed, "PID-8"))
        assertTrue(Er7.value(parsed, "PV1-7.2").isNotBlank())
        assertNotEquals("19790412", Er7.value(parsed, "PID-7"))
    }

    // Asserts every built-in sample loses its real names. Calls Samples.all and Anonymize.rewrite.
    @Test
    fun samplesLoseRealNames() = runBlocking {
        val dictionaries = mapOf(
            "2.4" to HapiEngine().dictionary("2.4"),
            "2.5" to HapiEngine().dictionary("2.5"),
            "2.5.1" to HapiEngine().dictionary("2.5.1"),
        )
        val forbidden = listOf("Okafor", "Lindqvist", "Weber", "Brandt", "Meyer", "Holm", "Birkenweg", "mara.lindqvist")
        for (text in Samples.all()) {
            val version = Er7.header(Er7.parse(text)).version.ifBlank { "2.5" }
            val dictionary = dictionaries[version] ?: dictionaries.getValue("2.5")
            val result = Anonymize.rewrite(text, dictionary)
            for (token in forbidden) assertFalse(result.contains(token), "$token still in\n$result")
        }
    }
}
