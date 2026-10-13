package com.mulkatz.lumacare.domain

import java.time.LocalDateTime

/** Routinetermin heute. Es gibt keinen Durchführungsstatus, die Routine ist immer nur „geplant“. */
data class RoutineToday(
    val routine: Routine,
    val time: LocalDateTime,
)

/** Eintrag im Personenverlauf: ein Luma-Hinweis, auch erledigt. */
data class PersonHistoryEntry(
    val event: LumaEvent,
    /** Gleiche Regel wie auf der Karte ([isFromNightShift]); erledigte Hinweise nie. */
    val isFromNightShift: Boolean,
) {
    val isDone: Boolean get() = event.status == EventStatus.DONE
}

/** Alles, was das Detail einer Person zeigt, ohne Texte und ohne Layout. */
data class PersonDetail(
    val resident: Resident,
    /** Zeitpunkt, für den das Detail berechnet wurde. */
    val now: LocalDateTime,
    /** Offene Hinweise, Reihenfolge wie auf der Karte: der maßgebliche zuerst. */
    val hints: List<EventEntry>,
    /** Routinetermine am Tag von [now] (Wochentag passend), aufsteigend. */
    val routinesToday: List<RoutineToday>,
    /** Angemeldete Aktivitäten, die in der laufenden Schicht beginnen, aufsteigend. */
    val activities: List<Activity>,
    /** Alle Hinweise der Person im Fenster [20:00 des Vortags, Schichtende), jüngste zuerst. */
    val history: List<PersonHistoryEntry>,
) {
    companion object {
        /**
         * Reine Funktion aus denselben [CareData] wie das Dashboard. `null`, wenn es die Person nicht gibt.
         * Offene Hinweise zählen unabhängig vom Zeitfenster (wie auf dem Dashboard), der Verlauf nur im Fenster.
         */
        fun build(data: CareData, residentId: String, now: LocalDateTime, shift: Shift): PersonDetail? {
            val resident = data.residents.firstOrNull { it.id == residentId } ?: return null
            val events = data.events.filter { it.residentId == residentId }

            val today = now.toLocalDate()
            val routines = data.routines
                .filter { it.residentId == residentId }
                .flatMap { routine ->
                    routine.occurrencesBetween(today.atStartOfDay(), today.plusDays(1).atStartOfDay().minusNanos(1))
                        .map { RoutineToday(routine, it) }
                }
                .sortedWith(compareBy({ it.time }, { it.routine.id }))

            val activities = data.activities
                .filter { residentId in it.registered && !it.start.isBefore(shift.start) && it.start.isBefore(shift.end) }
                .sortedWith(compareBy({ it.start }, { it.id }))

            val windowStart = Prioritizer.timelineStart(shift)
            val history = events
                .filter { !it.timestamp.isBefore(windowStart) && it.timestamp.isBefore(shift.end) }
                .sortedWith(compareByDescending<LumaEvent> { it.timestamp }.thenBy { it.id })
                .map { event ->
                    PersonHistoryEntry(
                        event = event,
                        isFromNightShift = event.isOpen && isFromNightShift(event, Prioritizer.handling(event, shift.type)),
                    )
                }

            return PersonDetail(
                resident = resident,
                now = now,
                hints = Prioritizer.eventEntries(events.filter { it.isOpen }, shift.type),
                routinesToday = routines,
                activities = activities,
                history = history,
            )
        }
    }
}
