package com.mulkatz.lumacare.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.max
import com.mulkatz.lumacare.R
import com.mulkatz.lumacare.domain.HourBlock
import com.mulkatz.lumacare.domain.HourPhase
import com.mulkatz.lumacare.domain.NowMarker
import com.mulkatz.lumacare.domain.Routine
import com.mulkatz.lumacare.domain.Resident
import com.mulkatz.lumacare.domain.Severity
import com.mulkatz.lumacare.domain.ShiftHours
import com.mulkatz.lumacare.domain.TimelineRoutine
import com.mulkatz.lumacare.domain.TimelineSummary
import com.mulkatz.lumacare.ui.theme.LumaSizes
import com.mulkatz.lumacare.ui.theme.LumaTheme
import com.mulkatz.lumacare.ui.theme.tabularNumbers
import java.time.DayOfWeek
import java.time.LocalDateTime

/**
 * Karte „Schichtverlauf ›“ mit Stundenblöcken. Die ganze Karte ist ein Tap-Ziel; einzelne Blöcke nicht.
 * TalkBack liest nur die Zusammenfassung aus [summary], die Blöcke selbst werden nicht vorgelesen.
 * Keine Legende: Die steht im Screen „Schichtverlauf“.
 */
@Composable
fun ShiftTimeline(
    hours: ShiftHours,
    summary: TimelineSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = shiftTimelineDescription(summary)
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = LumaSizes.minTouchTarget)
            .testTag("shift_timeline")
            .semantics {
                contentDescription = description
                role = Role.Button
            },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = BorderStroke(LumaSizes.border, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .clearAndSetSemantics {}
                .padding(
                    start = LumaTheme.spacing.l,
                    end = LumaTheme.spacing.l,
                    top = LumaTheme.spacing.xs,
                    bottom = LumaTheme.spacing.m,
                ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = LumaSizes.minTouchTarget),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LumaTheme.spacing.s),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_clock),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(LumaSizes.iconMedium),
                )
                Text(
                    text = stringResource(R.string.shift_history_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(LumaSizes.iconMedium),
                )
            }
            HourBlockRow(hours)
        }
    }
}

/** TalkBack-Ansage: „Schichtverlauf, 2 dringende Hinweise, nächste Routine 07:30 Hörgeräte einsetzen“. */
@Composable
fun shiftTimelineDescription(summary: TimelineSummary): String {
    val res = LocalResources.current
    val urgent = summary.openUrgentEvents
    val urgentText = if (urgent == 0) {
        res.getString(R.string.shift_timeline_no_urgent)
    } else {
        res.getQuantityString(R.plurals.shift_timeline_urgent, urgent, urgent)
    }
    val routineText = summary.nextRoutine?.let {
        res.getString(R.string.shift_timeline_next_routine, TimeFormat.clock(it.time), it.routine.title)
    } ?: res.getString(R.string.shift_timeline_no_routine)
    return res.getString(R.string.shift_timeline_description, res.getString(R.string.shift_history_title), urgentText, routineText)
}

/**
 * Blockreihe mit Routine-Ticks, Stundenlabels und Jetzt-Marker. Rein visuell, ohne eigene Semantik.
 * Blöcke teilen sich die Breite, sind aber nie schmaler als [LumaSizes.hourBlockMinWidth];
 * reicht die Breite nicht, lässt sich die Reihe waagerecht scrollen.
 */
@Composable
fun HourBlockRow(hours: ShiftHours, modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics {},
    ) {
        val count = hours.blocks.size.coerceAtLeast(1)
        val gap = LumaSizes.hourBlockGap
        val fitted = (maxWidth - gap * (count - 1)) / count
        val blockWidth = max(fitted, LumaSizes.hourBlockMinWidth)
        Row(
            modifier = if (fitted < LumaSizes.hourBlockMinWidth) Modifier.horizontalScroll(rememberScrollState()) else Modifier,
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            hours.blocks.forEachIndexed { index, block ->
                HourColumn(
                    block = block,
                    nowFraction = hours.nowMarker?.takeIf { it.blockIndex == index }?.fraction,
                    modifier = Modifier.width(blockWidth),
                )
            }
        }
    }
}

