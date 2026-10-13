package com.mulkatz.lumacare.domain

import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Fixtures: Montag, 12.10.2026, 07:15, Frühdienst 06:00–14:00. Alle Daten fiktiv. */
class PrioritizerTest {
    private val now = at("07:15")
    private val early = Shift(ShiftType.EARLY, at("06:00"), at("14:00"), unit = "Wohnbereich Test")

    private fun at(time: String, day: Int = 12) = LocalDateTime.parse("2026-10-%02dT%s:00".format(day, time))

    private fun resident(id: String, room: String = "1.$id", healthNotes: List<String> = emptyList()) = Resident(
        id = id,
        firstName = "Vorname",
        lastName = id,
        age = 80,
        room = room,
        careLevel = 2,
        biography = emptyList(),
        preferences = emptyList(),
        healthNotes = healthNotes,
        contactName = null,
        contactRelation = null,
    )

    private fun event(
        id: String,
        residentId: String,
        severity: Severity = Severity.ATTENTION,
        status: EventStatus = EventStatus.OPEN,
        type: EventType = EventType.HEALTH_HINT,
        timestamp: LocalDateTime = at("06:30"),
        assignedTo: ShiftType? = if (status == EventStatus.IN_PROGRESS) ShiftType.NIGHT else null,
    ) = LumaEvent(id, residentId, timestamp, type, severity, status, "Fiktiver Hinweis.", assignedTo, null, timestamp)

    private fun routine(
        id: String,
        residentId: String,
        time: String,
        days: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    ) = Routine(id, residentId, "Routine $id", LocalTime.parse(time), days, lumaCall = true)

    private fun activity(id: String, start: LocalDateTime, registered: List<String>) =
        Activity(id, "Aktivität $id", start, "Gemeinschaftsraum", registered)

    private fun build(
        residents: List<Resident>,
        events: List<LumaEvent> = emptyList(),
        routines: List<Routine> = emptyList(),
        activities: List<Activity> = emptyList(),
        now: LocalDateTime = this.now,
        shift: Shift = early,
    ) = Prioritizer.build(CareData(residents, events, routines, activities), now, shift)

    private fun List<PersonEntry>.ids() = map { it.resident.id }

    @Test
    fun `abschnitt ist höchste offene dringlichkeit und erledigte zählen nicht`() {
        val model = build(
            residents = listOf(resident("a"), resident("b"), resident("c"), resident("d")),
            events = listOf(
                event("e1", "a", Severity.INFO),
                event("e2", "a", Severity.URGENT),
                event("e3", "b", Severity.URGENT, EventStatus.DONE),
                event("e4", "b", Severity.INFO),
                event("e5", "c", Severity.ATTENTION, EventStatus.IN_PROGRESS),
                event("e6", "d", Severity.URGENT, EventStatus.DONE),
            ),
        )

        assertEquals(listOf("a"), model.actNow.ids())
        assertEquals(listOf("c"), model.checkSoon.ids())
        assertEquals(listOf("b"), model.organizational.ids())
        assertEquals("e2", model.actNow.single().leadEvent.event.id)
        assertEquals(listOf("e2", "e1"), model.actNow.single().events.map { it.event.id })
    }

    @Test
    fun `sortierung erst nach zuständigkeit dann ältestes zuerst`() {
        val model = build(
            residents = listOf(resident("own"), resident("prev"), resident("new"), resident("old")),
            events = listOf(
                event("e1", "own", status = EventStatus.IN_PROGRESS, assignedTo = ShiftType.EARLY, timestamp = at("05:00")),
                event("e2", "prev", status = EventStatus.IN_PROGRESS, timestamp = at("23:00", day = 11)),
                event("e3", "new", timestamp = at("07:00")),
                event("e4", "old", timestamp = at("06:10")),
            ),
        )

        assertEquals(listOf("old", "new", "prev", "own"), model.checkSoon.ids())
    }

    @Test
    fun `gleicher zeitstempel sortiert nach zimmer dann id`() {
        val model = build(
            residents = listOf(resident("x", room = "2.09"), resident("y", room = "2.01"), resident("z", room = "2.01")),
            events = listOf(
                event("e9", "x", Severity.INFO),
                event("e8", "z", Severity.INFO),
                event("e7", "y", Severity.INFO),
            ),
        )

        assertEquals(listOf("y", "z", "x"), model.organizational.ids())
    }

    @Test
    fun `maßgeblich ist bei gleicher dringlichkeit der hinweis mit dem kleinsten schlüssel`() {
        val model = build(
            residents = listOf(resident("a"), resident("b")),
            events = listOf(
                // a: eigener alter Vorgang und neuer offener Hinweis → der offene bestimmt die Position
                event("e1", "a", status = EventStatus.IN_PROGRESS, assignedTo = ShiftType.EARLY, timestamp = at("05:00")),
                event("e2", "a", timestamp = at("07:10")),
                event("e3", "b", timestamp = at("07:00")),
            ),
        )

        assertEquals(listOf("b", "a"), model.checkSoon.ids())
        assertEquals("e2", model.checkSoon.last().leadEvent.event.id)
    }

