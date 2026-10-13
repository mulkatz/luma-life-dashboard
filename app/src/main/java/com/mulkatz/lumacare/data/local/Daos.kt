package com.mulkatz.lumacare.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.mulkatz.lumacare.domain.EventStatus
import com.mulkatz.lumacare.domain.Resolution
import com.mulkatz.lumacare.domain.ShiftType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface ResidentDao {
    @Query("SELECT * FROM resident ORDER BY room, id")
    fun observeAll(): Flow<List<ResidentEntity>>

    @Insert
    suspend fun insertAll(residents: List<ResidentEntity>)

    @Query("DELETE FROM resident")
    suspend fun deleteAll()
}

@Dao
interface LumaEventDao {
    @Query("SELECT * FROM luma_event ORDER BY timestamp DESC, id")
    fun observeAll(): Flow<List<LumaEventEntity>>

    @Insert
    suspend fun insertAll(events: List<LumaEventEntity>)

    @Query("DELETE FROM luma_event")
    suspend fun deleteAll()

    /**
     * Bedingtes Update: greift nur, wenn der Status noch [expected] ist.
     * Rückgabe: Anzahl geänderter Zeilen (0 = veralteter Zustand).
     */
    @Query(
        """
        UPDATE luma_event
        SET status = :new, assigned_to = :assignedTo, resolution = :resolution, updated_at = :updatedAt
        WHERE id = :id AND status = :expected
        """,
    )
    suspend fun updateStatus(
        id: String,
        expected: EventStatus,
        new: EventStatus,
        assignedTo: ShiftType?,
        resolution: Resolution?,
        updatedAt: LocalDateTime,
    ): Int
}

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routine ORDER BY time, id")
    fun observeAll(): Flow<List<RoutineEntity>>

    @Insert
    suspend fun insertAll(routines: List<RoutineEntity>)

    @Query("DELETE FROM routine")
    suspend fun deleteAll()
}

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activity ORDER BY start, id")
    fun observeAll(): Flow<List<ActivityEntity>>

    @Insert
    suspend fun insertAll(activities: List<ActivityEntity>)

    @Query("DELETE FROM activity")
    suspend fun deleteAll()
}

@Dao
interface EventActionDao {
    @Query("SELECT * FROM event_action ORDER BY created_at, id")
    fun observeAll(): Flow<List<EventActionEntity>>

    @Query("DELETE FROM event_action")
    suspend fun deleteAll()
}
