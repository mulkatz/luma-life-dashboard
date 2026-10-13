package com.mulkatz.lumacare.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mulkatz.lumacare.R
import com.mulkatz.lumacare.ui.theme.LumaSizes
import com.mulkatz.lumacare.ui.theme.LumaTheme

/**
 * Mindesthöhe des Kopfs im Detailbereich (zweispaltig): die gemessene Höhe des ShiftHeaders links, damit beide
 * Köpfe bündig abschließen. Einspaltig 0.
 */
val LocalPaneHeaderHeight = compositionLocalOf { 0.dp }

/**
 * Appbar für Unterseiten wie der Kopfbereich des Dashboards: Fläche `secondary` bis unter die Statusleiste
 * (`safeDrawing`, auch Titelleiste im Desktop-Fenster), damit helle Icons lesbar bleiben.
 *
 * Mit [onBack] (einspaltig): Zurück 48 dp und optional [title] in `titleMedium`. Ohne [onBack] (Detailbereich auf
 * dem Tablet): so hoch wie der ShiftHeader ([LocalPaneHeaderHeight]), [title] unten in `headlineSmall` auf Höhe
 * der Uhrzeit links; ohne Titel bleibt nur die Fläche.
 */
@Composable
fun SubpageAppBar(title: String?, onBack: (() -> Unit)?, modifier: Modifier = Modifier) {
    if (onBack == null) {
        PaneHeader(title, modifier)
        return
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.onSecondary,
    ) {
        Row(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .heightIn(min = LumaSizes.appBarMinHeight)
                .padding(horizontal = LumaTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LumaTheme.spacing.xs),
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(LumaSizes.minTouchTarget)
                    .testTag("action_back"),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = stringResource(R.string.action_back),
                    tint = MaterialTheme.colorScheme.onSecondary,
                )
            }
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() },
                )
            }
        }
    }
}

@Composable
private fun PaneHeader(title: String?, modifier: Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = LocalPaneHeaderHeight.current),
        color = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.onSecondary,
    ) {
        Column(
            modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (title != null) {
                // Auf der Inhaltsspalte (max. 640 dp, zentriert), bündig mit dem Inhalt darunter.
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier
                        .widthIn(max = LumaSizes.contentMaxWidth)
                        .fillMaxWidth()
                        .padding(horizontal = LumaTheme.spacing.l, vertical = LumaTheme.spacing.m)
                        .semantics { heading() }
                        .testTag("pane_title"),
                )
            }
        }
    }
}
