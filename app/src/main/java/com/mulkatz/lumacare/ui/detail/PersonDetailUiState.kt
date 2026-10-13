package com.mulkatz.lumacare.ui.detail

import com.mulkatz.lumacare.domain.PersonDetail
import com.mulkatz.lumacare.ui.dashboard.DashboardUiState

/** Zustand des Personendetails, abgeleitet aus dem Dashboard-Zustand; keine eigene Datenquelle. */
sealed interface PersonDetailUiState {
    data object Loading : PersonDetailUiState

    data object Error : PersonDetailUiState

    /** Die gespeicherte Person steht nicht (mehr) in den Daten. */
    data object NotFound : PersonDetailUiState

    data class Ready(val detail: PersonDetail) : PersonDetailUiState

    companion object {
        fun from(state: DashboardUiState, personId: String): PersonDetailUiState = when (state) {
            DashboardUiState.Loading -> Loading
            DashboardUiState.Error -> Error
            is DashboardUiState.Ready ->
                PersonDetail.build(state.data, personId, state.model.now, state.shift)?.let(::Ready) ?: NotFound
        }
    }
}
