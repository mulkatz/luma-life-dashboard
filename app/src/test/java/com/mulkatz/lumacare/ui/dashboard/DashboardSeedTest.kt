package com.mulkatz.lumacare.ui.dashboard

import com.mulkatz.lumacare.data.local.LumaDatabase
import com.mulkatz.lumacare.data.repository.RoomCareRepository
import com.mulkatz.lumacare.data.seed.SeedImporter
import com.mulkatz.lumacare.data.seed.SeedParser
import com.mulkatz.lumacare.domain.ContextChip
import com.mulkatz.lumacare.domain.DashboardMetrics
import com.mulkatz.lumacare.domain.DashboardModel
import com.mulkatz.lumacare.domain.EventAction
import com.mulkatz.lumacare.domain.Handling
import com.mulkatz.lumacare.domain.HistoryEntry
import com.mulkatz.lumacare.domain.HourPhase
import com.mulkatz.lumacare.domain.NowMarker
import com.mulkatz.lumacare.domain.Prioritizer
import com.mulkatz.lumacare.domain.Severity
import com.mulkatz.lumacare.domain.UpcomingItem
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
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Echter Seed, In-Memory-DB, `now` = Mo 12.10.2026, 07:15 (Frühdienst). */
@RunWith(RobolectricTestRunner::class)
class DashboardSeedTest {
    private lateinit var db: LumaDatabase
    private lateinit var model: DashboardModel
    private val now = LocalDateTime.parse("2026-10-12T07:15:00")

    @Before
    fun setUp() = runTest {
        db = inMemoryDatabase()
        val seed = realSeedJson()
        val repository = RoomCareRepository(db, SeedImporter(db, InMemoryAppStateStore()) { seed })
        repository.ensureSeeded()
        val shift = SeedImporter.mapShift(SeedParser.parse(seed).meta)
        model = Prioritizer.build(repository.observeCareData().first(), now, shift)
    }

    @After
    fun tearDown() = db.close()

    private fun person(id: String) =
        (model.actNow + model.checkSoon + model.organizational).single { it.resident.id == id }

    @Test
    fun `jetzt handeln zeigt werner hoffmann vor rudolf schmidt`() {
        assertEquals(listOf("r04", "r12"), model.actNow.map { it.resident.id })
        assertEquals(listOf("Werner Hoffmann", "Rudolf Schmidt"), model.actNow.map { it.resident.fullName })
    }

    @Test
    fun `werner hat diabetes-hinweis und blutzucker-routine um 07_00 als kontext`() {
        val werner = person("r04")

        assertEquals("e005", werner.leadEvent.event.id)
        assertEquals(EventAction.TAKE_OVER, werner.leadEvent.action)
        assertEquals(ContextChip.HealthNote("Diabetes Typ 2, achtet auf Zucker"), werner.context[0])
        val routine = werner.context[1] as ContextChip.RoutineNearby
        assertEquals("Blutzucker messen vor dem Frühstück", routine.routine.title)
        assertEquals(LocalTime.of(7, 0), routine.time.toLocalTime())
        assertEquals(2, werner.context.size)
    }

    @Test
    fun `rudolf braucht übernahme-bestätigung und das zweite signal ist sichtbar`() {
        val rudolf = person("r12")

        assertEquals("e018", rudolf.leadEvent.event.id)
        assertEquals(Handling.PREVIOUS_SHIFT, rudolf.leadEvent.handling)
        assertEquals(EventAction.CONFIRM_HANDOVER, rudolf.leadEvent.action)
        assertEquals(listOf("e018", "e019"), rudolf.events.map { it.event.id })
        assertTrue(ContextChip.LumaNotReached in rudolf.context)
    }

