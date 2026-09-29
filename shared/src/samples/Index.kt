package hl7lookup.samples

object Samples {
    fun list(): List<SampleInfo> = sampleInfos()
    fun text(id: String): String? = sampleText(id)
    fun all(): List<String> = allSampleTexts()
    fun texts() = SampleTexts
}
