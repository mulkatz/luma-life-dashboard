package com.mulkatz.lumacare.data.repository

import android.database.sqlite.SQLiteException
import com.mulkatz.lumacare.data.local.LumaDatabase
import com.mulkatz.lumacare.data.seed.SeedImporter
import com.mulkatz.lumacare.domain.EventActionRecord
import com.mulkatz.lumacare.domain.EventActionType
import com.mulkatz.lumacare.domain.EventCommand
import com.mulkatz.lumacare.domain.EventState
import com.mulkatz.lumacare.domain.EventStatus
import com.mulkatz.lumacare.domain.ForwardTarget
import com.mulkatz.lumacare.domain.LumaEvent
import com.mulkatz.lumacare.domain.Resolution
import com.mulkatz.lumacare.domain.ShiftType
import com.mulkatz.lumacare.domain.TransitionError
import com.mulkatz.lumacare.domain.state
import com.mulkatz.lumacare.testing.InMemoryAppStateStore
import com.mulkatz.lumacare.testing.inMemoryDatabase
import com.mulkatz.lumacare.testing.realSeedJson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Schreibpfad gegen den echten Seed: Frühdienst, jetzt 07:15. */
@RunWith(RobolectricTestRunner::class)
class CareRepositoryWriteTest {
    private lateinit var db: LumaDatabase
    private lateinit var repository: RoomCareRepository

    private val t1 = LocalDateTime.parse("2026-10-12T07:20:00")
    private val t2 = LocalDateTime.parse("2026-10-12T07:25:00")
    private val t3 = LocalDateTime.parse("2026-10-12T07:30:00")

    @Before
    fun setUp() = runTest {
        db = inMemoryDatabase()
        val seed = realSeedJson()
        repository = RoomCareRepository(db, SeedImporter(db, InMemoryAppStateStore()) { seed })
        repository.ensureSeeded()
    }

    @After
    fun tearDown() = db.close()

    private suspend fun event(id: String): LumaEvent = repository.observeCareData().first().events.single { it.id == id }

    private suspend fun actions(): List<EventActionRecord> = repository.observeCareData().first().actions

    /** Wie die UI: mit dem gerade angezeigten Zustand als Erwartung. */
    private suspend fun apply(id: String, command: EventCommand, now: LocalDateTime = t1): ActionResult =
        repository.apply(id, event(id).state, command, ShiftType.EARLY, now)

    private fun ActionResult.actionId(): Long = assertIs<ActionResult.Applied>(this).actionId

    @Test
    fun `übernehmen e005 doppelt getippt schreibt genau einmal`() = runTest {
        val shown = event("e005").state

        val first = repository.apply("e005", shown, EventCommand.TakeOver, ShiftType.EARLY, t1)
        val second = repository.apply("e005", shown, EventCommand.TakeOver, ShiftType.EARLY, t1)

        assertIs<ActionResult.Applied>(first)
        assertEquals(ActionResult.Stale, second)
        val e005 = event("e005")
        assertEquals(EventState(EventStatus.IN_PROGRESS, ShiftType.EARLY, null), e005.state)
        assertEquals(t1, e005.updatedAt)
        val action = actions().single()
        assertEquals(first.actionId, action.id)
        assertEquals(EventActionType.ASSIGN, action.type)
        assertEquals(EventState(EventStatus.OPEN, null, null), action.previous)
    }

    @Test
    fun `übernahme bestätigen e018 ordnet dem frühdienst zu`() = runTest {
        val shown = event("e018").state

        apply("e018", EventCommand.ConfirmHandover).actionId()

        assertEquals(EventState(EventStatus.IN_PROGRESS, ShiftType.EARLY, null), event("e018").state)
        val action = actions().single()
        assertEquals(EventActionType.CONFIRM_HANDOVER, action.type)
        assertEquals(EventState(EventStatus.IN_PROGRESS, ShiftType.NIGHT, null), action.previous)
        // gleicher Status, andere Zuordnung: der zweite Tipp ist trotzdem veraltet
        assertEquals(ActionResult.Stale, repository.apply("e018", shown, EventCommand.ConfirmHandover, ShiftType.EARLY, t1))
        assertEquals(1, actions().size)
    }

    @Test
    fun `abschließen e005 ohne notiz wird abgelehnt und schreibt nichts`() = runTest {
        apply("e005", EventCommand.TakeOver).actionId()
        val before = event("e005")

        val result = apply("e005", EventCommand.Resolve(Resolution.DONE), t2)

        assertEquals(ActionResult.Rejected(TransitionError.NOTE_REQUIRED), result)
        assertEquals(before, event("e005"))
        assertEquals(1, actions().size)
    }

