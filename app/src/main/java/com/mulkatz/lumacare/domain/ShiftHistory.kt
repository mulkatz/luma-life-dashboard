package com.mulkatz.lumacare.domain

import java.time.LocalDateTime

/** Eintrag im Schichtverlauf: ein Luma-Hinweis, ein geplanter Routinetermin oder ein Abschluss der Pflege. */
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

    /** Abschluss bzw. Weiterleitung eines Hinweises zur Uhrzeit der Aktion. */
    data class Resolved(val entry: ResolvedEntry) : HistoryEntry {
        override val time: LocalDateTime get() = entry.time
        override val resident: Resident get() = entry.resident
    }
}

/** Chronologische Liste der Zeitleiste, jüngste zuerst, getrennt nach laufender Schicht und Vorschicht. */
data class ShiftHistory(
    /** Ab Schichtbeginn. */
    val currentShift: List<HistoryEntry>,
    /** [20:00 des Vortags, Schichtbeginn). */
    val previousShift: List<HistoryEntry>,
) {
    companion object {
        /** Jüngste zuerst; bei gleicher Zeit der neuere Abschluss zuerst, dann nach Zimmer und ID. */
        private val ORDER: Comparator<HistoryEntry> =
            compareByDescending<HistoryEntry> { it.time }
                .thenByDescending { (it as? HistoryEntry.Resolved)?.entry?.action?.record?.id ?: Long.MIN_VALUE }
                .thenBy { it.resident.room }
                .thenBy { it.id }

        /**
         * Reine Funktion: alle Hinweise der [timeline], die Routinetermine bis einschließlich [now] und die
         * Abschlüsse aus [resolved] im Fenster der Zeitleiste. Aktivitäten gehören nicht dazu.
         */
        fun from(timeline: Timeline, now: LocalDateTime, resolved: List<ResolvedEntry> = emptyList()): ShiftHistory {
            val hints = timeline.events.map { HistoryEntry.Hint(it.event, it.resident) }
            val routines = timeline.routines
                .filter { !it.time.isAfter(now) }
                .map { HistoryEntry.PlannedRoutine(it.routine, it.resident, it.time) }
            val resolutions = resolved
                .filter { !it.time.isBefore(timeline.start) && it.time.isBefore(timeline.end) }
                .map { HistoryEntry.Resolved(it) }
            val (current, previous) = (hints + routines + resolutions)
                .sortedWith(ORDER)
                .partition { !it.time.isBefore(timeline.shiftStart) }
            return ShiftHistory(currentShift = current, previousShift = previous)
        }

        private val HistoryEntry.id: String
            get() = when (this) {
                is HistoryEntry.Hint -> event.id
                is HistoryEntry.PlannedRoutine -> routine.id
                is HistoryEntry.Resolved -> entry.event.id
            }
    }
}
