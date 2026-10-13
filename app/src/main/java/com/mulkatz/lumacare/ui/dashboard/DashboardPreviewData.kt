package com.mulkatz.lumacare.ui.dashboard

import com.mulkatz.lumacare.domain.Activity
import com.mulkatz.lumacare.domain.CareData
import com.mulkatz.lumacare.domain.EventStatus
import com.mulkatz.lumacare.domain.EventType
import com.mulkatz.lumacare.domain.LumaEvent
import com.mulkatz.lumacare.domain.Prioritizer
import com.mulkatz.lumacare.domain.Resident
import com.mulkatz.lumacare.domain.Routine
import com.mulkatz.lumacare.domain.Severity
import com.mulkatz.lumacare.domain.Shift
import com.mulkatz.lumacare.domain.ShiftType
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/** Fiktive, seed-ähnliche Daten nur für Previews. Das Modell entsteht über den echten [Prioritizer]. */
internal object DashboardPreviewData {
    private val day = LocalDateTime.of(2026, 10, 12, 0, 0)
    private val now = day.withHour(7).withMinute(15)
    private val shift = Shift(ShiftType.EARLY, day.withHour(6), day.withHour(14), "Wohnbereich 2 – Ahorn")

    private val residents = listOf(
        resident("r04", "Werner", "Hoffmann", "2.03", 3, "Diabetes Typ 2, achtet auf Zucker"),
        resident("r12", "Rudolf", "Schmidt", "2.12", 5),
        resident("r15", "Lieselotte", "Zimmermann", "2.16", 4),
        resident("r10", "Horst", "Meyer", "2.10", 2),
        resident("r09", "Margarete", "Fischer", "2.09", 4),
        resident("r05", "Elfriede", "Schulz", "2.05", 4),
        resident("r08", "Günter", "Lange", "2.08", 3),
        resident("r14", "Dieter", "Schwarz", "2.15", 3),
    )

    private val events = listOf(
        event("e005", "r04", "06:30", EventType.HEALTH_HINT, Severity.URGENT,
            "Berichtet von Zittern und Schwitzen, hat laut eigener Aussage noch nicht gefrühstückt."),
        event("e018", "r12", "23:15", EventType.HEALTH_HINT, Severity.URGENT,
            "Hat im Abendgespräch über Schmerzen in der Brust gesprochen; Nachtdienst informiert.",
            status = EventStatus.IN_PROGRESS, previousDay = true),
        event("e019", "r12", "06:40", EventType.MISSED_CALL, Severity.ATTENTION, "Morgengespräch zweimal nicht angenommen."),
        event("e024", "r15", "06:15", EventType.HEALTH_HINT, Severity.ATTENTION,
            "Hat beim Gespräch kaum geantwortet, wirkt müder als sonst."),
        event("e015", "r10", "06:35", EventType.REQUEST, Severity.ATTENTION,
            "Beschwert sich, dass seine Zeitung seit zwei Tagen nicht kommt."),
        event("e014", "r09", "06:45", EventType.HEALTH_HINT, Severity.ATTENTION,
            "Sagt, ihre Hörgeräte pfeifen und sie verstehe schlecht."),
        event("e006", "r05", "02:10", EventType.HEALTH_HINT, Severity.ATTENTION,
            "Nachts wach und unruhig, fragte mehrfach nach ihrer Mutter.", status = EventStatus.IN_PROGRESS),
        event("e007", "r05", "06:50", EventType.MISSED_CALL, Severity.INFO, "Morgengespräch nicht angenommen, kein Rückruf."),
        event("e012", "r08", "06:10", EventType.MEAL_WISH, Severity.INFO,
            "Wünscht sich zum Kaffee Butterkuchen statt Marmorkuchen."),
        event("e022", "r14", "06:55", EventType.MEAL_WISH, Severity.INFO,
            "Wünscht sich heute Abend Bratkartoffeln mit Spiegelei."),
        event("e023", "r14", "07:05", EventType.FAMILY_MESSAGE, Severity.INFO,
            "Ehefrau Karin fragt, ob sie Donnerstag zum Mittagessen mitessen kann."),
    )

    private val routines = listOf(
        routine("rt02", "r04", "Blutzucker messen vor dem Frühstück", "07:00", lumaCall = true),
        routine("rt06", "r12", "Morgengespräch auf Plattdeutsch", "06:30", lumaCall = true),
        routine("rt03", "r05", "Morgengespräch", "06:45", lumaCall = true),
        routine("rt07", "r09", "Hörgeräte einsetzen", "07:30", lumaCall = false),
        routine("rt05", "r10", "Nachrichten vorlesen", "08:00", lumaCall = true),
    )

    private val activities = listOf(
        Activity("a02", "Singkreis", day.withHour(11).withMinute(15), "Wintergarten", listOf("r05", "r15")),
    )

    val ready: DashboardUiState.Ready = readyAt(now, CareData(residents, events, routines, activities))

    /** Alles erledigt um 12:40, keine Termine in der nächsten Stunde. */
    val empty: DashboardUiState.Ready = readyAt(
        day.withHour(12).withMinute(40),
        CareData(residents, events.map { it.copy(status = EventStatus.DONE) }, routines, activities),
    )

    private fun readyAt(time: LocalDateTime, data: CareData) = DashboardUiState.Ready(
        model = Prioritizer.build(data, time, shift),
        shift = shift,
        remaining = Duration.between(time, shift.end),
        isShiftOver = false,
    )

    private fun resident(id: String, first: String, last: String, room: String, careLevel: Int, vararg health: String) =
        Resident(id, first, last, age = 85, room, careLevel, emptyList(), emptyList(), health.toList(), null, null)

    private fun event(
        id: String,
        residentId: String,
        time: String,
        type: EventType,
        severity: Severity,
        summary: String,
        status: EventStatus = EventStatus.OPEN,
        previousDay: Boolean = false,
    ): LumaEvent {
        val timestamp = (if (previousDay) day.minusDays(1) else day).with(LocalTime.parse(time))
        return LumaEvent(
            id = id,
            residentId = residentId,
            timestamp = timestamp,
            type = type,
            severity = severity,
            status = status,
            summary = summary,
            assignedTo = if (status == EventStatus.IN_PROGRESS) ShiftType.NIGHT else null,
            resolution = null,
            updatedAt = timestamp,
        )
    }

    private fun routine(id: String, residentId: String, title: String, time: String, lumaCall: Boolean) =
        Routine(id, residentId, title, LocalTime.parse(time), DayOfWeek.entries.toSet(), lumaCall)
}
