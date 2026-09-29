// Sentence fragments. story/Index reads them.
package hl7lookup.story

import hl7lookup.i18n.Text

// Panel wording for the story view. story/Index reads it via texts().
object StoryTexts {
    val panelTitle = Text("Interpretation", "Interpretation")
    val empty = Text("Open or paste an HL7 message to read it here.", "Öffne oder füge eine HL7-Nachricht ein, um sie hier zu lesen.")
    val copy = Text("Copy text", "Text kopieren")
    val loadingDefinitions = Text("Reading with field names as soon as definitions are loaded.", "Feldnamen erscheinen, sobald die Definitionen geladen sind.")
}

internal val storyWords = StoryWords(
    and = Text(" and ", " und "),
    listSeparator = Text("; ", "; "),
    fieldIs = Text("{0} is {1}", "{0} ist {1}"),
    genericLead = Text("{0}: ", "{0}: "),
    sentenceEnd = Text(".", "."),
    repetitionJoin = Text(", ", ", "),
)

internal val locationWords = LocationWords(
    room = Text("room {0}", "Zimmer {0}"),
    bed = Text("bed {0}", "Bett {0}"),
    facility = Text("{0}", "{0}"),
)

// MSH sentence fragments. story/Index builds title and MSH clauses from these.
internal object Msh {
    val full = Text("{0} at {1} sent {2}^{3} ({4}) to {5} at {6} on {7}.", "{0} bei {1} hat {2}^{3} ({4}) an {5} bei {6} gesendet, am {7}.")
    val noFacilities = Text("{0} sent {1}^{2} ({3}) to {4} on {5}.", "{0} hat {1}^{2} ({3}) an {4} gesendet, am {5}.")
    val noReceiver = Text("{0} sent {1}^{2} ({3}) on {4}.", "{0} hat {1}^{2} ({3}) gesendet, am {4}.")
    val noSender = Text("This {0}^{1} message ({2}) was created on {3}.", "Diese Nachricht {0}^{1} ({2}) wurde am {3} erstellt.")
    val typeEvent = Text("This is a {0}^{1} message.", "Dies ist eine Nachricht {0}^{1}.")
    val typeOnly = Text("This is a {0} message.", "Dies ist eine Nachricht vom Typ {0}.")
    val control = Text("Control ID {0}, HL7 version {1}, processing mode {2}.", "Kontroll-ID {0}, HL7-Version {1}, Verarbeitungsmodus {2}.")
    val controlShort = Text("Control ID {0}, HL7 version {1}.", "Kontroll-ID {0}, HL7-Version {1}.")
    val versionOnly = Text("HL7 version {0}.", "HL7-Version {0}.")
    val acks = Text("Accept acknowledgment: {0}, application acknowledgment: {1}.", "Annahmequittung: {0}, Anwendungsquittung: {1}.")
}

// EVN sentence fragments. story/Index builds EVN clauses from these.
internal object Evn {
    val recorded = Text("The event {0} was recorded on {1}.", "Das Ereignis {0} wurde am {1} erfasst.")
    val recordedShort = Text("The event was recorded on {0}.", "Das Ereignis wurde am {0} erfasst.")
    val planned = Text("It is planned for {0}.", "Es ist für {0} geplant.")
    val reason = Text("Reason: {0}.", "Grund: {0}.")
    val operator = Text("Entered by {0}.", "Erfasst von {0}.")
    val occurred = Text("It occurred on {0}.", "Es fand am {0} statt.")
}