    @Test
    fun `verbotener übergang schreibt nichts`() = runTest {
        val before = event("e005")

        val result = apply("e005", EventCommand.Resolve(Resolution.DONE, note = "Werte gemessen"))

        assertEquals(ActionResult.Rejected(TransitionError.NOT_ALLOWED), result)
        assertEquals(before, event("e005"))
        assertTrue(actions().isEmpty())
    }

    @Test
    fun `abschließen e005 mit notiz speichert zustand und snapshot`() = runTest {
        apply("e005", EventCommand.TakeOver).actionId()

        val actionId = apply("e005", EventCommand.Resolve(Resolution.DONE, note = "Werte gemessen"), t2).actionId()

        val e005 = event("e005")
        assertEquals(EventState(EventStatus.DONE, ShiftType.EARLY, Resolution.DONE), e005.state)
        assertEquals(t2, e005.updatedAt)
        assertEquals(
            EventActionRecord(
                id = actionId,
                eventId = "e005",
                type = EventActionType.RESOLVE,
                createdAt = t2,
                note = "Werte gemessen",
                forwardedTo = null,
                previous = EventState(EventStatus.IN_PROGRESS, ShiftType.EARLY, null),
            ),
            actions().last(),
        )
    }

    @Test
    fun `rückgängig stellt den zustand vollständig wieder her und löscht die aktion`() = runTest {
        val original = event("e005").state
        val takeOver = apply("e005", EventCommand.TakeOver).actionId()
        val resolve = apply("e005", EventCommand.Resolve(Resolution.NO_ACTION_NEEDED, note = "Gesehen, wohlauf"), t2).actionId()

        assertEquals(UndoResult.Undone, repository.undo(resolve, t3))

        val afterFirstUndo = event("e005")
        assertEquals(EventState(EventStatus.IN_PROGRESS, ShiftType.EARLY, null), afterFirstUndo.state)
        assertEquals(t3, afterFirstUndo.updatedAt)
        assertEquals(listOf(takeOver), actions().map { it.id })

        assertEquals(UndoResult.Undone, repository.undo(takeOver, t3))

        assertEquals(original, event("e005").state)
        assertTrue(actions().isEmpty())
    }

    @Test
    fun `rückgängig der vorschicht-übernahme gibt e018 der nacht zurück`() = runTest {
        val actionId = apply("e018", EventCommand.ConfirmHandover).actionId()

        assertEquals(UndoResult.Undone, repository.undo(actionId, t2))

        assertEquals(EventState(EventStatus.IN_PROGRESS, ShiftType.NIGHT, null), event("e018").state)
        assertTrue(actions().isEmpty())
    }

    @Test
    fun `nur die letzte aktion ist rückgängig machbar, auch bei gleicher zeit`() = runTest {
        // beide Aktionen mit demselben created_at: die höhere ID ist die letzte
        val takeOver = apply("e005", EventCommand.TakeOver, t1).actionId()
        val note = apply("e005", EventCommand.AddNote("Werte gemessen"), t1).actionId()
        val before = event("e005")

        assertEquals(UndoResult.NotLatest, repository.undo(takeOver, t2))
        assertEquals(before, event("e005"))
        assertEquals(listOf(takeOver, note), actions().map { it.id })

        assertEquals(UndoResult.Undone, repository.undo(note, t2))
        assertEquals(UndoResult.Stale, repository.undo(note, t2)) // doppelt getippt
        assertEquals(UndoResult.Undone, repository.undo(takeOver, t2))
    }

    @Test
    fun `rückwärts laufende uhr ändert nicht, welche aktion die letzte ist`() = runTest {
        val first = apply("e005", EventCommand.TakeOver, t2).actionId()
        val second = apply("e005", EventCommand.AddNote("Werte gemessen"), t1).actionId() // früher als first

        assertEquals(listOf(first, second), actions().map { it.id })
        assertEquals(UndoResult.NotLatest, repository.undo(first, t3))
        assertEquals(UndoResult.Undone, repository.undo(second, t3))
        assertEquals(listOf(first), actions().map { it.id })
    }

