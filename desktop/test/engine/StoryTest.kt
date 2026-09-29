package hl7lookup.desktop.engine

import hl7lookup.datetime.DateStyle
import hl7lookup.document.Er7
import hl7lookup.engine.InspectRequest
import hl7lookup.engine.Severity
import hl7lookup.i18n.Language
import hl7lookup.samples.Samples
import hl7lookup.story.Stories
import hl7lookup.story.StoryContext
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

class StoryTest {
    private val engine = HapiEngine()
    private val now = 1_790_000_000_000L

    @Test
    fun everySampleReadsAsSentencesAndParses() = runBlocking {
        for (info in Samples.list()) {
            val text = Samples.text(info.id)!!
            val message = Er7.parse(text)
            val dictionary = engine.dictionary(Er7.header(message).version)
            val story = Stories.build(message, StoryContext(dictionary, DateStyle.EUROPEAN, now, Language.EN))
            val plain = Stories.plain(story)
            println("---- ${info.id}\n$plain")
            assertTrue(plain.isNotBlank(), info.id)
            val report = engine.inspect(InspectRequest(text))
            println("findings ${info.id}: ${report.structure} ${report.findings}")
            assertTrue(report.structure != null, info.id)
            assertTrue(report.findings.none { it.severity == Severity.ERROR }, "${info.id}: ${report.findings}")
        }
    }

    @Test
    fun admissionStoryNamesPatientAndCodes() = runBlocking {
        val message = Er7.parse(Samples.text("adt-a01")!!)
        val story = Stories.build(message, StoryContext(engine.dictionary("2.5"), DateStyle.EUROPEAN, now, Language.EN))
        val plain = Stories.plain(story)
        assertTrue("Mara Johanna Lindqvist" in plain, plain)
        assertTrue("Female" in plain, plain)
        assertTrue("Inpatient" in plain, plain)
        assertTrue("12.04.1979" in plain, plain)
    }
}