// PID sentence fragments. story/Index builds patient clauses from these.
internal object Pid {
    val full = Text("The patient is {0}, {1}, born on {2} ({3} years old).", "Der Patient ist {0}, {1}, geboren am {2} ({3} Jahre alt).")
    val noAge = Text("The patient is {0}, {1}, born on {2}.", "Der Patient ist {0}, {1}, geboren am {2}.")
    val noSex = Text("The patient is {0}, born on {1}.", "Der Patient ist {0}, geboren am {1}.")
    val noBirth = Text("The patient is {0}, {1}.", "Der Patient ist {0}, {1}.")
    val nameOnly = Text("The patient is {0}.", "Der Patient ist {0}.")
    val identifier = Text("Patient identifier {0} is a {1} issued by {2}.", "Die Patientenkennung {0} ist eine {1}, vergeben von {2}.")
    val identifierType = Text("Patient identifier {0} is a {1}.", "Die Patientenkennung {0} ist eine {1}.")
    val identifierOnly = Text("Patient identifier: {0}.", "Patientenkennung: {0}.")
    val address = Text("They live at {0}.", "Wohnhaft: {0}.")
    val homePhone = Text("Home phone: {0}.", "Telefon privat: {0}.")
    val workPhone = Text("Business phone: {0}.", "Telefon geschäftlich: {0}.")
    val marital = Text("Marital status: {0}.", "Familienstand: {0}.")
    val religion = Text("Religion: {0}.", "Religion: {0}.")
    val race = Text("Race: {0}.", "Herkunft: {0}.")
    val ethnic = Text("Ethnic group: {0}.", "Ethnische Gruppe: {0}.")
    val language = Text("Primary language: {0}.", "Sprache: {0}.")
    val mother = Text("Mother's maiden name: {0}.", "Geburtsname der Mutter: {0}.")
    val account = Text("Account number {0}.", "Fallnummer {0}.")
    val death = Text("Died on {0}.", "Verstorben am {0}.")
}

// NK1 sentence fragments. story/Index builds next-of-kin clauses from these.
internal object Nk1 {
    val relation = Text("{0} is the patient's {1}.", "{0} ist für den Patienten: {1}.")
    val nameOnly = Text("Next of kin: {0}.", "Angehörige Person: {0}.")
    val phone = Text("Their phone number is {0}.", "Telefonnummer: {0}.")
    val address = Text("Address: {0}.", "Adresse: {0}.")
    val role = Text("Contact role: {0}.", "Kontaktrolle: {0}.")
}

// PV1 sentence fragments. story/Index builds visit clauses from these.
internal object Pv1 {
    val full = Text("Patient class: {0}, located at {1}.", "Patientenart: {0}, Ort {1}.")
    val classOnly = Text("Patient class: {0}.", "Patientenart: {0}.")
    val attending = Text("The attending doctor is {0}.", "Behandelnd ist {0}.")
    val referring = Text("Referring doctor: {0}.", "Einweisend: {0}.")
    val consulting = Text("Consulting doctor: {0}.", "Konsiliarisch: {0}.")
    val admissionType = Text("Admission type: {0}.", "Aufnahmeart: {0}.")
    val service = Text("Hospital service: {0}.", "Fachabteilung: {0}.")
    val source = Text("Admit source: {0}.", "Aufnahmequelle: {0}.")
    val visit = Text("Visit number {0}.", "Besuchsnummer {0}.")
    val admitted = Text("Admitted on {0}.", "Aufgenommen am {0}.")
    val discharged = Text("Discharged on {0}.", "Entlassen am {0}.")
    val disposition = Text("Discharge disposition: {0}.", "Entlassart: {0}.")
    val prior = Text("Prior location: {0}.", "Vorheriger Ort: {0}.")
}

// PV2 sentence fragments. story/Index builds visit-detail clauses from these.
internal object Pv2 {
    val expectedAdmit = Text("Expected admission on {0}.", "Erwartete Aufnahme am {0}.")
    val expectedDischarge = Text("Expected discharge on {0}.", "Erwartete Entlassung am {0}.")
    val reason = Text("Admit reason: {0}.", "Aufnahmegrund: {0}.")
}

// AL1 sentence fragments. story/Index builds allergy clauses from these.
internal object Al1 {
    val full = Text("Allergy to {0} ({1}), severity {2}, reaction: {3}.", "Allergie gegen {0} ({1}), Schweregrad {2}, Reaktion: {3}.")
    val typed = Text("Allergy to {0} ({1}).", "Allergie gegen {0} ({1}).")
    val short = Text("Allergy to {0}.", "Allergie gegen {0}.")
}

