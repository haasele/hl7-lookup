package hl7lookup.samples

import hl7lookup.i18n.Text

object SampleTexts {
    val menu = Text("Samples", "Beispiele")
    val openAll = Text("Open all samples", "Alle Beispiele öffnen")
    val tabTitle = Text("Samples", "Beispiele")
}

internal class Sample(val id: String, val title: Text, val lines: List<String>)

internal val samples = listOf(
    Sample(
        "adt-a01",
        Text("ADT^A01 – Admission", "ADT^A01 – Aufnahme"),
        listOf(
            "MSH|^~\\&|WARDSYS|NORTHCLINIC|HISCORE|CENTRAL|20260301083015+0100||ADT^A01^ADT_A01|NC20260301-0001|P|2.5|||AL|NE|DE||DE",
            "EVN|A01|20260301083000+0100|||J.WEBER^Weber^Jana^^^^Nurse",
            "PID|1||4711023^^^NORTHCLINIC^MR~DE7719003412^^^AOK^NI||Lindqvist^Mara^Johanna^^^^L||19790412|F|||Birkenweg 3^^Kiel^SH^24103^DE^H||^PRN^PH^^49^431^5550199~^NET^Internet^mara.lindqvist@example.org|^WPN^PH^^49^431^5550788|DE|M|CHR|NC-CASE-88012||||||||||||N",
            "PD1|||Praxis am Hafen^^7710",
            "NK1|1|Lindqvist^Erik^^^^^L|SPO^Spouse^HL70063|Birkenweg 3^^Kiel^SH^24103^DE^H|^PRN^PH^^49^431^5550199||EMC",
            "PV1|1|I|STA3^12^2^NORTHCLINIC||||0815^Okafor^Daniel^^^Dr.^^^NORTHCLINIC|0921^Brandt^Ilse^^^Dr.||MED||||7|A0||0815^Okafor^Daniel^^^Dr.||NC-VISIT-55601|||||||||||||||||||||||||20260301082500+0100",
            "PV2|||R07.4^Chest pain, unspecified^I10",
            "AL1|1|DA|70618^Penicillin^RXNORM|SV|Hives",
            "DG1|1||I20.0^Unstable angina^I10||20260301084000+0100|A",
            "IN1|1|AOK-STD^Standard^L|104212505|AOK Nordwest||||G-2291||||20250101|20261231|||Lindqvist^Mara^Johanna|SEL|19790412||||||||||||||||||DE7719003412",
        ),
    ),
    Sample(
        "adt-a08",
        Text("ADT^A08 – Update patient information", "ADT^A08 – Patientendaten ändern"),
        listOf(
            "MSH|^~\\&|REGDESK|NORTHCLINIC|HISCORE|CENTRAL|20260302101244||ADT^A08^ADT_A01|NC20260302-0417|P|2.5",
            "EVN|A08|20260302101240",
            "PID|1||4711023^^^NORTHCLINIC^MR||Lindqvist^Mara^Johanna^^^^L||19790412|F|||Möwenstraße 18^^Kiel^SH^24105^DE^H||^PRN^CP^^49^171^5550143|||M",
            "PV1|1|I|STA3^14^1^NORTHCLINIC||||0815^Okafor^Daniel^^^Dr.",
        ),
    ),
    Sample(
        "orm-o01",
        Text("ORM^O01 – Laboratory order", "ORM^O01 – Laborauftrag"),
        listOf(
            "MSH|^~\\&|CPOE|NORTHCLINIC|LABSYS|CENTRALLAB|20260301091530||ORM^O01^ORM_O01|NC20260301-0093|P|2.4",
            "PID|1||4711023^^^NORTHCLINIC^MR||Lindqvist^Mara^Johanna||19790412|F",
            "PV1|1|I|STA3^12^2",
            "ORC|NW|ORD-448120^CPOE|||SC||^^^20260301093000^^R||20260301091500|J.WEBER^Weber^Jana||0815^Okafor^Daniel^^^Dr.",
            "OBR|1|ORD-448120^CPOE||10839-9^Troponin I cardiac^LN|R|20260301091500|20260301092000||||L||||SER&Serum&HL70070|0815^Okafor^Daniel^^^Dr.",
            "NTE|1|L|Please call the ward when the value exceeds the cut-off.",
            "ORC|NW|ORD-448121^CPOE|||SC||^^^20260301093000^^R||20260301091500|J.WEBER^Weber^Jana||0815^Okafor^Daniel^^^Dr.",
            "OBR|2|ORD-448121^CPOE||2951-2^Sodium^LN|R|20260301091500|20260301092000||||L",
        ),
    ),
    Sample(
        "oru-r01",
        Text("ORU^R01 – Laboratory results", "ORU^R01 – Laborbefund"),
        listOf(
            "MSH|^~\\&|LABSYS|CENTRALLAB|HISCORE|NORTHCLINIC|20260301104512||ORU^R01^ORU_R01|LAB-778120|P|2.5.1",
            "PID|1||4711023^^^NORTHCLINIC^MR||Lindqvist^Mara^Johanna||19790412|F",
            "PV1|1|I|STA3^12^2",
            "ORC|RE|ORD-448120^CPOE|LAB-448120^LABSYS||CM",
            "OBR|1|ORD-448120^CPOE|LAB-448120^LABSYS|10839-9^Troponin I cardiac^LN|||20260301092000|||||||||0815^Okafor^Daniel^^^Dr.||||||20260301104400|||F",
            "OBX|1|NM|10839-9^Troponin I cardiac^LN||0.42|ng/mL^nanogram per milliliter^UCUM|<0.04|HH|||F|||20260301103800",
            "NTE|1|L|Critical value phoned to the ward at 10:41 and read back.",
            "OBR|2|ORD-448121^CPOE|LAB-448121^LABSYS|2951-2^Sodium^LN|||20260301092000||||||||||||||||20260301104400|||F",
            "OBX|1|NM|2951-2^Sodium^LN||138|mmol/L^millimole per liter^UCUM|136-145|N|||F|||20260301103500",
            "OBX|2|NM|2823-3^Potassium^LN||5.4|mmol/L^millimole per liter^UCUM|3.5-5.1|H|||F|||20260301103500",
        ),
    ),
    Sample(
        "mdm-t02",
        Text("MDM^T02 – Discharge letter", "MDM^T02 – Entlassbrief"),
        listOf(
            "MSH|^~\\&|DOCWRITER|NORTHCLINIC|ARCHIVE|CENTRAL|20260306154210||MDM^T02^MDM_T02|DW-20260306-0071|P|2.5",
            "EVN|T02|20260306154200",
            "PID|1||4711023^^^NORTHCLINIC^MR||Lindqvist^Mara^Johanna||19790412|F",
            "PV1|1|I|STA3^14^1||||0815^Okafor^Daniel^^^Dr.",
            "TXA|1|DS|TX|20260306150000|0815^Okafor^Daniel^^^Dr.|20260306143000|20260306153500||0815^Okafor^Daniel^^^Dr.||C.MEYER^Meyer^Clara|DOC-2026-5512^DOCWRITER||||discharge_4711023.pdf|LA|U|AV|AC",
            "OBX|1|TX|18842-5^Discharge summary^LN||Admitted with unstable angina. Coronary angiography showed a single-vessel stenosis, treated with one drug-eluting stent. Discharged in stable condition.||||||F",
            "OBX|2|TX|18842-5^Discharge summary^LN||Follow-up with the general practitioner in seven days; continue dual antiplatelet therapy for twelve months.||||||F",
        ),
    ),
    Sample(
        "siu-s12",
        Text("SIU^S12 – New appointment", "SIU^S12 – Neuer Termin"),
        listOf(
            "MSH|^~\\&|SCHEDULER|NORTHCLINIC|HISCORE|CENTRAL|20260310081122||SIU^S12^SIU_S12|SCH-99102|P|2.5",
            "SCH|APT-20260318-01^SCHEDULER|APT-20260318-01^SCHEDULER||||NEW^New appointment^L|FOLLOWUP^Follow up visit^HL70276|Normal^Routine^HL70277|30|min^minutes^UCUM|^^30^20260318093000^20260318100000|||||0815^Okafor^Daniel^^^Dr.||||J.WEBER^Weber^Jana|||||Booked",
            "PID|1||4711023^^^NORTHCLINIC^MR||Lindqvist^Mara^Johanna||19790412|F",
            "RGS|1|A",
            "AIS|1|A|CARDIO-FU^Cardiology follow-up^L|20260318093000|||30|min^minutes^UCUM",
            "AIL|1|A|CARDIO^201^1^NORTHCLINIC|||20260318093000",
            "AIP|1|A|0815^Okafor^Daniel^^^Dr.|ATT^Attending^L||20260318093000",
        ),
    ),
    Sample(
        "ref-i12",
        Text("REF^I12 – Referral", "REF^I12 – Überweisung"),
        listOf(
            "MSH|^~\\&|PRACTICE|HAFEN7710|NORTHCLINIC|CENTRAL|20260228162033||REF^I12^REF_I12|PR-20260228-14|P|2.5",
            "RF1|P^Pending^HL70283|S^STAT^HL70280|Med^Medical^HL70281|SO^Second Opinion^HL70282||RF-7710-3321|20260228|20260331|20260228|O^Provider ordered^HL70336",
            "PRD|RP^Referring provider^HL70286|Holm^Anja^^^^Dr.|Hafenstraße 2^^Kiel^SH^24103^DE||^WPN^PH^^49^431^5550600",
            "PRD|RT^Referred to provider^HL70286|Okafor^Daniel^^^^Dr.|Klinikring 1^^Kiel^SH^24106^DE",
            "PID|1||4711023^^^NORTHCLINIC^MR||Lindqvist^Mara^Johanna||19790412|F|||Birkenweg 3^^Kiel^SH^24103^DE",
            "DG1|1||R07.4^Chest pain, unspecified^I10||20260228|W",
        ),
    ),
    Sample(
        "vxu-v04",
        Text("VXU^V04 – Vaccination record", "VXU^V04 – Impfdokumentation"),
        listOf(
            "MSH|^~\\&|VACCIDOC|HAFEN7710|REGISTRY|STATE|20260922111405||VXU^V04^VXU_V04|VX-20260922-3|P|2.5.1|||ER|AL",
            "PID|1||4711023^^^NORTHCLINIC^MR||Lindqvist^Mara^Johanna||19790412|F|||Birkenweg 3^^Kiel^SH^24103^DE",
            "ORC|RE||VX-99812^VACCIDOC",
            "RXA|0|1|20260922110500|20260922110500|141^Influenza, seasonal, injectable^CVX|0.5|mL^milliliter^UCUM||00^New immunization record^NIP001|H.BERG^Berg^Hanna^^^^RN|^^^HAFEN7710||||FLU2026-44A|20270630|SKB^GlaxoSmithKline^MVX|||CP|A",
            "RXR|IM^Intramuscular^HL70162|LD^Left deltoid^HL70163",
        ),
    ),
    Sample(
        "ack",
        Text("ACK – Application acknowledgment", "ACK – Anwendungsquittung"),
        listOf(
            "MSH|^~\\&|HISCORE|CENTRAL|WARDSYS|NORTHCLINIC|20260301083017||ACK^A01^ACK|HC-ACK-551203|P|2.5",
            "MSA|AE|NC20260301-0001|Bed STA3-12-2 is blocked for cleaning",
            "ERR||PV1^1^3|207^Application internal error^HL70357|E",
        ),
    ),
)
