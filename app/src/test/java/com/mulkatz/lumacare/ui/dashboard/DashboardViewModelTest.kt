package com.mulkatz.lumacare.ui.dashboard

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mulkatz.lumacare.data.local.LumaDatabase
import com.mulkatz.lumacare.data.repository.ActionResult
import com.mulkatz.lumacare.data.repository.CareRepository
import com.mulkatz.lumacare.data.repository.RoomCareRepository
import com.mulkatz.lumacare.data.repository.UndoResult
import com.mulkatz.lumacare.data.seed.SeedImporter
import com.mulkatz.lumacare.data.seed.SeedParser
import com.mulkatz.lumacare.domain.CareData
import com.mulkatz.lumacare.domain.EventCommand
import com.mulkatz.lumacare.domain.EventState
import com.mulkatz.lumacare.domain.EventStatus
import com.mulkatz.lumacare.domain.ShiftClock
import com.mulkatz.lumacare.domain.ShiftType
import com.mulkatz.lumacare.domain.UpcomingItem
import com.mulkatz.lumacare.testing.InMemoryAppStateStore
import com.mulkatz.lumacare.testing.inMemoryDatabase
import com.mulkatz.lumacare.testing.realSeedJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.time.Duration
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DashboardViewModelTest {
    private lateinit var db: LumaDatabase
    private val store = ViewModelStore()
    private val seed = realSeedJson()
    private val shift = SeedImporter.mapShift(SeedParser.parse(seed).meta)
    private val seedNow = LocalDateTime.parse("2026-10-12T07:15:00")

    /** Systemzeit der Uhr; bleibt konstant, solange ein Test sie nicht verstellt. */
    private var millis = 0L

    @Before
    fun setUp() {
        db = inMemoryDatabase()
    }

    @After
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
        db.close()
    }

    private fun seededRepository() = RoomCareRepository(db, SeedImporter(db, InMemoryAppStateStore()) { seed })

    /** Legt das ViewModel über den [store] an; Main und Hintergrund laufen auf dem Test-Scheduler. */
    private fun TestScope.viewModel(repository: CareRepository = seededRepository()): DashboardViewModel {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val clock = ShiftClock(seedNow, shift, InMemoryAppStateStore(), currentTimeMillis = { millis })
        val factory = viewModelFactory {
            initializer { DashboardViewModel(repository, loadClock = { clock }, backgroundDispatcher = dispatcher) }
        }
        return ViewModelProvider(store, factory)[DashboardViewModel::class.java]
    }

    private suspend fun DashboardViewModel.awaitReady(): DashboardUiState.Ready =
        uiState.first { it is DashboardUiState.Ready } as DashboardUiState.Ready

    @Test
    fun `zuerst loading, nach import und daten ready`() = runTest {
        val viewModel = viewModel()

        assertEquals(DashboardUiState.Loading, viewModel.uiState.value)

        val ready = viewModel.awaitReady()
        assertEquals(seedNow, ready.model.now)
        assertEquals(shift, ready.shift)
        assertEquals("Wohnbereich 2 – Ahorn", ready.shift.unit)
        assertEquals(Duration.ofHours(6).plusMinutes(45), ready.remaining)
        assertFalse(ready.isShiftOver)
        assertEquals(listOf("r04", "r12"), ready.model.actNow.map { it.resident.id })
    }

    @Test
    fun `statusänderung in der datenbank aktualisiert den zustand reaktiv`() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.awaitReady()

        val changed = db.eventDao().updateStatus(
            id = "e005",
            expected = EventStatus.OPEN,
            expectedAssignedTo = null,
            new = EventStatus.DONE,
            assignedTo = null,
            resolution = null,
            updatedAt = LocalDateTime.parse("2026-10-12T07:16:00"),
        )
        assertEquals(1, changed)

        val updated = viewModel.uiState.first {
            it is DashboardUiState.Ready && it.model.actNow.none { entry -> entry.resident.id == "r04" }
        } as DashboardUiState.Ready
        assertEquals(listOf("r12"), updated.model.actNow.map { it.resident.id })
        assertEquals(1, updated.model.metrics.actNowPersons)
    }

    @Test
    fun `der minuten-ticker schiebt jetzt, restzeit und demnächst weiter`() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.awaitReady()

        millis += Duration.ofMinutes(16).toMillis()
        advanceTimeBy(60_000)
        runCurrent()

        val ready = assertIs<DashboardUiState.Ready>(viewModel.uiState.value)
        assertEquals(LocalDateTime.parse("2026-10-12T07:31:00"), ready.model.now)
        assertEquals(Duration.ofHours(6).plusMinutes(29), ready.remaining)
        val routines = ready.model.upcoming.filterIsInstance<UpcomingItem.RoutineItem>()
        assertEquals(listOf("rt05"), routines.map { it.routine.id })
        assertFalse(ready.model.upcoming.any { it.time < ready.model.now })
    }

    @Test
    fun `gescheiterter import ergibt error und retry lädt neu bis ready`() = runTest {
        val repository = FlakyRepository()
        val viewModel = viewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.uiState.first { it is DashboardUiState.Error }

        viewModel.retry()

        val ready = viewModel.awaitReady()
        assertEquals(2, repository.seedCalls)
        assertEquals(seedNow, ready.model.now)
    }

    @Test
    fun `vor dem ersten gefüllten ready erscheint kein ready ohne personen`() = runTest {
        val seeded = seededRepository().also { it.ensureSeeded() }.observeCareData().first()
        val viewModel = viewModel(UnseededRepository(seeded))
        val states = mutableListOf<DashboardUiState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.toList(states) }

        viewModel.uiState.first { it.hasPersons() }

        assertEquals(DashboardUiState.Loading, states.first())
        val beforeFirstFilled = states.takeWhile { !it.hasPersons() }
        assertTrue(beforeFirstFilled.none { it is DashboardUiState.Ready }, "Zustände vor dem ersten Ready: $beforeFirstFilled")
    }

    private fun DashboardUiState.hasPersons(): Boolean =
        this is DashboardUiState.Ready && (model.actNow + model.checkSoon + model.organizational).isNotEmpty()

    /** Wie Room: vor dem Seeding leere Daten, danach der Seed. */
    private class UnseededRepository(private val seeded: CareData) : CareRepository {
        private val data = MutableStateFlow(CareData(emptyList(), emptyList(), emptyList(), emptyList()))

        override suspend fun ensureSeeded() {
            delay(100)
            data.value = seeded
        }

        override fun observeCareData(): Flow<CareData> = data

        override suspend fun apply(
            eventId: String,
            expected: EventState,
            command: EventCommand,
            shift: ShiftType,
            now: LocalDateTime,
        ): ActionResult = error("nicht benutzt")

        override suspend fun undo(actionId: Long, now: LocalDateTime): UndoResult = error("nicht benutzt")
    }

    /** Wirft beim ersten Import, danach liefert es einen leeren Datenstand. */
    private class FlakyRepository : CareRepository {
        var seedCalls = 0
            private set

        override suspend fun ensureSeeded() {
            seedCalls++
            if (seedCalls == 1) throw IOException("Seed nicht lesbar")
        }

        override fun observeCareData(): Flow<CareData> = flowOf(CareData(emptyList(), emptyList(), emptyList(), emptyList()))

        override suspend fun apply(
            eventId: String,
            expected: EventState,
            command: EventCommand,
            shift: ShiftType,
            now: LocalDateTime,
        ): ActionResult = error("nicht benutzt")

        override suspend fun undo(actionId: Long, now: LocalDateTime): UndoResult = error("nicht benutzt")
    }
}
