package com.mulkatz.lumacare.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mulkatz.lumacare.LumaApplication
import com.mulkatz.lumacare.R
import com.mulkatz.lumacare.domain.EventCommand
import com.mulkatz.lumacare.domain.EventEntry
import com.mulkatz.lumacare.domain.Resolution
import com.mulkatz.lumacare.ui.actions.ActionFeedback
import com.mulkatz.lumacare.ui.actions.PersonActionsViewModel
import com.mulkatz.lumacare.ui.actions.runAction
import com.mulkatz.lumacare.ui.components.EmptyState
import com.mulkatz.lumacare.ui.components.SubpageAppBar
import com.mulkatz.lumacare.ui.components.resolutionText
import com.mulkatz.lumacare.ui.dashboard.DashboardUiState
import com.mulkatz.lumacare.ui.dashboard.DashboardRoute
import com.mulkatz.lumacare.ui.dashboard.DashboardViewModel
import com.mulkatz.lumacare.ui.detail.PersonDetailRoute
import com.mulkatz.lumacare.ui.history.ShiftHistoryRoute
import com.mulkatz.lumacare.ui.theme.LumaSizes
import com.mulkatz.lumacare.ui.theme.LumaTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Ziel im Detailbereich. Der Navigator speichert nur [key] (ein String ist ohne Parcelize speicherbar);
 * [fromKey] macht daraus wieder das Ziel.
 */
sealed interface DetailTarget {
    val key: String

    data class Person(val id: String) : DetailTarget {
        override val key: String get() = PERSON_PREFIX + id
    }

    data object ShiftHistory : DetailTarget {
        override val key: String = "shift_history"
    }

    companion object {
        private const val PERSON_PREFIX = "person:"

        fun fromKey(key: String): DetailTarget? = when {
            key == ShiftHistory.key -> ShiftHistory
            key.startsWith(PERSON_PREFIX) -> Person(key.removePrefix(PERSON_PREFIX))
            else -> null
        }
    }
}

/**
 * Eine Spalte: Zurück führt zum vorherigen Inhalt (Person → Schichtverlauf → Dashboard).
 * Zwei Spalten: Zurück nur, wenn sich das Layout ändert; der Wechsel der Auswahl ist kein Schritt zurück,
 * System-Zurück verlässt dann die App.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private fun backBehavior(twoPane: Boolean): BackNavigationBehavior =
    if (twoPane) BackNavigationBehavior.PopUntilScaffoldValueChange else BackNavigationBehavior.PopUntilContentChange

/**
 * List-Detail nach `WindowSizeClass` über [NavigableListDetailPaneScaffold]: Compact und Medium eine Spalte,
 * das Detail als eigener Screen mit Zurück; Expanded links das Dashboard, rechts das Detail. Der Navigator
 * ist speicherbar, die Auswahl übersteht Rotation, Größenwechsel und Prozess-Tod. Alle Bereiche teilen sich
 * ein [DashboardViewModel] und ein [PersonActionsViewModel].
 *
 * Jede Statusänderung meldet sich über eine Snackbar (6 s bzw. nach Bedienungshilfen, mit „Rückgängig“) auf App-Ebene; sie ersetzt
 * Bestätigungsdialoge und bleibt beim Wechsel zwischen Dashboard und Detail stehen. Die Schnellaktion einer
 * Karte führt Übernehmen, Übernahme bestätigen und Zur Kenntnis direkt aus; Abschließen und Weiterleiten
 * brauchen Eingaben und öffnen das Detail mit dem Sheet.
 *
 * Neu geöffnete Ziele beginnen oben; per Zurück erreichte behalten ihre Scrollposition.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun LumaApp(
    viewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.factory((LocalContext.current.applicationContext as LumaApplication).container),
    ),
    actions: PersonActionsViewModel = viewModel(
        factory = PersonActionsViewModel.factory((LocalContext.current.applicationContext as LumaApplication).container),
    ),
) {
    val navigator = rememberListDetailPaneScaffoldNavigator<String>()
    val scope = rememberCoroutineScope()
    val detailStates = rememberSaveableStateHolder()
    val twoPane = navigator.scaffoldDirective.maxHorizontalPartitions > 1
    val target = navigator.currentDestination?.contentKey?.let(DetailTarget::fromKey)

    val open: (DetailTarget) -> Unit = { next ->
        if (next != target) {
            detailStates.removeState(next.key)
            scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, next.key) }
        }
    }
    val openPerson: (String) -> Unit = { open(DetailTarget.Person(it)) }
    // Auf dem Tablet bleibt die Liste sichtbar; dort braucht der Detailbereich kein Zurück.
    val backBehavior = backBehavior(twoPane)
    val onBack: (() -> Unit)? = if (twoPane) null else { { scope.launch { navigator.navigateBack(backBehavior) } } }
    val onEventAction: (EventEntry) -> Unit = { entry ->
        if (actions.runAction(entry)) openPerson(entry.event.residentId)
    }
    // Die Snackbar steht über der Aktionsleiste, aber nur, wenn das Detail eine hat (offene Hinweise).
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    val personId = (target as? DetailTarget.Person)?.id
    val hasActionBar by remember(personId) {
        derivedStateOf {
            val ready = uiState.value as? DashboardUiState.Ready
            personId != null && ready != null && ready.data.events.any { it.residentId == personId && it.isOpen }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        NavigableListDetailPaneScaffold(
            navigator = navigator,
            defaultBackBehavior = backBehavior,
            listPane = {
                AnimatedPane(modifier = Modifier.preferredWidth(LumaSizes.listPaneWidth)) {
                    DashboardRoute(
                        onPersonClick = openPerson,
                        onTimelineClick = { open(DetailTarget.ShiftHistory) },
                        onEventAction = onEventAction,
                        isBusy = actions::isBusy,
                        selectedPersonId = if (twoPane) (target as? DetailTarget.Person)?.id else null,
                        viewModel = viewModel,
                    )
                }
            },
            detailPane = {
                AnimatedPane {
                    when (target) {
                        null -> if (twoPane) SelectPersonPlaceholder()
                        else -> detailStates.SaveableStateProvider(target.key) {
                            when (target) {
                                is DetailTarget.Person -> PersonDetailRoute(
                                    personId = target.id,
                                    onBack = onBack,
                                    actions = actions,
                                    viewModel = viewModel,
                                )
                                DetailTarget.ShiftHistory -> ShiftHistoryRoute(
                                    onBack = onBack,
                                    onPersonClick = openPerson,
                                    viewModel = viewModel,
                                )
                            }
                        }
                    }
                }
            },
        )
        ActionSnackbar(
            actions = actions,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.exclude(WindowInsets.ime).only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                )
                .then(if (hasActionBar) Modifier.padding(bottom = LumaSizes.actionBarReserve) else Modifier),
        )
    }
}

/** Snackbar-Dauer ohne Bedienungshilfen: 6 s, länger als „kurz“ (4 s), damit „Rückgängig“ im Gehen erreichbar bleibt. */
private const val SNACKBAR_MILLIS = 6_000L

