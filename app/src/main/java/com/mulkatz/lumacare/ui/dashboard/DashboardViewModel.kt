package com.mulkatz.lumacare.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mulkatz.lumacare.AppContainer
import com.mulkatz.lumacare.data.repository.CareRepository
import com.mulkatz.lumacare.domain.Prioritizer
import com.mulkatz.lumacare.domain.ShiftClock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

/**
 * Dashboard-Zustand aus Repository-Daten und dem Minuten-Ticker der [ShiftClock].
 *
 * Die Uhr kommt als Lieferfunktion, weil ihr erster Zugriff die Platte liest (Seed-Meta, Uhr-Anker).
 * Sie wird erst im Flow auf [backgroundDispatcher] aufgerufen, nie auf dem Main-Thread.
 * Scheitert Import oder Laden, wird der Zustand [DashboardUiState.Error]; [retry] startet neu.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val repository: CareRepository,
    private val loadClock: () -> ShiftClock,
    backgroundDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val attempt = MutableStateFlow(0)

    val uiState: StateFlow<DashboardUiState> = attempt
        .flatMapLatest { load().flowOn(backgroundDispatcher) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), DashboardUiState.Loading)

    /** Lädt neu: Laden → Seed → Daten. */
    fun retry() {
        attempt.value++
    }

    private fun load(): Flow<DashboardUiState> = flow<DashboardUiState> {
        emit(DashboardUiState.Loading)
        repository.ensureSeeded()
        val clock = loadClock()
        emitAll(
            combine(repository.observeCareData(), clock.ticks) { data, now ->
                DashboardUiState.Ready(
                    model = Prioritizer.build(data, now, clock.shift),
                    shift = clock.shift,
                    remaining = clock.remaining(now),
                    isShiftOver = clock.isShiftOver(now),
                )
            },
        )
    }.catch { emit(DashboardUiState.Error) }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                DashboardViewModel(
                    repository = container.careRepository,
                    loadClock = { container.shiftClock },
                )
            }
        }
    }
}