    @Test
    fun `rückgängig nach zurückgeben stellt die eigene bearbeitung wieder her`() = runTest {
        apply("e005", EventCommand.TakeOver).actionId()
        val release = apply("e005", EventCommand.Release, t2).actionId()
        assertEquals(EventState(EventStatus.OPEN, null, null), event("e005").state)

        assertEquals(UndoResult.Undone, repository.undo(release, t3))

        assertEquals(EventState(EventStatus.IN_PROGRESS, ShiftType.EARLY, null), event("e005").state)
        assertEquals(1, actions().size)
    }

    @Test
    fun `rückgängig nach weiterleiten aus open setzt die abschlussart zurück`() = runTest {
        val forward = EventCommand.Resolve(Resolution.FORWARDED, forwardedTo = ForwardTarget.KITCHEN)
        val actionId = apply("e004", forward).actionId()

        assertEquals(UndoResult.Undone, repository.undo(actionId, t2))

        assertEquals(EventState(EventStatus.OPEN, null, null), event("e004").state)
        assertTrue(actions().isEmpty())
    }

    @Test
    fun `rückgängig nach weiterleiten aus eigener bearbeitung stellt den vorherigen status her`() = runTest {
        apply("e015", EventCommand.TakeOver).actionId()
        val forward = EventCommand.Resolve(Resolution.FORWARDED, note = "Zeitung nachbestellt", forwardedTo = ForwardTarget.ADMINISTRATION)
        val actionId = apply("e015", forward, t2).actionId()

        assertEquals(UndoResult.Undone, repository.undo(actionId, t3))

        assertEquals(EventState(EventStatus.IN_PROGRESS, ShiftType.EARLY, null), event("e015").state)
        assertEquals(1, actions().size)
    }

    @Test
    fun `rückgängig bei abweichendem zustand ist veraltet und ändert nichts`() = runTest {
        val actionId = apply("e005", EventCommand.TakeOver).actionId()
        // Zustand ändert sich an der Aktion vorbei (darf es im Betrieb nicht geben)
        db.eventDao().updateStatus(
            id = "e005",
            expected = EventStatus.IN_PROGRESS,
            expectedAssignedTo = ShiftType.EARLY,
            new = EventStatus.OPEN,
            assignedTo = null,
            resolution = null,
            updatedAt = t2,
        )
        val before = event("e005")

        assertEquals(UndoResult.Stale, repository.undo(actionId, t3))

        assertEquals(before, event("e005"))
        assertEquals(listOf(actionId), actions().map { it.id })
    }

    @Test
    fun `notiz schreibt nur den verlaufseintrag`() = runTest {
        val before = event("e018")

        val actionId = apply("e018", EventCommand.AddNote(" Angehörige informiert ")).actionId()

        assertEquals(before, event("e018"))
        val action = actions().single()
        assertEquals(actionId, action.id)
        assertEquals(EventActionType.NOTE, action.type)
        assertEquals("Angehörige informiert", action.note)
        assertEquals(before.state, action.previous)

        assertEquals(UndoResult.Undone, repository.undo(actionId, t2))
        assertEquals(before, event("e018"))
        assertTrue(actions().isEmpty())
    }

    @Test
    fun `weiterleiten e004 an die küche speichert das ziel`() = runTest {
        apply("e004", EventCommand.Resolve(Resolution.FORWARDED, forwardedTo = ForwardTarget.KITCHEN)).actionId()

        assertEquals(EventState(EventStatus.DONE, ShiftType.EARLY, Resolution.FORWARDED), event("e004").state)
        val action = actions().single()
        assertEquals(ForwardTarget.KITCHEN, action.forwardedTo)
        assertEquals(null, action.note)
        assertEquals(ForwardTarget.KITCHEN, db.eventActionDao().latestForEvent("e004")?.forwardedTo)
    }

    @Test
    fun `unbekannter hinweis ist veraltet`() = runTest {
        val result = repository.apply(
            "e999",
            EventState(EventStatus.OPEN, null, null),
            EventCommand.TakeOver,
            ShiftType.EARLY,
            t1,
        )

        assertEquals(ActionResult.Stale, result)
        assertTrue(actions().isEmpty())
    }

    @Test
    fun `fehler nach dem update rollt alles zurück`() = runTest {
        // Der Verlaufseintrag wird nach dem Status-Update geschrieben; der Trigger lässt genau ihn scheitern.
        db.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER fail_action BEFORE INSERT ON event_action BEGIN SELECT RAISE(ABORT, 'erzwungen'); END",
        )
        val before = event("e005")

        assertFailsWith<SQLiteException> { apply("e005", EventCommand.TakeOver) }

        assertEquals(before, event("e005"))
        assertTrue(actions().isEmpty())
    }
}
