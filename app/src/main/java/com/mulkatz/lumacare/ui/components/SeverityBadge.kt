package com.mulkatz.lumacare.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.mulkatz.lumacare.R
import com.mulkatz.lumacare.domain.Severity
import com.mulkatz.lumacare.ui.theme.LumaSizes
import com.mulkatz.lumacare.ui.theme.LumaTheme
import com.mulkatz.lumacare.ui.theme.StatusColor

@get:DrawableRes
val Severity.iconRes: Int
    get() = when (this) {
        Severity.URGENT -> R.drawable.ic_urgent
        Severity.ATTENTION -> R.drawable.ic_clock
        Severity.INFO -> R.drawable.ic_info
    }

@get:StringRes
val Severity.labelRes: Int
    get() = when (this) {
        Severity.URGENT -> R.string.severity_urgent
        Severity.ATTENTION -> R.string.severity_attention
        Severity.INFO -> R.string.severity_info
    }

val Severity.colors: StatusColor
    @Composable get() = when (this) {
        Severity.URGENT -> LumaTheme.status.urgent
        Severity.ATTENTION -> LumaTheme.status.attention
        Severity.INFO -> LumaTheme.status.info
    }

/** Dringlichkeit als Icon + Label + Farbe, nie nur als Farbpunkt. */
@Composable
fun SeverityBadge(severity: Severity, modifier: Modifier = Modifier) {
    val colors = severity.colors
    Row(
        modifier = modifier
            .background(colors.container, MaterialTheme.shapes.small)
            .padding(horizontal = LumaTheme.spacing.s, vertical = LumaTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LumaTheme.spacing.xs),
    ) {
        Icon(
            painter = painterResource(severity.iconRes),
            contentDescription = null,
            tint = colors.content,
            modifier = Modifier.size(LumaSizes.iconSmall),
        )
        Text(
            text = stringResource(severity.labelRes),
            style = MaterialTheme.typography.labelLarge,
            color = colors.content,
        )
    }
}

@Composable
private fun SeverityBadgeRow() {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier.padding(LumaTheme.spacing.s),
            horizontalArrangement = Arrangement.spacedBy(LumaTheme.spacing.s),
        ) {
            Severity.entries.forEach { SeverityBadge(it) }
        }
    }
}

@Preview(name = "Light")
@Composable
private fun SeverityBadgePreview() {
    LumaTheme(darkTheme = false) { SeverityBadgeRow() }
}

@Preview(name = "Dark")
@Composable
private fun SeverityBadgeDarkPreview() {
    LumaTheme(darkTheme = true) { SeverityBadgeRow() }
}
