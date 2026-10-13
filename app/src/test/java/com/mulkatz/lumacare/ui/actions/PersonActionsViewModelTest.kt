package com.mulkatz.lumacare.ui.actions

import androidx.lifecycle.SavedStateHandle
import com.mulkatz.lumacare.data.repository.ActionResult
import com.mulkatz.lumacare.data.repository.CareRepository
import com.mulkatz.lumacare.data.repository.UndoResult
import com.mulkatz.lumacare.domain.CareData
import com.mulkatz.lumacare.domain.EventCommand
import com.mulkatz.lumacare.domain.EventState
import com.mulkatz.lumacare.domain.EventStatus
import com.mulkatz.lumacare.domain.EventType
import com.mulkatz.lumacare.domain.ForwardTarget
import com.mulkatz.lumacare.domain.LumaEvent
import com.mulkatz.lumacare.domain.Resolution
import com.mulkatz.lumacare.domain.Severity
import com.mulkatz.lumacare.domain.Shift
import com.mulkatz.lumacare.domain.ShiftClock
import com.mulkatz.lumacare.domain.ShiftType
import com.mulkatz.lumacare.domain.TransitionError
import com.mulkatz.lumacare.domain.state
import com.mulkatz.lumacare.testing.InMemoryAppStateStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Aktionen ohne Datenbank: das Repository ist ein Fake, die Uhr steht auf Mo 12.10.2026, 07:15. Daten fiktiv. */
@OptIn(ExperimentalCoroutinesApi::class)
class PersonActionsViewModelTest {
    private val now = LocalDateTime.parse("2026-10-12T07:15:00")
    private val shift = Shift(ShiftType.EARLY, now.withHour(6).withMinute(0), now.withHour(14).withMinute(0), "Wohnbereich Test")
    private val clock = ShiftClock(now, shift, InMemoryAppStateStore(), currentTimeMillis = { 0L })

    private val e004 = LumaEvent(
        "e004", "r03", now.withHour(7).withMinute(5), EventType.MEAL_WISH, Severity.INFO, EventStatus.OPEN,
        "Fiktiver Essenswunsch.", null, null, now,
    )
    private val e005 = e004.copy(id = "e005", type = EventType.HEALTH_HINT, severity = Severity.URGENT)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel(repository: CareRepository, handle: SavedStateHandle = SavedStateHandle()): PersonActionsViewModel {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        return PersonActionsViewModel(handle, repository, loadClock = { clock }, backgroundDispatcher = dispatcher)
    }

    /** Neuer Prozess: nur, was im SavedStateHandle stand, kommt zurück. */
    private fun SavedStateHandle.afterProcessDeath() = SavedStateHandle(keys().associateWith { get<Any>(it) })

    @Test
    fun `entwurf, sheet, abschlussart und ziel überstehen den prozess-tod`() = runTest {
        val handle = SavedStateHandle()
        val before = viewModel(FakeRepository(), handle)
        before.openResolve(e004)
        before.selectForwardTarget(ForwardTarget.KITCHEN)
        before.updateDraft("e004", "Küche ")
        before.insertSnippet("e004", "Angehörige informiert")
        before.updateDraft("e005", "Werte gemessen")

        val after = viewModel(FakeRepository(), handle.afterProcessDeath())

        assertEquals(ActionSheet.Resolve("e004"), after.sheet)
        assertEquals(Resolution.FORWARDED, after.resolution)
        assertEquals(ForwardTarget.KITCHEN, after.forwardTarget)
        assertEquals("Küche. Angehörige informiert", after.draft("e004"))
        assertEquals("Werte gemessen", after.draft("e005"))
    }

    @Test
    fun `weiterleiten-typ öffnet mit vorgewähltem weitergeleitet, sonst erledigt`() = runTest {
        val vm = viewModel(FakeRepository())

        vm.openResolve(e004)
        assertEquals(Resolution.FORWARDED, vm.resolution)
        vm.openResolve(e005)
        assertEquals(Resolution.DONE, vm.resolution)
        assertNull(vm.resolveCommand("e005").note)
    }

