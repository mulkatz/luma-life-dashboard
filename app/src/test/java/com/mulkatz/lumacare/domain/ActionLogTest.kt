package com.mulkatz.lumacare.domain

import org.junit.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals

/** Verlauf aus `event_action`: Zustand nach einer Aktion und Abschlüsse mit Bestand. Alle Daten fiktiv. */
class ActionLogTest {
    private fun at(time: String) = LocalDateTime.parse("2026-10-12T$time:00")

    private val resident = Resident(
        id = "r1", firstName = "Vorname", lastName = "r1", age = 80, room = "2.01", careLevel = 2,
        biography = emptyList(), preferences = emptyList(), healthNotes = emptyList(),
        contactName = null, contactRelation = null,
    )

    private val open = EventState(EventStatus.OPEN, null, null)
    private val night = EventState(EventStatus.IN_PROGRESS, ShiftType.NIGHT, null)
    private val own = EventState(EventStatus.IN_PROGRESS, ShiftType.EARLY, null)

    private fun event(id: String, state: EventState, type: EventType = EventType.HEALTH_HINT) = LumaEvent(
        id, "r1", at("06:30"), type, Severity.URGENT, state.status, "Fiktiver Hinweis $id.",
        state.assignedTo, state.resolution, at("06:30"),
    )

    private fun record(id: Long, eventId: String, type: EventActionType, previous: EventState, time: String = "07:15") =
        EventActionRecord(id, eventId, type, at(time), note = null, forwardedTo = null, previous = previous)

    @Test
    fun `zustand danach ist der snapshot der nächsten aktion, bei der letzten der aktuelle`() {
        val done = EventState(EventStatus.DONE, ShiftType.EARLY, Resolution.DONE)
        val data = CareData(
            residents = listOf(resident),
            events = listOf(event("e1", done), event("e2", own)),
            routines = emptyList(),
            activities = emptyList(),
            actions = listOf(
                record(1, "e1", EventActionType.CONFIRM_HANDOVER, night),
                record(2, "e2", EventActionType.ASSIGN, open),
                record(3, "e1", EventActionType.NOTE, own),
                record(4, "e1", EventActionType.RESOLVE, own),
            ),
        )

        val entries = ActionLog.entries(data)

        assertEquals(listOf(1L, 2L, 3L, 4L), entries.map { it.record.id })
        assertEquals(listOf(own, own, own, done), entries.map { it.after })
    }

    @Test
    fun `abschlüsse nur mit bestand, aktionen ohne hinweis entfallen`() {
        val data = CareData(
            residents = listOf(resident),
            events = listOf(event("e1", EventState(EventStatus.DONE, ShiftType.EARLY, Resolution.NO_ACTION_NEEDED))),
            routines = emptyList(),
            activities = emptyList(),
            actions = listOf(
                record(1, "e1", EventActionType.RESOLVE, own, time = "07:20"),
                record(2, "e9", EventActionType.RESOLVE, open),
            ),
        )

        val resolved = ActionLog.resolved(data).single()

        assertEquals("e1", resolved.event.id)
        assertEquals(Resolution.NO_ACTION_NEEDED, resolved.resolution)
        assertEquals(at("07:20"), resolved.time)
        assertEquals(resident, resolved.resident)
    }
}
