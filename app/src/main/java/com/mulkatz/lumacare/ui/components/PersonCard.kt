package com.mulkatz.lumacare.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.mulkatz.lumacare.R
import com.mulkatz.lumacare.domain.Activity
import com.mulkatz.lumacare.domain.ContextChip
import com.mulkatz.lumacare.domain.EventAction
import com.mulkatz.lumacare.domain.EventEntry
import com.mulkatz.lumacare.domain.EventStatus
import com.mulkatz.lumacare.domain.EventType
import com.mulkatz.lumacare.domain.Handling
import com.mulkatz.lumacare.domain.LumaEvent
import com.mulkatz.lumacare.domain.PersonEntry
import com.mulkatz.lumacare.domain.Resident
import com.mulkatz.lumacare.domain.Routine
import com.mulkatz.lumacare.domain.Severity
import com.mulkatz.lumacare.ui.theme.LumaSizes
import com.mulkatz.lumacare.ui.theme.LumaTheme
import com.mulkatz.lumacare.ui.theme.tabularNumbers
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

/** Höchstens so viele Kontext-Chips pro Karte; der Rest erscheint als „+n“. */
const val MAX_CONTEXT_CHIPS = 2

/** Sichtbare Chips einer Karte und die Anzahl der ausgeblendeten. */
data class ChipSelection(val shown: List<ContextChip>, val hiddenCount: Int)

/**
 * Die ersten [max] Kontext-Chips in der Reihenfolge des Prioritizers, der Rest als Anzahl.
 * Weitere Hinweise stehen als Text unter dem Hinweis, nicht als Chip.
 */
fun selectContextChips(context: List<ContextChip>, max: Int = MAX_CONTEXT_CHIPS): ChipSelection {
    val candidates = context.filterNot { it is ContextChip.FurtherEvents }
    return ChipSelection(candidates.take(max), (candidates.size - max).coerceAtLeast(0))
}

/**
 * Personenkarte mit dem maßgeblichen Hinweis. [highlighted] (nur „Jetzt handeln“) zeigt den Streifen links,
 * das Dringlichkeits-Badge und die Aktion gefüllt; sonst ist die Aktion tonal. Die Karte ist eine
 * TalkBack-Ansage; die Aktion bleibt ein eigenes Tap-Ziel.
 *
 * Rand: hervorgehoben ohne grauen Rand (der Streifen ist die Kante), sonst `outlineVariant`; gewählt immer
 * 2 dp `primary`, beim Streifen über ihm gezeichnet, damit keine doppelte Kante entsteht.
 */
