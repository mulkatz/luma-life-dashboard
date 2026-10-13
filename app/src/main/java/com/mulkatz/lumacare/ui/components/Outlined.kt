package com.mulkatz.lumacare.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mulkatz.lumacare.ui.theme.LumaSizes

/*
 * Bedienbare Umrandungen an einer Stelle: Buttons und Chips mit Rand `outline` (≥ 3 : 1).
 * `outlineVariant` bleibt Trennlinien und Kartenrändern vorbehalten.
 */

/** Deckkraft des Rands bedienbarer Elemente im gesperrten Zustand (Material-Vorgabe). */
private const val DISABLED_BORDER_ALPHA = 0.12f

/** Rand eines bedienbaren Elements: `outline`, gesperrt abgeschwächt. */
@Composable
fun interactiveBorder(enabled: Boolean = true): BorderStroke = BorderStroke(
    LumaSizes.border,
    if (enabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_BORDER_ALPHA),
)

/** Umrandeter Button mit Rand `outline`, mindestens 48 dp hoch. */
@Composable
fun LumaOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = LumaSizes.minTouchTarget),
        enabled = enabled,
        border = interactiveBorder(enabled),
        contentPadding = contentPadding,
        content = content,
    )
}

/** Textbaustein als Chip: Text `onSurface` in `labelLarge`, Rand `outline`. */
@Composable
fun LumaAssistChip(onClick: () -> Unit, label: String, modifier: Modifier = Modifier) {
    AssistChip(
        onClick = onClick,
        label = { Text(text = label, style = MaterialTheme.typography.labelLarge) },
        modifier = modifier,
        colors = AssistChipDefaults.assistChipColors(labelColor = MaterialTheme.colorScheme.onSurface),
        border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = MaterialTheme.colorScheme.outline),
    )
}

/** Auswahl-Chip: ungewählt mit Rand `outline`, gewählt getönt ohne Rand. */
@Composable
fun LumaFilterChip(selected: Boolean, onClick: () -> Unit, label: String, modifier: Modifier = Modifier) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = label, style = MaterialTheme.typography.labelLarge) },
        modifier = modifier,
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outline,
        ),
    )
}
