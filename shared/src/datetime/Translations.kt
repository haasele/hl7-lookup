package hl7lookup.datetime

import hl7lookup.i18n.Text

internal class RelativeTexts(val pastOne: Text, val pastMany: Text, val futureOne: Text, val futureMany: Text)

internal val relativeTexts = mapOf(
    RelativeUnit.NOW to RelativeTexts(
        Text("just now", "gerade eben"), Text("just now", "gerade eben"),
        Text("in a moment", "gleich"), Text("in a moment", "gleich"),
    ),
    RelativeUnit.SECOND to RelativeTexts(
        Text("{0} second ago", "vor {0} Sekunde"), Text("{0} seconds ago", "vor {0} Sekunden"),
        Text("in {0} second", "in {0} Sekunde"), Text("in {0} seconds", "in {0} Sekunden"),
    ),
    RelativeUnit.MINUTE to RelativeTexts(
        Text("{0} minute ago", "vor {0} Minute"), Text("{0} minutes ago", "vor {0} Minuten"),
        Text("in {0} minute", "in {0} Minute"), Text("in {0} minutes", "in {0} Minuten"),
    ),
    RelativeUnit.HOUR to RelativeTexts(
        Text("{0} hour ago", "vor {0} Stunde"), Text("{0} hours ago", "vor {0} Stunden"),
        Text("in {0} hour", "in {0} Stunde"), Text("in {0} hours", "in {0} Stunden"),
    ),
    RelativeUnit.DAY to RelativeTexts(
        Text("{0} day ago", "vor {0} Tag"), Text("{0} days ago", "vor {0} Tagen"),
        Text("in {0} day", "in {0} Tag"), Text("in {0} days", "in {0} Tagen"),
    ),
    RelativeUnit.MONTH to RelativeTexts(
        Text("{0} month ago", "vor {0} Monat"), Text("{0} months ago", "vor {0} Monaten"),
        Text("in {0} month", "in {0} Monat"), Text("in {0} months", "in {0} Monaten"),
    ),
    RelativeUnit.YEAR to RelativeTexts(
        Text("{0} year ago", "vor {0} Jahr"), Text("{0} years ago", "vor {0} Jahren"),
        Text("in {0} year", "in {0} Jahr"), Text("in {0} years", "in {0} Jahren"),
    ),
)

internal val styleNames = mapOf(
    DateStyle.EUROPEAN to Text("European (31.12.2026 14:30)", "Europäisch (31.12.2026 14:30)"),
    DateStyle.AMERICAN to Text("US (12/31/2026 02:30 PM)", "US (12/31/2026 02:30 PM)"),
    DateStyle.ISO to Text("ISO (2026-12-31 14:30)", "ISO (2026-12-31 14:30)"),
)
