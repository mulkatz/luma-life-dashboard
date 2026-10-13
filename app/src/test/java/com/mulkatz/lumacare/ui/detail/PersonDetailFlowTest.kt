package com.mulkatz.lumacare.ui.detail

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollToKeyAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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

/** Personendetail auf dem Phone: echter Seed, feste Uhr Mo 12.10.2026, 07:15. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class PersonDetailFlowTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val E005 = "Berichtet von Zittern und Schwitzen, hat laut eigener Aussage noch nicht gefrühstückt."
    }

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

    private fun inDetail(tag: String, text: String): SemanticsNodeInteraction =
        compose.onNode(hasText(text) and hasAnyAncestor(hasTestTag(tag)))

    private fun headline(name: String): SemanticsNodeInteraction =
        compose.onNode(hasText(name) and hasAnyAncestor(hasTestTag("person_detail")))

    @Test
    fun `karte öffnet werners detail mit vollem hinweis und gesundheit, zurück führt aufs dashboard`() {
        show()

        compose.onNodeWithTag("person_card_r04").performClick()
        awaitTag("person_detail")

        headline("Werner Hoffmann").assertIsDisplayed()
        inDetail("person_detail", "Zimmer 2.03 · Pflegegrad 3").assertExists()
        // Die Hinweiskarte ist eine TalkBack-Ansage; ihre Texte liegen am Knoten der Karte.
        compose.onNodeWithTag("detail_hint_e005")
            .assertIsDisplayed()
            .assert(hasText(E005))
            .assert(hasText("Luma-Hinweis · Gespräch 06:30 · vor 45 Min."))
        inDetail("detail_context", "Diabetes Typ 2, achtet auf Zucker").performScrollTo().assertIsDisplayed()
        inDetail("detail_context", "07:00 Blutzucker messen vor dem Frühstück · Luma-Anruf · geplant").assertExists()
        inDetail("detail_context", "Monika Hoffmann, Ehefrau").assertExists()
        // B3 ist nur lesend: keine Aktionen im Detail.
        compose.onNode(hasTestTag("action_take_over") and hasAnyAncestor(hasTestTag("person_detail"))).assertDoesNotExist()

        compose.onNodeWithContentDescription("Zurück").performClick()
        awaitTag("shift_header")
        compose.onNodeWithTag("person_detail").assertDoesNotExist()
    }

    @Test
    fun `system-zurück schließt das detail`() {
        show()
        compose.onNodeWithTag("person_card_r04").performClick()
        awaitTag("person_detail")

        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }

        awaitTag("shift_header")
        compose.onNodeWithTag("person_detail").assertDoesNotExist()
    }

    @Test
    fun `aktion der karte öffnet vorerst das detail`() {
        show()

        compose.onNode(hasTestTag("action_confirm_handover") and hasAnyAncestor(hasTestTag("person_card_r12"))).performClick()
        awaitTag("detail_hint_e018")

        headline("Rudolf Schmidt").assertIsDisplayed()
    }

    @Test
    fun `offener hinweis bei margarete fischer öffnet r09`() {
        show()

        compose.onNodeWithTag("open_hint_r09").performScrollTo().performClick()
        awaitTag("person_detail")

        headline("Margarete Fischer").assertIsDisplayed()
        compose.onNodeWithTag("detail_hint_e014").assertExists()
        // Nachricht von Angehörigen: Quelle „Nachricht“, nicht „Gespräch“.
        compose.onNodeWithTag("detail_hint_e013")
            .performScrollTo()
            .assert(hasText("Luma-Hinweis · Nachricht gestern 22:40", substring = true))
    }

    @Test
    fun `schichtverlauf öffnet über e018 das detail von r12, zurück führt in den verlauf`() {
        show()
        compose.onNode(hasScrollToKeyAction()).performScrollToKey("shift_timeline")
        compose.onNodeWithTag("shift_timeline").performClick()
        awaitTag("shift_history")
        compose.onNode(hasScrollToKeyAction()).performScrollToKey("history_group_previous")

        compose.onNodeWithTag("history_item_e018").performScrollTo().performClick()
        awaitTag("person_detail")

        headline("Rudolf Schmidt").assertIsDisplayed()
        compose.onNodeWithTag("detail_hint_e018").assertIsDisplayed()

        compose.onNodeWithContentDescription("Zurück").performClick()
        awaitTag("shift_history")
        compose.onNodeWithTag("person_detail").assertDoesNotExist()
    }

    @Test
    fun `gewählte person übersteht die zustandswiederherstellung`() {
        val viewModel = viewModel()
        val restoration = StateRestorationTester(compose)
        restoration.setContent { LumaTheme(darkTheme = false) { LumaApp(viewModel = viewModel) } }
        awaitTag("person_card_r12")
        compose.onNodeWithTag("person_card_r12").performClick()
        awaitTag("person_detail")

        restoration.emulateSavedInstanceStateRestore()

        awaitTag("person_detail")
        headline("Rudolf Schmidt").assertIsDisplayed()
        compose.onNodeWithTag("detail_hint_e018").assertExists()
    }
}
