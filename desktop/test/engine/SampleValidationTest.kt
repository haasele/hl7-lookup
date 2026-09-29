package hl7lookup.desktop.engine

import hl7lookup.document.Er7
import hl7lookup.engine.InspectRequest
import hl7lookup.samples.Samples
import hl7lookup.validation.Validation
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

class SampleValidationTest {
    private val engine = HapiEngine()

    @Test
    fun samplesHaveNoFieldLevelFindings() = runBlocking {
        val problems = mutableListOf<String>()
        for (info in Samples.list()) {
            val text = Samples.text(info.id)!!
            val message = Er7.parse(text)
            val dictionary = engine.dictionary(Er7.header(message).version)
            val findings = Validation.merge(
                Validation.check(message, dictionary),
                Validation.fromEngine(message, engine.inspect(InspectRequest(text))),
            )
            findings.forEach { problems += "${info.id} ${it.severity} ${it.kind} ${it.label} ${it.args}" }
        }
        assertTrue(problems.isEmpty(), problems.joinToString("\n"))
    }
}
