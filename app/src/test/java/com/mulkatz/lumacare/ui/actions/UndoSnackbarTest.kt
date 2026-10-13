package com.mulkatz.lumacare.ui.actions

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
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

/** „Rückgängig“ in der Snackbar macht die Übernahme rückgängig: Phone, echter Seed, Uhr 07:15. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class UndoSnackbarTest {
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

    private fun awaitCount(tag: String, count: Int) {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().size == count
        }
    }

    @Test
    fun `rückgängig nach übernehmen stellt den offenen hinweis wieder her`() {
        val repository = RoomCareRepository(db, SeedImporter(db, InMemoryAppStateStore()) { seed })
        val factory = viewModelFactory {
            initializer { DashboardViewModel(repository, loadClock = { clock }) }
            initializer { PersonActionsViewModel(SavedStateHandle(), repository, loadClock = { clock }) }
        }
        val provider = ViewModelProvider(store, factory)
        val dashboard = provider[DashboardViewModel::class.java]
        val actions = provider[PersonActionsViewModel::class.java]
        compose.setContent { LumaTheme(darkTheme = false) { LumaApp(viewModel = dashboard, actions = actions) } }
        awaitCount("person_card_r04", 1)
        compose.onNodeWithTag("person_card_r04").performClick()
        awaitCount("action_bar", 1)

        compose.onNode(hasTestTag("action_take_over") and hasAnyAncestor(hasTestTag("action_bar"))).performClick()
        awaitCount("handling_own_e005", 1)
        compose.waitUntil(timeoutMillis = 10_000) { compose.onAllNodes(hasText("Rückgängig")).fetchSemanticsNodes().isNotEmpty() }

        compose.onNode(hasText("Rückgängig") and hasAnyAncestor(hasTestTag("snackbar"))).performClick()

        awaitCount("handling_own_e005", 0)
        compose.onNode(hasTestTag("action_take_over") and hasAnyAncestor(hasTestTag("action_bar"))).assertExists()
    }
}
