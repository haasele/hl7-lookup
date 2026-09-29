// Built-in sample messages. The File menu and the wiki call Samples.
package hl7lookup.samples

// Entry point other features use for built-in sample messages; forwards to Logic.
object Samples {
    // Returns sample ids and titles for the File menu; other features call this, it forwards to sampleInfos.
    fun list(): List<SampleInfo> = sampleInfos()
    // Looks up one sample message body by id; other features call this, it forwards to sampleText.
    fun text(id: String): String? = sampleText(id)
    // Returns every sample message body; other features call this, it forwards to allSampleTexts.
    fun all(): List<String> = allSampleTexts()
    // Exposes sample menu and tab wording; other features call this, it returns SampleTexts.
    fun texts() = SampleTexts
}
