package com.mulkatz.lumacare.ui.history

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToKeyAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToKey
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mulkatz.lumacare.data.local.LumaDatabase
import com.mulkatz.lumacare.data.repository.RoomCareRepository
import com.mulkatz.lumacare.data.seed.SeedImporter
import com.mulkatz.lumacare.data.seed.SeedParser
import com.mulkatz.lumacare.domain.ShiftClock
import com.mulkatz.lumacare.testing.InMemoryAppStateStore
import com.mulkatz.lumacare.testing.inMemoryDatabase
import com.mulkatz.lumacare.testing.realSeedJson
import com.mulkatz.lumacare.ui.LumaApp
import com.mulkatz.lumacare.ui.dashboard.DashboardViewModel
import com.mulkatz.lumacare.ui.theme.LumaTheme
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime
import kotlin.test.assertTrue

/** Karte „Schichtverlauf“ und Screen „Schichtverlauf“ auf dem Phone: echter Seed, feste Uhr Mo 12.10.2026, 07:15. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class ShiftHistoryFlowTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var db: LumaDatabase
    private val store = ViewModelStore()
    private val seed = realSeedJson()
    private val clock = ShiftClock(
        seedNow = LocalDateTime.parse("2026-10-12T07:15:00"),
        shift = SeedImporter.mapShift(SeedParser.parse(seed).meta),
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

    private fun viewModel(): DashboardViewModel {
        val repository = RoomCareRepository(db, SeedImporter(db, InMemoryAppStateStore()) { seed })
        val factory = viewModelFactory { initializer { DashboardViewModel(repository, loadClock = { clock }) } }
        return ViewModelProvider(store, factory)[DashboardViewModel::class.java]
    }

    private fun show() {
        val viewModel = viewModel()
        compose.setContent { LumaTheme(darkTheme = false) { LumaApp(viewModel = viewModel) } }
        awaitTag("person_card_r04")
    }

    private fun awaitTag(tag: String) {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Scrollt die (einzige) Lazy-Liste des sichtbaren Screens zum Eintrag mit [key]. */
    private fun scrollToKey(key: String) {
        compose.onNode(hasScrollToKeyAction()).performScrollToKey(key)
    }

    private fun openHistory() {
        scrollToKey("shift_timeline")
        compose.onNodeWithTag("shift_timeline").performClick()
        awaitTag("shift_history")
    }

    private fun entryInGroup(entry: String, group: String): SemanticsNodeInteraction =
        compose.onNode(hasTestTag(entry) and hasAnyAncestor(hasTestTag(group)))

    @Test
    fun `schichtverlauf liegt zwischen demnächst und zeitnah prüfen`() {
        show()
        scrollToKey("section_soon")

        val soon = compose.onNodeWithTag("section_soon").getUnclippedBoundsInRoot()
        val timeline = compose.onNodeWithTag("shift_timeline").getUnclippedBoundsInRoot()
        val attention = compose.onNodeWithTag("section_attention").getUnclippedBoundsInRoot()
        assertTrue(soon.bottom <= timeline.top, "Schichtverlauf (${timeline.top}) beginnt vor Ende von Demnächst (${soon.bottom})")
        assertTrue(timeline.bottom <= attention.top, "Zeitnah prüfen (${attention.top}) beginnt vor Ende des Schichtverlaufs (${timeline.bottom})")
    }

    @Test
    fun `karte ist eine talkback-ansage als button, die blöcke werden nicht vorgelesen`() {
        show()
        scrollToKey("shift_timeline")

        // Alles auf demselben, klickbaren Knoten; zusammengeführt ohne Text aus Blöcken oder Labels.
        compose.onNodeWithTag("shift_timeline")
            .assert(hasContentDescription("Schichtverlauf, 2 dringende Hinweise, nächste Routine 07:30 Hörgeräte einsetzen"))
            .assert(hasClickAction())
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Text))
    }

    @Test
    fun `karte öffnet den schichtverlauf mit gruppen und routinen bis jetzt, zurück führt aufs dashboard`() {
        show()
        openHistory()

        entryInGroup("history_item_e005", "history_group_current").assertExists()
        entryInGroup("history_routine_rt02", "history_group_current").assertExists()
        compose.onNodeWithTag("history_routine_rt07").assertDoesNotExist()

        scrollToKey("history_group_previous")
        entryInGroup("history_item_e018", "history_group_previous").assert(hasText("Aus Nachtdienst"))
        entryInGroup("history_item_e002", "history_group_previous").assert(hasText("Erledigt"))

        compose.onNodeWithContentDescription("Zurück").performClick()
        awaitTag("shift_header")
        compose.onNodeWithTag("shift_history").assertDoesNotExist()
    }

    @Test
    fun `system-zurück führt aufs dashboard, erneut geöffnet beginnt der verlauf oben`() {
        show()
        openHistory()
        scrollToKey("history_group_previous")

        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }

        awaitTag("shift_header")
        compose.onNodeWithTag("shift_history").assertDoesNotExist()

        // Dashboard behält seine Scrollposition: die Karte ist ohne Scrollen antippbar.
        compose.onNodeWithTag("shift_timeline").performClick()
        awaitTag("shift_history")
        compose.onNodeWithText("Jetzt · 07:15").assertIsDisplayed()
    }

    @Test
    fun `geöffneter schichtverlauf übersteht die zustandswiederherstellung`() {
        val viewModel = viewModel()
        val restoration = StateRestorationTester(compose)
        restoration.setContent { LumaTheme(darkTheme = false) { LumaApp(viewModel = viewModel) } }
        awaitTag("person_card_r04")
        openHistory()

        restoration.emulateSavedInstanceStateRestore()

        awaitTag("shift_history")
        compose.onNodeWithTag("shift_history").assertIsDisplayed()
    }
}