    @Test
    fun `abschluss geschrieben - snackbar mit aktion, entwurf geleert, sheet zu`() = runTest {
        val repository = FakeRepository()
        val vm = viewModel(repository)
        vm.openResolve(e005)
        vm.updateDraft("e005", "Werte gemessen")

        vm.perform(e005, vm.resolveCommand("e005"))
        runCurrent()

        val call = repository.calls.single()
        assertEquals(e005.state, call.expected)
        assertEquals(EventCommand.Resolve(Resolution.DONE, "Werte gemessen"), call.command)
        assertEquals(ShiftType.EARLY, call.shift)
        assertEquals(now, call.now)
        assertEquals(ActionFeedback.Applied(1, 1, "e005", call.command), vm.feedback)
        assertEquals("", vm.draft("e005"))
        assertNull(vm.sheet)
    }

    @Test
    fun `veralteter zustand - snackbar, entwurf bleibt`() = runTest {
        val vm = viewModel(FakeRepository(result = ActionResult.Stale))
        vm.openNote("e005")
        vm.updateDraft("e005", "Arzt informiert")

        vm.saveNote(e005)
        runCurrent()

        assertIs<ActionFeedback.Stale>(vm.feedback)
        assertEquals("Arzt informiert", vm.draft("e005"))
    }

    @Test
    fun `abgelehnte notiz zeigt den inline-fehler, ohne snackbar und ohne das sheet zu schließen`() = runTest {
        val vm = viewModel(FakeRepository(result = ActionResult.Rejected(TransitionError.NOTE_TOO_SHORT)))
        vm.openNote("e005")
        vm.updateDraft("e005", "ok")

        vm.saveNote(e005)
        runCurrent()

        assertTrue(vm.noteRejected)
        assertEquals(ActionSheet.Note("e005"), vm.sheet)
        vm.updateDraft("e005", "ok, Arzt informiert")
        assertFalse(vm.noteRejected)
    }

    @Test
    fun `doppeltipp - solange gespeichert wird, ist der hinweis gesperrt`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeRepository(gate = gate)
        val vm = viewModel(repository)

        vm.perform(e005, EventCommand.TakeOver)
        vm.perform(e005, EventCommand.TakeOver)
        runCurrent()

        assertTrue(vm.isBusy("e005"))
        gate.complete(Unit)
        runCurrent()
        assertEquals(1, repository.calls.size)
        assertFalse(vm.isBusy("e005"))
    }

    @Test
    fun `rückgängig, das nicht mehr geht, wird gemeldet`() = runTest {
        val vm = viewModel(FakeRepository(undoResult = UndoResult.NotLatest))
        vm.perform(e005, EventCommand.TakeOver)
        runCurrent()

        vm.undo(requireNotNull(vm.feedback).id)
        runCurrent()

        assertIs<ActionFeedback.UndoFailed>(vm.feedback)
    }

    private data class Call(val eventId: String, val expected: EventState, val command: EventCommand, val shift: ShiftType, val now: LocalDateTime)

    private class FakeRepository(
        private val result: ActionResult = ActionResult.Applied(1),
        private val undoResult: UndoResult = UndoResult.Undone,
        private val gate: CompletableDeferred<Unit>? = null,
    ) : CareRepository {
        val calls = mutableListOf<Call>()

        override suspend fun ensureSeeded() = Unit

        override fun observeCareData(): Flow<CareData> = emptyFlow()

        override suspend fun apply(
            eventId: String,
            expected: EventState,
            command: EventCommand,
            shift: ShiftType,
            now: LocalDateTime,
        ): ActionResult {
            calls += Call(eventId, expected, command, shift, now)
            gate?.await()
            return result
        }

        override suspend fun undo(actionId: Long, now: LocalDateTime): UndoResult = undoResult
    }
}
