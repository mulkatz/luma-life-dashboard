package com.mulkatz.lumacare.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mulkatz.lumacare.LumaApplication
import com.mulkatz.lumacare.ui.dashboard.DashboardRoute
import com.mulkatz.lumacare.ui.dashboard.DashboardViewModel
import com.mulkatz.lumacare.ui.history.ShiftHistoryRoute

/** Ziele der App. Der Zielzustand überlebt Rotation und Prozess-Tod. */
enum class AppDestination { DASHBOARD, SHIFT_HISTORY }

/**
 * Minimale Navigation ohne Bibliothek: ein gespeicherter Zielzustand. Dashboard und Schichtverlauf teilen
 * sich ein [DashboardViewModel]. Zurück per Appbar oder System-Zurück; der Zustand des Dashboards
 * (z. B. Scrollposition) bleibt dabei erhalten, der des Verlaufs wird verworfen, damit er beim nächsten
 * Öffnen wieder oben bei „jetzt“ beginnt.
 */
@Composable
fun LumaApp(
    viewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.factory((LocalContext.current.applicationContext as LumaApplication).container),
    ),
) {
    var destination by rememberSaveable { mutableStateOf(AppDestination.DASHBOARD) }
    val stateHolder = rememberSaveableStateHolder()
    val backToDashboard = {
        stateHolder.removeState(AppDestination.SHIFT_HISTORY)
        destination = AppDestination.DASHBOARD
    }
    stateHolder.SaveableStateProvider(destination) {
        when (destination) {
            AppDestination.DASHBOARD -> DashboardRoute(
                // B3: Personen-Navigation (Detail, Tablet List-Detail).
                onPersonClick = {},
                onTimelineClick = { destination = AppDestination.SHIFT_HISTORY },
                viewModel = viewModel,
            )
            AppDestination.SHIFT_HISTORY -> {
                BackHandler(onBack = backToDashboard)
                ShiftHistoryRoute(
                    onBack = backToDashboard,
                    // B3: öffnet die Person.
                    onPersonClick = {},
                    viewModel = viewModel,
                )
            }
        }
    }
}
