// HL7 timestamp parse and format. The editor and the grid call Hl7Dates.
package hl7lookup.datetime

import androidx.compose.runtime.Composable
import hl7lookup.i18n.I18n
import hl7lookup.i18n.Language
import hl7lookup.i18n.LocalLanguage
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone

// Public date helpers for editor and grid. Each method forwards into Logic.
object Hl7Dates {
    // Turns raw HL7 timestamp text into structured time. Forwards to parseHl7Time.
    fun parse(raw: String): Hl7Time? = parseHl7Time(raw)
    // Checks whether raw text is a valid HL7 timestamp. Forwards to isHl7Time.
    fun isValid(raw: String): Boolean = isHl7Time(raw)
    // Formats structured time for display in the chosen style. Forwards to formatLocal.
    fun format(time: Hl7Time, style: DateStyle, zone: TimeZone = systemZone()): String = formatLocal(time, style, zone)
    // Parses then formats raw HL7 text in one step. Calls parseHl7Time and formatLocal.
    fun format(raw: String, style: DateStyle): String? = parseHl7Time(raw)?.let { formatLocal(it, style, systemZone()) }
    // Converts structured time to a local date-time for display. Forwards to displayedLocal.
    fun local(time: Hl7Time): LocalDateTime = displayedLocal(time, systemZone())
    // Parses a human-typed date string in the chosen style. Forwards to parseLocalInput.
    fun parseInput(input: String, style: DateStyle): LocalDateTime? = parseLocalInput(input, style)
    // Returns the placeholder pattern for the date input field. Forwards to inputPattern.
    fun inputHint(style: DateStyle): String = inputPattern(style)
    // Detects whether typed input includes a clock time. Forwards to hasTime.
    fun inputHasTime(input: String): Boolean = hasTime(input)
    // Writes a local date-time back as HL7 timestamp text. Forwards to formatHl7.
    fun toHl7(value: LocalDateTime, precision: Precision, offsetMinutes: Int?): String = formatHl7(value, precision, offsetMinutes)
    // Measures how far a timestamp is from now. Forwards to relativeTo.
    fun relative(time: Hl7Time, nowMillis: Long): RelativeTime = relativeTo(time, nowMillis, systemZone())
    // Computes age in whole years from a birth timestamp. Forwards to ageInYears.
    fun age(birth: Hl7Time, nowMillis: Long): Int = ageInYears(birth, nowMillis, systemZone())
    // Moves an HL7 timestamp by a number of days. Forwards to shiftDays.
    fun shift(raw: String, days: Int): String? = shiftDays(raw, days)
    // Builds the current instant as HL7 timestamp text. Forwards to nowAsHl7.
    fun now(nowMillis: Long): String = nowAsHl7(nowMillis, systemZone())
    // Returns today's calendar date in the system zone. Forwards to localDateOf.
    fun today(nowMillis: Long): LocalDate = localDateOf(nowMillis, systemZone())
    // Renders a relative span as localized wording. Reads relativeTexts and I18n.
    fun describe(relative: RelativeTime, language: Language): String {
        val texts = relativeTexts.getValue(relative.unit)
        val chosen = if (relative.future) I18n.plural(relative.amount, texts.futureOne, texts.futureMany)
        else I18n.plural(relative.amount, texts.pastOne, texts.pastMany)
        return I18n.format(chosen, language, relative.amount)
    }
    // Looks up the display name for a date style. Reads styleNames.
    fun styleName(style: DateStyle) = styleNames.getValue(style)
    // Lists every available date display style. Returns DateStyle.entries.
    fun styles(): List<DateStyle> = DateStyle.entries
}

// Localized relative phrase for Compose UI. Calls relativeTo and Hl7Dates.describe.
@Composable
fun relativeLabel(time: Hl7Time, nowMillis: Long): String = Hl7Dates.describe(relativeTo(time, nowMillis, systemZone()), LocalLanguage.current)