@Composable
private fun HourColumn(block: HourBlock, nowFraction: Float?, modifier: Modifier = Modifier) {
    // Der aktuelle Block ist nie FUTURE und damit immer opak; das braucht der Jetzt-Marker.
    val future = block.phase == HourPhase.FUTURE
    val markerColor = MaterialTheme.colorScheme.onSurface
    val tickColor = MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (nowFraction != null) Modifier.nowMarker(nowFraction, markerColor) else Modifier),
        ) {
            HourTile(
                count = block.hintCount.takeIf { it > 0 && !future },
                severity = block.openSeverity.takeIf { !future },
                future = future,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = LumaSizes.hourBlockHeight),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(LumaSizes.tickAreaHeight)
                    .routineTicks(block.routineTicks, tickColor),
            )
        }
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (block.isNight) {
                Icon(
                    painter = painterResource(R.drawable.ic_moon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(LumaSizes.iconSmall),
                )
            } else {
                Text(
                    text = TimeFormat.hour(block.start),
                    style = MaterialTheme.typography.bodyMedium.tabularNumbers(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * Ein Block: getönt nach [severity] mit Icon, neutral (`surfaceVariant`) ohne offene Dringlichkeit,
 * gestrichelt und leer, wenn [future]. [count] erscheint nur, wenn nicht `null`.
 */
@Composable
private fun HourTile(
    count: Int?,
    severity: Severity?,
    future: Boolean,
    modifier: Modifier = Modifier,
    horizontal: Boolean = false,
) {
    val shape = MaterialTheme.shapes.small
    val colors = severity?.colors
    val tile = when {
        future -> modifier.dashedBorder(MaterialTheme.colorScheme.outline, shape)
        colors != null -> modifier.background(colors.container, shape)
        else -> modifier.background(MaterialTheme.colorScheme.surfaceVariant, shape)
    }
    val contentColor = colors?.content ?: MaterialTheme.colorScheme.onSurface
    if (future) {
        Box(modifier = tile)
        return
    }
    val content: @Composable () -> Unit = {
        if (severity != null) {
            Icon(
                painter = painterResource(severity.iconRes),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(LumaSizes.iconSmall),
            )
        }
        if (count != null) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelLarge.tabularNumbers(),
                color = contentColor,
                maxLines = 1,
            )
        }
    }
    if (horizontal) {
        Row(
            modifier = tile,
            horizontalArrangement = Arrangement.spacedBy(LumaTheme.spacing.xs, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) { content() }
    } else {
        Column(
            modifier = tile,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) { content() }
    }
}

/** Gestrichelter Rand (1 dp) entlang [shape], innen am Rand ausgerichtet. */
private fun Modifier.dashedBorder(color: Color, shape: Shape): Modifier = drawBehind {
    val stroke = LumaSizes.border.toPx()
    val dash = LumaSizes.dash.toPx()
    inset(stroke / 2) {
        drawOutline(
            outline = shape.createOutline(size, layoutDirection, this),
            color = color,
            style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash))),
        )
    }
}

/** Routinetermine als kurze senkrechte Striche, [ticks] als Anteil 0..1 der Breite. */
private fun Modifier.routineTicks(ticks: List<Float>, color: Color): Modifier = drawBehind {
    val width = LumaSizes.tickWidth.toPx()
    ticks.forEach { fraction ->
        drawRoundRect(
            color = color,
            topLeft = Offset(fraction * size.width - width / 2, LumaSizes.tickTop.toPx()),
            size = Size(width, LumaSizes.tickLength.toPx()),
            cornerRadius = CornerRadius(width / 2),
        )
    }
}

/**
 * Senkrechte Linie mit Punkt oben bei [fraction] der Breite, hinter den Inhalt gezeichnet: Die opake Kachel
 * verdeckt die Linie, sichtbar bleiben der Punkt über dem Block und der Strich in der Tick-Fläche.
 */
private fun Modifier.nowMarker(fraction: Float, color: Color): Modifier = drawBehind {
    val x = fraction * size.width
    val top = -LumaSizes.nowMarkerOverhang.toPx()
    val radius = LumaSizes.nowMarkerDot.toPx() / 2
    drawLine(color, Offset(x, top), Offset(x, size.height), strokeWidth = LumaSizes.nowMarkerWidth.toPx())
    drawCircle(color, radius, Offset(x, top + radius))
}

