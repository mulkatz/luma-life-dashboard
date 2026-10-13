package com.mulkatz.lumacare.domain

import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Stundenblöcke ([ShiftHours]) und Verlauf ([ShiftHistory]). Fixtures: Zeitleiste 11.10. 20:00 bis 12.10. 14:00, Schichtbeginn 06:00. Alle Daten fiktiv. */
class ShiftHoursTest {
    private fun at(time: String, day: Int = 12) = LocalDateTime.parse("2026-10-%02dT%s:00".format(day, time))

    private val resident = Resident("a", "Vorname", "Nachname", 80, "1.01", 2, emptyList(), emptyList(), emptyList(), null, null)
    private val other = resident.copy(id = "b", room = "1.02")

    private fun event(
        id: String,
        timestamp: LocalDateTime,
        severity: Severity = Severity.INFO,
        status: EventStatus = EventStatus.OPEN,
        who: Resident = resident,
    ) = TimelineEvent(
        LumaEvent(id, who.id, timestamp, EventType.MOOD, severity, status, "Fiktiver Hinweis.", null, null, timestamp),
        who,
    )

    private fun routine(id: String, time: LocalDateTime, who: Resident = resident) = TimelineRoutine(
        Routine(id, who.id, "Routine $id", time.toLocalTime(), DayOfWeek.entries.toSet(), lumaCall = false),
        who,
        time,
    )

    private fun timeline(
        events: List<TimelineEvent> = emptyList(),
        routines: List<TimelineRoutine> = emptyList(),
        end: LocalDateTime = at("14:00"),
    ) = Timeline(
        start = at("20:00", day = 11),
        end = end,
        shiftStart = at("06:00"),
        events = events,
        routines = routines,
        summary = TimelineSummary(openUrgentEvents = 0, nextRoutine = null),
    )

    @Test
    fun `nacht ist ein block, danach je ein block pro stunde bis schichtende`() {
        val hours = ShiftHours.from(timeline(), now = at("07:15"))

        assertEquals(9, hours.blocks.size)
        val night = hours.blocks.first()
        assertTrue(night.isNight)
        assertEquals(at("20:00", day = 11) to at("06:00"), night.start to night.end)
        assertEquals((6..13).map { at("%02d:00".format(it)) }, hours.blocks.drop(1).map { it.start })
        assertTrue(hours.blocks.drop(1).none { it.isNight })
        assertEquals(at("14:00"), hours.blocks.last().end)
    }

    @Test
    fun `letzte stunde endet am schichtende, wenn es keine volle stunde ist`() {
        val hours = ShiftHours.from(timeline(end = at("07:30")), now = at("07:15"))

        assertEquals(listOf(at("06:00"), at("07:00")), hours.blocks.drop(1).map { it.start })
        assertEquals(at("07:30"), hours.blocks.last().end)
        assertEquals(0.5f, hours.nowMarker?.fraction)
    }

    @Test
    fun `anzahl zählt auch erledigte, tönung nur nach offenen hinweisen`() {
        val hours = ShiftHours.from(
            timeline(
                events = listOf(
                    event("e1", at("06:10"), Severity.URGENT, EventStatus.DONE),
                    event("e2", at("06:20"), Severity.INFO),
                    event("e3", at("06:59"), Severity.ATTENTION, EventStatus.IN_PROGRESS),
                    event("e4", at("07:00"), Severity.URGENT, EventStatus.DONE),
                ),
            ),
            now = at("07:15"),
        )

        val six = hours.blocks[1]
        assertEquals(3, six.hintCount)
        assertEquals(Severity.ATTENTION, six.openSeverity)
        val seven = hours.blocks[2]
        assertEquals(1, seven.hintCount)
        assertNull(seven.openSeverity)
    }

    @Test
    fun `stunde ohne hinweise hat anzahl 0 und keine dringlichkeit`() {
        val block = ShiftHours.from(timeline(), now = at("07:15")).blocks[1]

        assertEquals(0, block.hintCount)
        assertNull(block.openSeverity)
    }

