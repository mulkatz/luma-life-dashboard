package com.mulkatz.lumacare.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.mulkatz.lumacare.R
import com.mulkatz.lumacare.ui.theme.LumaTheme
import com.mulkatz.lumacare.ui.theme.tabularNumbers

/**
 * Schichtkontext oben auf dem Dashboard, Fläche `secondary` (Aubergine) bis unter die Statusleiste.
 *
 * @param area Wohnbereich, z. B. „Ahorn“.
 * @param shift Schicht, z. B. „Frühdienst“.
 * @param time aktuelle Uhrzeit, z. B. „07:15“.
 * @param remaining Restzeit der Schicht, z. B. „6:45 h“; `null`, wenn die Schicht beendet ist.
 */
@Composable
fun ShiftHeader(
    area: String,
    shift: String,
    time: String,
    remaining: String?,
    modifier: Modifier = Modifier,
) {
    val status = if (remaining != null) {
        stringResource(R.string.shift_header_remaining, remaining)
    } else {
        stringResource(R.string.shift_header_ended)
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("shift_header"),
        color = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.onSecondary,
    ) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .padding(horizontal = LumaTheme.spacing.l, vertical = LumaTheme.spacing.l)
                .semantics(mergeDescendants = true) { heading() },
            verticalArrangement = Arrangement.spacedBy(LumaTheme.spacing.xs),
        ) {
            Text(
                text = stringResource(R.string.shift_header_title, area, shift),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(R.string.shift_header_time, time, status),
                style = MaterialTheme.typography.headlineSmall.tabularNumbers(),
            )
        }
    }
}

@Preview(name = "Light")
@Composable
private fun ShiftHeaderPreview() {
    LumaTheme(darkTheme = false) {
        ShiftHeader(area = "Ahorn", shift = "Frühdienst", time = "07:15", remaining = "6:45 h")
    }
}

@Preview(name = "Dark")
@Composable
private fun ShiftHeaderDarkPreview() {
    LumaTheme(darkTheme = true) {
        ShiftHeader(area = "Ahorn", shift = "Frühdienst", time = "07:15", remaining = "6:45 h")
    }
}

@Preview(name = "Schrift 200 %", fontScale = 2f)
@Composable
private fun ShiftHeaderLargeFontPreview() {
    LumaTheme(darkTheme = false) {
        ShiftHeader(area = "Ahorn", shift = "Frühdienst", time = "07:15", remaining = "6:45 h")
    }
}

@Preview(name = "Schicht beendet")
@Composable
private fun ShiftHeaderEndedPreview() {
    LumaTheme(darkTheme = false) {
        ShiftHeader(area = "Ahorn", shift = "Frühdienst", time = "14:05", remaining = null)
    }
}
