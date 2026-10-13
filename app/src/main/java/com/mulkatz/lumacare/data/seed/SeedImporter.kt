package com.mulkatz.lumacare.data.seed

import androidx.room.withTransaction
import com.mulkatz.lumacare.data.local.ActivityEntity
import com.mulkatz.lumacare.data.local.LumaDatabase
import com.mulkatz.lumacare.data.local.LumaEventEntity
import com.mulkatz.lumacare.data.local.ResidentEntity
import com.mulkatz.lumacare.data.local.RoutineEntity
import com.mulkatz.lumacare.domain.AppStateStore
import com.mulkatz.lumacare.domain.EventStatus
import com.mulkatz.lumacare.domain.EventType
import com.mulkatz.lumacare.domain.Severity
import com.mulkatz.lumacare.domain.ShiftType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.DateTimeException
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Importiert den Seed in die Datenbank.
 *
 * - Ist [SEED_VERSION] bereits gespeichert, passiert nichts (Änderungen der Pflegekraft bleiben erhalten).
 * - Sonst: in einer Transaktion alle Tabellen leeren und alles einfügen.
 * - `seed_version` wird erst nach dem Commit geschrieben; bei einem Fehler bleibt sie ungesetzt.
 */
class SeedImporter(
    private val db: LumaDatabase,
    private val store: AppStateStore,
    private val readSeed: () -> String,
) {
    private val mutex = Mutex()

    /**
     * Prüfen, Import und Setzen der Version laufen unter einem Lock, damit gleichzeitige Aufrufe nicht doppelt importieren.
     * @return `true`, wenn importiert wurde, `false`, wenn übersprungen.
     */
    suspend fun importIfNeeded(): Boolean = mutex.withLock {
        if (store.seedVersion == SEED_VERSION) return false

        val seed = SeedParser.parse(readSeed())
        val data = map(seed)

        db.withTransaction {
            db.eventActionDao().deleteAll()
            db.eventDao().deleteAll()
            db.routineDao().deleteAll()
            db.activityDao().deleteAll()
            db.residentDao().deleteAll()

            db.residentDao().insertAll(data.residents)
            db.eventDao().insertAll(data.events)
            db.routineDao().insertAll(data.routines)
            db.activityDao().insertAll(data.activities)
        }
        store.seedVersion = SEED_VERSION
        true
    }

    private class MappedSeed(
        val residents: List<ResidentEntity>,
        val events: List<LumaEventEntity>,
        val routines: List<RoutineEntity>,
        val activities: List<ActivityEntity>,
    )

    private fun map(seed: SeedFile): MappedSeed {
        val shiftStart = parseDateTime(seed.meta.shift.start, "meta.shift.start")
        mapShiftType(seed.meta.shift.type) // validiert den Schichttyp, auch wenn er hier nicht gebraucht wird
        return MappedSeed(
            residents = seed.residents.map(::mapResident),
            events = seed.lumaEvents.map { mapEvent(it, shiftStart) },
            routines = seed.routines.map(::mapRoutine),
            activities = seed.activities.map(::mapActivity),
        )
    }

    private fun mapResident(r: SeedResident): ResidentEntity {
        // Gesundheitsangaben stehen im Seed unter preferences; sie wandern unverändert nach health_notes.
        val (healthNotes, preferences) = r.preferences.partition { pref ->
            HEALTH_NOTE_PREFIXES.any { pref.startsWith(it) }
        }
        return ResidentEntity(
            id = r.id,
            firstName = r.firstName,
            lastName = r.lastName,
            age = r.age,
            room = r.room,
            careLevel = r.careLevel,
            biography = r.biography,
            preferences = preferences,
            healthNotes = healthNotes,
            contactName = r.contact?.name,
            contactRelation = r.contact?.relation,
        )
    }

    private fun mapEvent(e: SeedEvent, shiftStart: LocalDateTime): LumaEventEntity {
        val timestamp = parseDateTime(e.timestamp, "Event ${e.id}")
        val status = mapStatus(e.status, e.id)
        // In Bearbeitung vor Schichtbeginn = von der Vorschicht (Nacht) übernommen. Danach sagen die
        // Daten nicht, wer es übernommen hat; das wird nicht geraten.
        val assignedTo = when {
            status != EventStatus.IN_PROGRESS -> null
            timestamp.isBefore(shiftStart) -> ShiftType.NIGHT
            else -> throw SeedFormatException(
                "Event ${e.id}: in_progress nach Schichtbeginn: Zuständigkeit unbekannt",
            )
        }
        return LumaEventEntity(
            id = e.id,
            residentId = e.residentId,
            timestamp = timestamp,
            type = mapEventType(e.type, e.id),
            severity = mapSeverity(e.severity, e.id),
            status = status,
            summary = e.summary,
            assignedTo = assignedTo,
            resolution = null,
            updatedAt = timestamp,
        )
    }

    private fun mapRoutine(r: SeedRoutine): RoutineEntity = RoutineEntity(
        id = r.id,
        residentId = r.residentId,
        title = r.title,
        time = try {
            LocalTime.parse(r.time)
        } catch (e: DateTimeException) {
            throw SeedFormatException("Ungültige Uhrzeit '${r.time}' in Routine ${r.id}", e)
        },
        days = r.days.mapTo(mutableSetOf()) { mapDay(it, r.id) },
        lumaCall = r.lumaCall,
    )

    private fun mapActivity(a: SeedActivity): ActivityEntity = ActivityEntity(
        id = a.id,
        title = a.title,
        start = parseDateTime(a.start, "Aktivität ${a.id}"),
        location = a.location,
        registered = a.registered,
    )

    companion object {
        /** Bei inhaltlicher Änderung des Seeds oder des Mappings erhöhen. */
        const val SEED_VERSION = 1

        /** Präferenzen, die mit einem dieser Begriffe beginnen, sind Gesundheitsangaben. */
        val HEALTH_NOTE_PREFIXES = listOf("Diabetes Typ 2")

        fun mapShiftType(value: String): ShiftType = when (value) {
            "early" -> ShiftType.EARLY
            "night" -> ShiftType.NIGHT
            else -> throw SeedFormatException("Unbekannter Schichttyp '$value'")
        }

        fun parseDateTime(value: String, where: String): LocalDateTime =
            try {
                LocalDateTime.parse(value)
            } catch (e: DateTimeException) {
                throw SeedFormatException("Ungültiger Zeitpunkt '$value' in $where", e)
            }

        private fun mapEventType(value: String, id: String): EventType = when (value) {
            "health_hint" -> EventType.HEALTH_HINT
            "mood" -> EventType.MOOD
            "request" -> EventType.REQUEST
            "meal_wish" -> EventType.MEAL_WISH
            "activity_signup" -> EventType.ACTIVITY_SIGNUP
            "family_message" -> EventType.FAMILY_MESSAGE
            "missed_call" -> EventType.MISSED_CALL
            else -> throw SeedFormatException("Unbekannter Event-Typ '$value' in Event $id")
        }

        private fun mapSeverity(value: String, id: String): Severity = when (value) {
            "urgent" -> Severity.URGENT
            "attention" -> Severity.ATTENTION
            "info" -> Severity.INFO
            else -> throw SeedFormatException("Unbekannte Dringlichkeit '$value' in Event $id")
        }

        private fun mapStatus(value: String, id: String): EventStatus = when (value) {
            "open" -> EventStatus.OPEN
            "in_progress" -> EventStatus.IN_PROGRESS
            "done" -> EventStatus.DONE
            else -> throw SeedFormatException("Unbekannter Status '$value' in Event $id")
        }

        private fun mapDay(value: String, id: String): DayOfWeek = when (value) {
            "mon" -> DayOfWeek.MONDAY
            "tue" -> DayOfWeek.TUESDAY
            "wed" -> DayOfWeek.WEDNESDAY
            "thu" -> DayOfWeek.THURSDAY
            "fri" -> DayOfWeek.FRIDAY
            "sat" -> DayOfWeek.SATURDAY
            "sun" -> DayOfWeek.SUNDAY
            else -> throw SeedFormatException("Unbekannter Wochentag '$value' in Routine $id")
        }
    }
}
