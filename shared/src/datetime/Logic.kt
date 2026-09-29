// Precision and offset rules for HL7 dates. datetime/Index exposes them.
package hl7lookup.datetime

import kotlin.math.abs
import kotlin.time.Instant
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import kotlinx.datetime.number
import kotlinx.datetime.offsetAt
import kotlinx.datetime.periodUntil
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable

// Display format choices for dates. Index and Translations read it.
@Serializable
enum class DateStyle { EUROPEAN, AMERICAN, ISO }

// How much of an HL7 timestamp is present. Hl7Time and formatters use it.
enum class Precision { YEAR, MONTH, DAY, HOUR, MINUTE, SECOND, FRACTION }

// Parsed HL7 timestamp with fields and precision. Index and Logic helpers pass it around.
data class Hl7Time(
    val year: Int,
    val month: Int = 1,
    val day: Int = 1,
    val hour: Int = 0,
    val minute: Int = 0,
    val second: Int = 0,
    val fraction: String = "",
    val offsetMinutes: Int? = null,
    val precision: Precision,
)

// Unit used in relative phrasing. RelativeTime and Translations key off it.
enum class RelativeUnit { NOW, SECOND, MINUTE, HOUR, DAY, MONTH, YEAR }

// Amount and direction of a time difference. Index.describe turns it into wording.
data class RelativeTime(val unit: RelativeUnit, val amount: Long, val future: Boolean)

private val hl7TimePattern = Regex("^(\\d{4})(\\d{2})?(\\d{2})?(\\d{2})?(\\d{2})?(\\d{2})?(?:\\.(\\d{1,4}))?([+-]\\d{4})?$")

// Parses HL7 timestamp text into structured time. Index.parse and other Logic helpers call it.
internal fun parseHl7Time(raw: String): Hl7Time? {
    val match = hl7TimePattern.matchEntire(raw.trim()) ?: return null
    val g = match.groupValues
    val year = g[1].toInt()
    val month = g[2].toIntOrNull()
    val day = g[3].toIntOrNull()
    val hour = g[4].toIntOrNull()
    val minute = g[5].toIntOrNull()
    val second = g[6].toIntOrNull()
    if (month != null && month !in 1..12) return null
    if (day != null && (month == null || runCatching { LocalDate(year, month, day) }.isFailure)) return null
    if (hour != null && hour !in 0..23) return null
    if (minute != null && minute !in 0..59) return null
    if (second != null && second !in 0..59) return null
    val offset = g[8].takeIf { it.isNotEmpty() }?.let {
        val sign = if (it[0] == '-') -1 else 1
        val hh = it.substring(1, 3).toInt()
        val mm = it.substring(3, 5).toInt()
        if (hh > 14 || mm > 59) return null
        sign * (hh * 60 + mm)
    }
    val precision = when {
        g[7].isNotEmpty() -> Precision.FRACTION
        second != null -> Precision.SECOND
        minute != null -> Precision.MINUTE
        hour != null -> Precision.HOUR
        day != null -> Precision.DAY
        month != null -> Precision.MONTH
        else -> Precision.YEAR
    }
    return Hl7Time(year, month ?: 1, day ?: 1, hour ?: 0, minute ?: 0, second ?: 0, g[7], offset, precision)
}

// True when raw text parses as HL7 time. Index.isValid calls it.
internal fun isHl7Time(raw: String): Boolean = parseHl7Time(raw) != null

// Builds a LocalDateTime from timestamp fields. instantOf and formatters call it.
internal fun localDateTimeOf(time: Hl7Time): LocalDateTime =
    LocalDateTime(time.year, time.month, time.day, time.hour, time.minute, time.second)

// Turns structured time into an Instant using offset or zone. relativeTo and displayedLocal call it.
internal fun instantOf(time: Hl7Time, zone: TimeZone): Instant {
    val local = localDateTimeOf(time)
    val offset = time.offsetMinutes
    return if (offset != null) local.toInstant(UtcOffset(minutes = offset).asTimeZone()) else local.toInstant(zone)
}