    @Test
    fun `demnächst verknüpft hörgeräte 07_30 mit dem offenen hinweis e014`() {
        val routines = model.upcoming.filterIsInstance<UpcomingItem.RoutineItem>()

        assertEquals(listOf("rt07", "rt05"), model.upcoming.map { (it as UpcomingItem.RoutineItem).routine.id })
        val hoergeraete = routines.first()
        assertEquals("Hörgeräte einsetzen", hoergeraete.routine.title)
        assertEquals(LocalTime.of(7, 30), hoergeraete.time.toLocalTime())
        assertEquals(listOf("e014"), hoergeraete.openHints.map { it.id })
        assertEquals(listOf("e015"), routines[1].openHints.map { it.id })
    }

    @Test
    fun `abschnitte und kennzahlen entsprechen dem seed`() {
        assertEquals(listOf("r15", "r07", "r10", "r01", "r09", "r02", "r05"), model.checkSoon.map { it.resident.id })
        assertEquals(listOf("r08", "r13", "r14", "r03"), model.organizational.map { it.resident.id })
        assertEquals(
            DashboardMetrics(actNowPersons = 2, checkSoonPersons = 7, previousShiftPersons = 2, notReachedPersons = 2),
            model.metrics,
        )
    }

    @Test
    fun `zeitleiste enthält alle 26 hinweise und die montagsroutinen ohne rt04`() {
        val timeline = model.timeline

        assertEquals(26, timeline.events.size)
        assertEquals(listOf("rt06", "rt03", "rt02", "rt07", "rt05", "rt01"), timeline.routines.map { it.routine.id })
        assertEquals(2, timeline.summary.openUrgentEvents)
        assertEquals("rt07", timeline.summary.nextRoutine?.routine?.id)
    }

    @Test
    fun `stundenblöcke um 07_15 entsprechen dem seed`() {
        val blocks = model.hours.blocks

        assertEquals(listOf(7, 15, 4, 0, 0, 0, 0, 0, 0), blocks.map { it.hintCount })
        assertEquals(
            listOf(Severity.URGENT, Severity.URGENT, Severity.INFO, null, null, null, null, null, null),
            blocks.map { it.openSeverity },
        )
        assertEquals(
            listOf(HourPhase.PAST, HourPhase.PAST, HourPhase.CURRENT) + List(6) { HourPhase.FUTURE },
            blocks.map { it.phase },
        )
        assertEquals(NowMarker(blockIndex = 2, fraction = 0.25f), model.hours.nowMarker)
    }

    @Test
    fun `dringend in nacht und 06 kommt von e018 bzw_ e005`() {
        val openUrgent = model.timeline.events.map { it.event }.filter { it.isOpen && it.severity == Severity.URGENT }

        assertEquals(listOf("e018", "e005"), openUrgent.map { it.id })
        assertTrue(openUrgent[0].timestamp.isBefore(model.timeline.shiftStart))
        assertEquals(6, openUrgent[1].timestamp.hour)
    }

    @Test
    fun `routine-ticks bei 06_30, 06_45, 07_00, 07_30, 08_00 und 10_00, rt04 fehlt`() {
        val ticks = model.hours.blocks.map { it.routineTicks }

        assertEquals(
            listOf(emptyList(), listOf(0.5f, 0.75f), listOf(0f, 0.5f), listOf(0f), emptyList(), listOf(0f), emptyList(), emptyList(), emptyList()),
            ticks,
        )
        assertTrue(model.timeline.routines.none { it.routine.id == "rt04" })
    }

    @Test
    fun `verlauf gruppiert e018 unter vorschicht und e005 unter frühdienst, routinen nur bis now`() {
        val history = model.history
        fun List<HistoryEntry>.ids() = map {
            when (it) {
                is HistoryEntry.Hint -> it.event.id
                is HistoryEntry.PlannedRoutine -> it.routine.id
            }
        }

        assertEquals(listOf("e002", "e011", "e006", "e018", "e013", "e026", "e025"), history.previousShift.ids())
        assertTrue("e005" in history.currentShift.ids())
        assertEquals(listOf("rt02", "rt03", "rt06"), history.currentShift.filterIsInstance<HistoryEntry.PlannedRoutine>().map { it.routine.id })
        assertEquals(19 + 3, history.currentShift.size)
    }
}
