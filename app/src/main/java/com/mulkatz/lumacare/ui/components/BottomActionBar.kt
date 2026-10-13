package com.mulkatz.lumacare.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.mulkatz.lumacare.R
import com.mulkatz.lumacare.domain.EventAction
import com.mulkatz.lumacare.ui.theme.LumaSizes
import com.mulkatz.lumacare.ui.theme.LumaTheme
import com.mulkatz.lumacare.ui.theme.tabularNumbers

/**
 * Aktionsleiste unten im Detail, im Daumenbereich und über der Navigationsleiste: eine primäre Aktion
 * (gefüllt) und eine sekundäre (umrandet). Fläche `surface` mit Linie oben statt Schatten.
 * [caption] nennt über den Buttons, welchem Hinweis die Leiste gilt (nur bei mehreren offenen Hinweisen).
 */
@Composable
fun BottomActionBar(
    primary: @Composable (Modifier) -> Unit,
    secondary: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
    caption: String? = null,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("action_bar"),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Column(
                modifier = Modifier
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.exclude(WindowInsets.ime).only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                    )
                    .widthIn(max = LumaSizes.contentMaxWidth)
                    .fillMaxWidth()
                    .padding(horizontal = LumaTheme.spacing.l, vertical = LumaTheme.spacing.m),
                verticalArrangement = Arrangement.spacedBy(LumaTheme.spacing.s),
            ) {
                if (caption != null) {
                    Text(
                        text = caption,
                        style = MaterialTheme.typography.bodyMedium.tabularNumbers(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("action_bar_caption"),
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(LumaTheme.spacing.m),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    primary(Modifier.weight(1f))
                    secondary?.invoke()
                }
            }
        }
    }
}

/** Sekundäre Aktion: umrandet (Rand `outline`), optional mit Icon, mindestens 48 dp hoch. */
@Composable
fun SecondaryActionButton(
    text: String,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    enabled: Boolean = true,
) {
    LumaOutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.testTag(testTag),
    ) {
        if (icon != null) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier
                    .padding(end = LumaTheme.spacing.s)
                    .size(LumaSizes.iconSmall),
            )
        }
        Text(text = text)
    }
}

@Composable
private fun BottomActionBarSample() {
    BottomActionBar(
        caption = "Zum Hinweis von gestern 23:15",
        primary = { EventActionButton(EventAction.TAKE_OVER, onClick = {}, emphasis = ActionEmphasis.FILLED, modifier = it) },
        secondary = { SecondaryActionButton(stringResource(R.string.action_note), onClick = {}, testTag = "action_note", icon = R.drawable.ic_note) },
    )
}

@Preview(name = "Light", widthDp = 411)
@Composable
private fun BottomActionBarPreview() {
    LumaTheme(darkTheme = false) { BottomActionBarSample() }
}

@Preview(name = "Dark", widthDp = 411)
@Composable
private fun BottomActionBarDarkPreview() {
    LumaTheme(darkTheme = true) { BottomActionBarSample() }
}

@Preview(name = "Schrift 200 %", widthDp = 411, fontScale = 2f)
@Composable
private fun BottomActionBarLargeFontPreview() {
    LumaTheme(darkTheme = false) { BottomActionBarSample() }
}
