package com.mulkatz.lumacare.domain

import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Baut aus den Daten, `now` und der laufenden Schicht das [DashboardModel]. Reine Funktion.
 *
 * Regeln:
 * - **Offen** heißt `status != DONE`. Personen ohne offene Hinweise erscheinen in keinem Abschnitt.
 * - **Abschnitt** einer Person = höchste Dringlichkeit ihrer offenen Hinweise
 *   (urgent → Jetzt handeln, attention → Zeitnah prüfen, info → Organisatorisches).
 * - **Schlüssel** eines Hinweises: (1) [Handling] `open` → Vorschicht (`in_progress`, anderer Schicht
 *   zugeordnet) → eigene, (2) ältester Zeitstempel zuerst, (3) Zimmer, (4) ID.
 * - **Maßgeblich** ist unter den Hinweisen der höchsten Dringlichkeit der mit dem kleinsten Schlüssel;
 *   nach ihm werden die Personen im Abschnitt sortiert. Die Hinweise einer Person stehen nach
 *   Dringlichkeit, dann Schlüssel; der maßgebliche ist damit der erste.
 * - **Aktion:** `request`, `meal_wish`, `family_message` → Weiterleiten, unabhängig von Dringlichkeit und
 *   Status. Sonst `open` → Übernehmen, Vorschicht → Übernahme bestätigen, eigene → Abschließen.
 * - **Demnächst:** Routinetermine (Wochentag passend) und Aktivitäten im Fenster [now, now+60 min].
 *   Hat die Person der Routine bzw. eine angemeldete Person offene Hinweise, wird je Person deren
 *   maßgeblicher Hinweis verknüpft ([UpcomingItem.openHints]). Keine Bewertung als Konflikt.
 * - **Kontext-Chips** in fester Reihenfolge (einzige Quelle, die UI sortiert nicht neu): Gesundheit,
 *   Routine ±60 min, „Luma nicht erreicht“, Aktivität in der Schicht, zuletzt die Anzahl weiterer Hinweise.
 * - **Kennzahlen** zählen einheitlich Personen: in „Jetzt handeln“, in „Zeitnah prüfen“, mit
 *   Vorschicht-Hinweis ohne Übernahme, mit offenem `missed_call`.
 * - **Zeitleiste:** Fenster [20:00 des Vortags, Schichtende), ausgelegt auf den Frühdienst.
 */
object Prioritizer {
    val NEARBY_WINDOW: Duration = Duration.ofMinutes(60)
    private val TIMELINE_START_TIME: LocalTime = LocalTime.of(20, 0)

    private val FORWARD_TYPES = setOf(EventType.REQUEST, EventType.MEAL_WISH, EventType.FAMILY_MESSAGE)

    fun build(data: CareData, now: LocalDateTime, shift: Shift): DashboardModel {
        val residents = data.residents.associateBy { it.id }
        val openByResident = data.events
            .filter { it.isOpen && it.residentId in residents }
            .groupBy { it.residentId }

        val entries = openByResident.mapNotNull { (residentId, events) ->
            personEntry(residents.getValue(residentId), events, data, now, shift)
        }.sortedWith(compareBy<PersonEntry> { it.leadEvent.event.severity }.then(personOrder))

        val bySeverity = entries.groupBy { it.leadEvent.event.severity }
        val actNow = bySeverity[Severity.URGENT].orEmpty()
        val checkSoon = bySeverity[Severity.ATTENTION].orEmpty()
        val organizational = bySeverity[Severity.INFO].orEmpty()
        val leadByResident = entries.associate { it.resident.id to it.leadEvent.event }

        return DashboardModel(
            now = now,
            actNow = actNow,
            checkSoon = checkSoon,
            organizational = organizational,
            upcoming = upcoming(data, residents, leadByResident, now),
            metrics = DashboardMetrics(
                actNowPersons = actNow.size,
                checkSoonPersons = checkSoon.size,
                previousShiftPersons = entries.count { entry -> entry.events.any { it.handling == Handling.PREVIOUS_SHIFT } },
                notReachedPersons = entries.count { entry -> entry.events.any { it.event.type == EventType.MISSED_CALL } },
            ),
            timeline = timeline(data, residents, now, shift),
        )
    }

    fun handling(event: LumaEvent, currentShift: ShiftType): Handling = when {
        event.status == EventStatus.OPEN -> Handling.UNATTENDED
        event.assignedTo != currentShift -> Handling.PREVIOUS_SHIFT
        else -> Handling.OWN
    }

    fun action(event: LumaEvent, handling: Handling): EventAction = when {
        event.type in FORWARD_TYPES -> EventAction.FORWARD
        handling == Handling.UNATTENDED -> EventAction.TAKE_OVER
        handling == Handling.PREVIOUS_SHIFT -> EventAction.CONFIRM_HANDOVER
        else -> EventAction.RESOLVE
    }

