package com.mulkatz.lumacare.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mulkatz.lumacare.domain.EventActionType
import com.mulkatz.lumacare.domain.EventStatus
import com.mulkatz.lumacare.domain.EventType
import com.mulkatz.lumacare.domain.Resolution
import com.mulkatz.lumacare.domain.Severity
import com.mulkatz.lumacare.domain.ShiftType
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

@Entity(tableName = "resident")
data class ResidentEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "first_name") val firstName: String,
    @ColumnInfo(name = "last_name") val lastName: String,
    val age: Int,
    val room: String,
    @ColumnInfo(name = "care_level") val careLevel: Int,
    val biography: List<String>,
    val preferences: List<String>,
    @ColumnInfo(name = "health_notes") val healthNotes: List<String>,
    @ColumnInfo(name = "contact_name") val contactName: String?,
    @ColumnInfo(name = "contact_relation") val contactRelation: String?,
)

@Entity(
    tableName = "luma_event",
    foreignKeys = [
        ForeignKey(entity = ResidentEntity::class, parentColumns = ["id"], childColumns = ["resident_id"]),
    ],
    indices = [Index("resident_id")],
)
data class LumaEventEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "resident_id") val residentId: String,
    val timestamp: LocalDateTime,
    val type: EventType,
    val severity: Severity,
    val status: EventStatus,
    val summary: String,
    @ColumnInfo(name = "assigned_to") val assignedTo: ShiftType?,
    val resolution: Resolution?,
    @ColumnInfo(name = "updated_at") val updatedAt: LocalDateTime,
)

@Entity(tableName = "routine")
data class RoutineEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "resident_id") val residentId: String,
    val title: String,
    val time: LocalTime,
    val days: Set<DayOfWeek>,
    @ColumnInfo(name = "luma_call") val lumaCall: Boolean,
)

@Entity(tableName = "activity")
data class ActivityEntity(
    @PrimaryKey val id: String,
    val title: String,
    val start: LocalDateTime,
    val location: String,
    /** IDs der angemeldeten Bewohnenden. */
    val registered: List<String>,
)

/** Verlaufseintrag mit dem Zustand vor der Aktion (Grundlage für Rückgängig). */
@Entity(
    tableName = "event_action",
    foreignKeys = [
        ForeignKey(entity = LumaEventEntity::class, parentColumns = ["id"], childColumns = ["event_id"]),
    ],
    indices = [Index("event_id")],
)
data class EventActionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "event_id") val eventId: String,
    val action: EventActionType,
    @ColumnInfo(name = "created_at") val createdAt: LocalDateTime,
    val note: String?,
    @ColumnInfo(name = "prev_status") val prevStatus: EventStatus,
    @ColumnInfo(name = "prev_assigned_to") val prevAssignedTo: ShiftType?,
    @ColumnInfo(name = "prev_resolution") val prevResolution: Resolution?,
)
