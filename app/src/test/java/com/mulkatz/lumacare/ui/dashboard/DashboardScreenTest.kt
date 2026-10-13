package com.mulkatz.lumacare.ui.dashboard

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mulkatz.lumacare.data.local.LumaDatabase
import com.mulkatz.lumacare.data.repository.ActionResult
import com.mulkatz.lumacare.data.repository.CareRepository
import com.mulkatz.lumacare.data.repository.RoomCareRepository
import com.mulkatz.lumacare.data.repository.UndoResult
import com.mulkatz.lumacare.data.seed.SeedImporter
import com.mulkatz.lumacare.data.seed.SeedParser
import com.mulkatz.lumacare.domain.CareData
import com.mulkatz.lumacare.domain.EventCommand
import com.mulkatz.lumacare.domain.EventState
import com.mulkatz.lumacare.domain.ShiftClock
import com.mulkatz.lumacare.domain.ShiftType
import com.mulkatz.lumacare.testing.InMemoryAppStateStore
import com.mulkatz.lumacare.testing.inMemoryDatabase
import com.mulkatz.lumacare.testing.realSeedJson
import com.mulkatz.lumacare.ui.theme.LumaTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Dashboard auf dem Phone: In-Memory-DB, echter Seed, feste Uhr Mo 12.10.2026, 07:15. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class DashboardScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private companion object {
        // Robolectric rechnet ohne Systemleisten (Insets 0). Auf einem Gerät liegen Statusleiste oben und
        // Navigationsleiste unten im selben 891-dp-Fenster; beide großzügig mit 48 dp angesetzt.
        val STATUS_BAR = 48.dp
        val NAV_BAR = 48.dp
    }

    private lateinit var db: LumaDatabase
    private val store = ViewModelStore()
    private val seed = realSeedJson()
    private val shift = SeedImporter.mapShift(SeedParser.parse(seed).meta)
    private val clock = ShiftClock(
        seedNow = LocalDateTime.parse("2026-10-12T07:15:00"),
        shift = shift,
        anchorStore = InMemoryAppStateStore(),
        currentTimeMillis = { 0L },
    )

    @Before
    fun setUp() {
        db = inMemoryDatabase()
    }

    @After
    fun tearDown() {
        store.clear()
        db.close()
    }

    private fun seededRepository(): CareRepository = RoomCareRepository(db, SeedImporter(db, InMemoryAppStateStore()) { seed })

    private val clicked = mutableListOf<String>()

    private fun show(repository: CareRepository = seededRepository()) {
        val factory = viewModelFactory { initializer { DashboardViewModel(repository, loadClock = { clock }) } }
        val viewModel = ViewModelProvider(store, factory)[DashboardViewModel::class.java]
        compose.setContent {
            LumaTheme(darkTheme = false) { DashboardRoute(onPersonClick = { clicked += it }, onTimelineClick = {}, viewModel = viewModel) }
        }
    }

    private fun awaitTag(tag: String) {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun cardInSection(card: String, section: String): SemanticsNodeInteraction =
        compose.onNode(hasTestTag(card) and hasAnyAncestor(hasTestTag(section)))

    @Test
    fun `jetzt handeln zeigt werner hoffmann vor rudolf schmidt`() {
        show()
        awaitTag("person_card_r04")

        val werner = cardInSection("person_card_r04", "section_now").apply { assertExists() }
        val rudolf = cardInSection("person_card_r12", "section_now").apply { assertExists() }
        assertTrue(werner.getUnclippedBoundsInRoot().top < rudolf.getUnclippedBoundsInRoot().top)
    }

    @Test
    fun `rudolfs karte zeigt übernahme bestätigen`() {
        show()
        awaitTag("person_card_r12")

        compose.onNode(
            hasTestTag("action_confirm_handover") and hasText("Übernahme bestätigen") and
                hasAnyAncestor(hasTestTag("person_card_r12")),
        ).assertExists()
    }

    @Test
    fun `beide dringenden karten liegen ohne scrollen vollständig im sichtbaren bereich`() {
        show()
        awaitTag("person_card_r12")

        val rootHeight = compose.onRoot().getUnclippedBoundsInRoot().bottom
        listOf("person_card_r04", "person_card_r12").forEach { tag ->
            val bounds = compose.onNodeWithTag(tag).getUnclippedBoundsInRoot()
            assertTrue(bounds.top >= 0.dp, "$tag beginnt oberhalb des Bildschirms")
            assertTrue(
                bounds.bottom + STATUS_BAR <= rootHeight - NAV_BAR,
                "$tag endet bei ${bounds.bottom}, sichtbar bis ${rootHeight - NAV_BAR - STATUS_BAR}",
            )
        }
    }

    @Test
    fun `karte, aktion und offener hinweis unter demnächst öffnen die person`() {
        show()
        awaitTag("person_card_r04")

        compose.onNodeWithTag("person_card_r04").performClick()
        compose.onNode(hasTestTag("action_confirm_handover") and hasAnyAncestor(hasTestTag("person_card_r12"))).performClick()
        compose.onNodeWithTag("open_hint_r09").performScrollTo().performClick()

        assertEquals(listOf("r04", "r12", "r09"), clicked)
    }

    @Test
    fun `kopfbereich zeigt wohnbereich, uhrzeit und restzeit, talkback in worten`() {
        show()
        awaitTag("shift_header")

        compose.onNodeWithText("Ahorn · Frühdienst").assertExists()
        compose.onNodeWithText("07:15 · noch 6:45 h").assertExists()
        compose.onNode(hasContentDescription("Ahorn · Frühdienst, 07:15, noch 6 Stunden 45 Minuten")).assertExists()
    }

    @Test
    fun `während des ladens erscheint nie der leere zustand`() {
        val gate = CompletableDeferred<Unit>()
        show(GatedRepository(seededRepository(), gate))
        awaitTag("loading")

        compose.onNodeWithTag("empty_state").assertDoesNotExist()

        gate.complete(Unit)
        awaitTag("person_card_r04")
        compose.onNodeWithTag("empty_state").assertDoesNotExist()
        compose.onNodeWithTag("loading").assertDoesNotExist()
    }

    @Test
    fun `ohne offene hinweise erscheint nach dem laden alles erledigt`() {
        show(EmptyRepository())
        awaitTag("empty_state")

        compose.onNodeWithText("Alles erledigt").assertExists()
        compose.onNodeWithTag("section_now").assertDoesNotExist()
    }

    @Test
    fun `im leeren zustand steht der schichtverlauf unter alles erledigt`() {
        show(EmptyRepository())
        awaitTag("empty_state")

        val empty = compose.onNodeWithTag("empty_state").getUnclippedBoundsInRoot()
        val timeline = compose.onNodeWithTag("shift_timeline").getUnclippedBoundsInRoot()
        assertTrue(empty.bottom <= timeline.top, "Schichtverlauf beginnt bei ${timeline.top}, leerer Zustand endet bei ${empty.bottom}")
    }

    @Test
    fun `fehlerzustand bietet erneut versuchen und lädt danach`() {
        show(FlakyRepository(seededRepository()))
        awaitTag("error_state")
        compose.onNodeWithText("Daten konnten nicht geladen werden").assertExists()

        compose.onNodeWithTag("action_retry").performClick()

        awaitTag("person_card_r04")
        compose.onNodeWithTag("error_state").assertDoesNotExist()
    }

    /** Hält den Import an, bis [gate] freigegeben wird. */
    private class GatedRepository(
        private val delegate: CareRepository,
        private val gate: CompletableDeferred<Unit>,
    ) : CareRepository by delegate {
        override suspend fun ensureSeeded() {
            gate.await()
            delegate.ensureSeeded()
        }

        override fun observeCareData(): Flow<CareData> = delegate.observeCareData()
    }

    /** Erster Import wirft, danach arbeitet es wie [delegate]. */
    private class FlakyRepository(private val delegate: CareRepository) : CareRepository by delegate {
        private var attempts = 0

        override suspend fun ensureSeeded() {
            attempts++
            if (attempts == 1) throw IOException("Seed nicht lesbar")
            delegate.ensureSeeded()
        }

        override fun observeCareData(): Flow<CareData> = delegate.observeCareData()
    }

    private class EmptyRepository : CareRepository {
        override suspend fun ensureSeeded() = Unit

        override fun observeCareData(): Flow<CareData> = flowOf(CareData(emptyList(), emptyList(), emptyList(), emptyList()))

        override suspend fun apply(
            eventId: String,
            expected: EventState,
            command: EventCommand,
            shift: ShiftType,
            now: LocalDateTime,
        ): ActionResult = error("nicht benutzt")

        override suspend fun undo(actionId: Long, now: LocalDateTime): UndoResult = error("nicht benutzt")
    }
}
