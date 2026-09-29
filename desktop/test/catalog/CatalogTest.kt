package hl7lookup.catalog

import hl7lookup.dictionary.ComponentDef
import hl7lookup.dictionary.DatatypeDef
import hl7lookup.dictionary.FieldDef
import hl7lookup.dictionary.Hl7Dictionary
import hl7lookup.dictionary.SegmentDef
import hl7lookup.dictionary.StructureDef
import hl7lookup.dictionary.StructureElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CatalogTest {
    private val dictionary = Hl7Dictionary(
        version = "2.5",
        messageTypes = mapOf("ADT" to "Admit", "ORM" to "Order"),
        events = mapOf("A01" to "Admit a patient", "O01" to "Order message"),
        eventStructures = mapOf("ADT_A04" to "ADT_A01"),
        segments = mapOf(
            "PID" to SegmentDef("PID", "Patient identification", listOf(FieldDef(5, "Patient Name", "XPN", required = true))),
            "MSH" to SegmentDef("MSH", "Message header", listOf(FieldDef(4, "Sending Facility", "HD", required = true))),
        ),
        datatypes = mapOf("HD" to DatatypeDef("HD", "Hierarchic designator", listOf(ComponentDef(1, "Namespace ID", "IS"), ComponentDef(2, "Universal ID", "ST")))),
        structures = mapOf(
            "ORM_O01" to StructureDef("ORM_O01", emptyList()),
            "ADT_A02" to StructureDef("ADT_A02", emptyList()),
            "ADT_A03" to StructureDef("ADT_A03", emptyList()),
            "ADT_A01" to StructureDef(
                "ADT_A01",
                listOf(
                    StructureElement("MSH", required = true, repeating = false),
                    StructureElement("EVN", required = true, repeating = false),
                    StructureElement("PID", required = true, repeating = false),
                ),
            ),
        ),
    )

    @Test
    fun listsEveryTypeAndPrefersARealSample() {
        val sample = "MSH|^~\\&|A|B|C|D|20260301083000||ORM^O01^ORM_O01|1|P|2.5"
        val types = Catalog.types(dictionary)
        assertEquals(listOf("ADT", "ORM"), types.map { it.type })
        val adtEvents = types.single { it.type == "ADT" }.events
        assertEquals(listOf("A01", "A02", "A03", "A04"), adtEvents.map { it.code })
        assertEquals("ADT_A01", adtEvents.single { it.code == "A04" }.structure)
        assertEquals("Admit a patient", adtEvents.single { it.code == "A01" }.description)
        assertEquals(listOf("O01"), types.single { it.type == "ORM" }.events.map { it.code })
        assertEquals(sample, Catalog.example(dictionary, "ORM", "O01", "ORM_O01", listOf(sample)))
        val adt = Catalog.example(dictionary, "ADT", "A01", "ADT_A01", emptyList())
        assertTrue(adt.lines().first().contains("ADT^A01"))
        assertTrue(adt.contains("Adler^Lina"))
        val guide = Catalog.guide(dictionary, "ADT", "A01", "ADT_A01")
        assertTrue(guide.contains("ADT^A01^ADT_A01"))
        assertTrue(guide.lines().any { it.startsWith("PID|") && it.contains("Patient name") })
        assertTrue(!guide.contains("Universal ID"))
        assertTrue(!guide.contains("Namespace"))
    }

    @Test
    fun segmentListExplainsEachSlot() {
        val msh = Catalog.segments(dictionary).single { it.name == "MSH" }
        val clinic = msh.pieces.single { it.code == "MSH-4.1" }
        assertEquals("Clinic", clinic.purposeEn)
        assertEquals("NORTH", clinic.example)
        assertTrue(msh.pieces.none { it.purposeEn == "Namespace ID" || it.purposeEn == "Universal ID" })
        assertEquals(listOf("MSH"), Catalog.matchingSegments(Catalog.segments(dictionary), "clinic").map { it.name })
    }

    @Test
    fun searchMatchesTheDescription() {
        val types = Catalog.types(dictionary)
        assertEquals(listOf("ORM"), Catalog.matching(types, "order").map { it.type })
        assertEquals(listOf("A01"), Catalog.matching(types, "A01").single().events.map { it.code })
    }
}
