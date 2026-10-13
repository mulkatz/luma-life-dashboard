package com.mulkatz.lumacare.data.repository

import com.mulkatz.lumacare.domain.CareData
import com.mulkatz.lumacare.domain.EventCommand
import com.mulkatz.lumacare.domain.EventState
import com.mulkatz.lumacare.domain.ShiftType
import com.mulkatz.lumacare.domain.TransitionError
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/** Einziger Datenzugang der UI. Liefert Domain-Modelle, keine Room-Entities. */
interface CareRepository {
    /** Importiert den Seed, falls noch nicht geschehen. Läuft nicht auf dem Main-Thread. */
    suspend fun ensureSeeded()

    /** Bewohnende, Hinweise, Routinen, Aktivitäten und Verlauf als ein Stand; emittiert bei jeder Änderung neu. */
    fun observeCareData(): Flow<CareData>

    /**
     * Wendet [command] nach `EventTransitions` an und schreibt Zustand und Verlaufseintrag in einer Transaktion.
     *
     * @param expected der Zustand, den die Pflegekraft gesehen hat. Weicht der gespeicherte ab (doppeltes Tippen,
     *   veraltete Anzeige), ergibt das [ActionResult.Stale], ohne etwas zu schreiben.
     * @param shift die laufende Schicht, aus der `ShiftClock`.
     * @param now aus der `ShiftClock`, nie `LocalDateTime.now()`.
     */
    suspend fun apply(
        eventId: String,
        expected: EventState,
        command: EventCommand,
        shift: ShiftType,
        now: LocalDateTime,
    ): ActionResult

    /**
     * Macht die Aktion [actionId] rückgängig: stellt den Snapshot davor wieder her und löscht die Aktion.
     * Nur die jeweils letzte Aktion eines Hinweises.
     *
     * @param now aus der `ShiftClock`; wird `updated_at` des Hinweises.
     */
    suspend fun undo(actionId: Long, now: LocalDateTime): UndoResult
}

sealed interface ActionResult {
    /** Geschrieben; [actionId] ist der neue Verlaufseintrag (für „Rückgängig“). */
    data class Applied(val actionId: Long) : ActionResult

    /** Der Hinweis hat sich inzwischen geändert oder existiert nicht mehr; nichts geschrieben. */
    data object Stale : ActionResult

    /** Regel verletzt (z. B. Pflichtnotiz fehlt); nichts geschrieben. */
    data class Rejected(val error: TransitionError) : ActionResult
}

sealed interface UndoResult {
    data object Undone : UndoResult

    /** Nach dieser Aktion gibt es eine neuere zum selben Hinweis; nichts geändert. */
    data object NotLatest : UndoResult

    /** Aktion nicht mehr vorhanden (z. B. schon rückgängig gemacht) oder Zustand passt nicht; nichts geändert. */
    data object Stale : UndoResult
}
