package com.mulkatz.lumacare.ui

import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
import kotlin.math.roundToInt
import kotlin.test.assertTrue

/**
 * Titelleiste eines Desktop-Fensters (`captionBar`, 40 dp): Kopfbereich und Detail beginnen darunter.
 * `statusBars` allein enthält die Titelleiste nicht, `safeDrawing` schon.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class CaptionBarInsetsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        val CAPTION = 40.dp
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

    private fun awaitTag(tag: String) {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun applyCaptionBar() {
        compose.runOnUiThread {
            val composeView: View = compose.activity.findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
            val px = (CAPTION.value * composeView.resources.displayMetrics.density).roundToInt()
            val insets = WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.captionBar(), Insets.of(0, px, 0, 0))
                .setVisible(WindowInsetsCompat.Type.captionBar(), true)
                .build()
            ViewCompat.dispatchApplyWindowInsets(composeView, insets)
        }
        compose.waitForIdle()
    }

    private fun showWithCaptionBar() {
        val repository = RoomCareRepository(db, SeedImporter(db, InMemoryAppStateStore()) { seed })
        val factory = viewModelFactory { initializer { DashboardViewModel(repository, loadClock = { clock }) } }
        val viewModel = ViewModelProvider(store, factory)[DashboardViewModel::class.java]
        compose.setContent { LumaTheme(darkTheme = false) { LumaApp(viewModel = viewModel) } }
        awaitTag("person_card_r04")
        applyCaptionBar()
    }

    private fun assertDetailNameBelowCaption() {
        compose.onNodeWithTag("person_card_r04").performClick()
        awaitTag("person_detail")
        val name = compose.onNode(hasText("Werner Hoffmann") and hasAnyAncestor(hasTestTag("person_detail")))
            .getUnclippedBoundsInRoot()
        assertTrue(name.top >= CAPTION, "Name im Detail beginnt bei ${name.top}")
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp")
    fun `tablet - detailbereich ohne appbar liegt unter der titelleiste`() {
        showWithCaptionBar()
        assertDetailNameBelowCaption()
    }

    @Test
    fun `phone - kopfbereich und personendetail liegen unter der titelleiste`() {
        showWithCaptionBar()

        val headerText = compose.onNode(hasText("Ahorn · Frühdienst") and hasAnyAncestor(hasTestTag("shift_header")))
            .getUnclippedBoundsInRoot()
        assertTrue(headerText.top >= CAPTION, "Kopfbereich beginnt bei ${headerText.top}")
        assertDetailNameBelowCaption()
    }
}
