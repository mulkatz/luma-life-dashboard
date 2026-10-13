package com.mulkatz.lumacare.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mulkatz.lumacare.LumaApplication
import com.mulkatz.lumacare.R
import com.mulkatz.lumacare.ui.components.EmptyState
import com.mulkatz.lumacare.ui.components.SubpageAppBar
import com.mulkatz.lumacare.ui.dashboard.DashboardRoute
import com.mulkatz.lumacare.ui.dashboard.DashboardViewModel
import com.mulkatz.lumacare.ui.detail.PersonDetailRoute
import com.mulkatz.lumacare.ui.history.ShiftHistoryRoute
import com.mulkatz.lumacare.ui.theme.LumaSizes
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
 * ein [DashboardViewModel].
 *
 * Neu geöffnete Ziele beginnen oben; per Zurück erreichte behalten ihre Scrollposition.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun LumaApp(
    viewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.factory((LocalContext.current.applicationContext as LumaApplication).container),
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