/**
 * Snackbar aus [PersonActionsViewModel.feedback]: ein Zustand, keine verbrauchte Nachricht. Eine neue
 * Rückmeldung ersetzt die alte, nach der (von den Bedienungshilfen ggf. verlängerten) Anzeigezeit verschwindet
 * sie. „Rückgängig“ ruft das ViewModel direkt auf; dafür muss keine Coroutine auf ein Ergebnis warten.
 */
@Composable
private fun ActionSnackbar(actions: PersonActionsViewModel, modifier: Modifier = Modifier) {
    val feedback = actions.feedback ?: return
    val undoable = feedback is ActionFeedback.Applied
    val accessibility = LocalAccessibilityManager.current
    val timeout = accessibility?.calculateRecommendedTimeoutMillis(
        SNACKBAR_MILLIS,
        containsIcons = false,
        containsText = true,
        containsControls = undoable,
    ) ?: SNACKBAR_MILLIS
    LaunchedEffect(feedback.id) {
        delay(timeout)
        actions.dismissFeedback(feedback.id)
    }
    val text = when (feedback) {
        is ActionFeedback.Applied -> feedback.command.feedbackText()
        is ActionFeedback.Stale -> stringResource(R.string.snackbar_stale)
        is ActionFeedback.UndoFailed -> stringResource(R.string.snackbar_undo_failed)
    }
    key(feedback.id) {
        Snackbar(
            modifier = modifier
                .widthIn(max = LumaSizes.contentMaxWidth)
                .padding(LumaTheme.spacing.m)
                .semantics { liveRegion = LiveRegionMode.Polite }
                .testTag("snackbar"),
            action = if (undoable) {
                {
                    TextButton(
                        onClick = { actions.undo(feedback.id) },
                        colors = ButtonDefaults.textButtonColors(contentColor = SnackbarDefaults.actionColor),
                        modifier = Modifier
                            .heightIn(min = LumaSizes.minTouchTarget)
                            .testTag("action_undo"),
                    ) {
                        Text(stringResource(R.string.action_undo))
                    }
                }
            } else {
                null
            },
        ) {
            Text(text)
        }
    }
}

/** Snackbar-Text zu einem geschriebenen Befehl: „Übernommen“, „Weitergeleitet an Küche“, … */
@Composable
private fun EventCommand.feedbackText(): String = when (this) {
    EventCommand.TakeOver -> stringResource(R.string.snackbar_taken_over)
    EventCommand.ConfirmHandover -> stringResource(R.string.snackbar_handover_confirmed)
    EventCommand.Release -> stringResource(R.string.snackbar_released)
    is EventCommand.AddNote -> stringResource(R.string.snackbar_note_saved)
    is EventCommand.Resolve -> when (resolution) {
        Resolution.FORWARDED, Resolution.ACKNOWLEDGED -> resolutionText(resolution, forwardedTo)
        Resolution.DONE, Resolution.NO_ACTION_NEEDED -> stringResource(R.string.snackbar_resolved)
    }
}

/** Detailbereich auf dem Tablet ohne Auswahl. Bewusst nicht „Alles erledigt“. */
@Composable
private fun SelectPersonPlaceholder() {
    Column(modifier = Modifier.fillMaxSize()) {
        SubpageAppBar(title = null, onBack = null)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            EmptyState(
                title = stringResource(R.string.detail_select_person),
                body = null,
                icon = R.drawable.ic_back,
                testTag = "detail_empty",
            )
        }
    }
}
