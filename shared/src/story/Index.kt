// Plain-language reading of a message. Workspace shows it above the message.
package hl7lookup.story

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hl7lookup.controls.EmptyState
import hl7lookup.motion.IllustrationKind
import hl7lookup.document.FieldPath
import hl7lookup.document.ParsedMessage
import hl7lookup.i18n.tr
import hl7lookup.story.SlotFormat.ADDRESS
import hl7lookup.story.SlotFormat.AGE
import hl7lookup.story.SlotFormat.CODE
import hl7lookup.story.SlotFormat.DATE
import hl7lookup.story.SlotFormat.DAY
import hl7lookup.story.SlotFormat.IDENTIFIER
import hl7lookup.story.SlotFormat.LOCATION
import hl7lookup.story.SlotFormat.NAME
import hl7lookup.story.SlotFormat.PERSON
import hl7lookup.story.SlotFormat.PHONE
import hl7lookup.story.SlotFormat.TEXT
import hl7lookup.theme.LocalPalette

private val titleSentences = listOf(
    sentence(
        clause(Msh.full, slot(3, 1), slot(4, 1), slot(9, 1), slot(9, 2), slot(9, 2, CODE), slot(5, 1), slot(6, 1), slot(7, format = DATE)),
        clause(Msh.noFacilities, slot(3, 1), slot(9, 1), slot(9, 2), slot(9, 2, CODE), slot(5, 1), slot(7, format = DATE)),
        clause(Msh.noReceiver, slot(3, 1), slot(9, 1), slot(9, 2), slot(9, 2, CODE), slot(7, format = DATE)),
        clause(Msh.noSender, slot(9, 1), slot(9, 2), slot(9, 2, CODE), slot(7, format = DATE)),
        clause(Msh.typeEvent, slot(9, 1), slot(9, 2)),
        clause(Msh.typeOnly, slot(9, 1, CODE)),
    ),
    sentence(
        clause(Msh.control, slot(10), slot(12, 1), slot(11, 1, CODE)),
        clause(Msh.controlShort, slot(10), slot(12, 1)),
        clause(Msh.versionOnly, slot(12, 1)),
    ),
)