    @Test
    fun `hinweise am vorabend zählen zur nacht, grenzen sind halboffen`() {
        val hours = ShiftHours.from(
            timeline(
                events = listOf(
                    event("e1", at("20:00", day = 11), Severity.INFO),
                    event("e2", at("05:59"), Severity.URGENT, EventStatus.IN_PROGRESS),
                    event("e3", at("06:00"), Severity.INFO),
                ),
            ),
            now = at("07:15"),
        )

        assertEquals(2, hours.blocks[0].hintCount)
        assertEquals(Severity.URGENT, hours.blocks[0].openSeverity)
        assertEquals(1, hours.blocks[1].hintCount)
    }

    @Test
    fun `zustand vergangen, aktuell und zukünftig relativ zu now`() {
        val hours = ShiftHours.from(timeline(), now = at("07:15"))

        assertEquals(
            listOf(HourPhase.PAST, HourPhase.PAST, HourPhase.CURRENT) + List(6) { HourPhase.FUTURE },
            hours.blocks.map { it.phase },
        )
    }

    @Test
    fun `zur vollen stunde beginnt der neue block`() {
        val hours = ShiftHours.from(timeline(), now = at("08:00"))

        assertEquals(HourPhase.PAST, hours.blocks[2].phase)
        assertEquals(HourPhase.CURRENT, hours.blocks[3].phase)
        assertEquals(NowMarker(blockIndex = 3, fraction = 0f), hours.nowMarker)
    }

    @Test
    fun `vor schichtbeginn ist die nacht aktuell`() {
        val hours = ShiftHours.from(timeline(), now = at("01:00"))

        assertEquals(HourPhase.CURRENT, hours.blocks[0].phase)
        assertEquals(0, hours.nowMarker?.blockIndex)
        assertEquals(0.5f, hours.nowMarker?.fraction)
    }

    @Test
    fun `nach schichtende ist alles vergangen und es gibt keinen jetzt-marker`() {
        val hours = ShiftHours.from(timeline(), now = at("14:00"))

        assertTrue(hours.blocks.all { it.phase == HourPhase.PAST })
        assertNull(hours.nowMarker)
    }

    @Test
    fun `routinen erscheinen als anteil innerhalb ihres blocks`() {
        val hours = ShiftHours.from(
            timeline(routines = listOf(routine("r2", at("06:45")), routine("r1", at("06:30")), routine("r3", at("08:00")))),
            now = at("07:15"),
        )

        assertEquals(listOf(0.5f, 0.75f), hours.blocks[1].routineTicks)
        assertEquals(emptyList(), hours.blocks[2].routineTicks)
        assertEquals(listOf(0f), hours.blocks[3].routineTicks)
    }

    @Test
    fun `jetzt-marker steht im aktuellen block`() {
        val hours = ShiftHours.from(timeline(), now = at("07:15"))

        assertEquals(NowMarker(blockIndex = 2, fraction = 0.25f), hours.nowMarker)
    }

    @Test
    fun `verlauf trennt schicht und vorschicht, jüngste zuerst, gleiche zeit nach zimmer`() {
        val history = ShiftHistory.from(
            timeline(
                events = listOf(
                    event("e1", at("23:15", day = 11)),
                    event("e2", at("06:30"), who = other),
                    event("e3", at("06:30")),
                    event("e4", at("06:00")),
                ),
                routines = listOf(routine("rt1", at("07:00"), who = other), routine("rt2", at("06:30"), who = other)),
            ),
            now = at("07:15"),
        )

        assertEquals(
            listOf("rt1", "e3", "e2", "rt2", "e4"),
            history.currentShift.map { it.id() },
        )
        assertEquals(listOf("e1"), history.previousShift.map { it.id() })
    }

    @Test
    fun `verlauf zeigt routinen nur bis now`() {
        val history = ShiftHistory.from(
            timeline(routines = listOf(routine("rt1", at("07:15")), routine("rt2", at("07:30")))),
            now = at("07:15"),
        )

        assertEquals(listOf("rt1"), history.currentShift.map { it.id() })
    }

    private fun HistoryEntry.id(): String = when (this) {
        is HistoryEntry.Hint -> event.id
        is HistoryEntry.PlannedRoutine -> routine.id
    }
}
