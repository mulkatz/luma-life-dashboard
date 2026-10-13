package com.mulkatz.lumacare.data.repository

import com.mulkatz.lumacare.data.local.ActivityEntity
import com.mulkatz.lumacare.data.local.LumaDatabase
import com.mulkatz.lumacare.data.local.LumaEventEntity
import com.mulkatz.lumacare.data.local.ResidentEntity
import com.mulkatz.lumacare.data.local.RoutineEntity
import com.mulkatz.lumacare.data.seed.SeedImporter
import com.mulkatz.lumacare.domain.Activity
import com.mulkatz.lumacare.domain.CareData
import com.mulkatz.lumacare.domain.LumaEvent
import com.mulkatz.lumacare.domain.Resident
import com.mulkatz.lumacare.domain.Routine
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

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
    ) { residents, events, routines, activities ->
        CareData(
            residents = residents.map { it.toDomain() },
            events = events.map { it.toDomain() },
            routines = routines.map { it.toDomain() },
            activities = activities.map { it.toDomain() },
        )
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