    /** Schlüssel innerhalb gleicher Dringlichkeit: Zuständigkeit, Alter, Zimmer, ID. */
    private val personOrder: Comparator<PersonEntry> =
        compareBy<PersonEntry> { it.leadEvent.handling }
            .thenBy { it.leadEvent.event.timestamp }
            .thenBy { it.resident.room }
            .thenBy { it.leadEvent.event.id }

    private fun personEntry(
        resident: Resident,
        openEvents: List<LumaEvent>,
        data: CareData,
        now: LocalDateTime,
        shift: Shift,
    ): PersonEntry {
        // Zimmer ist innerhalb einer Person konstant, daher reichen Zuständigkeit, Alter und ID.
        val events = openEvents
            .map { event -> handling(event, shift.type).let { EventEntry(event, it, action(event, it)) } }
            .sortedWith(
                compareBy<EventEntry> { it.event.severity }
                    .thenBy { it.handling }
                    .thenBy { it.event.timestamp }
                    .thenBy { it.event.id },
            )
        return PersonEntry(resident, events, contextChips(resident, openEvents, data, now, shift))
    }

    private fun contextChips(
        resident: Resident,
        openEvents: List<LumaEvent>,
        data: CareData,
        now: LocalDateTime,
        shift: Shift,
    ): List<ContextChip> = buildList {
        resident.healthNotes.forEach { add(ContextChip.HealthNote(it)) }

        data.routines
            .filter { it.residentId == resident.id }
            .flatMap { routine ->
                routine.occurrencesBetween(now - NEARBY_WINDOW, now + NEARBY_WINDOW)
                    .map { ContextChip.RoutineNearby(routine, it) }
            }
            .sortedWith(compareBy({ it.time }, { it.routine.id }))
            .forEach(::add)

        if (openEvents.any { it.type == EventType.MISSED_CALL }) add(ContextChip.LumaNotReached)

        data.activities
            .filter { resident.id in it.registered && it.start.isWithin(now, shift.end) && !it.start.isBefore(shift.start) }
            .sortedWith(compareBy({ it.start }, { it.id }))
            .forEach { add(ContextChip.ActivityAhead(it)) }

        if (openEvents.size > 1) add(ContextChip.FurtherEvents(openEvents.size - 1))
    }

    private fun upcoming(
        data: CareData,
        residents: Map<String, Resident>,
        leadByResident: Map<String, LumaEvent>,
        now: LocalDateTime,
    ): List<UpcomingItem> {
        val until = now + NEARBY_WINDOW
        val routines = data.routines.flatMap { routine ->
            val resident = residents[routine.residentId] ?: return@flatMap emptyList()
            routine.occurrencesBetween(now, until).map { time ->
                UpcomingItem.RoutineItem(routine, resident, time, listOfNotNull(leadByResident[resident.id]))
            }
        }
        val activities = data.activities
            .filter { !it.start.isBefore(now) && !it.start.isAfter(until) }
            .map { activity ->
                val participants = activity.registered.mapNotNull { residents[it] }
                UpcomingItem.ActivityItem(activity, participants, participants.mapNotNull { leadByResident[it.id] })
            }
        return (routines + activities).sortedWith(compareBy({ it.time }, { it.id() }))
    }

    private fun UpcomingItem.id(): String = when (this) {
        is UpcomingItem.RoutineItem -> routine.id
        is UpcomingItem.ActivityItem -> activity.id
    }

    private fun timeline(
        data: CareData,
        residents: Map<String, Resident>,
        now: LocalDateTime,
        shift: Shift,
    ): Timeline {
        val start = shift.start.toLocalDate().minusDays(1).atTime(TIMELINE_START_TIME)
        val end = shift.end

        val events = data.events
            .filter { it.timestamp.isWithin(start, end) }
            .mapNotNull { event -> residents[event.residentId]?.let { TimelineEvent(event, it) } }
            .sortedWith(compareBy({ it.event.timestamp }, { it.event.id }))

        val routines = data.routines
            .flatMap { routine ->
                val resident = residents[routine.residentId] ?: return@flatMap emptyList()
                routine.occurrencesBetween(start, end)
                    .filter { it.isBefore(end) }
                    .map { TimelineRoutine(routine, resident, it) }
            }
            .sortedWith(compareBy({ it.time }, { it.routine.id }))

        return Timeline(
            start = start,
            end = end,
            shiftStart = shift.start,
            events = events,
            routines = routines,
            summary = TimelineSummary(
                openUrgentEvents = data.events.count { it.isOpen && it.severity == Severity.URGENT },
                nextRoutine = routines.firstOrNull { !it.time.isBefore(now) },
            ),
        )
    }

    /** Halboffenes Intervall [from, to). */
    private fun LocalDateTime.isWithin(from: LocalDateTime, to: LocalDateTime): Boolean =
        !isBefore(from) && isBefore(to)
}
