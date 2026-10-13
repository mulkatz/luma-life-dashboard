package com.mulkatz.lumacare.domain

/** Befehl der Pflegekraft für genau einen Hinweis. */
sealed interface EventCommand {
    /** `open → in_progress`, der aktuellen Schicht zugeordnet. */
    data object TakeOver : EventCommand

    /** `in_progress` der Vorschicht → `in_progress` der aktuellen Schicht. */
    data object ConfirmHandover : EventCommand

    /** Zurückgeben: eigenes `in_progress → open`, ohne Zuordnung. */
    data object Release : EventCommand

    /** Notiz ohne Statuswechsel. */
    data class AddNote(val text: String) : EventCommand

    /** `→ done` mit Abschlussart; [forwardedTo] nur und immer bei [Resolution.FORWARDED]. */
    data class Resolve(
        val resolution: Resolution,
        val note: String? = null,
        val forwardedTo: ForwardTarget? = null,
    ) : EventCommand
}

/** Der Teil eines Hinweises, den Übergänge ändern; zugleich der Snapshot für Rückgängig. */
data class EventState(
    val status: EventStatus,
    val assignedTo: ShiftType?,
    val resolution: Resolution?,
)

val LumaEvent.state: EventState get() = EventState(status, assignedTo, resolution)

enum class TransitionError {
    /** Der Übergang ist aus diesem Zustand verboten. */
    NOT_ALLOWED,

    /** Pflichtnotiz fehlt (leer oder nur Leerzeichen). */
    NOTE_REQUIRED,

    /** Notiz nach `trim()` kürzer als [EventTransitions.MIN_NOTE_LENGTH]. */
    NOTE_TOO_SHORT,

    /** Weiterleiten ohne Ziel. */
    FORWARD_TARGET_REQUIRED,
}

sealed interface TransitionResult {
    /** Neuer Zustand und der Verlaufseintrag dazu; [note] ist bereits getrimmt. */
    data class Allowed(
        val newState: EventState,
        val action: EventActionType,
        val note: String?,
        val forwardedTo: ForwardTarget?,
    ) : TransitionResult

    data class Rejected(val error: TransitionError) : TransitionResult
}

/**
 * Regeln für alle Statuswechsel des Kern-Flows. Reine Funktion, schreibt nichts.
 *
 * - **Übernehmen** nur aus `open`; **Übernahme bestätigen** nur für Vorschicht-Fälle
 *   ([Handling.PREVIOUS_SHIFT]); **Zurückgeben** nur für eigene Fälle.
 * - **Abschließen** nur aus `open` oder eigenem `in_progress`. Vorschicht-Fälle brauchen zuerst
 *   „Übernahme bestätigen“. `done` ist final (nur Rückgängig führt zurück).
 *   - aus `open`: `info` direkt, `attention` mit Notiz, `urgent` nie (erst übernehmen);
 *   - Notiz Pflicht bei `health_hint` und `urgent`, unabhängig vom Ausgangsstatus;
 *   - **Weiterleiten** nur für [Prioritizer.FORWARD_TYPES], aus `open` und eigenem `in_progress`,
 *     Ziel Pflicht; Notiz Pflicht bei `urgent`, sonst optional. Die Regeln für `open` oben gelten hier nicht.
 * - **Notiz** geht für alles, was nicht `done` ist, und ist dann Pflicht.
 * - Eine Notiz ist gültig, wenn sie nach `trim()` mindestens [MIN_NOTE_LENGTH] Zeichen hat. Eine optionale
 *   Notiz darf leer sein, eine angegebene muss trotzdem gültig sein.
 * - Beim Abschließen wird der Hinweis der aktuellen Schicht zugeordnet (sie hat abgeschlossen).
 */
object EventTransitions {
    const val MIN_NOTE_LENGTH = 3