// Local date-time shown after zone conversion. Index.local and formatLocal call it.
internal fun displayedLocal(time: Hl7Time, zone: TimeZone): LocalDateTime =
    if (time.offsetMinutes == null || time.precision <= Precision.DAY) localDateTimeOf(time)
    else instantOf(time, zone).toLocalDateTime(zone)

// Pads a number to two digits. formatLocal and formatHl7 call it.
private fun two(value: Int): String = value.toString().padStart(2, '0')

// Formats structured time for the chosen display style. Index.format calls it.
internal fun formatLocal(time: Hl7Time, style: DateStyle, zone: TimeZone): String {
    val t = displayedLocal(time, zone)
    val m = t.month.number
    val date = when (time.precision) {
        Precision.YEAR -> "${t.year}"
        Precision.MONTH -> when (style) {
            DateStyle.EUROPEAN -> "${two(m)}.${t.year}"
            DateStyle.AMERICAN -> "${two(m)}/${t.year}"
            DateStyle.ISO -> "${t.year}-${two(m)}"
        }
        else -> when (style) {
            DateStyle.EUROPEAN -> "${two(t.day)}.${two(m)}.${t.year}"
            DateStyle.AMERICAN -> "${two(m)}/${two(t.day)}/${t.year}"
            DateStyle.ISO -> "${t.year}-${two(m)}-${two(t.day)}"
        }
    }
    if (time.precision <= Precision.DAY) return date
    val withSeconds = time.precision >= Precision.SECOND
    val clock = when (style) {
        DateStyle.AMERICAN -> {
            val h12 = if (t.hour % 12 == 0) 12 else t.hour % 12
            val suffix = if (t.hour < 12) "AM" else "PM"
            "${two(h12)}:${two(t.minute)}" + (if (withSeconds) ":${two(t.second)}" else "") + " $suffix"
        }
        else -> "${two(t.hour)}:${two(t.minute)}" + if (withSeconds) ":${two(t.second)}" else ""
    }
    return "$date $clock"
}

// Placeholder pattern string for date entry. Index.inputHint calls it.
internal fun inputPattern(style: DateStyle): String = when (style) {
    DateStyle.EUROPEAN -> "dd.MM.yyyy HH:mm:ss"
    DateStyle.AMERICAN -> "MM/dd/yyyy hh:mm:ss AM"
    DateStyle.ISO -> "yyyy-MM-dd HH:mm:ss"
}

// Parses typed local date/time text. Index.parseInput calls it.
internal fun parseLocalInput(input: String, style: DateStyle): LocalDateTime? {
    val trimmed = input.trim()
    val pattern = when (style) {
        DateStyle.EUROPEAN -> Regex("^(\\d{1,2})\\.(\\d{1,2})\\.(\\d{4})(?:\\s+(\\d{1,2}):(\\d{2})(?::(\\d{2}))?)?$")
        DateStyle.AMERICAN -> Regex("^(\\d{1,2})/(\\d{1,2})/(\\d{4})(?:\\s+(\\d{1,2}):(\\d{2})(?::(\\d{2}))?\\s*([AaPp][Mm])?)?$")
        DateStyle.ISO -> Regex("^(\\d{4})-(\\d{1,2})-(\\d{1,2})(?:[ T](\\d{1,2}):(\\d{2})(?::(\\d{2}))?)?$")
    }
    val g = pattern.matchEntire(trimmed)?.groupValues ?: return null
    val (year, month, day) = when (style) {
        DateStyle.EUROPEAN -> Triple(g[3].toInt(), g[2].toInt(), g[1].toInt())
        DateStyle.AMERICAN -> Triple(g[3].toInt(), g[1].toInt(), g[2].toInt())
        DateStyle.ISO -> Triple(g[1].toInt(), g[2].toInt(), g[3].toInt())
    }
    var hour = g[4].toIntOrNull() ?: 0
    if (style == DateStyle.AMERICAN && g.getOrNull(7)?.isNotEmpty() == true) {
        val pm = g[7].lowercase() == "pm"
        hour = (hour % 12) + if (pm) 12 else 0
    }
    return runCatching { LocalDateTime(year, month, day, hour, g[5].toIntOrNull() ?: 0, g[6].toIntOrNull() ?: 0) }.getOrNull()
}

