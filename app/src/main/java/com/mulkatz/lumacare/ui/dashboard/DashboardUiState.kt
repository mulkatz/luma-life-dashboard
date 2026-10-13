package com.mulkatz.lumacare.ui.dashboard

import com.mulkatz.lumacare.domain.DashboardModel
import com.mulkatz.lumacare.domain.Shift
import java.time.Duration

sealed interface DashboardUiState {
    /** Seed-Import oder erste Daten stehen noch aus. Nie als leere Liste darstellen. */
    data object Loading : DashboardUiState

    /** Import oder Laden ist gescheitert; [DashboardViewModel.retry] versucht es erneut. */
    data object Error : DashboardUiState

    /** „Jetzt“ steht in [DashboardModel.now]. [shift] trägt Typ und Wohnbereich für den Kopfbereich. */
    data class Ready(
        val model: DashboardModel,
        val shift: Shift,
        val remaining: Duration,
        val isShiftOver: Boolean,
    ) : DashboardUiState
}
