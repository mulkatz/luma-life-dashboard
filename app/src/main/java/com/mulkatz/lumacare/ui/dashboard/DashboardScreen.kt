package com.mulkatz.lumacare.ui.dashboard

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mulkatz.lumacare.LumaApplication
import com.mulkatz.lumacare.R
import com.mulkatz.lumacare.domain.EventAction
import com.mulkatz.lumacare.domain.EventEntry
import com.mulkatz.lumacare.domain.EventType
import com.mulkatz.lumacare.domain.LumaEvent
import com.mulkatz.lumacare.domain.PersonEntry
import com.mulkatz.lumacare.domain.Resolution
import com.mulkatz.lumacare.domain.ResolvedEntry
import com.mulkatz.lumacare.domain.UpcomingItem
import com.mulkatz.lumacare.ui.components.EmptyState
import com.mulkatz.lumacare.ui.components.PersonCard
import com.mulkatz.lumacare.ui.components.ShiftHeader
import com.mulkatz.lumacare.ui.components.ShiftTimeline
import com.mulkatz.lumacare.ui.components.TimeFormat
import com.mulkatz.lumacare.ui.components.colors
import com.mulkatz.lumacare.ui.components.iconRes
import com.mulkatz.lumacare.ui.components.labelRes
import com.mulkatz.lumacare.ui.components.resolutionText
import com.mulkatz.lumacare.ui.components.shortUnitName
import com.mulkatz.lumacare.ui.components.testTag
import com.mulkatz.lumacare.ui.theme.LumaSizes
import com.mulkatz.lumacare.ui.theme.LumaTheme
import com.mulkatz.lumacare.ui.theme.tabularNumbers
import java.time.LocalDateTime

/** Holt das ViewModel und sammelt den Zustand lebenszyklusbewusst. */
@Composable
fun DashboardRoute(
    onPersonClick: (String) -> Unit,
    onTimelineClick: () -> Unit,
    /** Schnellaktion des maßgeblichen Hinweises einer Karte. */
    onEventAction: (EventEntry) -> Unit,
    /** `true`, solange zu diesem Hinweis gespeichert wird. */
    isBusy: (String) -> Boolean,
    modifier: Modifier = Modifier,
    /** Gewählte Person im List-Detail (Tablet); auf dem Phone `null`. */
    selectedPersonId: String? = null,
    viewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.factory((LocalContext.current.applicationContext as LumaApplication).container),
    ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DashboardScreen(
        state = state,
        onRetry = viewModel::retry,
        onPersonClick = onPersonClick,
        onTimelineClick = onTimelineClick,
        onEventAction = onEventAction,
        isBusy = isBusy,
        modifier = modifier,
        selectedPersonId = selectedPersonId,
    )
}

/**
 * Dashboard ohne eigenen Zustand. Reihenfolge: Jetzt handeln, Demnächst, Schichtverlauf, Zeitnah prüfen,
 * Organisatorisches. Abschnitte ohne Einträge entfallen; der Schichtverlauf steht immer da, auch unter
 * „Alles erledigt“, darunter dann „In dieser Schicht erledigt“. „Alles erledigt“ erscheint nur in
 * [DashboardUiState.Ready].
 */
@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onRetry: () -> Unit,
    onPersonClick: (String) -> Unit,
    onTimelineClick: () -> Unit,
    modifier: Modifier = Modifier,
    onEventAction: (EventEntry) -> Unit = {},
    isBusy: (String) -> Boolean = { false },
    selectedPersonId: String? = null,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            if (state is DashboardUiState.Ready) ReadyHeader(state) else HeaderPlaceholder()
        },
    ) { innerPadding ->
        when (state) {
            DashboardUiState.Loading -> LoadingContent(innerPadding)
            DashboardUiState.Error -> ErrorContent(innerPadding, onRetry)
            is DashboardUiState.Ready -> ReadyContent(
                state = state,
                innerPadding = innerPadding,
                selectedPersonId = selectedPersonId,
                onPersonClick = onPersonClick,
                onTimelineClick = onTimelineClick,
                actions = CardActions(onEventAction, isBusy),
            )
        }
    }
}

@Composable
private fun ReadyHeader(state: DashboardUiState.Ready) {
    val res = LocalResources.current
    val remaining = if (state.isShiftOver) null else TimeFormat.remainingShort(res, state.remaining)
    val remainingSpoken = if (state.isShiftOver) null else TimeFormat.remainingSpoken(res, state.remaining)
    ShiftHeader(
        area = shortUnitName(state.shift.unit),
        shift = stringResource(state.shift.type.labelRes),
        time = TimeFormat.clock(state.model.now),
        remaining = remaining,
        remainingSpoken = remainingSpoken,
    )
}