private val segmentSentences: Map<String, List<Sentence>> = mapOf(
    "MSH" to listOf(sentence(clause(Msh.acks, slot(15, format = CODE), slot(16, format = CODE)))),
    "EVN" to listOf(
        sentence(clause(Evn.recorded, slot(1, format = CODE), slot(2, format = DATE)), clause(Evn.recordedShort, slot(2, format = DATE))),
        sentence(clause(Evn.planned, slot(3, format = DATE))),
        sentence(clause(Evn.occurred, slot(6, format = DATE))),
        sentence(clause(Evn.reason, slot(4, format = CODE))),
        sentence(clause(Evn.operator, slot(5, format = PERSON))),
    ),
    "PID" to listOf(
        sentence(
            clause(Pid.full, slot(5, format = NAME), slot(8, format = CODE), slot(7, format = DAY), slot(7, format = AGE)),
            clause(Pid.noAge, slot(5, format = NAME), slot(8, format = CODE), slot(7, format = DAY)),
            clause(Pid.noSex, slot(5, format = NAME), slot(7, format = DAY)),
            clause(Pid.noBirth, slot(5, format = NAME), slot(8, format = CODE)),
            clause(Pid.nameOnly, slot(5, format = NAME)),
        ),
        sentence(
            clause(Pid.identifier, slot(3, 1, all = false), slot(3, 5, CODE, all = false), slot(3, 4, all = false)),
            clause(Pid.identifierType, slot(3, 1, all = false), slot(3, 5, CODE, all = false)),
            clause(Pid.identifierOnly, slot(3, 1)),
            clause(Pid.identifierOnly, slot(2, 1)),
        ),
        sentence(clause(Pid.address, slot(11, format = ADDRESS))),
        sentence(clause(Pid.homePhone, slot(13, format = PHONE))),
        sentence(clause(Pid.workPhone, slot(14, format = PHONE))),
        sentence(clause(Pid.marital, slot(16, format = CODE))),
        sentence(clause(Pid.religion, slot(17, format = CODE))),
        sentence(clause(Pid.race, slot(10, format = CODE))),
        sentence(clause(Pid.ethnic, slot(22, format = CODE))),
        sentence(clause(Pid.language, slot(15, format = CODE))),
        sentence(clause(Pid.mother, slot(6, format = NAME))),
        sentence(clause(Pid.account, slot(18, format = IDENTIFIER))),
        sentence(clause(Pid.death, slot(29, format = DATE))),
    ),
    "NK1" to listOf(
        sentence(clause(Nk1.relation, slot(2, format = NAME), slot(3, format = CODE)), clause(Nk1.nameOnly, slot(2, format = NAME))),
        sentence(clause(Nk1.phone, slot(5, format = PHONE))),
        sentence(clause(Nk1.address, slot(4, format = ADDRESS))),
        sentence(clause(Nk1.role, slot(7, format = CODE))),
    ),
    "PV1" to listOf(
        sentence(clause(Pv1.full, slot(2, format = CODE), slot(3, format = LOCATION)), clause(Pv1.classOnly, slot(2, format = CODE))),
        sentence(clause(Pv1.attending, slot(7, format = PERSON))),
        sentence(clause(Pv1.referring, slot(8, format = PERSON))),
        sentence(clause(Pv1.consulting, slot(9, format = PERSON))),
        sentence(clause(Pv1.admissionType, slot(4, format = CODE))),
        sentence(clause(Pv1.service, slot(10, format = CODE))),
        sentence(clause(Pv1.source, slot(14, format = CODE))),
        sentence(clause(Pv1.visit, slot(19, format = IDENTIFIER))),
        sentence(clause(Pv1.admitted, slot(44, format = DATE))),
        sentence(clause(Pv1.discharged, slot(45, format = DATE))),
        sentence(clause(Pv1.disposition, slot(36, format = CODE))),
        sentence(clause(Pv1.prior, slot(6, format = LOCATION))),
    ),
    "PV2" to listOf(
        sentence(clause(Pv2.reason, slot(3, format = CODE))),
        sentence(clause(Pv2.expectedAdmit, slot(8, format = DATE))),
        sentence(clause(Pv2.expectedDischarge, slot(9, format = DATE))),
    ),
    "AL1" to listOf(
        sentence(
            clause(Al1.full, slot(3, format = CODE), slot(2, format = CODE), slot(4, format = CODE), slot(5)),
            clause(Al1.typed, slot(3, format = CODE), slot(2, format = CODE)),
            clause(Al1.short, slot(3, format = CODE)),
        ),
    ),
    "DG1" to listOf(
        sentence(
            clause(Dg1.full, slot(3, format = CODE), slot(3, 1), slot(6, format = CODE), slot(5, format = DATE)),
            clause(Dg1.typed, slot(3, format = CODE), slot(3, 1), slot(6, format = CODE)),
            clause(Dg1.coded, slot(3, format = CODE), slot(3, 1)),
            clause(Dg1.described, slot(4)),
        ),
    ),
    "PR1" to listOf(sentence(clause(Pr1.full, slot(3, format = CODE), slot(5, format = DATE)), clause(Pr1.short, slot(3, format = CODE)))),
    "IN1" to listOf(
        sentence(
            clause(In1.full, slot(4, 1), slot(2, format = CODE), slot(36)),
            clause(In1.noPolicy, slot(4, 1), slot(2, format = CODE)),
            clause(In1.company, slot(4, 1)),
        ),
        sentence(clause(In1.insured, slot(16, format = NAME), slot(17, format = CODE)), clause(In1.insuredName, slot(16, format = NAME))),
        sentence(clause(In1.coverage, slot(12, format = DATE), slot(13, format = DATE)), clause(In1.coverageStart, slot(12, format = DATE))),
        sentence(clause(In1.group, slot(8))),
    ),
    "GT1" to listOf(
        sentence(clause(Gt1.full, slot(3, format = NAME), slot(11, format = CODE)), clause(Gt1.name, slot(3, format = NAME))),
        sentence(clause(Gt1.birth, slot(8, format = DAY))),
        sentence(clause(Gt1.address, slot(5, format = ADDRESS))),
        sentence(clause(Gt1.phone, slot(6, format = PHONE))),
    ),
    "ORC" to listOf(
        sentence(clause(Orc.control, slot(1, format = CODE))),
        sentence(clause(Orc.numbers, slot(2, 1), slot(3, 1)), clause(Orc.placer, slot(2, 1))),
        sentence(clause(Orc.status, slot(5, format = CODE))),
        sentence(
            clause(Orc.orderedBy, slot(9, format = DATE), slot(12, format = PERSON)),
            clause(Orc.orderedOn, slot(9, format = DATE)),
            clause(Orc.provider, slot(12, format = PERSON)),
        ),
        sentence(clause(Orc.enteredBy, slot(10, format = PERSON))),
    ),
    "OBR" to listOf(
        sentence(clause(Obr.service, slot(4, format = CODE), slot(4, 1)), clause(Obr.serviceShort, slot(4, format = CODE))),
        sentence(clause(Obr.numbers, slot(2, 1), slot(3, 1))),
        sentence(clause(Obr.requested, slot(6, format = DATE))),
        sentence(clause(Obr.observed, slot(7, format = DATE))),
        sentence(clause(Obr.provider, slot(16, format = PERSON))),
        sentence(clause(Obr.action, slot(11, format = CODE))),
        sentence(clause(Obr.reported, slot(22, format = DATE))),
        sentence(clause(Obr.status, slot(25, format = CODE))),
    ),
    "OBX" to listOf(
        sentence(
            clause(Obx.full, slot(3, format = CODE), slot(5, format = TEXT), slot(6, format = CODE), slot(7), slot(8, format = CODE)),
            clause(Obx.range, slot(3, format = CODE), slot(5, format = TEXT), slot(6, format = CODE), slot(7)),
            clause(Obx.units, slot(3, format = CODE), slot(5, format = TEXT), slot(6, format = CODE)),
            clause(Obx.value, slot(3, format = CODE), slot(5, format = TEXT)),
            clause(Obx.valueOnly, slot(5, format = TEXT)),
        ),
        sentence(clause(Obx.status, slot(11, format = CODE))),
        sentence(clause(Obx.observed, slot(14, format = DATE))),
    ),
    "NTE" to listOf(sentence(clause(Nte.note, slot(3, format = TEXT)))),
    "TXA" to listOf(
        sentence(clause(Txa.full, slot(2, format = CODE), slot(12, 1)), clause(Txa.type, slot(2, format = CODE))),
        sentence(clause(Txa.completion, slot(17, format = CODE))),
        sentence(clause(Txa.confidentiality, slot(18, format = CODE))),
        sentence(clause(Txa.availability, slot(19, format = CODE))),
        sentence(clause(Txa.storage, slot(20, format = CODE))),
        sentence(clause(Txa.activity, slot(4, format = DATE))),
        sentence(clause(Txa.origination, slot(6, format = DATE))),
        sentence(clause(Txa.transcribed, slot(7, format = DATE))),
        sentence(clause(Txa.author, slot(9, format = PERSON))),
        sentence(clause(Txa.transcriptionist, slot(11, format = PERSON))),
        sentence(clause(Txa.fileName, slot(16))),
    ),
    "SCH" to listOf(
        sentence(
            clause(Sch.full, slot(2, 1), slot(7, format = CODE), slot(25, format = CODE)),
            clause(Sch.reason, slot(2, 1), slot(7, format = CODE)),
            clause(Sch.status, slot(25, format = CODE)),
        ),
        sentence(clause(Sch.type, slot(8, format = CODE))),
        sentence(clause(Sch.duration, slot(9), slot(10, format = CODE))),
        sentence(clause(Sch.start, slot(11, 4, DATE))),
        sentence(clause(Sch.contact, slot(12, format = PERSON))),
        sentence(clause(Sch.enteredBy, slot(20, format = PERSON))),
    ),
    "AIS" to listOf(
        sentence(
            clause(Ais.full, slot(3, format = CODE), slot(4, format = DATE), slot(7), slot(8, format = CODE)),
            clause(Ais.start, slot(3, format = CODE), slot(4, format = DATE)),
            clause(Ais.short, slot(3, format = CODE)),
        ),
    ),
    "AIG" to listOf(
        sentence(clause(Aig.full, slot(3, format = CODE), slot(4, format = CODE)), clause(Aig.short, slot(3, format = CODE))),
        sentence(clause(Aig.start, slot(8, format = DATE))),
    ),
    "AIL" to listOf(sentence(clause(Ail.full, slot(3, format = LOCATION), slot(6, format = DATE)), clause(Ail.short, slot(3, format = LOCATION)))),
    "AIP" to listOf(
        sentence(
            clause(Aip.full, slot(3, format = PERSON), slot(4, format = CODE), slot(6, format = DATE)),
            clause(Aip.role, slot(3, format = PERSON), slot(4, format = CODE)),
            clause(Aip.short, slot(3, format = PERSON)),
        ),
    ),
    "RF1" to listOf(
        sentence(
            clause(Rf1.full, slot(6, 1), slot(1, format = CODE), slot(2, format = CODE)),
            clause(Rf1.status, slot(1, format = CODE), slot(2, format = CODE)),
            clause(Rf1.statusOnly, slot(1, format = CODE)),
        ),
        sentence(clause(Rf1.type, slot(3, format = CODE))),
        sentence(clause(Rf1.disposition, slot(4, format = CODE))),
        sentence(clause(Rf1.reason, slot(10, format = CODE))),
        sentence(clause(Rf1.validity, slot(7, format = DATE), slot(8, format = DATE)), clause(Rf1.effective, slot(7, format = DATE))),
        sentence(clause(Rf1.processed, slot(9, format = DATE))),
    ),
    "PRD" to listOf(
        sentence(clause(Prd.full, slot(2, format = NAME), slot(1, format = CODE)), clause(Prd.short, slot(2, format = NAME))),
        sentence(clause(Prd.address, slot(3, format = ADDRESS))),
        sentence(clause(Prd.phone, slot(5, format = PHONE))),
    ),
    "CTD" to listOf(sentence(clause(Ctd.full, slot(2, format = NAME), slot(1, format = CODE)), clause(Ctd.short, slot(2, format = NAME)))),
    "RXA" to listOf(
        sentence(
            clause(Rxa.full, slot(5, format = CODE), slot(3, format = DATE), slot(6), slot(7, format = CODE)),
            clause(Rxa.date, slot(5, format = CODE), slot(3, format = DATE)),
            clause(Rxa.short, slot(5, format = CODE)),
        ),
        sentence(clause(Rxa.lot, slot(15), slot(17, format = CODE)), clause(Rxa.lotOnly, slot(15))),
        sentence(clause(Rxa.by, slot(10, format = PERSON))),
        sentence(clause(Rxa.completion, slot(20, format = CODE))),
        sentence(clause(Rxa.action, slot(21, format = CODE))),
    ),
    "RXR" to listOf(sentence(clause(Rxr.full, slot(1, format = CODE), slot(2, format = CODE)), clause(Rxr.short, slot(1, format = CODE)))),
    "MSA" to listOf(
        sentence(clause(Msa.full, slot(2), slot(1, format = CODE)), clause(Msa.short, slot(1, format = CODE))),
        sentence(clause(Msa.text, slot(3, format = TEXT))),
        sentence(clause(Msa.error, slot(6, format = CODE))),
    ),
    "ERR" to listOf(
        sentence(clause(Err.full, slot(3, format = CODE), slot(4, format = CODE)), clause(Err.code, slot(3, format = CODE)), clause(Err.legacy, slot(1, format = TEXT))),
        sentence(clause(Err.message, slot(8, format = TEXT))),
    ),
    "MRG" to listOf(
        sentence(clause(Mrg.prior, slot(1, 1))),
        sentence(clause(Mrg.account, slot(3, format = IDENTIFIER))),
        sentence(clause(Mrg.name, slot(7, format = NAME))),
    ),
    "TQ1" to listOf(
        sentence(clause(Tq1.start, slot(7, format = DATE))),
        sentence(clause(Tq1.end, slot(8, format = DATE))),
        sentence(clause(Tq1.priority, slot(9, format = CODE))),
    ),
    "ROL" to listOf(sentence(clause(Rol.full, slot(4, format = PERSON), slot(3, format = CODE)))),
)