// DG1 sentence fragments. story/Index builds diagnosis clauses from these.
internal object Dg1 {
    val full = Text("Diagnosis {0} ({1}), type {2}, recorded on {3}.", "Diagnose {0} ({1}), Art {2}, erfasst am {3}.")
    val typed = Text("Diagnosis {0} ({1}), type {2}.", "Diagnose {0} ({1}), Art {2}.")
    val coded = Text("Diagnosis {0} ({1}).", "Diagnose {0} ({1}).")
    val described = Text("Diagnosis: {0}.", "Diagnose: {0}.")
}

// PR1 sentence fragments. story/Index builds procedure clauses from these.
internal object Pr1 {
    val full = Text("Procedure {0} performed on {1}.", "Prozedur {0}, durchgeführt am {1}.")
    val short = Text("Procedure {0}.", "Prozedur {0}.")
}

// IN1 sentence fragments. story/Index builds insurance clauses from these.
internal object In1 {
    val full = Text("Insured by {0} under plan {1}, policy number {2}.", "Versichert bei {0}, Tarif {1}, Versicherungsnummer {2}.")
    val noPolicy = Text("Insured by {0} under plan {1}.", "Versichert bei {0}, Tarif {1}.")
    val company = Text("Insured by {0}.", "Versichert bei {0}.")
    val insured = Text("The insured person is {0}, relationship {1}.", "Versicherte Person ist {0}, Beziehung {1}.")
    val insuredName = Text("The insured person is {0}.", "Versicherte Person ist {0}.")
    val coverage = Text("Coverage from {0} until {1}.", "Versicherungsschutz vom {0} bis {1}.")
    val coverageStart = Text("Coverage from {0}.", "Versicherungsschutz ab {0}.")
    val group = Text("Group number {0}.", "Gruppennummer {0}.")
}

// GT1 sentence fragments. story/Index builds guarantor clauses from these.
internal object Gt1 {
    val full = Text("The guarantor is {0}, {1}.", "Kostenträger ist {0}, {1}.")
    val name = Text("The guarantor is {0}.", "Kostenträger ist {0}.")
    val address = Text("Guarantor address: {0}.", "Adresse des Kostenträgers: {0}.")
    val phone = Text("Guarantor phone: {0}.", "Telefon des Kostenträgers: {0}.")
    val birth = Text("Born on {0}.", "Geboren am {0}.")
}

// ORC sentence fragments. story/Index builds order-control clauses from these.
internal object Orc {
    val control = Text("Order control: {0}.", "Auftragssteuerung: {0}.")
    val numbers = Text("Placer order {0}, filler order {1}.", "Auftragsnummer Anforderer {0}, Ausführer {1}.")
    val placer = Text("Placer order {0}.", "Auftragsnummer Anforderer {0}.")
    val status = Text("Order status: {0}.", "Auftragsstatus: {0}.")
    val orderedBy = Text("Ordered on {0} by {1}.", "Angefordert am {0} von {1}.")
    val orderedOn = Text("Ordered on {0}.", "Angefordert am {0}.")
    val provider = Text("Ordered by {0}.", "Angefordert von {0}.")
    val enteredBy = Text("Entered by {0}.", "Erfasst von {0}.")
}

// OBR sentence fragments. story/Index builds order-detail clauses from these.
internal object Obr {
    val service = Text("Requested service: {0} ({1}).", "Angeforderte Leistung: {0} ({1}).")
    val serviceShort = Text("Requested service: {0}.", "Angeforderte Leistung: {0}.")
    val numbers = Text("Placer order {0}, filler order {1}.", "Auftragsnummer Anforderer {0}, Ausführer {1}.")
    val observed = Text("Observed on {0}.", "Beobachtet am {0}.")
    val requested = Text("Requested on {0}.", "Angefordert am {0}.")
    val provider = Text("Ordering provider: {0}.", "Anfordernd: {0}.")
    val status = Text("Result status: {0}.", "Befundstatus: {0}.")
    val reported = Text("Results reported on {0}.", "Befund gemeldet am {0}.")
    val action = Text("Specimen action: {0}.", "Probenaktion: {0}.")
}

