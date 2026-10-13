package com.mulkatz.lumacare.ui.detail

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mulkatz.lumacare.LumaApplication
import com.mulkatz.lumacare.R
import com.mulkatz.lumacare.domain.EventActionType
import com.mulkatz.lumacare.domain.EventCommand
import com.mulkatz.lumacare.domain.EventEntry
import com.mulkatz.lumacare.domain.EventStatus
import com.mulkatz.lumacare.domain.EventType
import com.mulkatz.lumacare.domain.Handling
import com.mulkatz.lumacare.domain.LoggedAction
import com.mulkatz.lumacare.domain.PersonDetail
import com.mulkatz.lumacare.domain.PersonHistoryEntry
import com.mulkatz.lumacare.domain.Resolution
import com.mulkatz.lumacare.ui.actions.ActionInputState
import com.mulkatz.lumacare.ui.actions.ActionSheet
import com.mulkatz.lumacare.ui.actions.InputCheck
import com.mulkatz.lumacare.ui.actions.PersonActions
import com.mulkatz.lumacare.ui.actions.PersonActionsViewModel
import com.mulkatz.lumacare.ui.actions.runAction
import com.mulkatz.lumacare.ui.components.ActionEmphasis
import com.mulkatz.lumacare.ui.components.BottomActionBar
import com.mulkatz.lumacare.ui.components.EventActionButton
import com.mulkatz.lumacare.ui.components.IconLabel
import com.mulkatz.lumacare.ui.components.NoteSheet
import com.mulkatz.lumacare.ui.components.QuietNotice
import com.mulkatz.lumacare.ui.components.ResolutionInput
import com.mulkatz.lumacare.ui.components.ResolutionLabel
import com.mulkatz.lumacare.ui.components.ResolutionSheet
import com.mulkatz.lumacare.ui.components.SecondaryActionButton
import com.mulkatz.lumacare.ui.components.SectionHeader
import com.mulkatz.lumacare.ui.components.SheetSubject
import com.mulkatz.lumacare.ui.components.SeverityBadge
import com.mulkatz.lumacare.ui.components.SubpageAppBar
import com.mulkatz.lumacare.ui.components.TimeColumn
import com.mulkatz.lumacare.ui.components.TimeFormat
import com.mulkatz.lumacare.ui.components.colors
import com.mulkatz.lumacare.ui.components.iconRes
import com.mulkatz.lumacare.ui.components.title
import com.mulkatz.lumacare.ui.components.labelRes
import com.mulkatz.lumacare.ui.dashboard.DashboardPreviewData
import com.mulkatz.lumacare.ui.dashboard.DashboardViewModel
import com.mulkatz.lumacare.ui.theme.LumaSizes
import com.mulkatz.lumacare.ui.theme.LumaTheme
import com.mulkatz.lumacare.ui.theme.tabularNumbers
import java.time.LocalDateTime

/** Holt denselben Zustand wie das Dashboard und leitet das Detail der Person daraus ab; Aktionen über [actions]. */
@Composable
fun PersonDetailRoute(
    personId: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.factory((LocalContext.current.applicationContext as LumaApplication).container),
    ),
    actions: PersonActionsViewModel = viewModel(
        factory = PersonActionsViewModel.factory((LocalContext.current.applicationContext as LumaApplication).container),
    ),
) {
    val dashboardState by viewModel.uiState.collectAsStateWithLifecycle()
    val state = remember(dashboardState, personId) { PersonDetailUiState.from(dashboardState, personId) }
    PersonDetailScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
        modifier = modifier,
        input = actions.input,
        actions = actions,
    )
}

/**
 * Personendetail ohne eigenen Zustand: Kopf, offene Luma-Hinweise, Kontext, Verlauf (Hinweise und Aktionen der
 * Pflege), darunter „Über die Person“ (Vorlieben, Biografie). [onBack] `null` heißt Detailbereich auf dem Tablet, ohne
 * Zurück-Appbar.
 *
 * Aktionen: Die Leiste unten gilt dem maßgeblichen Hinweis (primär je nach Zustand, sekundär „Notiz“). Weitere
 * Hinweise haben ihre Aktion umrandet in der eigenen Karte; eigene Fälle zusätzlich „Zurückgeben“ als
 * Textaktion. Abschließen und Notiz laufen über Sheets, deren Eingabe in [input] liegt.
 */