@Composable
fun PersonCard(
    entry: PersonEntry,
    now: LocalDateTime,
    highlighted: Boolean,
    onClick: () -> Unit,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    /** Gewählt im List-Detail (Tablet): Rand `primary` 2 dp, Semantik `selected`. */
    selected: Boolean = false,
    /** `false`, solange eine Aktion zu diesem Hinweis gespeichert wird (kein Doppeltipp). */
    actionEnabled: Boolean = true,
) {
    val res = LocalResources.current
    val resident = entry.resident
    val lead = entry.leadEvent
    val severity = lead.event.severity
    val stripeColor = severity.colors.content
    val chips = selectContextChips(entry.context)
    val moreHints = entry.events.size - 1

    val metaSpoken = stringResource(R.string.person_meta_spoken, resident.room, resident.careLevel)
    val severityLabel = stringResource(severity.labelRes)
    val hintCount = res.getQuantityString(R.plurals.hint_count, entry.events.size, entry.events.size)
    val hintMeta = hintMetaText(lead, now)
    val chipTexts = chips.shown.map { it.text() }
    val chipTextsSpoken = chips.shown.map { it.spokenText() }
    val fontScale = LocalDensity.current.fontScale
    val chipMaxLines = if (fontScale > 1f) 2 else 1
    val moreChipsSpoken = res.getQuantityString(R.plurals.chip_more_spoken, chips.hiddenCount, chips.hiddenCount)
    val description = buildList {
        add(resident.fullName)
        add(metaSpoken)
        add(severityLabel)
        add(hintCount)
        add(hintMeta)
        add(lead.event.summary)
        addAll(chipTextsSpoken)
        if (chips.hiddenCount > 0) add(moreChipsSpoken)
    }.joinToString(separator = ", ")

    val shape = MaterialTheme.shapes.medium
    val primary = MaterialTheme.colorScheme.primary
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("person_card_${resident.id}")
            .semantics {
                contentDescription = description
                if (selected) this.selected = true
            }
            .then(if (highlighted) Modifier.leadingStripe(stripeColor, shape, selectedColor = primary.takeIf { selected }) else Modifier),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = when {
            highlighted -> null
            selected -> BorderStroke(LumaSizes.selectedBorder, primary)
            else -> BorderStroke(LumaSizes.border, MaterialTheme.colorScheme.outlineVariant)
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LumaTheme.spacing.l),
            verticalArrangement = Arrangement.spacedBy(LumaTheme.spacing.m),
        ) {
            // Pflegegrad nur in der Ansage und im Detail; lange Namen brechen um, das Badge rutscht bei
            // Platzmangel (große Schrift) unter den Namen.
            // Ohne weight: Passt der Name nicht neben das Badge, nimmt er die volle Breite und das Badge rutscht
            // in die nächste Zeile; passt beides, steht das Badge rechts (SpaceBetween, Mindestabstand am Namen).
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(LumaTheme.spacing.s),
            ) {
                Text(
                    text = stringResource(R.string.name_and_room, resident.fullName, resident.room),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(end = LumaTheme.spacing.s),
                )
                if (highlighted) SeverityBadge(severity)
            }

            Column(verticalArrangement = Arrangement.spacedBy(LumaTheme.spacing.xs)) {
                HintMeta(lead, hintMeta)
                // Hinweis statt Diagnose: Originalaussage unverändert; vollständig in der Ansage und im Detail.
                // Bei großer Schrift nie gekürzt.
                Text(
                    text = lead.event.summary,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = if (fontScale <= 1f) SUMMARY_MAX_LINES else Int.MAX_VALUE,
                    overflow = TextOverflow.Ellipsis,
                )
                if (moreHints > 0) {
                    Text(
                        text = res.getQuantityString(R.plurals.more_hints, moreHints, moreHints),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (chips.shown.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(LumaTheme.spacing.s),
                    verticalArrangement = Arrangement.spacedBy(LumaTheme.spacing.s),
                ) {
                    chipTexts.zip(chips.shown).forEach { (text, chip) -> InfoChip(text, chip.iconRes(), maxLines = chipMaxLines) }
                    if (chips.hiddenCount > 0) InfoChip(stringResource(R.string.chip_more, chips.hiddenCount), null)
                }
            }

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                EventActionButton(
                    action = lead.action,
                    onClick = onAction,
                    enabled = actionEnabled,
                    emphasis = if (highlighted) ActionEmphasis.FILLED else ActionEmphasis.TONAL,
                )
            }
        }
    }
}

/** Hinweistext auf der Karte bei Schrift ≤ 100 %; darüber unbegrenzt. */
private const val SUMMARY_MAX_LINES = 3

/**
 * Streifen links in [color] (4 dp), auf die Kartenform [shape] geclippt, sodass er der Rundung folgt.
 * Mit [selectedColor] liegt darüber der 2-dp-Auswahlrand; der Streifen wird um dessen Breite verbreitert und
 * bleibt so innen sichtbar.
 */
private fun Modifier.leadingStripe(color: Color, shape: Shape, selectedColor: Color?): Modifier = drawWithContent {
    drawContent()
    val outline = shape.createOutline(size, layoutDirection, this)
    val border = if (selectedColor != null) LumaSizes.selectedBorder.toPx() else 0f
    clipPath(Path().apply { addOutline(outline) }) {
        drawRect(color, size = Size(LumaSizes.stripe.toPx() + border, size.height))
    }
    if (selectedColor != null) {
        inset(border / 2) {
            drawOutline(shape.createOutline(size, layoutDirection, this), selectedColor, style = Stroke(width = border))
        }
    }
}

/**
 * Metazeile des Hinweises: „vor 45 Min. · 06:30“, „Aus Nachtdienst · gestern 23:15“ bzw. beim eigenen
 * Fall „Bei dir · vor 45 Min. · 06:30“.
 */
