package com.mulkatz.lumacare.data.local

import com.mulkatz.lumacare.domain.EventStatus
import com.mulkatz.lumacare.domain.EventType
import com.mulkatz.lumacare.domain.Severity
import com.mulkatz.lumacare.domain.ShiftType
import com.mulkatz.lumacare.testing.inMemoryDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDateTime
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class ConditionalUpdateTest {
    private lateinit var db: LumaDatabase
    private val created = LocalDateTime.parse("2026-10-12T06:30:00")
    private val takenOver = LocalDateTime.parse("2026-10-12T07:15:00")

    @Before
    fun setUp() = runTest {
        db = inMemoryDatabase()
        db.residentDao().insertAll(listOf(resident("r04")))
        db.eventDao().insertAll(listOf(event("e005", residentId = "r04")))
    }

    @After
    fun tearDown() = db.close()

    private suspend fun takeOver() = db.eventDao().updateStatus(
        id = "e005",
        expected = EventStatus.OPEN,
        new = EventStatus.IN_PROGRESS,
        assignedTo = ShiftType.EARLY,
        resolution = null,
        updatedAt = takenOver,
    )

    @Test
    fun `bedingtes update greift nur bei erwartetem status`() = runTest {
        assertEquals(1, takeOver())
        val afterFirst = db.eventDao().observeAll().first().single()
        assertEquals(EventStatus.IN_PROGRESS, afterFirst.status)
        assertEquals(ShiftType.EARLY, afterFirst.assignedTo)
        assertEquals(takenOver, afterFirst.updatedAt)

        assertEquals(0, takeOver())
        assertEquals(afterFirst, db.eventDao().observeAll().first().single())
    }

    private fun resident(id: String) = ResidentEntity(
        id = id,
        firstName = "Test",
        lastName = "Person",
        age = 80,
        room = "9.99",
        careLevel = 2,
        biography = emptyList(),
        preferences = emptyList(),
        healthNotes = emptyList(),
        contactName = null,
        contactRelation = null,
    )

    private fun event(id: String, residentId: String) = LumaEventEntity(
        id = id,
        residentId = residentId,
        timestamp = created,
        type = EventType.HEALTH_HINT,
        severity = Severity.URGENT,
        status = EventStatus.OPEN,
        summary = "Fiktiver Testhinweis.",
        assignedTo = null,
        resolution = null,
        updatedAt = created,
    )
}
