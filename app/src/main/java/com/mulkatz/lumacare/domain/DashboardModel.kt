package com.mulkatz.lumacare.domain

import java.time.LocalDateTime

/** Ergebnis des [Prioritizer]: alles, was das Dashboard anzeigt, ohne Texte und ohne Layout. */
data class DashboardModel(
    /** Zeitpunkt, für den das Modell berechnet wurde; einzige Quelle für „jetzt“ im Dashboard-Zustand. */
    val now: LocalDateTime,
    /** Personen mit offenem `urgent`. */
    val actNow: List<PersonEntry>,
    /** Personen mit `attention` als höchster offener Dringlichkeit. */
    val checkSoon: List<PersonEntry>,
    /** Personen nur mit offenen `info`-Hinweisen. */
    val organizational: List<PersonEntry>,
    val upcoming: List<UpcomingItem>,
    val metrics: DashboardMetrics,
    val timeline: Timeline,
    /** [timeline] als Stundenblöcke für die Karte „Schichtverlauf“. */
    val hours: ShiftHours,
    /** [timeline] als chronologische Liste für den Screen „Schichtverlauf“, mit den Abschlüssen. */
    val history: ShiftHistory,
    /** Abschlüsse ab Schichtbeginn, die noch Bestand haben, jüngste zuerst („In dieser Schicht erledigt“). */
    val doneInShift: List<ResolvedEntry>,
)

/** Eine Personenkarte: alle offenen Hinweise der Person, der maßgebliche zuerst. */
data class PersonEntry(
    val resident: Resident,
    val events: List<EventEntry>,
    val context: List<ContextChip>,
) {
    /** Der Hinweis, der Abschnitt und Position der Person bestimmt. */
    val leadEvent: EventEntry get() = events.first()
}

data class EventEntry(
    val event: LumaEvent,
    val handling: Handling,
    val action: EventAction,
) {
    /** Herkunft „Aus Nachtdienst“, siehe [isFromNightShift]. */
    val isFromNightShift: Boolean get() = isFromNightShift(event, handling)
}

/**
 * Herkunft „Aus Nachtdienst“ nur, wenn der Nachtdienst den Hinweis nachweislich bearbeitet: Vorschicht
 * ([Handling.PREVIOUS_SHIFT]) und dem Nachtdienst zugeordnet. Einzige Stelle dieser Regel.
 */
fun isFromNightShift(event: LumaEvent, handling: Handling): Boolean =
    handling == Handling.PREVIOUS_SHIFT && event.assignedTo == ShiftType.NIGHT

/** Zuständigkeit eines offenen Hinweises; zugleich die erste Sortierstufe. */
enum class Handling {
    /** `open`: noch niemand hat den Hinweis angenommen. */
    UNATTENDED,

    /** `in_progress`, aber einer anderen Schicht zugeordnet: Übernahme steht aus. */
    PREVIOUS_SHIFT,

    /** `in_progress` und der aktuellen Schicht zugeordnet. */
    OWN,
}

/**
 * Schnellaktion für einen Hinweis. Nur die Auswahl; die Übergänge selbst regelt `EventTransitions`.
 * [ACKNOWLEDGE] ist „Zur Kenntnis“ (Abschluss als [Resolution.ACKNOWLEDGED]).
 */
enum class EventAction {
    TAKE_OVER,
    CONFIRM_HANDOVER,
    RESOLVE,
    FORWARD,
    ACKNOWLEDGE,
    ;

    /** Befehl, den die Aktion ohne weitere Eingabe ausführt; `null` bei Abschließen und Weiterleiten (Sheet). */
    val directCommand: EventCommand?
        get() = when (this) {
            TAKE_OVER -> EventCommand.TakeOver
            CONFIRM_HANDOVER -> EventCommand.ConfirmHandover
            ACKNOWLEDGE -> EventCommand.Resolve(Resolution.ACKNOWLEDGED)
            RESOLVE, FORWARD -> null
        }
}

/** Kontext zu einer Person, ausschließlich aus den Daten abgeleitet. */
sealed interface ContextChip {
    data class HealthNote(val text: String) : ContextChip

    /** Routine heute mit Termin im Fenster now ±60 min. */
    data class RoutineNearby(val routine: Routine, val time: LocalDateTime) : ContextChip

    /** Angemeldete Aktivität, die in der laufenden Schicht noch beginnt. */
    data class ActivityAhead(val activity: Activity) : ContextChip

    /** Anzahl weiterer offener Hinweise neben dem maßgeblichen. */
    data class FurtherEvents(val count: Int) : ContextChip

    /**
     * Die Person hat einen offenen `missed_call`-Hinweis. Ohne Zahl: Anrufversuche stehen nur im Freitext,
     * die Anzahl der Hinweise ist keine Anzahl von Versuchen.
     */
    data object LumaNotReached : ContextChip
}

/**
 * Eintrag unter „Demnächst“. [openHints] verknüpft je beteiligter Person deren maßgeblichen offenen Hinweis.
 * Das ist nur eine Verknüpfung, keine Bewertung, ob sich Termin und Hinweis stören.
 */
sealed interface UpcomingItem {
    val time: LocalDateTime
    val openHints: List<LumaEvent>

    data class RoutineItem(
        val routine: Routine,
        val resident: Resident,
        override val time: LocalDateTime,
        override val openHints: List<LumaEvent>,
    ) : UpcomingItem

    data class ActivityItem(
        val activity: Activity,
        val participants: List<Resident>,
        override val openHints: List<LumaEvent>,
    ) : UpcomingItem {
        override val time: LocalDateTime get() = activity.start
    }
}

/** Kennzahlen, einheitlich als Anzahl Personen. */
data class DashboardMetrics(
    /** Personen unter „Jetzt handeln“. */
    val actNowPersons: Int,
    /** Personen unter „Zeitnah prüfen“. */
    val checkSoonPersons: Int,
    /** Personen mit mindestens einem Hinweis der Vorschicht, dessen Übernahme aussteht. */
    val previousShiftPersons: Int,
    /** Personen mit mindestens einem offenen `missed_call`-Hinweis. */
    val notReachedPersons: Int,
)

/**
 * Zeitleiste von [start] (20:00 des Vortags der Schicht) bis [end] (Schichtende), halboffen [start, end).
 * [shiftStart] ist die Grenze zwischen Vorschicht und Schicht; der Marker „jetzt“ ist [DashboardModel.now].
 *
 * Annahme: Das Fenster ab 20:00 des Vortags ist auf den Frühdienst ausgelegt (Abend und Nacht davor
 * plus eigene Schicht). Für andere Schichttypen ist es nicht angepasst.
 */
data class Timeline(
    val start: LocalDateTime,
    val end: LocalDateTime,
    val shiftStart: LocalDateTime,
    /** Alle Hinweise im Fenster, auch erledigte. */
    val events: List<TimelineEvent>,
    /** Termine heute gültiger Routinen im Fenster; immer nur „geplant“. */
    val routines: List<TimelineRoutine>,
    val summary: TimelineSummary,
)

data class TimelineEvent(val event: LumaEvent, val resident: Resident)

data class TimelineRoutine(val routine: Routine, val resident: Resident, val time: LocalDateTime)

/** Daten für die TalkBack-Zusammenfassung der Zeitleiste; den Text baut die UI. */
data class TimelineSummary(
    /** Alle offenen dringenden Hinweise, unabhängig vom Zeitfenster (wie „Jetzt handeln“). */
    val openUrgentEvents: Int,
    /** Nächster Routinetermin ab now im Fenster, sonst `null`. */
    val nextRoutine: TimelineRoutine?,
)
