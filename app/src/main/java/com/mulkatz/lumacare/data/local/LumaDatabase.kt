package com.mulkatz.lumacare.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        ResidentEntity::class,
        LumaEventEntity::class,
        RoutineEntity::class,
        ActivityEntity::class,
        EventActionEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class LumaDatabase : RoomDatabase() {
    abstract fun residentDao(): ResidentDao
    abstract fun eventDao(): LumaEventDao
    abstract fun routineDao(): RoutineDao
    abstract fun activityDao(): ActivityDao
    abstract fun eventActionDao(): EventActionDao

    companion object {
        const val NAME = "luma.db"
    }
}
