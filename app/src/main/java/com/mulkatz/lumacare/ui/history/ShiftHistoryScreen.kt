package com.mulkatz.lumacare.ui.history

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
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
import com.mulkatz.lumacare.domain.EventStatus
import com.mulkatz.lumacare.domain.HistoryEntry
import com.mulkatz.lumacare.domain.isFromNightShift
import com.mulkatz.lumacare.domain.Prioritizer
import com.mulkatz.lumacare.domain.ShiftHours
import com.mulkatz.lumacare.domain.ShiftType
import com.mulkatz.lumacare.ui.components.HourBlockLegend
import com.mulkatz.lumacare.ui.components.HourBlockRow
import com.mulkatz.lumacare.ui.components.TimeFormat
import com.mulkatz.lumacare.ui.components.colors
import com.mulkatz.lumacare.ui.components.SubpageAppBar
import com.mulkatz.lumacare.ui.components.iconRes
import com.mulkatz.lumacare.ui.components.labelRes
import com.mulkatz.lumacare.ui.dashboard.DashboardPreviewData
import com.mulkatz.lumacare.ui.dashboard.DashboardUiState
import com.mulkatz.lumacare.ui.dashboard.DashboardViewModel
import com.mulkatz.lumacare.ui.dashboard.labelRes
import com.mulkatz.lumacare.ui.theme.LumaSizes
import com.mulkatz.lumacare.ui.theme.LumaTheme
import com.mulkatz.lumacare.ui.theme.tabularNumbers
import java.time.LocalDateTime

/** Holt denselben Zustand wie das Dashboard; der Schichtverlauf hat keine eigene Datenquelle. */
@Composable
fun ShiftHistoryRoute(
    onBack: (() -> Unit)?,
    onPersonClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.factory((LocalContext.current.applicationContext as LumaApplication).container),
    ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ShiftHistoryScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
        onPersonClick = onPersonClick,
        modifier = modifier,
    )
}

/**
 * Schichtverlauf ohne eigenen Zustand: oben die Stundenblöcke mit Legende, darunter alle Hinweise der
 * Zeitleiste und die Routinen bis jetzt, jüngste oben, gruppiert nach laufender Schicht und Vorschicht.
 * [onBack] `null` heißt Detailbereich auf dem Tablet, ohne Zurück.
 */
