package com.mulkatz.lumacare.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mulkatz.lumacare.domain.EventActionType
import com.mulkatz.lumacare.domain.EventStatus
import com.mulkatz.lumacare.domain.ForwardTarget
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.time.LocalDateTime
import kotlin.test.assertEquals

/**
 * Migration 1 → 2 ohne `room-testing` (kein `MigrationTestHelper` im Katalog): Die Datenbank wird mit
 * dem exportierten Schema 1 angelegt, danach öffnet Room sie als Version 2. Room prüft dabei das Schema
 * gegen die Entities und wirft bei Abweichung.
 */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val dbName = "migration-test.db"
    private var room: LumaDatabase? = null

    @After
    fun tearDown() {
        room?.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun `1 nach 2 behält den verlauf und ergänzt forwarded_to`() = runTest {
        createVersion1 { db ->
            db.execSQL(
                "INSERT INTO resident VALUES ('r04', 'Test', 'Person', 80, '9.99', 2, '[]', '[]', '[]', NULL, NULL)",
            )
            db.execSQL(
                "INSERT INTO luma_event VALUES ('e005', 'r04', '2026-10-12T06:30:00', 'HEALTH_HINT', 'URGENT', " +
                    "'IN_PROGRESS', 'Fiktiver Testhinweis.', 'EARLY', NULL, '2026-10-12T07:20:00')",
            )
            db.execSQL(
                "INSERT INTO event_action (event_id, action, created_at, note, prev_status, prev_assigned_to, prev_resolution) " +
                    "VALUES ('e005', 'ASSIGN', '2026-10-12T07:20:00', NULL, 'OPEN', NULL, NULL)",
            )
        }

        val migrated = Room.databaseBuilder(context, LumaDatabase::class.java, dbName)
            .allowMainThreadQueries()
            .build()
            .also { room = it }

        val old = migrated.eventActionDao().observeAll().first().single()
        assertEquals(EventActionType.ASSIGN, old.action)
        assertEquals(EventStatus.OPEN, old.prevStatus)
        assertEquals(LocalDateTime.parse("2026-10-12T07:20:00"), old.createdAt)
        assertEquals(null, old.forwardedTo)

        val newId = migrated.eventActionDao().insert(
            old.copy(id = 0, action = EventActionType.RESOLVE, forwardedTo = ForwardTarget.KITCHEN),
        )
        assertEquals(ForwardTarget.KITCHEN, migrated.eventActionDao().getById(newId)?.forwardedTo)
    }

    /** Legt die Datenbank exakt nach `schemas/…/1.json` an (inkl. Identity-Hash) und befüllt sie. */
    private fun createVersion1(fill: (SQLiteDatabase) -> Unit) {
        val schema = Json.parseToJsonElement(File(SCHEMA_1).readText()).jsonObject.getValue("database").jsonObject
        val file = context.getDatabasePath(dbName).also { it.parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            for (entity in schema.getValue("entities").jsonArray.map { it.jsonObject }) {
                val table = entity.getValue("tableName").jsonPrimitive.content
                val statements = listOf(entity.getValue("createSql")) + entity["indices"]?.jsonArray.orEmpty()
                    .map { it.jsonObject.getValue("createSql") }
                statements.forEach { db.execSQL(it.jsonPrimitive.content.replace("\${TABLE_NAME}", table)) }
            }
            schema.getValue("setupQueries").jsonArray.forEach { db.execSQL(it.jsonPrimitive.content) }
            fill(db)
            db.version = 1
        }
    }

    private companion object {
        /** Gradle führt Unit-Tests im Modulverzeichnis aus. */
        const val SCHEMA_1 = "schemas/com.mulkatz.lumacare.data.local.LumaDatabase/1.json"
    }
}
