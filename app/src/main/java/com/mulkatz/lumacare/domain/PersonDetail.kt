package com.mulkatz.lumacare.domain

import java.time.LocalDateTime

/** Routinetermin heute. Es gibt keinen Durchführungsstatus, die Routine ist immer nur „geplant“. */
data class RoutineToday(
    val routine: Routine,
    val time: LocalDateTime,
)

/**
 * Eintrag im Personenverlauf zu einem Hinweis: der Luma-Hinweis selbst (auch erledigt) oder, mit [action],
 * eine Aktion der Pflege dazu (übernommen, bestätigt, zurückgegeben, Notiz, Abschluss). Ohne Namen der
 * Pflege, es gibt kein Login.
 */
data class PersonHistoryEntry(
    val event: LumaEvent,
    /** Gleiche Regel wie auf der Karte ([isFromNightShift]); erledigte Hinweise und Aktionen nie. */
    val isFromNightShift: Boolean,
    /** `null`: der Eintrag ist der Hinweis selbst. */
    val action: LoggedAction? = null,
) {
    val time: LocalDateTime get() = action?.time ?: event.timestamp
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
    /**
     * Hinweise und Aktionen der Person im Fenster [20:00 des Vortags, Schichtende), jüngste zuerst; bei
     * gleicher Zeit die Aktion vor dem Hinweis, neuere Aktion zuerst.
     */
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
            fun inWindow(time: LocalDateTime) = !time.isBefore(windowStart) && time.isBefore(shift.end)
            val hints = events
                .filter { inWindow(it.timestamp) }
                .sortedWith(compareByDescending<LumaEvent> { it.timestamp }.thenBy { it.id })
                .map { event ->
                    PersonHistoryEntry(
                        event = event,
                        isFromNightShift = event.isOpen && isFromNightShift(event, Prioritizer.handling(event, shift.type)),
                    )
                }
            val actions = ActionLog.entries(data)
                .filter { it.event.residentId == residentId && inWindow(it.time) }
                .sortedByDescending { it.record.id }
                .map { PersonHistoryEntry(event = it.event, isFromNightShift = false, action = it) }
            // Stabil sortiert: bei gleicher Zeit bleiben Aktionen (neueste zuerst) vor den Hinweisen.
            val history = (actions + hints).sortedByDescending { it.time }

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
