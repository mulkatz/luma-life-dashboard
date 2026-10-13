package com.mulkatz.lumacare.domain

import java.time.LocalDateTime

/** Eintrag im Schichtverlauf: ein Luma-Hinweis oder ein geplanter Routinetermin. */
sealed interface HistoryEntry {
    val time: LocalDateTime
    val resident: Resident

    /** Hinweis mit Originaltext, auch erledigt. */
    data class Hint(val event: LumaEvent, override val resident: Resident) : HistoryEntry {
        override val time: LocalDateTime get() = event.timestamp
    }

    /** Routinetermin bis „jetzt“, immer nur „geplant“: Durchführungsdaten gibt es nicht. */
    data class PlannedRoutine(
        val routine: Routine,
        override val resident: Resident,
        override val time: LocalDateTime,
    ) : HistoryEntry
}

/** Chronologische Liste der Zeitleiste, jüngste zuerst, getrennt nach laufender Schicht und Vorschicht. */
data class ShiftHistory(
    /** Ab Schichtbeginn. */
    val currentShift: List<HistoryEntry>,
    /** [20:00 des Vortags, Schichtbeginn). */
    val previousShift: List<HistoryEntry>,
) {
    companion object {
        /** Jüngste zuerst; bei gleicher Zeit nach Zimmer, dann ID. */
        private val ORDER: Comparator<HistoryEntry> =
            compareByDescending<HistoryEntry> { it.time }
                .thenBy { it.resident.room }
                .thenBy { it.id }

        /**
         * Reine Funktion: alle Hinweise der [timeline] und die Routinetermine bis einschließlich [now].
         * Aktivitäten gehören nicht dazu.
         */
        fun from(timeline: Timeline, now: LocalDateTime): ShiftHistory {
            val hints = timeline.events.map { HistoryEntry.Hint(it.event, it.resident) }
            val routines = timeline.routines
                .filter { !it.time.isAfter(now) }
                .map { HistoryEntry.PlannedRoutine(it.routine, it.resident, it.time) }
            val (current, previous) = (hints + routines)
                .sortedWith(ORDER)
                .partition { !it.time.isBefore(timeline.shiftStart) }
            return ShiftHistory(currentShift = current, previousShift = previous)
        }

        private val HistoryEntry.id: String
            get() = when (this) {
                is HistoryEntry.Hint -> event.id
                is HistoryEntry.PlannedRoutine -> routine.id
            }
    }
}
