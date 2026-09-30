// Notice text and component credits. license/Index reads them.
package hl7lookup.license

import hl7lookup.i18n.Text

// English and German notice strings. Index and LicenseDialog read them via tr().
object LicenseTexts {
    val menu = Text("License…", "Lizenz…")
    val title = Text("License", "Lizenz")
    val app = Text("HL7 Lookup", "HL7 Lookup")
    val appNotice = Text(
        "HL7 Lookup reads, edits, validates, anonymizes, sends and receives HL7 v2 messages. Sample messages, sentences and word lists are original to this application.",
        "HL7 Lookup liest, bearbeitet, prüft, anonymisiert, sendet und empfängt HL7-v2-Nachrichten. Beispielnachrichten, Sätze und Wortlisten sind eigene Inhalte dieser Anwendung.",
    )
    val hl7Notice = Text(
        "HL7 and Health Level Seven are registered trademarks of Health Level Seven International. Field names, datatypes and table descriptions come from the HAPI structure libraries.",
        "HL7 und Health Level Seven sind eingetragene Marken von Health Level Seven International. Feldnamen, Datentypen und Tabellenbeschreibungen stammen aus den HAPI-Strukturbibliotheken.",
    )
    val components = Text("Third-party components", "Komponenten von Dritten")
    val hapiNote = Text(
        "Runs in the desktop process. The browser client uses it through the local desktop server.",
        "Läuft im Desktop-Prozess. Der Browser-Client nutzt HAPI über den lokalen Desktop-Server.",
    )
    val version = Text("Version {0}", "Version {0}")
}

internal val thirdParty = listOf(
    Component("HAPI HL7v2", "2.6.0", "Mozilla Public License 1.1 or GPL 3.0 (dual license)", "https://hapifhir.github.io/hapi-hl7v2/", LicenseTexts.hapiNote),
    Component("Kotlin", "2.3", "Apache License 2.0", "https://kotlinlang.org"),
    Component("Compose Multiplatform", "1.10", "Apache License 2.0", "https://www.jetbrains.com/compose-multiplatform/"),
    Component("kotlinx.coroutines, kotlinx.serialization, kotlinx-datetime", "", "Apache License 2.0", "https://github.com/Kotlin"),
    Component("Skiko", "0.9", "Apache License 2.0", "https://github.com/JetBrains/skiko"),
    Component("js-joda", "", "BSD 3-Clause License", "https://js-joda.github.io/js-joda/"),
    Component("Droid Sans and Droid Sans Mono", "1.00", "Apache License 2.0", "https://android.googlesource.com/platform/frameworks/base/+/master/data/fonts/"),
    Component("SLF4J", "2.0", "MIT License", "https://www.slf4j.org"),
)
