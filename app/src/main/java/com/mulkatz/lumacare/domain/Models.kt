package com.mulkatz.lumacare.domain

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

/** Bewohnerin oder Bewohner. Gesundheitsangaben stehen getrennt von den Vorlieben in [healthNotes]. */
data class Resident(
    val id: String,
    val firstName: String,
    val lastName: String,
    val age: Int,
    val room: String,
    val careLevel: Int,
    val biography: List<String>,
    val preferences: List<String>,
    val healthNotes: List<String>,
    val contactName: String?,
    val contactRelation: String?,
) {
    val fullName: String get() = "$firstName $lastName"
}

/** Ein Hinweis, den Luma aus einem Gespräch gemeldet hat. */
data class LumaEvent(
    val id: String,
    val residentId: String,
    val timestamp: LocalDateTime,
    val type: EventType,
    val severity: Severity,
    val status: EventStatus,
    val summary: String,
    val assignedTo: ShiftType?,
    val resolution: Resolution?,
    val updatedAt: LocalDateTime,
) {
    /** Offen im Sinne der Priorisierung: alles, was nicht erledigt ist. */
    val isOpen: Boolean get() = status != EventStatus.DONE
}

/** Wiederkehrende Routine zu einer Uhrzeit an bestimmten Wochentagen. Es gibt keinen Durchführungsstatus. */
data class Routine(
    val id: String,
    val residentId: String,
    val title: String,
    val time: LocalTime,
    val days: Set<DayOfWeek>,
    val lumaCall: Boolean,
) {
    /** Alle Termine der Routine im geschlossenen Intervall [from, to], nur an passenden Wochentagen. */
    fun occurrencesBetween(from: LocalDateTime, to: LocalDateTime): List<LocalDateTime> =
        generateSequence(from.toLocalDate()) { it.plusDays(1) }
            .takeWhile { !it.isAfter(to.toLocalDate()) }
            .filter { it.dayOfWeek in days }
            .map { it.atTime(time) }
            .filter { !it.isBefore(from) && !it.isAfter(to) }
            .toList()
}

/** Gruppenangebot mit den IDs der angemeldeten Bewohnenden. */
data class Activity(
    val id: String,
    val title: String,
    val start: LocalDateTime,
    val location: String,
    val registered: List<String>,
)

/** Die laufende Schicht laut Seed-Meta, mit dem Wohnbereich für den Kopfbereich. */
data class Shift(
    val type: ShiftType,
    val start: LocalDateTime,
    val end: LocalDateTime,
    /** Wohnbereich, z. B. „Wohnbereich 2 – Ahorn“. */
    val unit: String,
)

/**
 * Verlaufseintrag zu einem Hinweis: was getan wurde und der Zustand davor (Grundlage für Rückgängig).
 * Heißt nicht `EventAction`, weil das die Schnellaktion im Dashboard ist.
 */
data class EventActionRecord(
    val id: Long,
    val eventId: String,
    val type: EventActionType,
    val createdAt: LocalDateTime,
    val note: String?,
    val forwardedTo: ForwardTarget?,
    val previous: EventState,
)

/** Alle Daten, die das Dashboard braucht, als ein Stand. */
data class CareData(
    val residents: List<Resident>,
    val events: List<LumaEvent>,
    val routines: List<Routine>,
    val activities: List<Activity>,
    /** Verlauf aller Hinweise in Reihenfolge des Entstehens (ID). */
    val actions: List<EventActionRecord> = emptyList(),
)
