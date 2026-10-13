package com.mulkatz.lumacare

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollToKeyAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToKey
import androidx.lifecycle.SavedStateHandle
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
import com.mulkatz.lumacare.ui.actions.PersonActionsViewModel
import com.mulkatz.lumacare.ui.dashboard.DashboardViewModel
import com.mulkatz.lumacare.ui.theme.LumaTheme
import org.junit.After
import org.junit.Assert.assertNotSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Kern-Flow durch die echte UI ([LumaApp]): Start → Import → Werner unter „Jetzt handeln“ → Detail → Übernehmen
 * → Notiz → Abschließen (mit Rückgängig) → Werner nicht mehr unter „Jetzt handeln“ → Activity neu erzeugen →
 * Zustand bleibt. Echter Seed, feste Uhr Mo 12.10.2026, 07:15, als Phone und als Tablet.
 *
 * Datenbank und Zustandsspeicher gehören dem Test und überstehen das Neuerzeugen wie Datei und SharedPreferences
 * der App. Danach entstehen Repository und ViewModels neu: Was die UI dann zeigt, kommt aus der Datenbank.
 */
@RunWith(RobolectricTestRunner::class)
class CoreFlowE2ETest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var db: LumaDatabase
    private val seed = realSeedJson()
    private val appState = InMemoryAppStateStore()
    private val viewModelStores = mutableListOf<ViewModelStore>()

    /** Hält den Seed-Import an, bis der Ladezustand geprüft ist; sonst hängt sein Anblick vom Timing ab. */
    private val importGate = CountDownLatch(1)

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
        viewModelStores.forEach(ViewModelStore::clear)
        db.close()
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `phone - übernehmen, notiz, abschließen mit rückgängig, neu erzeugen`() = coreFlow(twoPane = false)

    @Test
    @Config(qualifiers = "w1280dp-h800dp")
    fun `tablet - übernehmen, notiz, abschließen mit rückgängig, neu erzeugen`() = coreFlow(twoPane = true)

    private fun coreFlow(twoPane: Boolean) {
        // 1. Start: erst der Ladezustand, nach dem Import steht Werner unter „Jetzt handeln“.
        startApp()
        awaitExists(hasTestTag("loading"))
        compose.onNodeWithTag("loading").assertIsDisplayed()
        importGate.countDown()
        awaitExists(card("r04", inSection = "section_now"))
        compose.onNodeWithTag("loading").assertDoesNotExist()

        // 2. Werner öffnen: Phone als eigener Screen, Tablet rechts neben der Liste.
        compose.onNodeWithTag("person_card_r04").performClick()
        awaitExists(hasTestTag("action_bar"))
        compose.onNodeWithTag("detail_hint_e005").assertIsDisplayed()
        if (twoPane) {
            compose.onNodeWithTag("shift_header").assertIsDisplayed()
        } else {
            compose.onNodeWithTag("shift_header").assertDoesNotExist()
        }

        // 3. Übernehmen: Der Hinweis ist danach „Bei dir“.
        barAction("action_take_over").performClick()
        awaitExists(ownHint())

        // 4. Notiz per Baustein: Speichern erst mit Text, danach steht sie im Verlauf.
        barAction("action_note").performClick()
        awaitExists(hasTestTag("note_sheet"))
        compose.onNodeWithTag("action_save_note").assertIsNotEnabled()
        compose.onNodeWithTag("note_snippet_measured").performClick()
        compose.onNodeWithTag("action_save_note").assertIsEnabled().performClick()
        awaitExists(hasText("Werte gemessen") and hasAnyAncestor(hasTestTag("detail_history")))
        compose.onNode(hasText("Werte gemessen") and hasAnyAncestor(hasTestTag("detail_history"))).assert(hasText("Notiz"))
        compose.onNodeWithTag("note_sheet").assertDoesNotExist()

        // 5. Abschließen „Erledigt“: ohne Notiz Inline-Fehler und inaktiver Button, mit Baustein abschließen.
        barAction("action_resolve").performClick()
        awaitExists(hasTestTag("resolution_sheet"))
        compose.onNodeWithTag("resolution_done").performClick().assertIsSelected()
        compose.onNodeWithTag("note_error", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("action_confirm_resolve").assertIsNotEnabled()
        compose.onNodeWithTag("note_snippet_doctor").performClick()
        compose.onNodeWithTag("note_error", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("action_confirm_resolve").assertIsEnabled().performClick()
        awaitCount(hasTestTag("detail_hint_e005"), 0)

        // 6. Rückgängig in der Snackbar: wieder „Bei dir“, die Abschlussnotiz kommt als Entwurf zurück.
        awaitExists(hasTestTag("action_undo"))
        compose.onNodeWithTag("action_undo").performClick()
        awaitExists(ownHint())
        resolvedInDetail().assertCountEquals(0)
        barAction("action_resolve").performClick()
        awaitExists(hasTestTag("resolution_sheet"))
        compose.onNodeWithTag("resolution_done").assertIsSelected()
        compose.onNodeWithTag("note_text").assert(hasText("Arzt informiert"))
        compose.onNodeWithTag("action_confirm_resolve").assertIsEnabled().performClick()
        awaitCount(hasTestTag("detail_hint_e005"), 0)
        resolvedInDetail().assertCountEquals(1)

        // 7. Dashboard: Werner nicht mehr unter „Jetzt handeln“, Rudolf schon.
        if (!twoPane) {
            compose.onNodeWithTag("action_back").performClick()
            awaitExists(hasTestTag("shift_header"))
        }
        assertActNowWithoutWerner()

        // 8. Activity neu erzeugen: Der Zustand kommt aus der Datenbank, im Verlauf steht der Abschluss.
        val before = compose.activity
        compose.activityRule.scenario.recreate()
        assertNotSame(before, compose.activity)
        startApp()
        awaitExists(card("r12", inSection = "section_now"))
        assertActNowWithoutWerner()

        compose.onNode(hasScrollToKeyAction()).performScrollToKey("shift_timeline")
        compose.onNodeWithTag("shift_timeline").performClick()
        awaitExists(hasTestTag("history_resolved_e005"))
        compose.onNodeWithTag("history_resolved_e005")
            .performScrollTo()
            .assert(hasText("Abgeschlossen: Erledigt"))
            .assert(hasText("Werner Hoffmann · 2.03"))
            .performClick()
        awaitExists(hasTestTag("detail_history"))
        resolvedInDetail().assertCountEquals(1)
        compose.onNodeWithTag("detail_hint_e005").assertDoesNotExist()
    }

    /** Baut Repository und ViewModels auf der Test-Datenbank und setzt [LumaApp] als Inhalt der aktuellen Activity. */
    private fun startApp() {
        val store = ViewModelStore().also(viewModelStores::add)
        val importer = SeedImporter(db, appState) {
            check(importGate.await(10, TimeUnit.SECONDS)) { "Import nicht freigegeben" }
            seed
        }
        val repository = RoomCareRepository(db, importer)
        val factory = viewModelFactory {
            initializer { DashboardViewModel(repository, loadClock = { clock }) }
            initializer { PersonActionsViewModel(SavedStateHandle(), repository, loadClock = { clock }) }
        }
        val provider = ViewModelProvider(store, factory)
        val dashboard = provider[DashboardViewModel::class.java]
        val actions = provider[PersonActionsViewModel::class.java]
        val activity = compose.activity
        compose.runOnUiThread {
            activity.setContent { LumaTheme(darkTheme = false) { LumaApp(viewModel = dashboard, actions = actions) } }
        }
    }

    private fun assertActNowWithoutWerner() {
        compose.onNode(hasScrollToKeyAction()).performScrollToKey("section_now")
        awaitCount(card("r04", inSection = "section_now"), 0)
        compose.onNode(card("r12", inSection = "section_now")).assertExists()
    }

    private fun card(residentId: String, inSection: String): SemanticsMatcher =
        hasTestTag("person_card_$residentId") and hasAnyAncestor(hasTestTag(inSection))

    private fun ownHint(): SemanticsMatcher = hasTestTag("detail_hint_e005") and hasText("Bei dir")

    private fun barAction(tag: String): SemanticsNodeInteraction =
        compose.onNode(hasTestTag(tag) and hasAnyAncestor(hasTestTag("action_bar")))

    private fun resolvedInDetail() =
        compose.onAllNodes(hasText("Abgeschlossen: Erledigt") and hasAnyAncestor(hasTestTag("detail_history")))

    private fun awaitExists(matcher: SemanticsMatcher) {
        compose.waitUntil(timeoutMillis = 10_000) { compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun awaitCount(matcher: SemanticsMatcher, count: Int) {
        compose.waitUntil(timeoutMillis = 10_000) { compose.onAllNodes(matcher).fetchSemanticsNodes().size == count }
    }
}
