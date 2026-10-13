package com.mulkatz.lumacare.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollToKeyAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.unit.dp
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

/** List-Detail auf dem Tablet (Expanded): echter Seed, feste Uhr Mo 12.10.2026, 07:15. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w1280dp-h800dp")
class ListDetailTabletTest {
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

    @Test
    fun `ohne auswahl stehen dashboard links und die aufforderung rechts`() {
        show()
        awaitTag("detail_empty")

        compose.onNodeWithTag("shift_header").assertIsDisplayed()
        compose.onNodeWithText("Wähle links eine Person aus.").assertIsDisplayed()
        compose.onNodeWithTag("empty_state").assertDoesNotExist()

        val list = compose.onNodeWithTag("shift_header").getUnclippedBoundsInRoot()
        val detail = compose.onNodeWithTag("detail_empty").getUnclippedBoundsInRoot()
        val listWidth = list.right - list.left
        assertTrue(listWidth >= 360.dp && listWidth <= 420.dp, "Liste ist $listWidth breit")
        assertTrue(list.right <= detail.left, "Detail (${detail.left}) beginnt vor dem Ende der Liste (${list.right})")
    }

    @Test
    fun `klick auf r12 zeigt beide hinweise rechts und markiert die karte links`() {
        show()

        compose.onNodeWithTag("person_card_r12").performClick()
        awaitTag("person_detail")

        compose.onNodeWithTag("detail_hint_e018").assertIsDisplayed()
        compose.onNodeWithTag("detail_hint_e019").assertIsDisplayed()
        compose.onNodeWithTag("shift_header").assertIsDisplayed()
        compose.onNodeWithTag("person_card_r12").assertIsSelected()
        compose.onNodeWithTag("person_card_r04").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Selected))
        // Auf dem Tablet hat der Detailbereich keine Zurück-Appbar.
        compose.onNodeWithContentDescription("Zurück").assertDoesNotExist()
        compose.onNodeWithTag("detail_empty").assertDoesNotExist()
    }

    @Test
    fun `system-zurück wechselt nicht durch die auswahl, sondern verlässt die app`() {
        show()
        compose.onNodeWithTag("person_card_r12").performClick()
        awaitTag("detail_hint_e018")
        compose.onNodeWithTag("person_card_r04").performClick()
        awaitTag("detail_hint_e005")

        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()

        assertTrue(compose.activity.isFinishing, "Activity wird nicht beendet")
    }

    @Test
    fun `gewählte person übersteht die zustandswiederherstellung`() {
        val viewModel = viewModel()
        val restoration = StateRestorationTester(compose)
        restoration.setContent { LumaTheme(darkTheme = false) { LumaApp(viewModel = viewModel) } }
        awaitTag("person_card_r12")
        compose.onNodeWithTag("person_card_r12").performClick()
        awaitTag("detail_hint_e018")

        restoration.emulateSavedInstanceStateRestore()

        awaitTag("detail_hint_e018")
        compose.onNodeWithTag("detail_hint_e018").assertIsDisplayed()
        compose.onNodeWithTag("person_card_r12").assertIsSelected()
    }

    @Test
    fun `timeline-karte öffnet den schichtverlauf rechts`() {
        show()
        compose.onNode(hasScrollToKeyAction()).performScrollToKey("shift_timeline")

        compose.onNodeWithTag("shift_timeline").performClick()
        awaitTag("shift_history")

        compose.onNodeWithTag("shift_history").assertIsDisplayed()
        compose.onNodeWithTag("shift_timeline").assertIsDisplayed()
        val list = compose.onNodeWithTag("shift_timeline").getUnclippedBoundsInRoot()
        val history = compose.onNodeWithTag("shift_history").getUnclippedBoundsInRoot()
        assertTrue(list.right <= history.left, "Verlauf (${history.left}) beginnt vor dem Ende der Liste (${list.right})")
    }

    @Test
    fun `eintrag im schichtverlauf öffnet die person rechts`() {
        show()
        compose.onNode(hasScrollToKeyAction()).performScrollToKey("shift_timeline")
        compose.onNodeWithTag("shift_timeline").performClick()
        awaitTag("history_item_e005")

        compose.onNodeWithTag("history_item_e005").performScrollTo().performClick()
        awaitTag("person_detail")

        compose.onNodeWithTag("detail_hint_e005").assertIsDisplayed()
        compose.onNode(hasScrollToKeyAction()).performScrollToKey("section_now")
        compose.onNodeWithTag("person_card_r04").assertIsSelected()
    }
}