@Composable
fun ShiftHistoryScreen(
    state: DashboardUiState,
    onBack: (() -> Unit)?,
    onRetry: () -> Unit,
    onPersonClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { SubpageAppBar(title = stringResource(R.string.shift_history_title), onBack = onBack) },
    ) { innerPadding ->
        when (state) {
            DashboardUiState.Loading -> CenteredMessage(innerPadding) {
                Text(
                    text = stringResource(R.string.dashboard_loading),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            DashboardUiState.Error -> CenteredMessage(innerPadding) {
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
            is DashboardUiState.Ready -> HistoryContent(state, innerPadding, onPersonClick)
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

@Composable
private fun HistoryContent(
    state: DashboardUiState.Ready,
    innerPadding: PaddingValues,
    onPersonClick: (String) -> Unit,
) {
    val model = state.model
    val history = model.history
    val layoutDirection = LocalLayoutDirection.current
    val spacing = LumaTheme.spacing
    val shiftLabel = stringResource(state.shift.type.labelRes)
    val shiftStart = TimeFormat.clock(model.timeline.shiftStart)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("shift_history"),
        contentPadding = PaddingValues(
            start = innerPadding.calculateStartPadding(layoutDirection),
            end = innerPadding.calculateEndPadding(layoutDirection),
            top = innerPadding.calculateTopPadding() + spacing.l,
            bottom = innerPadding.calculateBottomPadding() + spacing.xxl,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.xxl),
    ) {
        item(key = "hours") { HoursCard(model.hours, model.now) }
        if (history.currentShift.isNotEmpty()) {
            item(key = "history_group_current") {
                HistoryGroup(
                    tag = "history_group_current",
                    title = stringResource(R.string.shift_history_group_current, shiftLabel, shiftStart),
                    entries = history.currentShift,
                    currentShift = state.shift.type,
                    onPersonClick = onPersonClick,
                )
            }
        }
        if (history.previousShift.isNotEmpty()) {
            item(key = "history_group_previous") {
                HistoryGroup(
                    tag = "history_group_previous",
                    title = stringResource(
                        R.string.shift_history_group_previous,
                        TimeFormat.clock(model.timeline.start),
                        shiftStart,
                    ),
                    entries = history.previousShift,
                    currentShift = state.shift.type,
                    onPersonClick = onPersonClick,
                )
            }
        }
    }
}

/** Inhalt höchstens [LumaSizes.contentMaxWidth] breit, mit Seitenrand. */
@Composable
private fun Modifier.contentWidth(): Modifier =
    widthIn(max = LumaSizes.contentMaxWidth)
        .fillMaxWidth()
        .padding(horizontal = LumaTheme.spacing.l)

@Composable
private fun HistoryCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
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

/** Blockreihe wie auf dem Dashboard, hier mit Legende. */
@Composable
private fun HoursCard(hours: ShiftHours, now: LocalDateTime) {
    HistoryCard(modifier = Modifier.contentWidth()) {
        Column(
            modifier = Modifier.padding(
                start = LumaTheme.spacing.l,
                end = LumaTheme.spacing.l,
                top = LumaTheme.spacing.xs,
                bottom = LumaTheme.spacing.l,
            ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = LumaSizes.minTouchTarget),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = stringResource(R.string.shift_history_now, TimeFormat.clock(now)),
                    style = MaterialTheme.typography.titleMedium.tabularNumbers(),
                )
            }
            HourBlockRow(hours)
            HourBlockLegend()
        }
    }
}

@Composable
private fun HistoryGroup(
    tag: String,
    title: String,
    entries: List<HistoryEntry>,
    currentShift: ShiftType,
    onPersonClick: (String) -> Unit,
) {
    Column(modifier = Modifier.contentWidth().testTag(tag)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .padding(bottom = LumaTheme.spacing.m)
                .semantics { heading() },
        )
        HistoryCard {
            entries.forEachIndexed { index, entry ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                HistoryRow(entry, currentShift, onClick = { onPersonClick(entry.resident.id) })
            }
        }
    }
}

@Composable
private fun HistoryRow(entry: HistoryEntry, currentShift: ShiftType, onClick: () -> Unit) {
    val tag = when (entry) {
        is HistoryEntry.Hint -> "history_item_${entry.event.id}"
        is HistoryEntry.PlannedRoutine -> "history_routine_${entry.routine.id}"
    }
    val text = when (entry) {
        is HistoryEntry.Hint -> entry.event.summary
        is HistoryEntry.PlannedRoutine -> entry.routine.title
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = LumaSizes.minTouchTarget)
            .clickable(onClick = onClick)
            .testTag(tag)
            .padding(LumaTheme.spacing.l),
        horizontalArrangement = Arrangement.spacedBy(LumaTheme.spacing.m),
    ) {
        Text(
            text = TimeFormat.clock(entry.time),
            style = MaterialTheme.typography.titleSmall.tabularNumbers(),
            modifier = Modifier.widthIn(min = LumaSizes.timeColumnMinWidth),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(LumaTheme.spacing.xs),
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(LumaTheme.spacing.m),
                verticalArrangement = Arrangement.spacedBy(LumaTheme.spacing.xs),
            ) {
                when (entry) {
                    is HistoryEntry.Hint -> {
                        val event = entry.event
                        val done = event.status == EventStatus.DONE
                        Label(
                            icon = event.severity.iconRes,
                            text = stringResource(event.severity.labelRes),
                            // Erledigt: Dringlichkeit nur noch als Text, Icon neutral.
                            iconTint = if (done) MaterialTheme.colorScheme.onSurfaceVariant else event.severity.colors.content,
                            textColor = MaterialTheme.colorScheme.onSurface,
                        )
                        when {
                            done -> Label(
                                icon = R.drawable.ic_check_circle,
                                text = stringResource(R.string.status_done),
                                iconTint = LumaTheme.status.done.content,
                                textColor = LumaTheme.status.done.content,
                            )
                            isFromNightShift(event, Prioritizer.handling(event, currentShift)) -> Label(
                                icon = R.drawable.ic_moon,
                                text = stringResource(R.string.handling_from_night),
                                iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                                textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    is HistoryEntry.PlannedRoutine -> Label(
                        icon = R.drawable.ic_repeat,
                        text = stringResource(R.string.shift_history_routine),
                        iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                        textColor = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Text(
                text = stringResource(R.string.name_and_room, entry.resident.fullName, entry.resident.room),
                style = MaterialTheme.typography.titleSmall,
            )
            // Originaltext ungekürzt; Hinweis statt Diagnose.
            Text(text = text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun Label(@DrawableRes icon: Int, text: String, iconTint: Color, textColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LumaTheme.spacing.xs),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(LumaSizes.iconSmall),
        )
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = textColor)
    }
}

// ---------- Previews ----------

@Preview(name = "Light", heightDp = 2400)
@Composable
private fun ShiftHistoryPreview() {
    LumaTheme(darkTheme = false) {
        ShiftHistoryScreen(DashboardPreviewData.ready, onBack = {}, onRetry = {}, onPersonClick = {})
    }
}

@Preview(name = "Dark", heightDp = 2400)
@Composable
private fun ShiftHistoryDarkPreview() {
    LumaTheme(darkTheme = true) {
        ShiftHistoryScreen(DashboardPreviewData.ready, onBack = {}, onRetry = {}, onPersonClick = {})
    }
}

@Preview(name = "Schrift 200 %", heightDp = 2400, fontScale = 2f)
@Composable
private fun ShiftHistoryLargeFontPreview() {
    LumaTheme(darkTheme = false) {
        ShiftHistoryScreen(DashboardPreviewData.ready, onBack = {}, onRetry = {}, onPersonClick = {})
    }
}

@Preview(name = "Tablet", device = Devices.TABLET)
@Composable
private fun ShiftHistoryTabletPreview() {
    LumaTheme(darkTheme = false) {
        ShiftHistoryScreen(DashboardPreviewData.ready, onBack = {}, onRetry = {}, onPersonClick = {})
    }
}
