package com.mulkatz.lumacare.data.seed

import android.database.sqlite.SQLiteConstraintException
import com.mulkatz.lumacare.data.local.ActivityEntity
import com.mulkatz.lumacare.data.local.LumaDatabase
import com.mulkatz.lumacare.data.local.LumaEventEntity
import com.mulkatz.lumacare.data.local.ResidentEntity
import com.mulkatz.lumacare.data.local.RoutineEntity
import com.mulkatz.lumacare.domain.EventStatus
import com.mulkatz.lumacare.domain.ShiftType
import com.mulkatz.lumacare.testing.InMemoryAppStateStore
import com.mulkatz.lumacare.testing.inMemoryDatabase
import com.mulkatz.lumacare.testing.realSeedJson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.DayOfWeek
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class SeedImporterTest {
    private lateinit var db: LumaDatabase
    private val store = InMemoryAppStateStore()
    private val seed = realSeedJson()

    @Before
    fun setUp() {
        db = inMemoryDatabase()
    }

    @After
    fun tearDown() = db.close()

    private fun importer(json: String = seed) = SeedImporter(db, store) { json }

    private data class Snapshot(
        val residents: List<ResidentEntity>,
        val events: List<LumaEventEntity>,
        val routines: List<RoutineEntity>,
        val activities: List<ActivityEntity>,
    )

    private suspend fun snapshot() = Snapshot(
        db.residentDao().observeAll().first(),
        db.eventDao().observeAll().first(),
        db.routineDao().observeAll().first(),
        db.activityDao().observeAll().first(),
    )

    private fun Snapshot.isEmpty() =
        residents.isEmpty() && events.isEmpty() && routines.isEmpty() && activities.isEmpty()

    @Test
    fun `import übernimmt alle datensätze und setzt die seed-version`() = runTest {
        assertTrue(importer().importIfNeeded())

        val state = snapshot()
        assertEquals(15, state.residents.size)
        assertEquals(26, state.events.size)
        assertEquals(7, state.routines.size)
        assertEquals(4, state.activities.size)
        assertEquals(SeedImporter.SEED_VERSION, store.seedVersion)
    }

    @Test
    fun `diabetes-angabe wandert vollständig von preferences nach health_notes`() = runTest {
        importer().importIfNeeded()

        val werner = snapshot().residents.single { it.id == "r04" }
        assertEquals(listOf("Diabetes Typ 2, achtet auf Zucker"), werner.healthNotes)
        assertEquals(listOf("Fußball im Radio", "Schwarzer Kaffee"), werner.preferences)
    }

    @Test
    fun `vorschicht-vorgänge gehören der nacht und offene niemandem`() = runTest {
        importer().importIfNeeded()

        val events = snapshot().events.associateBy { it.id }
        assertEquals(ShiftType.NIGHT, events.getValue("e018").assignedTo)
        assertEquals(ShiftType.NIGHT, events.getValue("e006").assignedTo)
        val open = events.values.filter { it.status == EventStatus.OPEN }
        assertTrue(open.isNotEmpty())
        assertTrue(open.all { it.assignedTo == null })
        assertTrue(events.values.all { it.resolution == null && it.updatedAt == it.timestamp })
    }

    @Test
    fun `wochentage werden auf DayOfWeek abgebildet`() = runTest {
        importer().importIfNeeded()

        val routines = snapshot().routines.associateBy { it.id }
        assertEquals(setOf(DayOfWeek.SUNDAY), routines.getValue("rt04").days)
        assertEquals(DayOfWeek.entries.toSet(), routines.getValue("rt01").days)
    }

    @Test
    fun `zweimal importieren ergibt denselben zustand`() = runTest {
        importer().importIfNeeded()
        val first = snapshot()

        assertFalse(importer().importIfNeeded())
        assertEquals(first, snapshot())

        // erzwungener Neuimport (Version fehlt) leert vorher und ergibt ebenfalls denselben Zustand
        store.seedVersion = null
        assertTrue(importer().importIfNeeded())
        assertEquals(first, snapshot())
    }

    @Test
    fun `erneuter aufruf überschreibt keine statusänderung`() = runTest {
        importer().importIfNeeded()
        val changed = db.eventDao().updateStatus(
            id = "e005",
            expected = EventStatus.OPEN,
            expectedAssignedTo = null,
            new = EventStatus.IN_PROGRESS,
            assignedTo = ShiftType.EARLY,
            resolution = null,
            updatedAt = LocalDateTime.parse("2026-10-12T07:20:00"),
        )
        assertEquals(1, changed)

        assertFalse(importer().importIfNeeded())

        val e005 = snapshot().events.single { it.id == "e005" }
        assertEquals(EventStatus.IN_PROGRESS, e005.status)
        assertEquals(ShiftType.EARLY, e005.assignedTo)
    }

    @Test
    fun `ungültiges json bricht ab ohne daten und ohne seed-version`() = runTest {
        assertFailsWith<SeedFormatException> { importer("{ kein json").importIfNeeded() }

        assertTrue(snapshot().isEmpty())
        assertNull(store.seedVersion)
    }

    @Test
    fun `unbekannter event-typ bricht ab ohne daten und ohne seed-version`() = runTest {
        val broken = seed.replaceFirst(""""type": "mood"""", """"type": "dance"""")
        check(broken != seed)

        val error = assertFailsWith<SeedFormatException> { importer(broken).importIfNeeded() }

        assertTrue(error.message!!.contains("dance"))
        assertTrue(snapshot().isEmpty())
        assertNull(store.seedVersion)
    }

    @Test
    fun `in bearbeitung nach schichtbeginn bricht ab, weil die zuständigkeit unbekannt ist`() = runTest {
        val broken = seed.replaceFirst(
            Regex(""""timestamp": "2026-10-12T02:10:00",(\s*"type": "[a-z_]+",\s*"severity": "[a-z]+",\s*"status": "in_progress")"""),
            """"timestamp": "2026-10-12T06:10:00",$1""",
        )
        check(broken != seed)

        val error = assertFailsWith<SeedFormatException> { importer(broken).importIfNeeded() }

        assertTrue(error.message!!.contains("Zuständigkeit unbekannt"))
        assertTrue(snapshot().isEmpty())
        assertNull(store.seedVersion)
    }

    /** Bewohnende werden zuerst eingefügt, ein Event mit unbekannter Person verletzt danach den Fremdschlüssel. */
    private fun seedWithForeignKeyViolation(): String =
        seed.replaceFirst(""""resident_id": "r""", """"resident_id": "x""").also { check(it != seed) }

    @Test
    fun `fehler mitten in der transaktion rollt alles zurück`() = runTest {
        assertFailsWith<SQLiteConstraintException> { importer(seedWithForeignKeyViolation()).importIfNeeded() }

        assertTrue(snapshot().isEmpty())
        assertNull(store.seedVersion)
    }

    @Test
    fun `gescheiterter neuimport lässt den vorherigen bestand vollständig stehen`() = runTest {
        importer().importIfNeeded()
        db.eventDao().updateStatus(
            id = "e005",
            expected = EventStatus.OPEN,
            expectedAssignedTo = null,
            new = EventStatus.IN_PROGRESS,
            assignedTo = ShiftType.EARLY,
            resolution = null,
            updatedAt = LocalDateTime.parse("2026-10-12T07:20:00"),
        )
        val before = snapshot()
        store.seedVersion = null

        assertFailsWith<SQLiteConstraintException> { importer(seedWithForeignKeyViolation()).importIfNeeded() }

        // Leeren und Einfügen liefen in derselben Transaktion: alles zurückgerollt, inkl. Statusänderung
        val after = snapshot()
        assertEquals(before, after)
        assertEquals(15, after.residents.size)
        assertEquals(26, after.events.size)
        assertEquals(EventStatus.IN_PROGRESS, after.events.single { it.id == "e005" }.status)
        assertNull(store.seedVersion)
    }
}