    @Test
    fun `vorschicht hängt an der zuständigkeit nicht am zeitstempel`() {
        val residents = listOf(resident("a"))
        val night = event("e1", "a", Severity.URGENT, EventStatus.IN_PROGRESS, assignedTo = ShiftType.NIGHT)
        val own = night.copy(assignedTo = ShiftType.EARLY)

        val fromNight = build(residents, listOf(night)).actNow.single().leadEvent
        assertEquals(Handling.PREVIOUS_SHIFT, fromNight.handling)
        assertEquals(EventAction.CONFIRM_HANDOVER, fromNight.action)

        val mine = build(residents, listOf(own)).actNow.single().leadEvent
        assertEquals(Handling.OWN, mine.handling)
        assertEquals(EventAction.RESOLVE, mine.action)

        // im Nachtdienst ist derselbe Vorgang der eigene
        val nightShift = Shift(ShiftType.NIGHT, at("22:00", day = 11), at("06:00"), unit = "Wohnbereich Test")
        assertEquals(Handling.OWN, build(residents, listOf(night), shift = nightShift).actNow.single().leadEvent.handling)

        val open = build(residents, listOf(night.copy(status = EventStatus.OPEN, assignedTo = null))).actNow.single()
        assertEquals(EventAction.TAKE_OVER, open.leadEvent.action)
    }

    @Test
    fun `request essenswunsch und angehörigennachricht werden weitergeleitet, auch wenn dringend`() {
        val model = build(
            residents = listOf(resident("a")),
            events = listOf(
                event("e1", "a", Severity.URGENT, type = EventType.REQUEST),
                event("e2", "a", Severity.INFO, type = EventType.MEAL_WISH),
                event("e3", "a", Severity.INFO, type = EventType.FAMILY_MESSAGE, status = EventStatus.IN_PROGRESS),
                event("e4", "a", Severity.INFO, type = EventType.MOOD),
                event("e5", "a", Severity.INFO, type = EventType.MISSED_CALL),
            ),
        )

        val actions = model.actNow.single().events.associate { it.event.id to it.action }
        assertEquals(EventAction.FORWARD, actions["e1"])
        assertEquals(EventAction.FORWARD, actions["e2"])
        assertEquals(EventAction.FORWARD, actions["e3"])
        assertEquals(EventAction.TAKE_OVER, actions["e4"])
        assertEquals(EventAction.TAKE_OVER, actions["e5"])
    }

    @Test
    fun `kontext-chips kommen nur aus den daten`() {
        val sitzgymnastik = activity("a1", at("10:30"), listOf("a"))
        val model = build(
            residents = listOf(resident("a", healthNotes = listOf("Diabetes Typ 2, achtet auf Zucker"))),
            events = listOf(
                event("e1", "a", Severity.URGENT),
                event("e2", "a", Severity.INFO, type = EventType.MISSED_CALL),
                event("e3", "a", Severity.INFO, type = EventType.MISSED_CALL),
                event("e4", "a", Severity.INFO, type = EventType.MISSED_CALL, status = EventStatus.DONE),
            ),
            routines = listOf(
                routine("rt1", "a", "06:15"), // genau now − 60 min
                routine("rt2", "a", "08:15"), // genau now + 60 min
                routine("rt3", "a", "08:16"), // außerhalb
                routine("rt4", "a", "07:00", days = setOf(DayOfWeek.SUNDAY)), // falscher Wochentag
            ),
            activities = listOf(
                sitzgymnastik,
                activity("a2", at("07:00"), listOf("a")), // schon begonnen
                activity("a3", at("15:00"), listOf("a")), // nach Schichtende
                activity("a4", at("11:00"), listOf("x")), // nicht angemeldet
            ),
        )

        assertEquals(
            listOf(
                ContextChip.HealthNote("Diabetes Typ 2, achtet auf Zucker"),
                ContextChip.RoutineNearby(routine("rt1", "a", "06:15"), at("06:15")),
                ContextChip.RoutineNearby(routine("rt2", "a", "08:15"), at("08:15")),
                ContextChip.ActivityAhead(sitzgymnastik),
                ContextChip.FurtherEvents(2),
                ContextChip.LumaNotReached,
            ),
            model.actNow.single().context,
        )
    }

    @Test
    fun `demnächst enthält nur termine von now bis plus 60 minuten am passenden wochentag`() {
        val model = build(
            residents = listOf(resident("a"), resident("b")),
            routines = listOf(
                routine("rt1", "a", "07:14"), // knapp vorbei
                routine("rt2", "a", "07:30"),
                routine("rt3", "a", "08:15"), // Grenze, enthalten
                routine("rt4", "a", "08:16"),
                routine("rt5", "b", "07:45", days = setOf(DayOfWeek.SUNDAY)),
                routine("rt6", "b", "07:15"), // genau now
            ),
            activities = listOf(
                activity("a1", at("08:00"), listOf("b")),
                activity("a2", at("10:30"), listOf("b")),
            ),
        )

        assertEquals(
            listOf("07:15" to "rt6", "07:30" to "rt2", "08:00" to "a1", "08:15" to "rt3"),
            model.upcoming.map { item ->
                val id = when (item) {
                    is UpcomingItem.RoutineItem -> item.routine.id
                    is UpcomingItem.ActivityItem -> item.activity.id
                }
                item.time.toLocalTime().toString() to id
            },
        )
        assertTrue(model.upcoming.all { it.openHints.isEmpty() })
    }

