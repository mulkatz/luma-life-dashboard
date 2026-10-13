package com.mulkatz.lumacare.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.mulkatz.lumacare.R
import com.mulkatz.lumacare.ui.theme.LumaSizes
import com.mulkatz.lumacare.ui.theme.LumaTheme

/**
 * Appbar für Unterseiten wie der Kopfbereich des Dashboards: Fläche `secondary` bis unter die Statusleiste.
 * Ohne [onBack] entfällt „Zurück“ (Detailbereich auf dem Tablet). Ohne Titel und ohne Zurück bleibt nur die
 * Fläche hinter Status- bzw. Titelleiste (`safeDrawing`, auch im Desktop-Fenster), damit helle Icons lesbar bleiben.
 */
@Composable
fun SubpageAppBar(title: String?, onBack: (() -> Unit)?, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.onSecondary,
    ) {
        if (title == null && onBack == null) {
            Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.safeDrawing))
            return@Surface
        }
        Row(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .heightIn(min = LumaSizes.appBarMinHeight)
                .padding(horizontal = if (onBack != null) LumaTheme.spacing.s else LumaTheme.spacing.l),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LumaTheme.spacing.xs),
        ) {
            if (onBack != null) {
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
