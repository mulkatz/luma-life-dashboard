package com.mulkatz.lumacare.ui.actions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mulkatz.lumacare.AppContainer
import com.mulkatz.lumacare.data.repository.ActionResult
import com.mulkatz.lumacare.data.repository.CareRepository
import com.mulkatz.lumacare.data.repository.UndoResult
import com.mulkatz.lumacare.domain.EventCommand
import com.mulkatz.lumacare.domain.EventEntry
import com.mulkatz.lumacare.domain.EventState
import com.mulkatz.lumacare.domain.ForwardTarget
import com.mulkatz.lumacare.domain.LumaEvent
import com.mulkatz.lumacare.domain.Prioritizer
import com.mulkatz.lumacare.domain.Resolution
import com.mulkatz.lumacare.domain.ShiftClock
import com.mulkatz.lumacare.domain.TransitionError
import com.mulkatz.lumacare.domain.state
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Offenes Sheet zu einem Hinweis. */
sealed interface ActionSheet {
    val eventId: String

    /** Abschließen bzw. Weiterleiten. */
    data class Resolve(override val eventId: String) : ActionSheet

    /** Notiz ohne Statuswechsel. */
    data class Note(override val eventId: String) : ActionSheet
}

/** Eingaben und Sperren der Aktionen; Grundlage für Sheets und Buttons. */
data class ActionInputState(
    val sheet: ActionSheet? = null,
    /** Gewählte Abschlussart im offenen Sheet. */
    val resolution: Resolution? = null,
    /** Gewähltes Ziel beim Weiterleiten. */
    val forwardTarget: ForwardTarget? = null,
    /** Notizentwürfe pro Hinweis. */
    val drafts: Map<String, String> = emptyMap(),
    /** Das Repository hat die Notiz abgelehnt (fehlt oder zu kurz); bis zur nächsten Eingabe sichtbar. */
    val noteRejected: Boolean = false,
    /** Hinweise, zu denen gerade ein Befehl gespeichert wird. */
    val busy: Set<String> = emptySet(),
) {
    fun draft(eventId: String): String = drafts[eventId].orEmpty()

    fun isBusy(eventId: String): Boolean = eventId in busy

    /** Der Befehl, den das Abschluss-Sheet mit der aktuellen Eingabe ausführen würde. */
    fun resolveCommand(eventId: String): EventCommand.Resolve {
        val chosen = resolution ?: Resolution.DONE
        return EventCommand.Resolve(
            resolution = chosen,
            note = draft(eventId).takeIf { it.isNotBlank() },
            forwardedTo = forwardTarget.takeIf { chosen == Resolution.FORWARDED },
        )
    }
}

/** Ereignisse der Aktionen. Das ViewModel setzt sie um; Previews nutzen [PersonActions.None]. */
interface PersonActions {
    /** Direkter Befehl ohne Eingabe: Übernehmen, Übernahme bestätigen, Zur Kenntnis, Zurückgeben. */
    fun perform(event: LumaEvent, command: EventCommand)

    fun openResolve(event: LumaEvent)

    fun openNote(eventId: String)

    fun dismissSheet()

    fun selectResolution(value: Resolution)

    fun selectForwardTarget(value: ForwardTarget)

    fun updateDraft(eventId: String, text: String)

    fun insertSnippet(eventId: String, snippet: String)

    fun saveNote(event: LumaEvent)

    companion object {
        /** Tut nichts; für Previews. */
        val None: PersonActions = object : PersonActions {
            override fun perform(event: LumaEvent, command: EventCommand) = Unit
            override fun openResolve(event: LumaEvent) = Unit
            override fun openNote(eventId: String) = Unit
            override fun dismissSheet() = Unit
            override fun selectResolution(value: Resolution) = Unit
            override fun selectForwardTarget(value: ForwardTarget) = Unit
            override fun updateDraft(eventId: String, text: String) = Unit
            override fun insertSnippet(eventId: String, snippet: String) = Unit
            override fun saveNote(event: LumaEvent) = Unit
        }
    }
}

