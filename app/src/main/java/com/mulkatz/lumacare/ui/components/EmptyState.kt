package com.mulkatz.lumacare.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.mulkatz.lumacare.R
import com.mulkatz.lumacare.ui.theme.LumaSizes
import com.mulkatz.lumacare.ui.theme.LumaTheme

/**
 * Leerer Zustand, standardmäßig „Alles erledigt“ – dann nur nach abgeschlossenem Laden zeigen. Breite und
 * Innenabstand legt nur diese Komponente fest. Der Detailbereich auf dem Tablet nutzt sie mit eigenem Text.
 */
@Composable
fun EmptyState(
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.empty_state_title),
    body: String? = stringResource(R.string.empty_state_body),
    @DrawableRes icon: Int = R.drawable.ic_task_alt,
    testTag: String = "empty_state",
) {
    Column(
        modifier = modifier
            .widthIn(max = LumaSizes.contentMaxWidth)
            .fillMaxWidth()
            .padding(LumaTheme.spacing.xl)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LumaTheme.spacing.s),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(LumaSizes.iconLarge),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        if (body != null) {
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(name = "Light")
@Composable
private fun EmptyStatePreview() {
    LumaTheme(darkTheme = false) { Surface(color = MaterialTheme.colorScheme.background) { EmptyState() } }
}

@Preview(name = "Dark")
@Composable
private fun EmptyStateDarkPreview() {
    LumaTheme(darkTheme = true) { Surface(color = MaterialTheme.colorScheme.background) { EmptyState() } }
}
