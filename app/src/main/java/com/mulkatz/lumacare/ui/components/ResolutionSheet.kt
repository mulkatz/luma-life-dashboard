package com.mulkatz.lumacare.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.mulkatz.lumacare.R
import com.mulkatz.lumacare.domain.ForwardTarget
import com.mulkatz.lumacare.domain.Resolution
import com.mulkatz.lumacare.ui.theme.LumaSizes
import com.mulkatz.lumacare.ui.theme.LumaTheme

/** Worum es geht: Person und Hinweis, damit das Sheet ohne den Hintergrund verständlich ist. */
data class SheetSubject(val personLabel: String, val hintText: String)

/** Eingabe im Abschluss-Sheet; der Zustand liegt beim Aufrufer (SavedStateHandle). */
data class ResolutionInput(
    val resolution: Resolution,
    val forwardTarget: ForwardTarget?,
    val note: String,
    val noteRequired: Boolean,
    val showNoteError: Boolean,
    /** Erst `true`, wenn die Eingabe nach `EventTransitions` gültig ist und nichts gespeichert wird. */
    val confirmEnabled: Boolean,
)

/**
 * Abschließen als Modal Bottom Sheet (Radius 28 oben): große Radio-Zeilen für die erlaubten [options]
 * („Erledigt“, „Weitergeleitet an …“ mit Ziel, „Kein Handlungsbedarf“), darunter [NoteInput].
 * „Abschließen“ ist erst aktiv, wenn die Eingabe gültig ist. Kein Bestätigungsdialog; Rückgängig folgt per Snackbar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResolutionSheet(
    subject: SheetSubject,
    options: List<Resolution>,
    input: ResolutionInput,
    onResolutionChange: (Resolution) -> Unit,
    onForwardTargetChange: (ForwardTarget) -> Unit,
    onNoteChange: (String) -> Unit,
    onSnippet: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        ResolutionSheetContent(
            subject, options, input, onResolutionChange, onForwardTargetChange, onNoteChange, onSnippet, onConfirm,
        )
    }
}

@Composable
private fun ResolutionSheetContent(
    subject: SheetSubject,
    options: List<Resolution>,
    input: ResolutionInput,
    onResolutionChange: (Resolution) -> Unit,
    onForwardTargetChange: (ForwardTarget) -> Unit,
    onNoteChange: (String) -> Unit,
    onSnippet: (String) -> Unit,
    onConfirm: () -> Unit,
) {
    SheetColumn(tag = "resolution_sheet") {
        SheetHeader(stringResource(R.string.resolution_sheet_title), subject)
        Column(modifier = Modifier.selectableGroup()) {
            options.forEach { option ->
                OptionRow(
                    text = stringResource(option.labelRes),
                    selected = input.resolution == option,
                    tag = "resolution_${option.name.lowercase()}",
                ) { onResolutionChange(option) }
                if (option == Resolution.FORWARDED && input.resolution == Resolution.FORWARDED) {
                    ForwardTargets(input.forwardTarget, onForwardTargetChange)
                }
            }
        }
        NoteInput(
            value = input.note,
            onValueChange = onNoteChange,
            onSnippet = onSnippet,
            required = input.noteRequired,
            showError = input.showNoteError,
        )
        Button(
            onClick = onConfirm,
            enabled = input.confirmEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = LumaSizes.minTouchTarget)
                .testTag("action_confirm_resolve"),
        ) {
            Text(stringResource(R.string.action_resolve))
        }
    }
}

/** Notiz ohne Statuswechsel, als Modal Bottom Sheet. Speichern ist gesperrt, solange die Notiz ungültig ist. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteSheet(
    subject: SheetSubject,
    note: String,
    showNoteError: Boolean,
    saveEnabled: Boolean,
    onNoteChange: (String) -> Unit,
    onSnippet: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        NoteSheetContent(subject, note, showNoteError, saveEnabled, onNoteChange, onSnippet, onSave)
    }
}

@Composable
private fun NoteSheetContent(
    subject: SheetSubject,
    note: String,
    showNoteError: Boolean,
    saveEnabled: Boolean,
    onNoteChange: (String) -> Unit,
    onSnippet: (String) -> Unit,
    onSave: () -> Unit,
) {
    SheetColumn(tag = "note_sheet") {
        SheetHeader(stringResource(R.string.note_sheet_title), subject)
        NoteInput(value = note, onValueChange = onNoteChange, onSnippet = onSnippet, required = true, showError = showNoteError)
        Button(
            onClick = onSave,
            enabled = saveEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = LumaSizes.minTouchTarget)
                .testTag("action_save_note"),
        ) {
            Text(stringResource(R.string.action_save_note))
        }
    }
}

@Composable
private fun SheetColumn(tag: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = LumaTheme.spacing.l, end = LumaTheme.spacing.l, bottom = LumaTheme.spacing.xl)
            .testTag(tag),
        verticalArrangement = Arrangement.spacedBy(LumaTheme.spacing.l),
        content = content,
    )
}

@Composable
private fun SheetHeader(title: String, subject: SheetSubject) {
    Column(verticalArrangement = Arrangement.spacedBy(LumaTheme.spacing.xs)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        Text(text = subject.personLabel, style = MaterialTheme.typography.titleSmall)
        // Originalaussage, gekürzt; vollständig steht sie im Detail dahinter.
        Text(
            text = subject.hintText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Große Radio-Zeile (≥ 56 dp), die ganze Zeile ist das Tap-Ziel. */