// Detects a colon meaning clock time is present. Index.inputHasTime calls it.
internal fun hasTime(input: String): Boolean = input.contains(':')

// Serializes a local date-time into HL7 timestamp text. Index.toHl7 and nowAsHl7 call it.
internal fun formatHl7(value: LocalDateTime, precision: Precision, offsetMinutes: Int?): String {
    val base = buildString {
        append(value.year.toString().padStart(4, '0'))
        if (precision >= Precision.MONTH) append(two(value.month.number))
        if (precision >= Precision.DAY) append(two(value.day))
        if (precision >= Precision.HOUR) append(two(value.hour))
        if (precision >= Precision.MINUTE) append(two(value.minute))
        if (precision >= Precision.SECOND) append(two(value.second))
    }
    if (offsetMinutes == null || precision <= Precision.DAY) return base
    val sign = if (offsetMinutes < 0) '-' else '+'
    val a = abs(offsetMinutes)
    return base + sign + two(a / 60) + two(a % 60)
}

// Computes relative span from a timestamp versus now. Index.relative and relativeLabel call it.
internal fun relativeTo(time: Hl7Time, nowMillis: Long, zone: TimeZone): RelativeTime {
    val then = instantOf(time, zone)
    val now = Instant.fromEpochMilliseconds(nowMillis)
    val future = then > now
    val seconds = abs((then - now).inWholeSeconds)
    if (seconds < 45) return RelativeTime(RelativeUnit.NOW, 0, future)
    if (seconds < 3600) return RelativeTime(RelativeUnit.MINUTE, (seconds / 60).coerceAtLeast(1), future)
    if (seconds < 86_400) return RelativeTime(RelativeUnit.HOUR, seconds / 3600, future)
    val a = then.toLocalDateTime(zone).date
    val b = now.toLocalDateTime(zone).date
    val period = if (future) b.periodUntil(a) else a.periodUntil(b)
    return when {
        period.years > 0 -> RelativeTime(RelativeUnit.YEAR, period.years.toLong(), future)
        period.months > 0 -> RelativeTime(RelativeUnit.MONTH, period.months.toLong(), future)
        else -> RelativeTime(RelativeUnit.DAY, (seconds / 86_400).coerceAtLeast(1), future)
    }
}

// Whole years between birth and now. Index.age calls it.
internal fun ageInYears(birth: Hl7Time, nowMillis: Long, zone: TimeZone): Int {
    val born = localDateTimeOf(birth).date
    val today = Instant.fromEpochMilliseconds(nowMillis).toLocalDateTime(zone).date
    return born.periodUntil(today).years
}

// Adds days to an HL7 timestamp string. Index.shift calls it.
internal fun shiftDays(raw: String, days: Int): String? {
    val time = parseHl7Time(raw) ?: return null
    if (time.precision < Precision.DAY) return raw
    val date = localDateTimeOf(time).date.plus(DatePeriod(days = days))
    val shifted = LocalDateTime(date, LocalTime(time.hour, time.minute, time.second))
    val fraction = if (time.precision == Precision.FRACTION) ".${time.fraction}" else ""
    val body = formatHl7(shifted, if (time.precision == Precision.FRACTION) Precision.SECOND else time.precision, null)
    val offset = time.offsetMinutes?.let { formatHl7(shifted, Precision.SECOND, it).takeLast(5) } ?: ""
    return body + fraction + offset
}

// Current instant as HL7 timestamp with zone offset. Index.now calls it.
internal fun nowAsHl7(nowMillis: Long, zone: TimeZone): String {
    val local = Instant.fromEpochMilliseconds(nowMillis).toLocalDateTime(zone)
    val offset = zone.offsetAt(Instant.fromEpochMilliseconds(nowMillis)).totalSeconds / 60
    return formatHl7(local, Precision.SECOND, offset)
}

// System default time zone. Most Index date helpers pass it in.
internal fun systemZone(): TimeZone = TimeZone.currentSystemDefault()

// Calendar date for an epoch millis value. Index.today calls it.
internal fun localDateOf(millis: Long, zone: TimeZone): LocalDate = Instant.fromEpochMilliseconds(millis).toLocalDateTime(zone).date