@Composable
private fun hintMetaText(entry: EventEntry, now: LocalDateTime): String {
    val res = LocalResources.current
    val time = entry.event.timestamp
    return when {
        entry.isFromNightShift ->
            res.getString(R.string.hint_meta, res.getString(R.string.handling_from_night), TimeFormat.clockLabel(res, time, now))
        entry.handling == Handling.OWN ->
            res.getString(R.string.hint_meta, res.getString(R.string.handling_own), TimeFormat.ageAndClock(res, time, now))
        else -> TimeFormat.ageAndClock(res, time, now)
    }
}

@Composable
private fun HintMeta(entry: EventEntry, text: String) {
    val style = MaterialTheme.typography.bodyMedium.tabularNumbers()
    Row(horizontalArrangement = Arrangement.spacedBy(LumaTheme.spacing.xs)) {
        val icon = when {
            entry.isFromNightShift -> R.drawable.ic_moon
            entry.handling == Handling.OWN -> R.drawable.ic_person
            else -> null
        }
        if (icon != null) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(top = firstLineIconPadding(style))
                    .size(LumaSizes.iconSmall),
            )
        }
        Text(text = text, style = style, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Sichtbarer Chip-Text; die Routine erkennt man am Icon. */
@Composable
fun ContextChip.text(): String = when (this) {
    is ContextChip.RoutineNearby -> stringResource(R.string.chip_routine, TimeFormat.clock(time), routine.title)
    else -> spokenText()
}

/** Chip-Text für TalkBack, vollständig: „Routine 07:00 Blutzucker messen vor dem Frühstück“. */
@Composable
fun ContextChip.spokenText(): String = when (this) {
    is ContextChip.HealthNote -> text
    is ContextChip.RoutineNearby -> stringResource(R.string.chip_routine_spoken, TimeFormat.clock(time), routine.title)
    is ContextChip.ActivityAhead -> stringResource(R.string.chip_activity, activity.title, TimeFormat.clock(activity.start))
    ContextChip.LumaNotReached -> stringResource(R.string.chip_not_reached)
    is ContextChip.FurtherEvents -> LocalResources.current.getQuantityString(R.plurals.more_hints, count, count)
}

@DrawableRes
private fun ContextChip.iconRes(): Int? = when (this) {
    is ContextChip.HealthNote, is ContextChip.FurtherEvents -> null
    is ContextChip.RoutineNearby -> R.drawable.ic_repeat
    is ContextChip.ActivityAhead -> R.drawable.ic_group
    ContextChip.LumaNotReached -> R.drawable.ic_missed
}

/** Neutraler Kontext-Chip: Rand `outline`, Text in `labelLarge`, [maxLines] Zeilen (bei großer Schrift 2), Icon optional. */
@Composable
fun InfoChip(text: String, @DrawableRes icon: Int?, modifier: Modifier = Modifier, maxLines: Int = 1) {
    val style = MaterialTheme.typography.labelLarge.tabularNumbers()
    Row(
        modifier = modifier
            .border(LumaSizes.border, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small)
            .padding(horizontal = LumaTheme.spacing.s, vertical = LumaTheme.spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(LumaTheme.spacing.xs),
    ) {
        if (icon != null) {
            // Oben auf Höhe der ersten Zeile, auch wenn der Chip bei großer Schrift zweizeilig wird.
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(top = firstLineIconPadding(style))
                    .size(LumaSizes.iconSmall),
            )
        }
        Text(
            text = text,
            style = style,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
}

@get:StringRes
val EventAction.labelRes: Int
    get() = when (this) {
        EventAction.TAKE_OVER -> R.string.action_take_over
        EventAction.CONFIRM_HANDOVER -> R.string.action_confirm_handover
        EventAction.RESOLVE -> R.string.action_resolve
        EventAction.FORWARD -> R.string.action_forward
        EventAction.ACKNOWLEDGE -> R.string.action_acknowledge
    }

/**
 * testTag-Name der Aktion: `action_take_over`, `action_confirm_handover`, `action_resolve`, `action_forward`,
 * `action_acknowledge`.
 */
val EventAction.testTag: String get() = "action_${name.lowercase()}"

/** Gewicht einer Aktion: gefüllt (Jetzt handeln, Aktionsleiste), tonal (Zeitnah prüfen), umrandet, Text. */
enum class ActionEmphasis { FILLED, TONAL, OUTLINED, TEXT }

/**
 * Aktion eines Hinweises in der Gewichtung [emphasis], mindestens 48 dp hoch. „Weiterleiten“ ist außerhalb der
 * Aktionsleiste immer umrandet und trägt überall den Pfeil. [enabled] `false` sperrt sie, solange gespeichert wird.
 */
@Composable
fun EventActionButton(
    action: EventAction,
    onClick: () -> Unit,
    emphasis: ActionEmphasis,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val buttonModifier = modifier
        .heightIn(min = LumaSizes.minTouchTarget)
        .testTag(action.testTag)
    val effective = if (action == EventAction.FORWARD && emphasis != ActionEmphasis.FILLED) ActionEmphasis.OUTLINED else emphasis
    val content: @Composable RowScope.() -> Unit = {
        if (action == EventAction.FORWARD) {
            Icon(
                painter = painterResource(R.drawable.ic_forward),
                contentDescription = null,
                modifier = Modifier
                    .padding(end = LumaTheme.spacing.s)
                    .size(LumaSizes.iconSmall),
            )
        }
        Text(text = stringResource(action.labelRes))
    }
    when (effective) {
        ActionEmphasis.FILLED -> Button(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            content = content,
        )
        ActionEmphasis.TONAL -> FilledTonalButton(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
            content = content,
        )
        ActionEmphasis.OUTLINED -> LumaOutlinedButton(onClick = onClick, modifier = buttonModifier, enabled = enabled, content = content)
        ActionEmphasis.TEXT -> TextButton(onClick = onClick, modifier = buttonModifier, enabled = enabled, content = content)
    }
}

// ---------- Previews ----------

private val previewNow = LocalDateTime.of(2026, 10, 12, 7, 15)

private val previewWerner = PersonEntry(
    resident = Resident(
        id = "r04", firstName = "Werner", lastName = "Hoffmann", age = 79, room = "2.03", careLevel = 3,
        biography = emptyList(), preferences = emptyList(),
        healthNotes = listOf("Diabetes Typ 2, achtet auf Zucker"), contactName = null, contactRelation = null,
    ),
    events = listOf(
        EventEntry(
            event = LumaEvent(
                id = "e005", residentId = "r04", timestamp = previewNow.withHour(6).withMinute(30),
                type = EventType.HEALTH_HINT, severity = Severity.URGENT, status = EventStatus.OPEN,
                summary = "Berichtet von Zittern und Schwitzen, hat laut eigener Aussage noch nicht gefrühstückt.",
                assignedTo = null, resolution = null, updatedAt = previewNow.withHour(6).withMinute(30),
            ),
            handling = Handling.UNATTENDED,
            action = EventAction.TAKE_OVER,
        ),
    ),
    context = listOf(
        ContextChip.HealthNote("Diabetes Typ 2, achtet auf Zucker"),
        ContextChip.RoutineNearby(
            Routine("rt02", "r04", "Blutzucker messen vor dem Frühstück", LocalTime.of(7, 0), DayOfWeek.entries.toSet(), true),
            previewNow.withHour(7).withMinute(0),
        ),
        ContextChip.ActivityAhead(Activity("a01", "Sitzgymnastik", previewNow.withHour(10).withMinute(30), "Gemeinschaftsraum", listOf("r04"))),
    ),
)

@Preview(name = "Light", widthDp = 380)
@Composable
private fun PersonCardPreview() {
    LumaTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            PersonCard(previewWerner, previewNow, highlighted = true, onClick = {}, onAction = {}, Modifier.padding(LumaTheme.spacing.l))
        }
    }
}

@Preview(name = "Dark", widthDp = 380)
@Composable
private fun PersonCardDarkPreview() {
    LumaTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            PersonCard(previewWerner, previewNow, highlighted = true, onClick = {}, onAction = {}, Modifier.padding(LumaTheme.spacing.l))
        }
    }
}

@Preview(name = "Schrift 200 %", widthDp = 380, fontScale = 2f)
@Composable
private fun PersonCardLargeFontPreview() {
    LumaTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            PersonCard(previewWerner, previewNow, highlighted = false, onClick = {}, onAction = {}, Modifier.padding(LumaTheme.spacing.l))
        }
    }
}
