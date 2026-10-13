package com.mulkatz.lumacare.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import com.mulkatz.lumacare.R
import com.mulkatz.lumacare.ui.theme.LumaSizes
import com.mulkatz.lumacare.ui.theme.LumaTheme

/** Textbausteine in fester Reihenfolge mit testTag; Tippen hängt den Text an den Entwurf an. */
private val NOTE_SNIPPETS: List<Pair<Int, String>> = listOf(
    R.string.note_chip_seen to "note_snippet_seen",
    R.string.note_chip_doctor to "note_snippet_doctor",
    R.string.note_chip_measured to "note_snippet_measured",
    R.string.note_chip_family to "note_snippet_family",
)

/**
 * Notiz: Textbausteine als Chips, darunter das Freitextfeld. Bei Pflicht steht zuerst ein neutraler Hinweis
 * („Pflichtnotiz: …“, `onSurfaceVariant`). [showError] (ungültig) zeigt den Inline-Fehler „Bitte kurz notieren,
 * was du getan hast.“ mit Icon (Farbe nie allein) erst, wenn das Feld bearbeitet oder verlassen wurde bzw. ein
 * Baustein gewählt ist. Es gibt keinen Dialog.
 */
@Composable
fun NoteInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSnippet: (String) -> Unit,
    required: Boolean,
    showError: Boolean,
    modifier: Modifier = Modifier,
) {
    // Berührt: bearbeitet, Baustein gewählt oder nach Fokus wieder verlassen; ein Entwurf zählt als bearbeitet.
    var touched by rememberSaveable { mutableStateOf(value.isNotEmpty()) }
    var focused by remember { mutableStateOf(false) }
    val error = showError && touched
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("note_input"),
        verticalArrangement = Arrangement.spacedBy(LumaTheme.spacing.s),
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(LumaTheme.spacing.s)) {
            NOTE_SNIPPETS.forEach { (text, tag) ->
                SnippetChip(text, tag) {
                    touched = true
                    onSnippet(it)
                }
            }
        }
        OutlinedTextField(
            value = value,
            onValueChange = {
                touched = true
                onValueChange(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { state ->
                    if (focused && !state.isFocused) touched = true
                    focused = state.isFocused
                }
                .testTag("note_text"),
            label = { Text(stringResource(if (required) R.string.note_label_required else R.string.note_label_optional)) },
            textStyle = MaterialTheme.typography.bodyLarge,
            minLines = 2,
            isError = error,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            supportingText = when {
                error -> {
                    { NoteError() }
                }
                required -> {
                    { NoteRequiredHint() }
                }
                else -> null
            },
        )
    }
}

@Composable
private fun SnippetChip(@StringRes text: Int, tag: String, onSnippet: (String) -> Unit) {
    val label = stringResource(text)
    val spoken = stringResource(R.string.note_chip_spoken, label)
    LumaAssistChip(
        onClick = { onSnippet(label) },
        label = label,
        modifier = Modifier
            .testTag(tag)
            .semantics { contentDescription = spoken },
    )
}

/** Neutraler Pflichthinweis, bevor das Feld berührt wurde. */
@Composable
private fun NoteRequiredHint() {
    Text(
        text = stringResource(R.string.note_required_hint),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag("note_required_hint"),
    )
}

@Composable
private fun NoteError() {
    Row(
        modifier = Modifier.testTag("note_error"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LumaTheme.spacing.xs),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_info),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(LumaSizes.iconSmall),
        )
        Text(
            text = stringResource(R.string.note_error),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Preview(name = "Leer, Pflicht · Light", widthDp = 411)
@Composable
private fun NoteInputPreview() {
    LumaTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            NoteInput("", {}, {}, required = true, showError = true, modifier = Modifier.padding(LumaTheme.spacing.l))
        }
    }
}

@Preview(name = "Mit Text · Dark", widthDp = 411)
@Composable
private fun NoteInputDarkPreview() {
    LumaTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            NoteInput("Werte gemessen", {}, {}, required = true, showError = false, modifier = Modifier.padding(LumaTheme.spacing.l))
        }
    }
}

@Preview(name = "Schrift 200 %", widthDp = 411, fontScale = 2f)
@Composable
private fun NoteInputLargeFontPreview() {
    LumaTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            NoteInput("", {}, {}, required = false, showError = false, modifier = Modifier.padding(LumaTheme.spacing.l))
        }
    }
}
