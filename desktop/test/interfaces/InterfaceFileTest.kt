package hl7lookup.interfaces

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InterfaceFileTest {
    private val definition = InterfaceDefinition(
        id = "if-1",
        name = "Admit",
        description = "ADT A01",
        messageType = "ADT",
        event = "A01",
        structure = "ADT_A01",
        version = "2.5",
        rules = listOf(InterfaceRule("r1", RuleKind.REQUIRED, "PID-5"), InterfaceRule("r2", RuleKind.HIGHLIGHT, "PID-5", color = 2)),
        tables = listOf(CustomTable("0004", "Patient class", listOf(hl7lookup.dictionary.TableEntry("I", "Inpatient")))),
    )

    @Test
    fun exportRoundTrips() {
        val decoded = Interfaces.decode(Interfaces.encode(listOf(definition)))
        assertEquals(listOf(definition), decoded)
    }

    @Test
    fun aSingleDefinitionImportsToo() {
        val json = """{"id":"if-1","name":"Admit","messageType":"ADT","event":"A01","structure":"ADT_A01","version":"2.5","rules":[],"tables":[]}"""
        assertEquals("Admit", Interfaces.decode(json)?.single()?.name)
    }

    @Test
    fun rejectsAForeignFile() {
        assertNull(Interfaces.decode("this is not an interface"))
    }
}