    @Test
    fun `demnächst verknüpft den maßgeblichen offenen hinweis der beteiligten personen`() {
        val hoergeraete = event("e14", "m", Severity.ATTENTION, timestamp = at("06:45"))
        val besuch = event("e13", "m", Severity.INFO, type = EventType.FAMILY_MESSAGE, timestamp = at("22:40", day = 11))
        val muede = event("e24", "l", Severity.ATTENTION)
        val model = build(
            residents = listOf(resident("m"), resident("l"), resident("o")),
            events = listOf(hoergeraete, besuch, muede, event("e99", "o", status = EventStatus.DONE)),
            routines = listOf(routine("rt7", "m", "07:30"), routine("rt8", "o", "07:40")),
            activities = listOf(activity("a2", at("08:00"), listOf("o", "l"))),
        )

        val byId = model.upcoming.associateBy {
            when (it) {
                is UpcomingItem.RoutineItem -> it.routine.id
                is UpcomingItem.ActivityItem -> it.activity.id
            }
        }
        assertEquals(listOf(hoergeraete), byId.getValue("rt7").openHints)
        assertEquals(emptyList(), byId.getValue("rt8").openHints)
        assertEquals(listOf(muede), byId.getValue("a2").openHints)
    }

    @Test
    fun `kennzahlen zählen einheitlich personen, nicht hinweise`() {
        val model = build(
            residents = listOf(resident("a"), resident("b"), resident("c"), resident("d")),
            events = listOf(
                event("e1", "a", Severity.URGENT),
                event("e2", "a", Severity.URGENT, EventStatus.IN_PROGRESS),
                event("e3", "a", Severity.INFO, EventStatus.IN_PROGRESS),
                event("e4", "b", Severity.URGENT, EventStatus.IN_PROGRESS, assignedTo = ShiftType.EARLY),
                event("e5", "c", Severity.ATTENTION, EventStatus.IN_PROGRESS),
                event("e6", "c", Severity.INFO, type = EventType.MISSED_CALL),
                event("e7", "c", Severity.INFO, type = EventType.MISSED_CALL),
                event("e8", "d", Severity.INFO, type = EventType.MISSED_CALL, status = EventStatus.DONE),
            ),
        )

        assertEquals(
            DashboardMetrics(actNowPersons = 2, checkSoonPersons = 1, previousShiftPersons = 2, notReachedPersons = 1),
            model.metrics,
        )
    }

    @Test
    fun `zeitleiste reicht von 20 uhr am vortag bis schichtende, halboffen`() {
        val model = build(
            residents = listOf(resident("a")),
            events = listOf(
                event("e1", "a", timestamp = at("19:59", day = 11)),
                event("e2", "a", timestamp = at("20:00", day = 11), status = EventStatus.DONE),
                event("e3", "a", timestamp = at("13:59")),
                event("e4", "a", timestamp = at("14:00")),
            ),
            routines = listOf(routine("rt1", "a", "14:00"), routine("rt2", "a", "06:30")),
        )
        val timeline = model.timeline

        assertEquals(at("20:00", day = 11), timeline.start)
        assertEquals(at("14:00"), timeline.end)
        assertEquals(at("06:00"), timeline.shiftStart)
        assertEquals(now, model.now)
        assertEquals(listOf("e2", "e3"), timeline.events.map { it.event.id })
        assertEquals(EventStatus.DONE, timeline.events.first().event.status)
        assertEquals(listOf(at("06:30")), timeline.routines.map { it.time })
    }

    @Test
    fun `sonntägliche routine fehlt am montag und erscheint am sonntagabend davor`() {
        val sunday = setOf(DayOfWeek.SUNDAY)
        val model = build(
            residents = listOf(resident("a")),
            routines = listOf(routine("rt1", "a", "09:00", sunday), routine("rt2", "a", "21:00", sunday)),
        )

        assertEquals(listOf(at("21:00", day = 11)), model.timeline.routines.map { it.time })
    }

    @Test
    fun `zusammenfassung zählt alle offenen dringenden hinweise und nennt die nächste routine`() {
        val model = build(
            residents = listOf(resident("a")),
            events = listOf(
                event("e1", "a", Severity.URGENT, timestamp = at("18:00", day = 11)), // vor dem Zeitfenster
                event("e2", "a", Severity.URGENT, EventStatus.IN_PROGRESS),
                event("e3", "a", Severity.URGENT, EventStatus.DONE),
                event("e4", "a", Severity.ATTENTION),
            ),
            routines = listOf(routine("rt1", "a", "07:00"), routine("rt2", "a", "07:30")),
        )

        assertEquals(2, model.timeline.summary.openUrgentEvents)
        assertEquals("rt2", model.timeline.summary.nextRoutine?.routine?.id)
        assertNull(build(listOf(resident("a")), now = at("13:00")).timeline.summary.nextRoutine)
    }
}