/** Aubergine-Fläche hinter der Statusleiste, solange noch keine Schichtdaten da sind. */
@Composable
private fun HeaderPlaceholder() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.onSecondary,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .padding(LumaTheme.spacing.l)
                .semantics { heading() },
        )
    }
}

// ---------- Laden und Fehler ----------

@Composable
private fun LoadingContent(innerPadding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(LumaTheme.spacing.l)
            .testTag("loading"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LumaTheme.spacing.m),
    ) {
        Text(
            text = stringResource(R.string.dashboard_loading),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.contentWidth(),
        )
        repeat(SKELETON_CARDS) { SkeletonCard() }
    }
}

private const val SKELETON_CARDS = 2

/** Ruhige Platzhalterkarte ohne Animation; rein dekorativ. */
@Composable
private fun SkeletonCard() {
    val block = MaterialTheme.colorScheme.surfaceVariant
    Card(
        modifier = Modifier
            .contentWidth()
            .clearAndSetSemantics {},
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(LumaSizes.border, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(LumaTheme.spacing.l),
            verticalArrangement = Arrangement.spacedBy(LumaTheme.spacing.s),
        ) {
            SkeletonLine(fraction = 0.5f, height = LumaTheme.spacing.xl, color = block)
            SkeletonLine(fraction = 0.3f, height = LumaTheme.spacing.l, color = block)
            SkeletonLine(fraction = 1f, height = LumaTheme.spacing.l, color = block)
            SkeletonLine(fraction = 0.8f, height = LumaTheme.spacing.l, color = block)
        }
    }
}

@Composable
private fun SkeletonLine(fraction: Float, height: Dp, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth(fraction)
            .height(height)
            .background(color, MaterialTheme.shapes.small),
    )
}

@Composable
private fun ErrorContent(innerPadding: PaddingValues, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(LumaTheme.spacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .contentWidth()
                .testTag("error_state"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LumaTheme.spacing.l),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_info),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(LumaSizes.iconLarge),
            )
            Text(
                text = stringResource(R.string.dashboard_error_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Button(
                onClick = onRetry,
                modifier = Modifier
                    .heightIn(min = LumaSizes.minTouchTarget)
                    .testTag("action_retry"),
            ) {
                Text(text = stringResource(R.string.action_retry))
            }
        }
    }
}

// ---------- Inhalt ----------

/** Schnellaktion einer Karte und ihre Sperre während des Speicherns. */
private class CardActions(val onEventAction: (EventEntry) -> Unit, val isBusy: (String) -> Boolean)

@Composable
private fun ReadyContent(
    state: DashboardUiState.Ready,
    innerPadding: PaddingValues,
    selectedPersonId: String?,
    onPersonClick: (String) -> Unit,
    onTimelineClick: () -> Unit,
    actions: CardActions,
) {
    val model = state.model
    val layoutDirection = LocalLayoutDirection.current
    val spacing = LumaTheme.spacing
    val noPersons = model.actNow.isEmpty() && model.checkSoon.isEmpty() && model.organizational.isEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = innerPadding.calculateStartPadding(layoutDirection),
            end = innerPadding.calculateEndPadding(layoutDirection),
            top = innerPadding.calculateTopPadding() + spacing.l,
            bottom = innerPadding.calculateBottomPadding() + spacing.xxl,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.xxl),
    ) {
        if (noPersons) {
            item(key = "empty_state") { EmptyState() }
        }
        if (model.actNow.isNotEmpty()) {
            item(key = "section_now") {
                PersonSection(
                    tag = "section_now",
                    section = Section.NOW,
                    entries = model.actNow,
                    now = model.now,
                    highlighted = true,
                    selectedPersonId = selectedPersonId,
                    onPersonClick = onPersonClick,
                    actions = actions,
                )
            }
        }
        if (model.upcoming.isNotEmpty()) {
            item(key = "section_soon") { UpcomingSection(model.upcoming, model.now, onPersonClick) }
        }
        item(key = "shift_timeline") {
            ShiftTimeline(
                hours = model.hours,
                summary = model.timeline.summary,
                onClick = onTimelineClick,
                modifier = Modifier.contentWidth(),
            )
        }
        if (model.checkSoon.isNotEmpty()) {
            item(key = "section_attention") {
                PersonSection(
                    tag = "section_attention",
                    section = Section.ATTENTION,
                    entries = model.checkSoon,
                    now = model.now,
                    highlighted = false,
                    selectedPersonId = selectedPersonId,
                    onPersonClick = onPersonClick,
                    actions = actions,
                )
            }
        }
        if (model.organizational.isNotEmpty()) {
            item(key = "section_info") {
                OrganizationalSection(model.organizational, model.now, selectedPersonId, onPersonClick, actions)
            }
        }
        if (noPersons && model.doneInShift.isNotEmpty()) {
            item(key = "section_done") { DoneSection(model.doneInShift) }
        }
    }
}

