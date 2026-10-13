package com.mulkatz.lumacare.domain

import java.time.LocalDateTime

/**
 * Eine Aktion der Pflege mit dem Hinweis und dem Zustand **nach** der Aktion. Gespeichert ist nur der
 * Zustand davor ([EventActionRecord.previous]); der Zustand danach ist der Snapshot der nächsten Aktion
 * desselben Hinweises oder, bei der letzten, der aktuelle Zustand des Hinweises.
 */
data class LoggedAction(
    val record: EventActionRecord,
    val event: LumaEvent,
    val after: EventState,
) {
    val time: LocalDateTime get() = record.createdAt
}

/** Abschluss eines Hinweises, der noch Bestand hat (nicht rückgängig gemacht). */
data class ResolvedEntry(
    val action: LoggedAction,
    val resident: Resident,
    val resolution: Resolution,
) {
    val time: LocalDateTime get() = action.time
    val event: LumaEvent get() = action.event
    val note: String? get() = action.record.note
    val forwardedTo: ForwardTarget? get() = action.record.forwardedTo
}

/** Liest den Verlauf aus [CareData.actions]. Reine Funktionen. */
object ActionLog {
    /** Alle Aktionen mit Zustand danach, in Reihenfolge des Entstehens (ID); Aktionen ohne Hinweis entfallen. */
    fun entries(data: CareData): List<LoggedAction> {
        val events = data.events.associateBy { it.id }
        return data.actions
            .sortedBy { it.id }
            .groupBy { it.eventId }
            .flatMap { (eventId, actions) ->
                val event = events[eventId] ?: return@flatMap emptyList()
                actions.mapIndexed { index, record ->
                    LoggedAction(record, event, after = actions.getOrNull(index + 1)?.previous ?: event.state)
                }
            }
            .sortedBy { it.record.id }
    }

    /**
     * Abschlüsse, die noch Bestand haben: `resolve`-Aktionen, nach denen der Hinweis erledigt ist.
     * Rückgängig gemachte Abschlüsse gibt es nicht mehr (die Aktion ist gelöscht). Jüngste zuerst.
     */
    fun resolved(data: CareData): List<ResolvedEntry> {
        val residents = data.residents.associateBy { it.id }
        return entries(data)
            .filter { it.record.type == EventActionType.RESOLVE && it.after.status == EventStatus.DONE }
            .mapNotNull { action ->
                val resident = residents[action.event.residentId] ?: return@mapNotNull null
                val resolution = action.after.resolution ?: return@mapNotNull null
                ResolvedEntry(action, resident, resolution)
            }
            .sortedWith(compareByDescending<ResolvedEntry> { it.time }.thenByDescending { it.action.record.id })
    }
}
