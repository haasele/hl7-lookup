// Sample message texts. samples/Index exposes them.
package hl7lookup.samples

import hl7lookup.i18n.Text

// Holds one sample's id and display title; sampleInfos builds these for Samples.list.
data class SampleInfo(val id: String, val title: Text)

// Builds the sample catalog from the built-in list; Samples.list calls this.
internal fun sampleInfos(): List<SampleInfo> = samples.map { SampleInfo(it.id, it.title) }

// Joins one sample's lines into a message body by id; Samples.text calls this.
internal fun sampleText(id: String): String? = samples.firstOrNull { it.id == id }?.lines?.joinToString("\n")

// Joins every sample's lines into message bodies; Samples.all calls this.
internal fun allSampleTexts(): List<String> = samples.map { it.lines.joinToString("\n") }