/** Inhalt höchstens [LumaSizes.contentMaxWidth] breit, mit Seitenrand. */
@Composable
private fun Modifier.contentWidth(): Modifier =
    widthIn(max = LumaSizes.contentMaxWidth)
        .fillMaxWidth()
        .padding(horizontal = LumaTheme.spacing.l)

private enum class Section(@StringRes val title: Int, @DrawableRes val icon: Int) {
    NOW(R.string.section_now, R.drawable.ic_urgent),
    SOON(R.string.section_soon, R.drawable.ic_calendar),
    ATTENTION(R.string.section_attention, R.drawable.ic_clock),
    INFO(R.string.section_info, R.drawable.ic_info),
    DONE(R.string.section_done, R.drawable.ic_check_circle),
}

@Composable
private fun SectionHeader(section: Section, count: Int) {
    val tint = when (section) {
        Section.NOW -> LumaTheme.status.urgent.content
        Section.SOON -> MaterialTheme.colorScheme.onSurfaceVariant
        Section.ATTENTION -> LumaTheme.status.attention.content
        Section.INFO -> LumaTheme.status.info.content
        Section.DONE -> LumaTheme.status.done.content
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = LumaTheme.spacing.m)
            .semantics(mergeDescendants = true) { heading() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LumaTheme.spacing.s),
    ) {
        Icon(
            painter = painterResource(section.icon),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(LumaSizes.iconMedium),
        )
        Text(
            text = stringResource(section.title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelLarge.tabularNumbers(),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
                .padding(horizontal = LumaTheme.spacing.s),
        )
    }
}

@Composable
private fun PersonSection(
    tag: String,
    section: Section,
    entries: List<PersonEntry>,
    now: LocalDateTime,
    highlighted: Boolean,
    selectedPersonId: String?,
    onPersonClick: (String) -> Unit,
    actions: CardActions,
) {
    Column(modifier = Modifier.contentWidth().testTag(tag)) {
        SectionHeader(section, entries.size)
        Column(verticalArrangement = Arrangement.spacedBy(LumaTheme.spacing.m)) {
            entries.forEach { entry ->
                key(entry.resident.id) {
                    val id = entry.resident.id
                    PersonCard(
                        entry = entry,
                        now = now,
                        highlighted = highlighted,
                        selected = id == selectedPersonId,
                        onClick = { onPersonClick(id) },
                        onAction = { actions.onEventAction(entry.leadEvent) },
                        actionEnabled = !actions.isBusy(entry.leadEvent.event.id),
                    )
                }
            }
        }
    }
}

// ---------- Demnächst ----------

@Composable
private fun UpcomingSection(items: List<UpcomingItem>, now: LocalDateTime, onPersonClick: (String) -> Unit) {
    Column(modifier = Modifier.contentWidth().testTag("section_soon")) {
        SectionHeader(Section.SOON, items.size)
        ListCard {
            items.forEachIndexed { index, item ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                UpcomingRow(item, now, onPersonClick)
            }
        }
    }
}

