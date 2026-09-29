package hl7lookup.datetime

import androidx.compose.runtime.Composable
import hl7lookup.i18n.I18n
import hl7lookup.i18n.Language
import hl7lookup.i18n.LocalLanguage
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone

object Hl7Dates {
    fun parse(raw: String): Hl7Time? = parseHl7Time(raw)
    fun isValid(raw: String): Boolean = isHl7Time(raw)
    fun format(time: Hl7Time, style: DateStyle, zone: TimeZone = systemZone()): String = formatLocal(time, style, zone)
    fun format(raw: String, style: DateStyle): String? = parseHl7Time(raw)?.let { formatLocal(it, style, systemZone()) }
    fun local(time: Hl7Time): LocalDateTime = displayedLocal(time, systemZone())
    fun parseInput(input: String, style: DateStyle): LocalDateTime? = parseLocalInput(input, style)
    fun inputHint(style: DateStyle): String = inputPattern(style)
    fun inputHasTime(input: String): Boolean = hasTime(input)
    fun toHl7(value: LocalDateTime, precision: Precision, offsetMinutes: Int?): String = formatHl7(value, precision, offsetMinutes)
    fun relative(time: Hl7Time, nowMillis: Long): RelativeTime = relativeTo(time, nowMillis, systemZone())
    fun age(birth: Hl7Time, nowMillis: Long): Int = ageInYears(birth, nowMillis, systemZone())
    fun shift(raw: String, days: Int): String? = shiftDays(raw, days)
    fun now(nowMillis: Long): String = nowAsHl7(nowMillis, systemZone())
    fun today(nowMillis: Long): LocalDate = localDateOf(nowMillis, systemZone())
    fun describe(relative: RelativeTime, language: Language): String {
        val texts = relativeTexts.getValue(relative.unit)
        val chosen = if (relative.future) I18n.plural(relative.amount, texts.futureOne, texts.futureMany)
        else I18n.plural(relative.amount, texts.pastOne, texts.pastMany)
        return I18n.format(chosen, language, relative.amount)
    }
    fun styleName(style: DateStyle) = styleNames.getValue(style)
    fun styles(): List<DateStyle> = DateStyle.entries
}

@Composable
fun relativeLabel(time: Hl7Time, nowMillis: Long): String = Hl7Dates.describe(relativeTo(time, nowMillis, systemZone()), LocalLanguage.current)
