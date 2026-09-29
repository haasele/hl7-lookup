package hl7lookup.samples

import hl7lookup.i18n.Text

data class SampleInfo(val id: String, val title: Text)

internal fun sampleInfos(): List<SampleInfo> = samples.map { SampleInfo(it.id, it.title) }

internal fun sampleText(id: String): String? = samples.firstOrNull { it.id == id }?.lines?.joinToString("\n")

internal fun allSampleTexts(): List<String> = samples.map { it.lines.joinToString("\n") }
