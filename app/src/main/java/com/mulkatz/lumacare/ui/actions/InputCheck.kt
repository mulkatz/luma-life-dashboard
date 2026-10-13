package com.mulkatz.lumacare.ui.actions

import com.mulkatz.lumacare.domain.EventCommand
import com.mulkatz.lumacare.domain.EventTransitions
import com.mulkatz.lumacare.domain.ForwardTarget
import com.mulkatz.lumacare.domain.LumaEvent
import com.mulkatz.lumacare.domain.Resolution
import com.mulkatz.lumacare.domain.ShiftType
import com.mulkatz.lumacare.domain.TransitionError
import com.mulkatz.lumacare.domain.TransitionResult

/**
 * Prüfung einer Sheet-Eingabe mit denselben Regeln wie beim Schreiben ([EventTransitions]), ohne zu schreiben.
 *
 * @property valid der Befehl wäre erlaubt; erst dann ist der Button aktiv.
 * @property noteRequired eine Notiz ist Pflicht.
 * @property noteInvalid die Notiz fehlt (bei Pflicht) oder ist zu kurz; dann erscheint der Inline-Fehler.
 */
data class InputCheck(val valid: Boolean, val noteRequired: Boolean, val noteInvalid: Boolean) {
    companion object {
        private val NOTE_ERRORS = setOf(TransitionError.NOTE_REQUIRED, TransitionError.NOTE_TOO_SHORT)

        fun resolve(event: LumaEvent, shift: ShiftType, command: EventCommand.Resolve): InputCheck {
            // Ein fehlendes Ziel verdeckt sonst die Notizregel; für sie zählt nur, dass es ein Ziel gibt.
            val withTarget = if (command.resolution == Resolution.FORWARDED && command.forwardedTo == null) {
                command.copy(forwardedTo = ForwardTarget.entries.first())
            } else {
                command
            }
            return InputCheck(
                valid = EventTransitions.apply(event, shift, command) is TransitionResult.Allowed,
                noteRequired = EventTransitions.apply(event, shift, withTarget.copy(note = null)).isNoteError(),
                noteInvalid = EventTransitions.apply(event, shift, withTarget).isNoteError(),
            )
        }

        /** Abschlussarten im Sheet, in fester Reihenfolge; „Zur Kenntnis“ ist eine Schnellaktion, kein Sheet-Eintrag. */
        private val SHEET_RESOLUTIONS = listOf(Resolution.DONE, Resolution.FORWARDED, Resolution.NO_ACTION_NEEDED)

        /**
         * Abschlussarten, die [EventTransitions] für [event] grundsätzlich erlaubt: Ein Weiterleiten-Ziel gilt als
         * gewählt, die Notiz zählt nicht (sie macht eine Art nur unvollständig, nicht verboten).
         */
        fun resolutionOptions(event: LumaEvent, shift: ShiftType): List<Resolution> = SHEET_RESOLUTIONS.filter { resolution ->
            val target = if (resolution == Resolution.FORWARDED) ForwardTarget.entries.first() else null
            EventTransitions.apply(event, shift, EventCommand.Resolve(resolution, note = null, forwardedTo = target)) !=
                TransitionResult.Rejected(TransitionError.NOT_ALLOWED)
        }

        fun note(event: LumaEvent, shift: ShiftType, text: String): InputCheck {
            val result = EventTransitions.apply(event, shift, EventCommand.AddNote(text))
            return InputCheck(valid = result is TransitionResult.Allowed, noteRequired = true, noteInvalid = result.isNoteError())
        }

        private fun TransitionResult.isNoteError(): Boolean =
            this is TransitionResult.Rejected && error in NOTE_ERRORS
    }
}