// OBX sentence fragments. story/Index builds observation clauses from these.
internal object Obx {
    val full = Text("{0} is {1} {2}, reference range {3}, {4}.", "{0} ist {1} {2}, Referenzbereich {3}, {4}.")
    val range = Text("{0} is {1} {2}, reference range {3}.", "{0} ist {1} {2}, Referenzbereich {3}.")
    val units = Text("{0} is {1} {2}.", "{0} ist {1} {2}.")
    val value = Text("{0} is {1}.", "{0} ist {1}.")
    val valueOnly = Text("Observation value: {0}.", "Beobachtungswert: {0}.")
    val status = Text("Status: {0}.", "Status: {0}.")
    val observed = Text("Observed on {0}.", "Beobachtet am {0}.")
    val type = Text("Value type: {0}.", "Werttyp: {0}.")
}

// NTE sentence fragments. story/Index builds note clauses from these.
internal object Nte {
    val note = Text("Note: {0}", "Hinweis: {0}")
}

// TXA sentence fragments. story/Index builds document clauses from these.
internal object Txa {
    val full = Text("This document is a {0} with the ID {1}.", "Dieses Dokument ist vom Typ {0} und hat die ID {1}.")
    val type = Text("Document type: {0}.", "Dokumenttyp: {0}.")
    val completion = Text("Its completion status is {0}.", "Fertigstellungsstatus: {0}.")
    val confidentiality = Text("Confidentiality: {0}.", "Vertraulichkeit: {0}.")
    val availability = Text("Availability: {0}.", "Verfügbarkeit: {0}.")
    val storage = Text("Storage: {0}.", "Speicherung: {0}.")
    val activity = Text("The document activity took place on {0}.", "Die Dokumentaktivität fand am {0} statt.")
    val origination = Text("Originated on {0}.", "Entstanden am {0}.")
    val transcribed = Text("Transcribed on {0}.", "Transkribiert am {0}.")
    val author = Text("Authored by {0}.", "Verfasst von {0}.")
    val transcriptionist = Text("Transcribed by {0}.", "Transkribiert von {0}.")
    val fileName = Text("File name: {0}.", "Dateiname: {0}.")
}

// SCH sentence fragments. story/Index builds appointment clauses from these.
internal object Sch {
    val full = Text("Appointment {0}: {1}, status {2}.", "Termin {0}: {1}, Status {2}.")
    val reason = Text("Appointment {0}: {1}.", "Termin {0}: {1}.")
    val status = Text("Appointment status: {0}.", "Terminstatus: {0}.")
    val type = Text("Appointment type: {0}.", "Terminart: {0}.")
    val duration = Text("Duration {0} {1}.", "Dauer {0} {1}.")
    val start = Text("Scheduled from {0}.", "Geplant ab {0}.")
    val contact = Text("Contact: {0}.", "Kontakt: {0}.")
    val enteredBy = Text("Entered by {0}.", "Erfasst von {0}.")
}

// AIS sentence fragments. story/Index builds appointment-service clauses from these.
internal object Ais {
    val full = Text("Service {0} starting {1} for {2} {3}.", "Leistung {0} ab {1} für {2} {3}.")
    val start = Text("Service {0} starting {1}.", "Leistung {0} ab {1}.")
    val short = Text("Service {0}.", "Leistung {0}.")
}

// AIG sentence fragments. story/Index builds appointment-resource clauses from these.
internal object Aig {
    val full = Text("Resource {0} ({1}).", "Ressource {0} ({1}).")
    val short = Text("Resource {0}.", "Ressource {0}.")
    val start = Text("Starting {0}.", "Beginn {0}.")
}

// AIL sentence fragments. story/Index builds appointment-location clauses from these.
internal object Ail {
    val full = Text("Location {0}, starting {1}.", "Ort {0}, Beginn {1}.")
    val short = Text("Location {0}.", "Ort {0}.")
}

// AIP sentence fragments. story/Index builds appointment-person clauses from these.
internal object Aip {
    val full = Text("{0} takes part as {1}, starting {2}.", "{0} nimmt teil als {1}, Beginn {2}.")
    val role = Text("{0} takes part as {1}.", "{0} nimmt teil als {1}.")
    val short = Text("Personnel: {0}.", "Personal: {0}.")
}

