// Anonymise wording. anonymize/Index reads it.
package hl7lookup.anonymize

import hl7lookup.i18n.Text

// Anonymize dialog wording. anonymize/Index reads it via texts().
object AnonymizeTexts {
    val menu = Text("Anonymize", "Anonymisieren")
    val current = Text("Anonymize current message…", "Aktuelle Nachricht anonymisieren…")
    val tab = Text("Anonymize all messages in tab…", "Alle Nachrichten im Tab anonymisieren…")
    val title = Text("Anonymize", "Anonymisieren")
    val scope = Text("Messages", "Nachrichten")
    val scopeCurrent = Text("Current message", "Aktuelle Nachricht")
    val scopeTabOne = Text("All {0} message in this tab", "Alle {0} Nachricht in diesem Tab")
    val scopeTab = Text("All {0} messages in this tab", "Alle {0} Nachrichten in diesem Tab")
    val newTab = Text("Put the result into a new tab", "Ergebnis in einen neuen Tab legen")
    val names = Text("Names and organizations", "Namen und Organisationen")
    val identifiers = Text("Identifiers, account and insurance numbers", "Kennungen, Fall- und Versicherungsnummern")
    val addresses = Text("Addresses", "Adressen")
    val phones = Text("Phone numbers and e-mail", "Telefonnummern und E-Mail")
    val freeText = Text("Free text in NTE and OBX", "Freitext in NTE und OBX")
    val shiftDates = Text("Shift all dates by {0} days", "Alle Datumswerte um {0} Tage verschieben")
    val coverage = Text("Replaces names, identifiers, addresses, phones and organizations wherever the HL7 datatype marks them, plus free text in NTE and OBX. Segment structure stays the same.", "Ersetzt Namen, Kennungen, Adressen, Telefonnummern und Organisationen überall, wo der HL7-Datentyp sie ausweist, plus Freitext in NTE und OBX. Die Segmentstruktur bleibt erhalten.")
    val cacheOne = Text("{0} remembered replacement. The same original value always gets the same replacement.", "{0} gemerkte Ersetzung. Derselbe Originalwert bekommt immer dieselbe Ersetzung.")
    val cache = Text("{0} remembered replacements. The same original value always gets the same replacement.", "{0} gemerkte Ersetzungen. Derselbe Originalwert bekommt immer dieselbe Ersetzung.")
    val resetCache = Text("Forget replacements", "Ersetzungen vergessen")
    val start = Text("Anonymize", "Anonymisieren")
    val progressOne = Text("{0} of {1} message", "{0} von {1} Nachricht")
    val progress = Text("{0} of {1} messages", "{0} von {1} Nachrichten")
    val doneOne = Text("{0} message anonymized.", "{0} Nachricht anonymisiert.")
    val done = Text("{0} messages anonymized.", "{0} Nachrichten anonymisiert.")
    val tabTitle = Text("{0} (anonymized)", "{0} (anonymisiert)")
}

internal val englishWordbook = Wordbook(
    families = listOf(
        "Ashford", "Bramley", "Carver", "Dunmore", "Ellery", "Fenwick", "Garland", "Holloway", "Ingram", "Jessop",
        "Kendrick", "Langley", "Marlow", "Norcross", "Oakley", "Pembroke", "Quimby", "Radley", "Stanton", "Thorne",
        "Underhill", "Vance", "Whitlock", "Yardley", "Alder", "Birch", "Cobb", "Dray", "Everly", "Frost",
        "Greaves", "Hartley", "Irwin", "Keel", "Lowell", "Mercer", "Nash", "Orwin", "Prescott", "Rowan",
        "Sutter", "Tolland", "Upton", "Wren", "Brandt", "Falk", "Hagen", "Lund", "Moser", "Reuter",
    ),
    givens = listOf(
        "Alex", "Robin", "Sam", "Jamie", "Morgan", "Taylor", "Casey", "Jordan", "Riley", "Avery",
        "Quinn", "Rowan", "Parker", "Reese", "Skyler", "Emery", "Finley", "Harper", "Kai", "Logan",
        "Noa", "Luca", "Mika", "Toni", "Kim", "Charlie", "Dana", "Eli", "Jules", "Sasha",
        "Ari", "Blake", "Cameron", "Drew", "Elliot", "Frankie", "Hayden", "Jesse", "Lane", "Marlo",
    ),
    streets = listOf(
        "Maple Lane", "Harbor Road", "Linden Way", "Mill Street", "Orchard Close", "Station Road", "Hill Crescent",
        "Brook Avenue", "Meadow Drive", "Chapel Row", "Garden Walk", "River Terrace", "Elm Court", "Quarry Path",
    ),
    cities = listOf(
        "Northfield", "Easton", "Westbrook", "Southport", "Lakeside", "Riverton", "Fairview", "Oakridge",
        "Greenhill", "Stonebridge", "Clearwater", "Ashby", "Millbrook", "Redcliff",
    ),
    organizations = listOf(
        "Sample Works", "Placeholder Ltd", "Example Services", "Generic Holding", "Demo Logistics", "Test Engineering",
    ),
    words = listOf(
        "patient", "reports", "stable", "follow", "up", "review", "noted", "within", "normal", "limits",
        "advised", "rest", "daily", "observed", "plan", "continue", "current", "therapy", "result", "pending",
        "discussed", "with", "family", "no", "acute", "findings", "repeat", "in", "two", "weeks",
    ),
)
