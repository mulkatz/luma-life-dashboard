package com.mulkatz.lumacare.data.repository

import androidx.room.withTransaction
import com.mulkatz.lumacare.data.local.ActivityEntity
import com.mulkatz.lumacare.data.local.EventActionEntity
import com.mulkatz.lumacare.data.local.LumaDatabase
import com.mulkatz.lumacare.data.local.LumaEventEntity
import com.mulkatz.lumacare.data.local.ResidentEntity
import com.mulkatz.lumacare.data.local.RoutineEntity
import com.mulkatz.lumacare.data.seed.SeedImporter
import com.mulkatz.lumacare.domain.Activity
import com.mulkatz.lumacare.domain.CareData
import com.mulkatz.lumacare.domain.EventActionRecord
import com.mulkatz.lumacare.domain.EventActionType
import com.mulkatz.lumacare.domain.EventCommand
import com.mulkatz.lumacare.domain.EventState
import com.mulkatz.lumacare.domain.EventTransitions
import com.mulkatz.lumacare.domain.LumaEvent
import com.mulkatz.lumacare.domain.Resident
import com.mulkatz.lumacare.domain.Routine
import com.mulkatz.lumacare.domain.ShiftType
import com.mulkatz.lumacare.domain.TransitionResult
import com.mulkatz.lumacare.domain.state
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

class RoomCareRepository(
    private val db: LumaDatabase,
    private val seedImporter: SeedImporter,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : CareRepository {

    override suspend fun ensureSeeded() {
        withContext(ioDispatcher) { seedImporter.importIfNeeded() }
    }

    override fun observeCareData(): Flow<CareData> = combine(
        db.residentDao().observeAll(),
        db.eventDao().observeAll(),
        db.routineDao().observeAll(),
        db.activityDao().observeAll(),
        db.eventActionDao().observeAll(),
    ) { residents, events, routines, activities, actions ->
        CareData(
            residents = residents.map { it.toDomain() },
            events = events.map { it.toDomain() },
            routines = routines.map { it.toDomain() },
            activities = activities.map { it.toDomain() },
            actions = actions.map { it.toDomain() },
        )
    }

    override suspend fun apply(
        eventId: String,
        expected: EventState,
        command: EventCommand,
        shift: ShiftType,
        now: LocalDateTime,
    ): ActionResult = db.withTransaction {
        val event = db.eventDao().getById(eventId)?.toDomain() ?: return@withTransaction ActionResult.Stale
        if (event.state != expected) return@withTransaction ActionResult.Stale

        when (val result = EventTransitions.apply(event, shift, command)) {
            is TransitionResult.Rejected -> ActionResult.Rejected(result.error)
            is TransitionResult.Allowed -> {
                if (result.action != EventActionType.NOTE) {
                    // Der Schutz vor doppeltem Tippen ist der Vergleich mit `expected` oben; die Bedingung
                    // im Update ist nur die zweite Sicherung.
                    val changed = db.eventDao().updateStatus(
                        id = eventId,
                        expected = event.status,
                        expectedAssignedTo = event.assignedTo,
                        new = result.newState.status,
                        assignedTo = result.newState.assignedTo,
                        resolution = result.newState.resolution,
                        updatedAt = now,
                    )
                    if (changed == 0) return@withTransaction ActionResult.Stale
                }
                val actionId = db.eventActionDao().insert(
                    EventActionEntity(
                        eventId = eventId,
                        action = result.action,
                        createdAt = now,
                        note = result.note,
                        forwardedTo = result.forwardedTo,
                        prevStatus = event.status,
                        prevAssignedTo = event.assignedTo,
                        prevResolution = event.resolution,
                    ),
                )
                ActionResult.Applied(actionId)
            }
        }
    }

    override suspend fun undo(actionId: Long, now: LocalDateTime): UndoResult = db.withTransaction {
        val action = db.eventActionDao().getById(actionId) ?: return@withTransaction UndoResult.Stale
        if (db.eventActionDao().latestForEvent(action.eventId)?.id != actionId) {
            return@withTransaction UndoResult.NotLatest
        }
        // Eine Notiz hat den Hinweis nicht verändert; es bleibt nur der Verlaufseintrag zu löschen.
        if (action.action != EventActionType.NOTE) {
            val event = db.eventDao().getById(action.eventId) ?: return@withTransaction UndoResult.Stale
            val changed = db.eventDao().updateStatus(
                id = action.eventId,
                expected = EventTransitions.statusAfter(action.action, action.prevStatus),
                // Der Schutz ist die Prüfung auf die letzte Aktion oben; die Bedingung (Status nach der
                // Aktion, aktuelle Zuordnung) ist nur die zweite Sicherung.
                expectedAssignedTo = event.assignedTo,
                new = action.prevStatus,
                assignedTo = action.prevAssignedTo,
                resolution = action.prevResolution,
                updatedAt = now,
            )
            if (changed == 0) return@withTransaction UndoResult.Stale
        }
        db.eventActionDao().deleteById(actionId)
        UndoResult.Undone
    }
}

private fun ResidentEntity.toDomain() = Resident(
    id = id,
    firstName = firstName,
    lastName = lastName,
    age = age,
    room = room,
    careLevel = careLevel,
    biography = biography,
    preferences = preferences,
    healthNotes = healthNotes,
    contactName = contactName,
    contactRelation = contactRelation,
)

private fun LumaEventEntity.toDomain() = LumaEvent(
    id = id,
    residentId = residentId,
    timestamp = timestamp,
    type = type,
    severity = severity,
    status = status,
    summary = summary,
    assignedTo = assignedTo,
    resolution = resolution,
    updatedAt = updatedAt,
)

private fun RoutineEntity.toDomain() = Routine(
    id = id,
    residentId = residentId,
    title = title,
    time = time,
    days = days,
    lumaCall = lumaCall,
)

private fun ActivityEntity.toDomain() = Activity(
    id = id,
    title = title,
    start = start,
    location = location,
    registered = registered,
)

private fun EventActionEntity.toDomain() = EventActionRecord(
    id = id,
    eventId = eventId,
    type = action,
    createdAt = createdAt,
    note = note,
    forwardedTo = forwardedTo,
    previous = EventState(prevStatus, prevAssignedTo, prevResolution),
)
