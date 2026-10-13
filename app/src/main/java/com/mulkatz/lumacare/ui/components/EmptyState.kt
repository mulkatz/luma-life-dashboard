package com.mulkatz.lumacare.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.mulkatz.lumacare.R
import com.mulkatz.lumacare.ui.theme.LumaTheme

/** „Alles erledigt“ – nur nach abgeschlossenem Laden zeigen. */
@Composable
fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .widthIn(max = 640.dp)
            .fillMaxWidth()
            .padding(LumaTheme.spacing.xl)
            .testTag("empty_state"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LumaTheme.spacing.s),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_task_alt),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp),
        )
        Text(
            text = stringResource(R.string.empty_state_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.empty_state_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun EmptyStatePreview() {
    LumaTheme(darkTheme = false) { EmptyState() }
}

@Preview(name = "Dark", showBackground = true, backgroundColor = 0xFF17151F)
@Composable
private fun EmptyStateDarkPreview() {
    LumaTheme(darkTheme = true) { EmptyState() }
}