@Composable
fun PersonDetailScreen(
    state: PersonDetailUiState,
    onBack: (() -> Unit)?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    input: ActionInputState = ActionInputState(),
    actions: PersonActions = PersonActions.None,
) {
    val detail = (state as? PersonDetailUiState.Ready)?.detail
    val lead = detail?.hints?.firstOrNull()
    // Zweispaltig (ohne Zurück) steht der Name im Kopf, bündig mit dem ShiftHeader links.
    val inPane = onBack == null
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { SubpageAppBar(title = if (inPane) detail?.resident?.fullName else null, onBack = onBack) },
        bottomBar = {
            if (lead != null) DetailActionBar(lead, referToLead = detail.hints.size > 1, now = detail.now, input = input, actions = actions)
        },
    ) { innerPadding ->
        when (state) {
            PersonDetailUiState.Loading -> CenteredMessage(innerPadding) {
                Text(
                    text = stringResource(R.string.dashboard_loading),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            PersonDetailUiState.Error -> CenteredMessage(innerPadding) {
                Text(
                    text = stringResource(R.string.dashboard_error_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                Button(onClick = onRetry, modifier = Modifier.heightIn(min = LumaSizes.minTouchTarget)) {
                    Text(text = stringResource(R.string.action_retry))
                }
            }
            PersonDetailUiState.NotFound -> CenteredMessage(innerPadding) {
                Text(
                    text = stringResource(R.string.detail_not_found),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            is PersonDetailUiState.Ready -> DetailContent(state.detail, innerPadding, showName = !inPane, input = input, actions = actions)
        }
    }
    if (state is PersonDetailUiState.Ready) ActionSheets(state, input, actions)
}

/**
 * Aktionen des maßgeblichen Hinweises; gesperrt, solange zu ihm gespeichert wird. Bei mehreren offenen
 * Hinweisen ([referToLead]) nennt eine Zeile über den Buttons den Hinweis („Zum Hinweis von gestern 23:15“).
 */
@Composable
private fun DetailActionBar(
    lead: EventEntry,
    referToLead: Boolean,
    now: LocalDateTime,
    input: ActionInputState,
    actions: PersonActions,
) {
    val res = LocalResources.current
    val busy = input.isBusy(lead.event.id)
    BottomActionBar(
        caption = if (referToLead) {
            stringResource(R.string.history_hint_reference, TimeFormat.clockLabel(res, lead.event.timestamp, now))
        } else {
            null
        },
        primary = { modifier ->
            EventActionButton(
                action = lead.action,
                onClick = { actions.runAction(lead) },
                emphasis = ActionEmphasis.FILLED,
                modifier = modifier,
                enabled = !busy,
            )
        },
        secondary = {
            SecondaryActionButton(
                text = stringResource(R.string.action_note),
                onClick = { actions.openNote(lead.event.id) },
                testTag = "action_note",
                icon = R.drawable.ic_note,
                enabled = !busy,
            )
        },
    )
}

/** Abschluss- bzw. Notiz-Sheet, wenn es einem offenen Hinweis dieser Person gilt. */
@Composable
private fun ActionSheets(state: PersonDetailUiState.Ready, input: ActionInputState, actions: PersonActions) {
    val sheet = input.sheet ?: return
    val event = state.detail.hints.firstOrNull { it.event.id == sheet.eventId }?.event ?: return
    val resident = state.detail.resident
    val subject = SheetSubject(stringResource(R.string.name_and_room, resident.fullName, resident.room), event.summary)
    val draft = input.draft(event.id)
    val busy = input.isBusy(event.id)
    when (sheet) {
        is ActionSheet.Resolve -> {
            // Nur erlaubte Abschlussarten; eine nicht (mehr) erlaubte Wahl fällt auf die erste erlaubte zurück.
            val options = InputCheck.resolutionOptions(event, state.shift)
            val chosen = input.resolution?.takeIf { it in options } ?: options.firstOrNull() ?: return
            val command = input.resolveCommand(event.id).let {
                it.copy(resolution = chosen, forwardedTo = input.forwardTarget.takeIf { chosen == Resolution.FORWARDED })
            }
            val check = InputCheck.resolve(event, state.shift, command)
            ResolutionSheet(
                subject = subject,
                options = options,
                input = ResolutionInput(
                    resolution = command.resolution,
                    forwardTarget = input.forwardTarget,
                    note = draft,
                    noteRequired = check.noteRequired,
                    showNoteError = input.noteRejected || check.noteInvalid,
                    confirmEnabled = check.valid && !busy,
                ),
                onResolutionChange = actions::selectResolution,
                onForwardTargetChange = actions::selectForwardTarget,
                onNoteChange = { actions.updateDraft(event.id, it) },
                onSnippet = { actions.insertSnippet(event.id, it) },
                onConfirm = { actions.perform(event, command) },
                onDismiss = actions::dismissSheet,
            )
        }
        is ActionSheet.Note -> {
            val check = InputCheck.note(event, state.shift, draft)
            NoteSheet(
                subject = subject,
                note = draft,
                showNoteError = input.noteRejected || check.noteInvalid,
                saveEnabled = check.valid && !busy,
                onNoteChange = { actions.updateDraft(event.id, it) },
                onSnippet = { actions.insertSnippet(event.id, it) },
                onSave = { actions.saveNote(event) },
                onDismiss = actions::dismissSheet,
            )
        }
    }
}

@Composable
private fun CenteredMessage(innerPadding: PaddingValues, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(LumaTheme.spacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LumaTheme.spacing.l),
        ) { content() }
    }
}

/** Inhalt höchstens [LumaSizes.contentMaxWidth] breit, mit Seitenrand. */
@Composable
private fun Modifier.contentWidth(): Modifier =
    widthIn(max = LumaSizes.contentMaxWidth)
        .fillMaxWidth()
        .padding(horizontal = LumaTheme.spacing.l)

@Composable
private fun DetailContent(
    detail: PersonDetail,
    innerPadding: PaddingValues,
    showName: Boolean,
    input: ActionInputState,
    actions: PersonActions,
) {
    val spacing = LumaTheme.spacing
    val layoutDirection = LocalLayoutDirection.current
    // Oben endet die Liste unter der Appbar; unten scrollt der Inhalt unter die Navigationsleiste.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = innerPadding.calculateStartPadding(layoutDirection),
                top = innerPadding.calculateTopPadding(),
                end = innerPadding.calculateEndPadding(layoutDirection),
            )
            .verticalScroll(rememberScrollState())
            .padding(top = spacing.l, bottom = innerPadding.calculateBottomPadding() + spacing.xxl)
            .testTag("person_detail"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.xl),
    ) {
        Header(detail, showName)
        if (detail.hints.isNotEmpty()) {
            HintsSection(detail, input, actions)
        } else {
            // Nach dem Abschluss: ruhiger Hinweis statt eines Sprungs zum Kontext.
            QuietNotice(
                icon = R.drawable.ic_check_circle,
                text = stringResource(R.string.detail_no_open_hints),
                modifier = Modifier.contentWidth().testTag("detail_no_hints"),
            )
        }
        ContextSection(detail)
        if (detail.history.isNotEmpty()) HistorySection(detail)
        AboutSection(detail)
    }
}

/** Name (nur einspaltig; zweispaltig steht er im Kopf) und „Zimmer · Pflegegrad“. */
@Composable
private fun Header(detail: PersonDetail, showName: Boolean) {
    val resident = detail.resident
    Column(
        modifier = Modifier.contentWidth(),
        verticalArrangement = Arrangement.spacedBy(LumaTheme.spacing.xs),
    ) {
        if (showName) {
            Text(
                text = resident.fullName,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { heading() },
            )
        }
        Text(
            text = stringResource(R.string.detail_room_care_level, resident.room, resident.careLevel),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Karte im Detail; [highlighted] (Bezug der Aktionsleiste) mit Rand 2 dp `primary`, sonst `outlineVariant`. */
@Composable
private fun DetailCard(modifier: Modifier = Modifier, highlighted: Boolean = false, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = if (highlighted) {
            BorderStroke(LumaSizes.selectedBorder, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(LumaSizes.border, MaterialTheme.colorScheme.outlineVariant)
        },
    ) {
        Column { content() }
    }
}

// ---------- Luma-Hinweise ----------

@Composable
private fun HintsSection(detail: PersonDetail, input: ActionInputState, actions: PersonActions) {
    Column(modifier = Modifier.contentWidth()) {
        SectionHeader(title = stringResource(R.string.detail_section_hints), count = detail.hints.size)
        val several = detail.hints.size > 1
        Column(verticalArrangement = Arrangement.spacedBy(LumaTheme.spacing.m)) {
            detail.hints.forEachIndexed { index, entry ->
                key(entry.event.id) {
                    HintCard(
                        entry = entry,
                        detail = detail,
                        isLead = index == 0,
                        // Bei mehreren Hinweisen markiert der Rand, welchem die Aktionsleiste gilt.
                        highlighted = several && index == 0,
                        busy = input.isBusy(entry.event.id),
                        actions = actions,
                    )
                }
            }
        }
    }
}

/**
 * Ein offener Hinweis: Dringlichkeit, Herkunft bzw. „Bei dir“, Metazeile und die Originalaussage ungekürzt.
 * Der maßgebliche Hinweis ([isLead]) hat seine Aktion in der Leiste unten, alle anderen umrandet in der Karte.
 * Eigene Fälle bieten „Zurückgeben“ als Textaktion.
 */
@Composable
private fun HintCard(
    entry: EventEntry,
    detail: PersonDetail,
    isLead: Boolean,
    highlighted: Boolean,
    busy: Boolean,
    actions: PersonActions,
) {
    val res = LocalResources.current
    val event = entry.event
    val own = entry.handling == Handling.OWN
    val source = stringResource(event.type.sourceRes)
    val meta = stringResource(
        R.string.detail_hint_meta,
        source,
        TimeFormat.clockLabel(res, event.timestamp, detail.now),
        TimeFormat.age(res, event.timestamp, detail.now),
    )
    val actionRow = own || !isLead
    DetailCard(
        modifier = Modifier
            .testTag("detail_hint_${event.id}")
            .semantics(mergeDescendants = true) {},
        highlighted = highlighted,
    ) {
        Column(
            // Mit Aktionszeile unten weniger Rand: Die Buttons bringen ihre 48-dp-Fläche selbst mit.
            modifier = Modifier.padding(
                start = LumaTheme.spacing.l,
                end = LumaTheme.spacing.l,
                top = LumaTheme.spacing.l,
                bottom = if (actionRow) LumaTheme.spacing.s else LumaTheme.spacing.l,
            ),
            verticalArrangement = Arrangement.spacedBy(LumaTheme.spacing.s),
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(LumaTheme.spacing.m),
                verticalArrangement = Arrangement.spacedBy(LumaTheme.spacing.xs),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                SeverityBadge(event.severity)
                if (entry.isFromNightShift) {
                    IconLabel(
                        icon = R.drawable.ic_moon,
                        text = stringResource(R.string.handling_from_night),
                        iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                        textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (own) {
                    // Neutral wie „Aus Nachtdienst“, nicht in `primary` (sähe aus wie ein Link).
                    IconLabel(
                        icon = R.drawable.ic_person,
                        text = stringResource(R.string.handling_own),
                        iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                        textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("handling_own_${event.id}"),
                    )
                }
            }
            Text(
                text = meta,
                style = MaterialTheme.typography.bodyMedium.tabularNumbers(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Hinweis statt Diagnose: Originalaussage unverändert und ungekürzt.
            Text(text = event.summary, style = MaterialTheme.typography.bodyLarge)
            if (actionRow) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (own) {
                        // Bündig zur Textkante: links ohne Innenabstand, Tap-Fläche bleibt 48 dp hoch.
                        TextButton(
                            onClick = { actions.perform(event, EventCommand.Release) },
                            enabled = !busy,
                            contentPadding = PaddingValues(end = LumaTheme.spacing.m),
                            modifier = Modifier
                                .heightIn(min = LumaSizes.minTouchTarget)
                                .testTag("action_release"),
                        ) {
                            Text(stringResource(R.string.action_release))
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    if (!isLead) {
                        EventActionButton(
                            action = entry.action,
                            onClick = { actions.runAction(entry) },
                            emphasis = ActionEmphasis.OUTLINED,
                            enabled = !busy,
                        )
                    }
                }
            }
        }
    }
}

/** Wie Luma vom Anliegen erfahren hat: „Anruf“, „Nachricht“, sonst „Gespräch“. */
@get:StringRes
private val EventType.sourceRes: Int
    get() = when (this) {
        EventType.MISSED_CALL -> R.string.detail_source_call
        EventType.FAMILY_MESSAGE -> R.string.detail_source_message
        EventType.HEALTH_HINT,
        EventType.MOOD,
        EventType.REQUEST,
        EventType.MEAL_WISH,
        EventType.ACTIVITY_SIGNUP,
        -> R.string.detail_source_conversation
    }

// ---------- Kontext ----------

@Composable
private fun ContextSection(detail: PersonDetail) {
    val resident = detail.resident
    val separator = stringResource(R.string.list_separator)
    val rows = buildList<@Composable () -> Unit> {
        if (resident.healthNotes.isNotEmpty()) {
            add { ContextRow(stringResource(R.string.detail_context_health)) { ContextText(resident.healthNotes.joinToString(separator)) } }
        }
        if (detail.routinesToday.isNotEmpty()) {
            add {
                ContextRow(stringResource(R.string.detail_context_routine)) {
                    detail.routinesToday.forEach { item ->
                        val line = stringResource(
                            if (item.routine.lumaCall) R.string.detail_routine_planned_call else R.string.detail_routine_planned,
                            TimeFormat.clock(item.time),
                            item.routine.title,
                        )
                        ContextText(line, icon = R.drawable.ic_repeat)
                    }
                }
            }
        }
        if (detail.activities.isNotEmpty()) {
            add {
                ContextRow(stringResource(R.string.detail_context_activity)) {
                    detail.activities.forEach { activity ->
                        ContextText(
                            stringResource(R.string.detail_activity_line, TimeFormat.clock(activity.start), activity.title, activity.location),
                            icon = R.drawable.ic_group,
                        )
                    }
                }
            }
        }
        val contactName = resident.contactName
        if (contactName != null) {
            add {
                val relation = resident.contactRelation
                ContextRow(stringResource(R.string.detail_context_contact)) {
                    ContextText(if (relation != null) stringResource(R.string.detail_contact_line, contactName, relation) else contactName)
                }
            }
        }
    }
    if (rows.isEmpty()) return

    Column(modifier = Modifier.contentWidth().testTag("detail_context")) {
        SectionHeader(title = stringResource(R.string.detail_section_context))
        DetailCard {
            rows.forEachIndexed { index, row ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                row()
            }
        }
    }
}

@Composable
private fun ContextRow(label: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .padding(LumaTheme.spacing.l),
        verticalArrangement = Arrangement.spacedBy(LumaTheme.spacing.xs),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

@Composable
private fun ContextText(text: String, @DrawableRes icon: Int? = null) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(LumaTheme.spacing.s),
    ) {
        if (icon != null) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(top = LumaTheme.spacing.xs)
                    .size(LumaSizes.iconSmall),
            )
        }
        Text(text = text, style = MaterialTheme.typography.bodyLarge.tabularNumbers())
    }
}

// ---------- Verlauf ----------

@Composable
private fun HistorySection(detail: PersonDetail) {
    Column(modifier = Modifier.contentWidth().testTag("detail_history")) {
        SectionHeader(title = stringResource(R.string.detail_section_history))
        DetailCard {
            detail.history.forEachIndexed { index, entry ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                val action = entry.action
                if (action != null) ActionHistoryRow(action, detail) else HistoryRow(entry, detail)
            }
        }
    }
}

@Composable
private fun HistoryRow(entry: PersonHistoryEntry, detail: PersonDetail) {
    val res = LocalResources.current
    val event = entry.event
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .testTag("detail_history_item_${event.id}")
            .padding(LumaTheme.spacing.l),
        horizontalArrangement = Arrangement.spacedBy(LumaTheme.spacing.m),
    ) {
        TimeColumn(time = event.timestamp, now = detail.now)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(LumaTheme.spacing.xs),
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(LumaTheme.spacing.m),
                verticalArrangement = Arrangement.spacedBy(LumaTheme.spacing.xs),
            ) {
                IconLabel(
                    icon = event.severity.iconRes,
                    text = stringResource(event.severity.labelRes),
                    // Erledigt: Dringlichkeit nur noch als Text, Icon neutral (wie im Schichtverlauf).
                    iconTint = if (entry.isDone) MaterialTheme.colorScheme.onSurfaceVariant else event.severity.colors.content,
                    textColor = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.detail_history_luma),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                when {
                    entry.isDone -> IconLabel(
                        icon = R.drawable.ic_check_circle,
                        text = stringResource(R.string.status_done),
                        iconTint = LumaTheme.status.done.content,
                        textColor = LumaTheme.status.done.content,
                    )
                    entry.isFromNightShift -> IconLabel(
                        icon = R.drawable.ic_moon,
                        text = stringResource(R.string.handling_from_night),
                        iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                        textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            // Ungekürzt: Erledigte Hinweise stehen nur hier.
            Text(text = event.summary, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/**
 * Aktion der Pflege im Verlauf: „Übernommen (Frühdienst)“, „Abgeschlossen: Erledigt“ usw., der zugehörige
 * Hinweis über seine Uhrzeit und die Notiz ungekürzt. Ohne Namen der Pflege.
 */
@Composable
private fun ActionHistoryRow(action: LoggedAction, detail: PersonDetail) {
    val res = LocalResources.current
    val note = action.record.note
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .testTag("detail_history_action_${action.record.id}")
            .padding(LumaTheme.spacing.l),
        horizontalArrangement = Arrangement.spacedBy(LumaTheme.spacing.m),
    ) {
        TimeColumn(time = action.time, now = detail.now)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(LumaTheme.spacing.xs),
        ) {
            val resolution = action.after.resolution
            if (action.record.type == EventActionType.RESOLVE && resolution != null) {
                ResolutionLabel(resolution, action.record.forwardedTo)
            } else {
                IconLabel(
                    icon = action.iconRes,
                    text = action.title(),
                    iconTint = if (action.after.status == EventStatus.DONE) LumaTheme.status.done.content else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(R.string.history_hint_reference, TimeFormat.clockLabel(res, action.event.timestamp, detail.now)),
                style = MaterialTheme.typography.bodyMedium.tabularNumbers(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (note != null) Text(text = note, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

// ---------- Über die Person ----------

/** Vorlieben und Biografie: was die Person ausmacht, nicht was jetzt zu tun ist. Gleicher Aufbau wie „Kontext“. */
@Composable
private fun AboutSection(detail: PersonDetail) {
    val resident = detail.resident
    val separator = stringResource(R.string.list_separator)
    if (resident.preferences.isEmpty() && resident.biography.isEmpty()) return
    Column(modifier = Modifier.contentWidth().testTag("detail_about")) {
        SectionHeader(title = stringResource(R.string.detail_section_about))
        DetailCard {
            if (resident.preferences.isNotEmpty()) {
                ContextRow(stringResource(R.string.detail_preferences)) { ContextText(resident.preferences.joinToString(separator)) }
            }
            if (resident.preferences.isNotEmpty() && resident.biography.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            if (resident.biography.isNotEmpty()) {
                ContextRow(stringResource(R.string.detail_biography)) { ContextText(resident.biography.joinToString(separator)) }
            }
        }
    }
}

// ---------- Previews ----------

/** Rudolf Schmidt aus den Preview-Daten, ergänzt um fiktive Vorlieben, Biografie und Kontakt. */
private val previewState: PersonDetailUiState by lazy {
    val ready = DashboardPreviewData.ready
    val residents = ready.data.residents.map { resident ->
        if (resident.id != "r12") {
            resident
        } else {
            resident.copy(
                biography = listOf("Landwirt in Dithmarschen", "Spricht hauptsächlich Platt"),
                preferences = listOf("Ruhige Umgebung", "Landleben-Erinnerungen", "Milchreis"),
                contactName = "Hans Schmidt",
                contactRelation = "Sohn",
            )
        }
    }
    PersonDetailUiState.from(ready.copy(data = ready.data.copy(residents = residents)), "r12")
}

@Preview(name = "Phone · Light", heightDp = 1400)
@Composable
private fun PersonDetailPreview() {
    LumaTheme(darkTheme = false) { PersonDetailScreen(previewState, onBack = {}, onRetry = {}) }
}

@Preview(name = "Phone · Dark", heightDp = 1400)
@Composable
private fun PersonDetailDarkPreview() {
    LumaTheme(darkTheme = true) { PersonDetailScreen(previewState, onBack = {}, onRetry = {}) }
}

@Preview(name = "Phone · Schrift 200 %", heightDp = 2400, fontScale = 2f)
@Composable
private fun PersonDetailLargeFontPreview() {
    LumaTheme(darkTheme = false) { PersonDetailScreen(previewState, onBack = {}, onRetry = {}) }
}

@Preview(name = "Tablet · Detailbereich", device = Devices.TABLET)
@Composable
private fun PersonDetailTabletPreview() {
    LumaTheme(darkTheme = false) { PersonDetailScreen(previewState, onBack = null, onRetry = {}) }
}