// RF1 sentence fragments. story/Index builds referral clauses from these.
internal object Rf1 {
    val full = Text("Referral {0} is {1}, priority {2}.", "Überweisung {0} ist {1}, Priorität {2}.")
    val status = Text("Referral status: {0}, priority {1}.", "Überweisungsstatus: {0}, Priorität {1}.")
    val statusOnly = Text("Referral status: {0}.", "Überweisungsstatus: {0}.")
    val type = Text("Referral type: {0}.", "Überweisungsart: {0}.")
    val disposition = Text("Disposition: {0}.", "Erwartetes Ergebnis: {0}.")
    val reason = Text("Reason: {0}.", "Grund: {0}.")
    val validity = Text("Effective on {0}, expires on {1}.", "Gültig ab {0}, bis {1}.")
    val effective = Text("Effective on {0}.", "Gültig ab {0}.")
    val processed = Text("Processed on {0}.", "Bearbeitet am {0}.")
}

// PRD sentence fragments. story/Index builds provider clauses from these.
internal object Prd {
    val full = Text("Provider {0} with the role {1}.", "Leistungserbringer {0} mit der Rolle {1}.")
    val short = Text("Provider {0}.", "Leistungserbringer {0}.")
    val address = Text("Provider address: {0}.", "Adresse: {0}.")
    val phone = Text("Provider phone: {0}.", "Telefon: {0}.")
}

// CTD sentence fragments. story/Index builds contact clauses from these.
internal object Ctd {
    val full = Text("Contact {0} ({1}).", "Kontakt {0} ({1}).")
    val short = Text("Contact {0}.", "Kontakt {0}.")
}

// RXA sentence fragments. story/Index builds administration clauses from these.
internal object Rxa {
    val full = Text("{0} was administered on {1}, dose {2} {3}.", "{0} wurde am {1} verabreicht, Dosis {2} {3}.")
    val date = Text("{0} was administered on {1}.", "{0} wurde am {1} verabreicht.")
    val short = Text("Administered: {0}.", "Verabreicht: {0}.")
    val lot = Text("Lot {0} made by {1}.", "Charge {0}, Hersteller {1}.")
    val lotOnly = Text("Lot {0}.", "Charge {0}.")
    val by = Text("Given by {0}.", "Verabreicht von {0}.")
    val completion = Text("Completion status: {0}.", "Status: {0}.")
    val action = Text("Action: {0}.", "Aktion: {0}.")
}

// RXR sentence fragments. story/Index builds route clauses from these.
internal object Rxr {
    val full = Text("Route {0}, site {1}.", "Weg {0}, Stelle {1}.")
    val short = Text("Route {0}.", "Weg {0}.")
}

// MSA sentence fragments. story/Index builds acknowledgement clauses from these.
internal object Msa {
    val full = Text("The message {0} was answered with {1}.", "Die Nachricht {0} wurde beantwortet mit: {1}.")
    val short = Text("Acknowledgment: {0}.", "Quittung: {0}.")
    val text = Text("Text: {0}.", "Text: {0}.")
    val error = Text("Error: {0}.", "Fehler: {0}.")
}

// ERR sentence fragments. story/Index builds error clauses from these.
internal object Err {
    val full = Text("Error {0}, severity {1}.", "Fehler {0}, Schwere {1}.")
    val code = Text("Error {0}.", "Fehler {0}.")
    val legacy = Text("Error location: {0}.", "Fehlerort: {0}.")
    val message = Text("Message: {0}.", "Meldung: {0}.")
}

// MRG sentence fragments. story/Index builds merge clauses from these.
internal object Mrg {
    val prior = Text("Merges prior patient identifier {0}.", "Führt die frühere Patientenkennung {0} zusammen.")
    val account = Text("Prior account number {0}.", "Frühere Fallnummer {0}.")
    val name = Text("Prior name: {0}.", "Früherer Name: {0}.")
}

// TQ1 sentence fragments. story/Index builds timing clauses from these.
internal object Tq1 {
    val start = Text("Starting {0}.", "Beginn {0}.")
    val end = Text("Ending {0}.", "Ende {0}.")
    val priority = Text("Priority: {0}.", "Priorität: {0}.")
}

// ROL sentence fragments. story/Index builds role clauses from these.
internal object Rol {
    val full = Text("{0} acts as {1}.", "{0} handelt als {1}.")
}