@Composable
private fun OptionRow(text: String, selected: Boolean, tag: String, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = LumaSizes.optionRowMinHeight)
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .testTag(tag)
            .padding(horizontal = LumaTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LumaTheme.spacing.m),
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Ziele der Weiterleitung als Auswahl-Chips; Pflicht, bevor „Abschließen“ aktiv wird. */
@Composable
private fun ForwardTargets(selected: ForwardTarget?, onSelect: (ForwardTarget) -> Unit) {
    Column(
        modifier = Modifier.padding(start = LumaTheme.spacing.xxl + LumaTheme.spacing.l, bottom = LumaTheme.spacing.s),
        verticalArrangement = Arrangement.spacedBy(LumaTheme.spacing.xs),
    ) {
        Text(
            text = stringResource(R.string.forward_targets_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(LumaTheme.spacing.s)) {
            ForwardTarget.entries.forEach { target ->
                LumaFilterChip(
                    selected = target == selected,
                    onClick = { onSelect(target) },
                    label = stringResource(target.labelRes),
                    modifier = Modifier.testTag("forward_target_${target.name.lowercase()}"),
                )
            }
        }
    }
}

// ---------- Previews (Inhalt ohne Sheet-Fenster) ----------

private val previewSubject = SheetSubject(
    personLabel = "Ilse Wagner · 2.02",
    hintText = "Wünscht sich zum Mittag statt Fisch die vegetarische Alternative.",
)

private val previewInput = ResolutionInput(
    resolution = Resolution.FORWARDED,
    forwardTarget = ForwardTarget.KITCHEN,
    note = "",
    noteRequired = false,
    showNoteError = false,
    confirmEnabled = true,
)

@Composable
private fun ResolutionSample(input: ResolutionInput) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        ResolutionSheetContent(previewSubject, Resolution.entries - Resolution.ACKNOWLEDGED, input, {}, {}, {}, {}, {})
    }
}

@Preview(name = "Weiterleiten · Light", widthDp = 411)
@Composable
private fun ResolutionSheetPreview() {
    LumaTheme(darkTheme = false) { ResolutionSample(previewInput) }
}

@Preview(name = "Pflichtnotiz fehlt · Dark", widthDp = 411)
@Composable
private fun ResolutionSheetDarkPreview() {
    LumaTheme(darkTheme = true) {
        ResolutionSample(
            previewInput.copy(resolution = Resolution.DONE, noteRequired = true, showNoteError = true, confirmEnabled = false),
        )
    }
}

@Preview(name = "Schrift 200 %", widthDp = 411, heightDp = 1600, fontScale = 2f)
@Composable
private fun ResolutionSheetLargeFontPreview() {
    LumaTheme(darkTheme = false) { ResolutionSample(previewInput) }
}

@Preview(name = "Notiz · Light", widthDp = 411)
@Composable
private fun NoteSheetPreview() {
    LumaTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            NoteSheetContent(previewSubject, note = "Werte gemessen", showNoteError = false, saveEnabled = true, {}, {}, {})
        }
    }
}