@Composable
private fun UpcomingRow(item: UpcomingItem, now: LocalDateTime, onPersonClick: (String) -> Unit) {
    val res = LocalResources.current
    val (title, meta, people) = when (item) {
        is UpcomingItem.RoutineItem -> Triple(
            item.routine.title,
            stringResource(R.string.upcoming_routine_meta, item.resident.fullName, item.resident.room),
            listOf(item.resident),
        )
        is UpcomingItem.ActivityItem -> Triple(
            item.activity.title,
            stringResource(R.string.upcoming_activity_meta, item.activity.location),
            item.participants,
        )
    }
    val names = people.associate { it.id to it.fullName }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .padding(LumaTheme.spacing.l),
        horizontalArrangement = Arrangement.spacedBy(LumaTheme.spacing.m),
    ) {
        Text(
            text = TimeFormat.clock(item.time),
            style = MaterialTheme.typography.titleMedium.tabularNumbers(),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(LumaTheme.spacing.xs),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = meta,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            item.openHints.forEach { hint ->
                names[hint.residentId]?.let { name ->
                    OpenHintLine(hint, name, onClick = { onPersonClick(hint.residentId) })
                }
            }
        }
        Text(
            text = TimeFormat.until(res, item.time, now),
            style = MaterialTheme.typography.bodyMedium.tabularNumbers(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Verknüpfung ohne Wiederholung des Hinweistexts und ohne Bewertung; Tippen öffnet die Person. */
@Composable
private fun OpenHintLine(hint: LumaEvent, name: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .heightIn(min = LumaSizes.minTouchTarget)
            .clickable(onClick = onClick)
            .testTag("open_hint_${hint.residentId}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LumaTheme.spacing.xs),
    ) {
        Icon(
            painter = painterResource(hint.severity.iconRes),
            contentDescription = stringResource(hint.severity.labelRes),
            tint = hint.severity.colors.content,
            modifier = Modifier.size(LumaSizes.iconSmall),
        )
        Text(
            text = stringResource(R.string.upcoming_open_hint, name),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

// ---------- Organisatorisches ----------

@Composable
private fun OrganizationalSection(
    entries: List<PersonEntry>,
    now: LocalDateTime,
    selectedPersonId: String?,
    onPersonClick: (String) -> Unit,
    actions: CardActions,
) {
    Column(modifier = Modifier.contentWidth().testTag("section_info")) {
        SectionHeader(Section.INFO, entries.size)
        ListCard {
            entries.forEachIndexed { index, entry ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                key(entry.resident.id) {
                    val id = entry.resident.id
                    OrganizationalRow(
                        entry = entry,
                        now = now,
                        selected = id == selectedPersonId,
                        onClick = { onPersonClick(id) },
                        onAction = { actions.onEventAction(entry.leadEvent) },
                        actionEnabled = !actions.isBusy(entry.leadEvent.event.id),
                    )
                }
            }
        }
    }
}

@Composable
private fun OrganizationalRow(
    entry: PersonEntry,
    now: LocalDateTime,
    selected: Boolean,
    onClick: () -> Unit,
    onAction: () -> Unit,
    actionEnabled: Boolean,
) {
    val res = LocalResources.current
    val lead = entry.leadEvent
    val moreHints = entry.events.size - 1
    // Eine Zeile bei Schrift ≤ 100 %, darüber nie gekürzt.
    val compact = LocalDensity.current.fontScale <= 1f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = LumaSizes.listRowMinHeight)
            .then(
                if (selected) {
                    Modifier
                        .border(LumaSizes.selectedBorder, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small)
                        .semantics { this.selected = true }
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick)
            .testTag("person_card_${entry.resident.id}")
            .padding(start = LumaTheme.spacing.l, end = LumaTheme.spacing.xs, top = LumaTheme.spacing.s, bottom = LumaTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LumaTheme.spacing.s),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.name_and_room, entry.resident.fullName, entry.resident.room),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(
                    R.string.hint_meta,
                    stringResource(lead.event.type.labelRes),
                    TimeFormat.clockLabel(res, lead.event.timestamp, now),
                ),
                style = MaterialTheme.typography.bodyMedium.tabularNumbers(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = lead.event.summary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (compact) 1 else Int.MAX_VALUE,
                overflow = TextOverflow.Ellipsis,
            )
            if (moreHints > 0) {
                Text(
                    text = res.getQuantityString(R.plurals.more_hints, moreHints, moreHints),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        TextButton(
            onClick = onAction,
            enabled = actionEnabled,
            modifier = Modifier
                .heightIn(min = LumaSizes.minTouchTarget)
                .testTag(lead.action.testTag),
        ) {
            if (lead.action == EventAction.FORWARD) {
                Icon(
                    painter = painterResource(R.drawable.ic_forward),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = LumaTheme.spacing.s)
                        .size(LumaSizes.iconSmall),
                )
            }
            Text(text = stringResource(lead.action.labelRes))
        }
    }
}

// ---------- In dieser Schicht erledigt (nur im leeren Zustand) ----------

@Composable
private fun DoneSection(entries: List<ResolvedEntry>) {
    Column(modifier = Modifier.contentWidth().testTag("section_done")) {
        SectionHeader(Section.DONE, entries.size)
        ListCard {
            entries.forEachIndexed { index, entry ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                key(entry.action.record.id) { DoneRow(entry) }
            }
        }
    }
}

/** „Werner Hoffmann · 2.03“, darunter das Ergebnis („Abgeschlossen: Erledigt · Notiz „…““), rechts die Uhrzeit. */
@Composable
private fun DoneRow(entry: ResolvedEntry) {
    val result = resolutionText(entry.resolution, entry.forwardedTo)
    val note = entry.note
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = LumaSizes.listRowMinHeight)
            .semantics(mergeDescendants = true) {}
            .testTag("done_${entry.event.id}")
            .padding(horizontal = LumaTheme.spacing.l, vertical = LumaTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LumaTheme.spacing.m),
    ) {
        Icon(
            painter = painterResource(if (entry.resolution == Resolution.FORWARDED) R.drawable.ic_forward else R.drawable.ic_check_circle),
            contentDescription = null,
            tint = LumaTheme.status.done.content,
            modifier = Modifier.size(LumaSizes.iconMedium),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.name_and_room, entry.resident.fullName, entry.resident.room),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = if (note != null) {
                    stringResource(R.string.hint_meta, result, stringResource(R.string.history_note_quoted, note))
                } else {
                    result
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = TimeFormat.clock(entry.time),
            style = MaterialTheme.typography.bodyMedium.tabularNumbers(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@get:StringRes
private val EventType.labelRes: Int
    get() = when (this) {
        EventType.HEALTH_HINT -> R.string.event_type_health_hint
        EventType.MOOD -> R.string.event_type_mood
        EventType.REQUEST -> R.string.event_type_request
        EventType.MEAL_WISH -> R.string.event_type_meal_wish
        EventType.ACTIVITY_SIGNUP -> R.string.event_type_activity_signup
        EventType.FAMILY_MESSAGE -> R.string.event_type_family_message
        EventType.MISSED_CALL -> R.string.event_type_missed_call
    }

@Composable
private fun ListCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = BorderStroke(LumaSizes.border, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column { content() }
    }
}

// ---------- Previews ----------

@Preview(name = "Ready · Light", heightDp = 1600)
@Composable
private fun DashboardReadyPreview() {
    LumaTheme(darkTheme = false) { DashboardScreen(DashboardPreviewData.ready, onRetry = {}, onPersonClick = {}, onTimelineClick = {}) }
}

@Preview(name = "Ready · Dark", heightDp = 1600)
@Composable
private fun DashboardReadyDarkPreview() {
    LumaTheme(darkTheme = true) { DashboardScreen(DashboardPreviewData.ready, onRetry = {}, onPersonClick = {}, onTimelineClick = {}) }
}

@Preview(name = "Ready · Schrift 200 %", heightDp = 1600, fontScale = 2f)
@Composable
private fun DashboardReadyLargeFontPreview() {
    LumaTheme(darkTheme = false) { DashboardScreen(DashboardPreviewData.ready, onRetry = {}, onPersonClick = {}, onTimelineClick = {}) }
}

@Preview(name = "Ready · Tablet", device = Devices.TABLET)
@Composable
private fun DashboardReadyTabletPreview() {
    LumaTheme(darkTheme = false) { DashboardScreen(DashboardPreviewData.ready, onRetry = {}, onPersonClick = {}, onTimelineClick = {}) }
}

@Preview(name = "Leer · Light")
@Composable
private fun DashboardEmptyPreview() {
    LumaTheme(darkTheme = false) { DashboardScreen(DashboardPreviewData.empty, onRetry = {}, onPersonClick = {}, onTimelineClick = {}) }
}

@Preview(name = "Leer · Dark")
@Composable
private fun DashboardEmptyDarkPreview() {
    LumaTheme(darkTheme = true) { DashboardScreen(DashboardPreviewData.empty, onRetry = {}, onPersonClick = {}, onTimelineClick = {}) }
}

@Preview(name = "Fehler · Light")
@Composable
private fun DashboardErrorPreview() {
    LumaTheme(darkTheme = false) { DashboardScreen(DashboardUiState.Error, onRetry = {}, onPersonClick = {}, onTimelineClick = {}) }
}

@Preview(name = "Fehler · Dark")
@Composable
private fun DashboardErrorDarkPreview() {
    LumaTheme(darkTheme = true) { DashboardScreen(DashboardUiState.Error, onRetry = {}, onPersonClick = {}, onTimelineClick = {}) }
}

@Preview(name = "Laden · Light")
@Composable
private fun DashboardLoadingPreview() {
    LumaTheme(darkTheme = false) { DashboardScreen(DashboardUiState.Loading, onRetry = {}, onPersonClick = {}, onTimelineClick = {}) }
}

@Preview(name = "Laden · Dark")
@Composable
private fun DashboardLoadingDarkPreview() {
    LumaTheme(darkTheme = true) { DashboardScreen(DashboardUiState.Loading, onRetry = {}, onPersonClick = {}, onTimelineClick = {}) }
}