    fun apply(event: LumaEvent, currentShift: ShiftType, command: EventCommand): TransitionResult {
        // done hat keine Zuständigkeit mehr; Prioritizer.handling ist nur für nicht erledigte Hinweise gemeint.
        val handling = if (event.status == EventStatus.DONE) null else Prioritizer.handling(event, currentShift)
        val takenOver = EventState(EventStatus.IN_PROGRESS, currentShift, null)
        return when (command) {
            EventCommand.TakeOver ->
                if (handling == Handling.UNATTENDED) allowed(takenOver, EventActionType.ASSIGN) else NOT_ALLOWED

            EventCommand.ConfirmHandover ->
                if (handling == Handling.PREVIOUS_SHIFT) allowed(takenOver, EventActionType.CONFIRM_HANDOVER) else NOT_ALLOWED

            EventCommand.Release ->
                if (handling == Handling.OWN) {
                    allowed(EventState(EventStatus.OPEN, null, null), EventActionType.RELEASE)
                } else {
                    NOT_ALLOWED
                }

            is EventCommand.AddNote ->
                if (handling == null) {
                    NOT_ALLOWED
                } else {
                    withNote(command.text, required = true) { allowed(event.state, EventActionType.NOTE, it) }
                }

            is EventCommand.Resolve -> resolve(event, handling, currentShift, command)
        }
    }

    /** Status nach einer Aktion; Grundlage für das bedingte Zurückschreiben beim Rückgängigmachen. */
    fun statusAfter(action: EventActionType, previous: EventStatus): EventStatus = when (action) {
        EventActionType.ASSIGN, EventActionType.CONFIRM_HANDOVER -> EventStatus.IN_PROGRESS
        EventActionType.RELEASE -> EventStatus.OPEN
        EventActionType.RESOLVE -> EventStatus.DONE
        EventActionType.NOTE -> previous
    }

    private fun resolve(
        event: LumaEvent,
        handling: Handling?,
        currentShift: ShiftType,
        command: EventCommand.Resolve,
    ): TransitionResult {
        if (handling != Handling.UNATTENDED && handling != Handling.OWN) return NOT_ALLOWED
        val done = EventState(EventStatus.DONE, currentShift, command.resolution)

        if (command.resolution == Resolution.FORWARDED) {
            if (event.type !in Prioritizer.FORWARD_TYPES) return NOT_ALLOWED
            val target = command.forwardedTo ?: return TransitionResult.Rejected(TransitionError.FORWARD_TARGET_REQUIRED)
            return withNote(command.note, required = event.severity == Severity.URGENT) {
                allowed(done, EventActionType.RESOLVE, it, target)
            }
        }
        if (command.forwardedTo != null) return NOT_ALLOWED

        val fromOpenNeedsNote = when (event.severity) {
            Severity.URGENT -> if (handling == Handling.UNATTENDED) return NOT_ALLOWED else true
            Severity.ATTENTION -> handling == Handling.UNATTENDED
            Severity.INFO -> false
        }
        val required = fromOpenNeedsNote || event.type == EventType.HEALTH_HINT || event.severity == Severity.URGENT
        return withNote(command.note, required) { allowed(done, EventActionType.RESOLVE, it) }
    }

    private inline fun withNote(note: String?, required: Boolean, onValid: (String?) -> TransitionResult): TransitionResult {
        val trimmed = note?.trim()?.takeIf { it.isNotEmpty() }
        return when {
            trimmed == null && required -> TransitionResult.Rejected(TransitionError.NOTE_REQUIRED)
            trimmed == null -> onValid(null)
            trimmed.length < MIN_NOTE_LENGTH -> TransitionResult.Rejected(TransitionError.NOTE_TOO_SHORT)
            else -> onValid(trimmed)
        }
    }

    private fun allowed(
        state: EventState,
        action: EventActionType,
        note: String? = null,
        forwardedTo: ForwardTarget? = null,
    ) = TransitionResult.Allowed(state, action, note, forwardedTo)

    private val NOT_ALLOWED = TransitionResult.Rejected(TransitionError.NOT_ALLOWED)
}
