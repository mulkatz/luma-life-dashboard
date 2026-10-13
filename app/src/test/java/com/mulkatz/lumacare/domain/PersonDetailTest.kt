package com.mulkatz.lumacare.domain

import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Fixtures nach dem Seed: Montag, 12.10.2026, 07:15, Frühdienst 06:00–14:00. Alle Daten fiktiv. */
class PersonDetailTest {
    private val now = at("07:15")
    private val early = Shift(ShiftType.EARLY, at("06:00"), at("14:00"), unit = "Wohnbereich Test")

    private fun at(time: String, day: Int = 12) = LocalDateTime.parse("2026-10-%02dT%s:00".format(day, time))

    private fun resident(id: String, healthNotes: List<String> = emptyList()) = Resident(
        id = id,
        firstName = "Vorname",
        lastName = id,
        age = 80,
        room = "2.$id",
        careLevel = 3,
        biography = listOf("Fiktive Biografie"),
        preferences = listOf("Fiktive Vorliebe"),
        healthNotes = healthNotes,
        contactName = "Kontakt $id",
        contactRelation = "Tochter",
    )

    private fun event(
        id: String,
        residentId: String,
        timestamp: LocalDateTime,
        severity: Severity = Severity.ATTENTION,
        status: EventStatus = EventStatus.OPEN,
        type: EventType = EventType.HEALTH_HINT,
        assignedTo: ShiftType? = if (status == EventStatus.IN_PROGRESS) ShiftType.NIGHT else null,
    ) = LumaEvent(id, residentId, timestamp, type, severity, status, "Fiktiver Hinweis $id.", assignedTo, null, timestamp)

    private fun routine(id: String, residentId: String, time: String, days: Set<DayOfWeek> = DayOfWeek.entries.toSet()) =
        Routine(id, residentId, "Routine $id", LocalTime.parse(time), days, lumaCall = true)

    private val residents = listOf(
        resident("r04", healthNotes = listOf("Diabetes Typ 2, achtet auf Zucker")),
        resident("r05"),
        resident("r07"),
        resident("r12"),
    )

    private val events = listOf(
        event("e005", "r04", at("06:30"), Severity.URGENT),
        event("e018", "r12", at("23:15", day = 11), Severity.URGENT, EventStatus.IN_PROGRESS),
        event("e019", "r12", at("06:40"), Severity.ATTENTION, type = EventType.MISSED_CALL),
        // Elfriede: nachts in Bearbeitung, früher erledigt, morgens verpasst; einer vor dem Fenster.
        event("e006", "r05", at("02:10"), status = EventStatus.IN_PROGRESS),
        event("e002", "r05", at("05:45"), Severity.INFO, EventStatus.DONE),
        event("e007", "r05", at("06:50"), Severity.INFO, type = EventType.MISSED_CALL),
        event("e000", "r05", at("19:30", day = 11), Severity.INFO, EventStatus.DONE),
    )

    private val routines = listOf(
        routine("rt02", "r04", "07:00"),
        routine("rt01", "r07", "10:00"),
        routine("rt04", "r07", "16:00", days = setOf(DayOfWeek.SUNDAY)),
    )

    private val activities = listOf(
        Activity("a02", "Singkreis", at("11:15"), "Wintergarten", listOf("r05", "r07")),
        Activity("a03", "Spielenachmittag", at("15:00"), "Gemeinschaftsraum", listOf("r05")),
    )

    private val data = CareData(residents, events, routines, activities)

    private fun detail(id: String) = requireNotNull(PersonDetail.build(data, id, now, early))

    @Test
    fun `r04 hat gesundheit, routine 07_00 und genau den offenen hinweis e005`() {
        val werner = detail("r04")

        assertEquals(listOf("Diabetes Typ 2, achtet auf Zucker"), werner.resident.healthNotes)
        assertEquals(listOf("e005"), werner.hints.map { it.event.id })
        assertEquals(Severity.URGENT, werner.hints.single().event.severity)
        assertFalse(werner.hints.single().isFromNightShift)
        val routine = werner.routinesToday.single()
        assertEquals("rt02", routine.routine.id)
        assertEquals(at("07:00"), routine.time)
        assertEquals(now, werner.now)
    }

    @Test
    fun `r12 zeigt e018 vor e019 und nur e018 aus nachtdienst`() {
        val rudolf = detail("r12")

        assertEquals(listOf("e018", "e019"), rudolf.hints.map { it.event.id })
        assertTrue(rudolf.hints[0].isFromNightShift)
        assertFalse(rudolf.hints[1].isFromNightShift)
        assertEquals(EventAction.CONFIRM_HANDOVER, rudolf.hints[0].action)
    }

    @Test
    fun `verlauf zeigt alle hinweise im fenster jüngste oben, auch erledigte`() {
        val elfriede = detail("r05")

        assertEquals(listOf("e007", "e002", "e006"), elfriede.history.map { it.event.id })
        assertEquals(listOf(false, true, false), elfriede.history.map { it.isDone })
        assertEquals(listOf(false, false, true), elfriede.history.map { it.isFromNightShift })
        // Offen sind nur die nicht erledigten, unabhängig vom Fenster.
        assertEquals(listOf("e006", "e007"), elfriede.hints.map { it.event.id })
    }

    @Test
    fun `rt04 erscheint am montag nicht`() {
        assertEquals(listOf("rt01"), detail("r07").routinesToday.map { it.routine.id })
    }

    @Test
    fun `aktivitäten nur in der schicht`() {
        assertEquals(listOf("a02"), detail("r05").activities.map { it.id })
    }

    @Test
    fun `unbekannte person ergibt kein detail`() {
        assertNull(PersonDetail.build(data, "r99", now, early))
    }

    @Test
    fun `verlauf mischt hinweise und aktionen der pflege, jüngste oben`() {
        val taken = EventState(EventStatus.IN_PROGRESS, ShiftType.EARLY, null)
        val resolved = EventState(EventStatus.DONE, ShiftType.EARLY, Resolution.DONE)
        val actions = listOf(
            EventActionRecord(1, "e005", EventActionType.ASSIGN, at("07:15"), null, null, EventState(EventStatus.OPEN, null, null)),
            EventActionRecord(2, "e018", EventActionType.CONFIRM_HANDOVER, at("07:16"), null, null, EventState(EventStatus.IN_PROGRESS, ShiftType.NIGHT, null)),
            EventActionRecord(3, "e005", EventActionType.RESOLVE, at("07:15"), "Werte gemessen", null, taken),
        )
        val werner = events.map { if (it.id == "e005") it.copy(status = EventStatus.DONE, assignedTo = ShiftType.EARLY, resolution = Resolution.DONE) else it }
        val detail = requireNotNull(PersonDetail.build(data.copy(events = werner, actions = actions), "r04", now, early))

        // Gleiche Uhrzeit: neuere Aktion zuerst; Aktionen anderer Personen fehlen.
        assertEquals(listOf(3L, 1L, null), detail.history.map { it.action?.record?.id })
        assertEquals(listOf(resolved, taken), detail.history.mapNotNull { it.action?.after })
        assertEquals("Werte gemessen", detail.history.first().action?.record?.note)
        assertEquals(listOf("e005", "e005", "e005"), detail.history.map { it.event.id })
        assertTrue(detail.hints.isEmpty())
    }
}