// Facade that builds and flattens stories. Workspace and StoryTest call it.
object Stories {
    // Renders a message into title and paragraphs. Workspace asks Stories.build for the pane.
    fun build(message: ParsedMessage, context: StoryContext): Story =
        buildStory(message, context, segmentSentences, titleSentences, storyWords, locationWords)
    // Flattens a story to plain text. Copy actions and tests call it.
    fun plain(story: Story): String = plainText(story)
    // Segment names that have narrated templates. Workspace and tests ask which segments are covered.
    fun narratedSegments(): Set<String> = segmentSentences.keys
    // Exposes story wording. Workspace reads it via tr().
    fun texts() = StoryTexts
}

// Clickable story paragraphs with highlights. Workspace shows it above the message.
@Composable
fun StoryView(
    story: Story?,
    modifier: Modifier = Modifier,
    isSelected: (FieldPath) -> Boolean,
    highlight: (FieldPath) -> Color?,
    flagged: (FieldPath) -> Color?,
    onPick: (FieldPath) -> Unit,
) {
    val palette = LocalPalette.current
    if (story == null || (story.title.isEmpty() && story.paragraphs.isEmpty())) {
        EmptyState(tr(StoryTexts.empty), modifier, illustration = IllustrationKind.STORY)
        return
    }
    // Turns story pieces into an AnnotatedString with links. StoryView calls it for title and body.
    fun render(pieces: List<StoryPiece>, strong: Boolean): AnnotatedString = buildAnnotatedString {
        for (piece in pieces) {
            when (piece) {
                is StoryPiece.Words -> append(piece.text)
                is StoryPiece.Value -> {
                    val selected = isSelected(piece.path)
                    val mark = highlight(piece.path)
                    val issue = flagged(piece.path)
                    val style = SpanStyle(
                        color = mark ?: palette.link,
                        fontWeight = if (strong || selected) FontWeight.SemiBold else FontWeight.Normal,
                        background = when {
                            selected -> palette.selected
                            issue != null -> issue.copy(alpha = 0.22f)
                            else -> Color.Transparent
                        },
                    )
                    withLink(LinkAnnotation.Clickable("${piece.path}", TextLinkStyles(style = style)) { onPick(piece.path) }) {
                        append(piece.text)
                    }
                }
            }
        }
    }
    SelectionContainer(modifier) {
        Column(
            Modifier.fillMaxSize().background(palette.surface).verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (story.title.isNotEmpty()) {
                Text(render(story.title, strong = true), color = palette.text, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.fillMaxWidth())
            }
            for (paragraph in story.paragraphs) {
                Text(render(paragraph.pieces, strong = false), color = palette.text, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
