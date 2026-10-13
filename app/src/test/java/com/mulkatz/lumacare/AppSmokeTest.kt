package com.mulkatz.lumacare

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Startet die App unter Robolectric (SDK 37): nach Import und Laden erscheinen Schichtkopf und Dashboard. */
@RunWith(AndroidJUnit4::class)
class AppSmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun awaitDashboard() {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("shift_header")).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun phone_showsShiftHeaderAndDashboard() {
        awaitDashboard()
        composeRule.onNodeWithTag("shift_header").assertIsDisplayed()
        composeRule.onNodeWithTag("section_now").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp-land")
    fun tablet_showsShiftHeaderAndDashboard() {
        awaitDashboard()
        composeRule.onNodeWithTag("shift_header").assertIsDisplayed()
        composeRule.onNodeWithTag("section_now").assertIsDisplayed()
    }
}