/** Legende der Blöcke; nur im Screen „Schichtverlauf“, nie auf dem Dashboard. */
@Composable
fun HourBlockLegend(modifier: Modifier = Modifier) {
    val swatch = Modifier
        .width(LumaSizes.legendSwatchWidth)
        .height(LumaSizes.legendSwatchHeight)
    val markerColor = MaterialTheme.colorScheme.onSurface
    val tickColor = MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(top = LumaTheme.spacing.m),
        )
        Column(
            modifier = Modifier.padding(top = LumaTheme.spacing.m),
            verticalArrangement = Arrangement.spacedBy(LumaTheme.spacing.m),
        ) {
            LegendItem(stringResource(R.string.legend_blocks)) {
                HourTile(count = 2, severity = Severity.URGENT, future = false, modifier = swatch, horizontal = true)
            }
            LegendItem(stringResource(R.string.legend_severity)) {
                HourTile(count = null, severity = Severity.ATTENTION, future = false, modifier = swatch, horizontal = true)
            }
            LegendItem(stringResource(R.string.legend_done)) {
                HourTile(count = 4, severity = null, future = false, modifier = swatch, horizontal = true)
            }
            LegendItem(stringResource(R.string.legend_future)) {
                HourTile(count = null, severity = null, future = true, modifier = swatch)
            }
            LegendItem(stringResource(R.string.legend_routine)) {
                Box(
                    modifier = Modifier
                        .width(LumaSizes.legendSwatchWidth)
                        .height(LumaSizes.tickAreaHeight)
                        .routineTicks(listOf(0.5f), tickColor),
                )
            }
            LegendItem(stringResource(R.string.legend_now)) {
                Box(
                    modifier = swatch
                        .padding(top = LumaSizes.nowMarkerOverhang)
                        .nowMarker(0.5f, markerColor),
                )
            }
        }
    }
}

@Composable
private fun LegendItem(text: String, swatch: @Composable () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LumaTheme.spacing.m),
    ) {
        Box(
            modifier = Modifier
                .width(LumaSizes.legendSwatchWidth)
                .clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) { swatch() }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}

// ---------- Previews ----------

private val previewDay = LocalDateTime.of(2026, 10, 12, 0, 0)

private fun previewBlock(hour: Int, count: Int, severity: Severity?, phase: HourPhase, ticks: List<Float> = emptyList()) =
    HourBlock(
        start = previewDay.withHour(hour),
        end = previewDay.withHour(hour + 1),
        isNight = false,
        hintCount = count,
        openSeverity = severity,
        phase = phase,
        routineTicks = ticks,
    )

private val previewHours = ShiftHours(
    blocks = listOf(
        HourBlock(previewDay.minusDays(1).withHour(20), previewDay.withHour(6), true, 7, Severity.URGENT, HourPhase.PAST, emptyList()),
        previewBlock(6, 15, Severity.URGENT, HourPhase.PAST, listOf(0.5f, 0.75f)),
        previewBlock(7, 4, Severity.INFO, HourPhase.CURRENT, listOf(0f, 0.5f)),
    ) + (8..13).map { previewBlock(it, 0, null, HourPhase.FUTURE, if (it == 8 || it == 10) listOf(0f) else emptyList()) },
    nowMarker = NowMarker(blockIndex = 2, fraction = 0.25f),
)

private val previewSummary = TimelineSummary(
    openUrgentEvents = 2,
    nextRoutine = TimelineRoutine(
        Routine("rt07", "r09", "Hörgeräte einsetzen", previewDay.withHour(7).withMinute(30).toLocalTime(), DayOfWeek.entries.toSet(), false),
        Resident("r09", "Margarete", "Fischer", 88, "2.09", 4, emptyList(), emptyList(), emptyList(), null, null),
        previewDay.withHour(7).withMinute(30),
    ),
)

@Composable
private fun ShiftTimelinePreviewContent() {
    Surface(color = MaterialTheme.colorScheme.background) {
        ShiftTimeline(previewHours, previewSummary, onClick = {}, modifier = Modifier.padding(LumaTheme.spacing.l))
    }
}

@Preview(name = "Light", widthDp = 411)
@Composable
private fun ShiftTimelinePreview() {
    LumaTheme(darkTheme = false) { ShiftTimelinePreviewContent() }
}

@Preview(name = "Dark", widthDp = 411)
@Composable
private fun ShiftTimelineDarkPreview() {
    LumaTheme(darkTheme = true) { ShiftTimelinePreviewContent() }
}

@Preview(name = "Schrift 200 %", widthDp = 411, fontScale = 2f)
@Composable
private fun ShiftTimelineLargeFontPreview() {
    LumaTheme(darkTheme = false) { ShiftTimelinePreviewContent() }
}

@Preview(name = "Legende", widthDp = 411)
@Composable
private fun HourBlockLegendPreview() {
    LumaTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.surface) { HourBlockLegend(Modifier.padding(LumaTheme.spacing.l)) }
    }
}