/**
 * Zuletzt gemeldete Rückmeldung für die Snackbar, als Zustand im ViewModel: Sie übersteht eine Rotation
 * innerhalb der Anzeigezeit. [id] unterscheidet aufeinanderfolgende gleiche Meldungen.
 */
sealed interface ActionFeedback {
    val id: Long

    /** Geschrieben; [actionId] macht „Rückgängig“ möglich, [command] bestimmt den Text. */
    data class Applied(override val id: Long, val actionId: Long, val eventId: String, val command: EventCommand) : ActionFeedback

    /** Der Hinweis hatte sich inzwischen geändert; nichts geschrieben. */
    data class Stale(override val id: Long) : ActionFeedback

    /** Rückgängig ging nicht (nicht mehr die letzte Aktion oder Zustand geändert); nichts geändert. */
    data class UndoFailed(override val id: Long) : ActionFeedback
}

/**
 * Aktionen der Pflegekraft für alle Hinweise: direkte Befehle (Übernehmen, Übernahme bestätigen, Zur Kenntnis,
 * Zurückgeben), Notiz und Abschluss über Sheets, Rückgängig.
 *
 * - Was eine Unterbrechung überleben muss, liegt im [SavedStateHandle]: Notizentwurf pro Hinweis, offenes
 *   Sheet, gewählte Abschlussart und gewähltes Ziel. Der Zustand [input] ist Compose-State, damit
 *   Texteingaben synchron bleiben.
 * - Geschrieben wird mit dem angezeigten Zustand als Erwartung (`expected`); weicht er ab, kommt
 *   [ActionFeedback.Stale], der Entwurf bleibt. Doppeltipp: Solange ein Befehl zu einem Hinweis läuft, ist
 *   er gesperrt ([ActionInputState.busy]); danach wird ein Befehl ignoriert, solange die Anzeige noch den
 *   Zustand vor dem letzten erfolgreichen Statuswechsel zeigt. So ersetzt kein „Wurde inzwischen geändert“
 *   die Snackbar mit „Rückgängig“.
 * - Die letzte Rückmeldung liegt in [feedback]; die Snackbar zeigt sie, [undo] und [dismissFeedback] beenden sie.
 * - Die Uhr kommt als Lieferfunktion und wird erst im Hintergrund gelesen (erster Zugriff liest die Platte).
 */
class PersonActionsViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: CareRepository,
    private val loadClock: () -> ShiftClock,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel(), PersonActions {

    var input: ActionInputState by mutableStateOf(restore())
        private set

    val sheet: ActionSheet? get() = input.sheet
    val resolution: Resolution? get() = input.resolution
    val forwardTarget: ForwardTarget? get() = input.forwardTarget
    val noteRejected: Boolean get() = input.noteRejected

    /** Die jüngste Rückmeldung; `null`, wenn keine Snackbar zu zeigen ist. */
    var feedback: ActionFeedback? by mutableStateOf(null)
        private set

    private var feedbackCount = 0L

    /** Pro Hinweis der Zustand vor dem letzten erfolgreichen Statuswechsel (Schutz vor dem zweiten Tipp). */
    private val lastExpected = mutableMapOf<String, EventState>()

    fun isBusy(eventId: String): Boolean = input.isBusy(eventId)

    fun draft(eventId: String): String = input.draft(eventId)

    fun resolveCommand(eventId: String): EventCommand.Resolve = input.resolveCommand(eventId)

    override fun updateDraft(eventId: String, text: String) {
        input = input.copy(drafts = if (text.isEmpty()) input.drafts - eventId else input.drafts + (eventId to text), noteRejected = false)
        savedStateHandle[DRAFT_PREFIX + eventId] = text.ifEmpty { null }
    }

    /** Textbaustein an den Entwurf anhängen. */
    override fun insertSnippet(eventId: String, snippet: String) = updateDraft(eventId, appendSnippet(draft(eventId), snippet))

    /** Öffnet das Abschluss-Sheet; Weiterleiten-Typen mit vorgewähltem „Weitergeleitet an …“. */
    override fun openResolve(event: LumaEvent) {
        showSheet(ActionSheet.Resolve(event.id))
        chooseResolution(if (event.type in Prioritizer.FORWARD_TYPES) Resolution.FORWARDED else Resolution.DONE)
        chooseTarget(null)
    }

    override fun openNote(eventId: String) = showSheet(ActionSheet.Note(eventId))

    override fun dismissSheet() {
        showSheet(null)
        chooseResolution(null)
        chooseTarget(null)
    }

    override fun selectResolution(value: Resolution) = chooseResolution(value)

    override fun selectForwardTarget(value: ForwardTarget) = chooseTarget(value)

    override fun saveNote(event: LumaEvent) = perform(event, EventCommand.AddNote(draft(event.id)))

    /**
     * Wendet [command] auf [event] an, mit dem angezeigten Zustand als Erwartung. Läuft zu diesem Hinweis
     * schon ein Befehl oder zeigt die Anzeige noch den Stand vor dem letzten Statuswechsel, passiert nichts.
     */
    override fun perform(event: LumaEvent, command: EventCommand) {
        val eventId = event.id
        if (input.isBusy(eventId) || lastExpected[eventId] == event.state) return
        input = input.copy(busy = input.busy + eventId)
        val expected = event.state
        viewModelScope.launch {
            try {
                val clock = withContext(backgroundDispatcher) { loadClock() }
                val result = repository.apply(eventId, expected, command, clock.shift.type, clock.now())
                onResult(eventId, expected, command, result)
            } finally {
                input = input.copy(busy = input.busy - eventId)
            }
        }
    }

    /**
     * „Rückgängig“ aus der Snackbar [feedbackId]. Gilt nur, solange diese Rückmeldung die aktuelle ist.
     * Bei Abschluss oder Notiz kommt die Notiz als Entwurf zurück; scheitert es, folgt [ActionFeedback.UndoFailed].
     */
    fun undo(feedbackId: Long) {
        val applied = feedback as? ActionFeedback.Applied ?: return
        if (applied.id != feedbackId) return
        feedback = null
        viewModelScope.launch {
            val clock = withContext(backgroundDispatcher) { loadClock() }
            if (repository.undo(applied.actionId, clock.now()) == UndoResult.Undone) {
                lastExpected.remove(applied.eventId)
                restoreDraft(applied)
            } else {
                report { ActionFeedback.UndoFailed(it) }
            }
        }
    }

    /** Snackbar ausgeblendet (Zeit abgelaufen); eine neuere Rückmeldung bleibt unberührt. */
    fun dismissFeedback(feedbackId: Long) {
        if (feedback?.id == feedbackId) feedback = null
    }

    private fun onResult(eventId: String, expected: EventState, command: EventCommand, result: ActionResult) {
        when (result) {
            is ActionResult.Applied -> {
                // Eine Notiz ändert den Zustand nicht; sie sperrt deshalb keinen weiteren Befehl.
                if (command !is EventCommand.AddNote) lastExpected[eventId] = expected
                if (command is EventCommand.AddNote || command is EventCommand.Resolve) updateDraft(eventId, "")
                if (sheet?.eventId == eventId) dismissSheet()
                report { ActionFeedback.Applied(it, result.actionId, eventId, command) }
            }
            ActionResult.Stale -> {
                // Entwurf bleibt; die Anzeige zeigt ohnehin den aktuellen Stand.
                if (sheet?.eventId == eventId) dismissSheet()
                report { ActionFeedback.Stale(it) }
            }
            is ActionResult.Rejected -> when (result.error) {
                TransitionError.NOTE_REQUIRED, TransitionError.NOTE_TOO_SHORT -> input = input.copy(noteRejected = true)
                TransitionError.NOT_ALLOWED, TransitionError.FORWARD_TARGET_REQUIRED -> {
                    if (sheet?.eventId == eventId) dismissSheet()
                    report { ActionFeedback.Stale(it) }
                }
            }
        }
    }

    private fun report(create: (Long) -> ActionFeedback) {
        feedback = create(++feedbackCount)
    }

    /** Notiz des rückgängig gemachten Abschlusses bzw. der Notiz als Entwurf, falls keiner da ist. */
    private fun restoreDraft(applied: ActionFeedback.Applied) {
        val note = when (val command = applied.command) {
            is EventCommand.AddNote -> command.text
            is EventCommand.Resolve -> command.note
            else -> null
        }?.trim()
        if (!note.isNullOrEmpty() && draft(applied.eventId).isEmpty()) updateDraft(applied.eventId, note)
    }

    private fun showSheet(value: ActionSheet?) {
        input = input.copy(sheet = value, noteRejected = false)
        savedStateHandle[KEY_SHEET_KIND] = when (value) {
            is ActionSheet.Resolve -> SHEET_RESOLVE
            is ActionSheet.Note -> SHEET_NOTE
            null -> null
        }
        savedStateHandle[KEY_SHEET_EVENT] = value?.eventId
    }

    private fun chooseResolution(value: Resolution?) {
        input = input.copy(resolution = value)
        savedStateHandle[KEY_RESOLUTION] = value?.name
    }

    private fun chooseTarget(value: ForwardTarget?) {
        input = input.copy(forwardTarget = value)
        savedStateHandle[KEY_TARGET] = value?.name
    }

    private fun restore(): ActionInputState {
        val eventId = savedStateHandle.get<String>(KEY_SHEET_EVENT)
        val sheet = when (savedStateHandle.get<String>(KEY_SHEET_KIND)) {
            SHEET_RESOLVE -> eventId?.let { ActionSheet.Resolve(it) }
            SHEET_NOTE -> eventId?.let { ActionSheet.Note(it) }
            else -> null
        }
        val drafts = savedStateHandle.keys()
            .filter { it.startsWith(DRAFT_PREFIX) }
            .mapNotNull { key -> savedStateHandle.get<String>(key)?.let { key.removePrefix(DRAFT_PREFIX) to it } }
            .toMap()
        return ActionInputState(
            sheet = sheet,
            resolution = savedStateHandle.get<String>(KEY_RESOLUTION)?.let(Resolution::valueOf),
            forwardTarget = savedStateHandle.get<String>(KEY_TARGET)?.let(ForwardTarget::valueOf),
            drafts = drafts,
        )
    }

    companion object {
        private const val DRAFT_PREFIX = "note_draft:"
        private const val KEY_SHEET_KIND = "sheet_kind"
        private const val KEY_SHEET_EVENT = "sheet_event"
        private const val KEY_RESOLUTION = "resolution"
        private const val KEY_TARGET = "forward_target"
        private const val SHEET_RESOLVE = "resolve"
        private const val SHEET_NOTE = "note"

        /** Baustein ans Ende: „Werte gemessen. Arzt informiert“. */
        fun appendSnippet(draft: String, snippet: String): String {
            val base = draft.trimEnd().trimEnd('.')
            return if (base.isEmpty()) snippet else "$base. $snippet"
        }

        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                PersonActionsViewModel(
                    savedStateHandle = createSavedStateHandle(),
                    repository = container.careRepository,
                    loadClock = { container.shiftClock },
                )
            }
        }
    }
}

/**
 * Schnellaktion eines Hinweises: führt den direkten Befehl aus oder öffnet das Abschluss-Sheet
 * (Abschließen, Weiterleiten). `true`, wenn ein Sheet geöffnet wurde.
 */
fun PersonActions.runAction(entry: EventEntry): Boolean {
    val command = entry.action.directCommand
    if (command == null) {
        openResolve(entry.event)
        return true
    }
    perform(entry.event, command)
    return false
}
