package com.mulkatz.lumacare.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mulkatz.lumacare.R
import com.mulkatz.lumacare.domain.ForwardTarget
import com.mulkatz.lumacare.domain.Resolution
import com.mulkatz.lumacare.ui.theme.LumaSizes
import com.mulkatz.lumacare.ui.theme.LumaTheme
import com.mulkatz.lumacare.ui.theme.tabularNumbers
import java.time.LocalDateTime

/**
 * Abstand oben, der ein Icon der Größe [iconSize] mittig auf die erste Zeile von [style] setzt. Wächst mit
 * der Schriftgröße, sodass Icons bei umbrechendem Text oben bei der ersten Zeile bleiben.
 */
@Composable
fun firstLineIconPadding(style: TextStyle, iconSize: Dp = LumaSizes.iconSmall): Dp {
    val lineHeight = with(LocalDensity.current) { style.lineHeight.toDp() }
    return ((lineHeight - iconSize) / 2).coerceAtLeast(0.dp)
}

/** Icon und Text in einer Zeile; das Icon steht auf Höhe der ersten Textzeile. */
@Composable
fun IconLabel(
    @DrawableRes icon: Int,
    text: String,
    iconTint: Color,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    style: TextStyle = MaterialTheme.typography.labelLarge,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(LumaTheme.spacing.xs),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier
                .padding(top = firstLineIconPadding(style))
                .size(LumaSizes.iconSmall),
        )
        Text(text = text, style = style, color = textColor)
    }
}

/**
 * Ergebnis eines Abschlusses („Abgeschlossen: Erledigt“, „Weitergeleitet an Küche“), gleich im Personen-Verlauf
 * und im Schichtverlauf: Icon in der Statusfarbe, Text `titleSmall` in `onSurface`.
 */
@Composable
fun ResolutionLabel(resolution: Resolution, forwardedTo: ForwardTarget?, modifier: Modifier = Modifier) {
    IconLabel(
        icon = if (resolution == Resolution.FORWARDED) R.drawable.ic_forward else R.drawable.ic_check_circle,
        text = resolutionText(resolution, forwardedTo),
        iconTint = LumaTheme.status.done.content,
        style = MaterialTheme.typography.titleSmall,
        modifier = modifier,
    )
}

/**
 * Abschnittsüberschrift `titleMedium`, optional mit Icon und Anzahl als „· 2“ in `onSurfaceVariant`.
 * Einheitlich auf Dashboard, im Detail und im Schichtverlauf.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
    @DrawableRes icon: Int? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val style = MaterialTheme.typography.titleMedium
    val separator = stringResource(R.string.list_separator)
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val text = buildAnnotatedString {
        append(title)
        if (count != null) withStyle(SpanStyle(color = muted, fontFeatureSettings = "tnum")) { append(separator + count) }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = LumaTheme.spacing.m)
            .semantics(mergeDescendants = true) { heading() },
        horizontalArrangement = Arrangement.spacedBy(LumaTheme.spacing.s),
    ) {
        if (icon != null) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier
                    .padding(top = firstLineIconPadding(style, LumaSizes.iconMedium))
                    .size(LumaSizes.iconMedium),
            )
        }
        Text(text = text, style = style, color = MaterialTheme.colorScheme.onBackground)
    }
}

/**
 * Zeitspalte in Verlaufslisten: feste Breite ([LumaSizes.timeColumn], mit der Schrift skaliert), Uhrzeit
 * `titleSmall` mit `tnum`, darunter „gestern“ bzw. das Datum, wenn der Zeitpunkt nicht von heute ist.
 */
@Composable
fun TimeColumn(time: LocalDateTime, now: LocalDateTime, modifier: Modifier = Modifier) {
    val res = LocalResources.current
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    val day = TimeFormat.dayLabel(res, time, now)
    Column(modifier = modifier.width(LumaSizes.timeColumn * fontScale)) {
        Text(text = TimeFormat.clock(time), style = MaterialTheme.typography.titleSmall.tabularNumbers())
        if (day != null) {
            Text(
                text = day,
                style = MaterialTheme.typography.bodyMedium.tabularNumbers(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Ruhige Zeile mit Icon und Text in `onSurfaceVariant`, z. B. „Keine offenen Hinweise“. */
@Composable
fun QuietNotice(@DrawableRes icon: Int, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LumaTheme.spacing.s),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(LumaSizes.iconMedium),
        )
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
