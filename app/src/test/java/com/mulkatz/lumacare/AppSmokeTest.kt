package com.mulkatz.lumacare

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Startet die App unter Robolectric (SDK 37) und prüft, dass der Schichtkopf erscheint. */
@RunWith(AndroidJUnit4::class)
class AppSmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun phone_showsShiftHeader() {
        composeRule.onNodeWithTag("shift_header").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp-land")
    fun tablet_showsShiftHeader() {
        composeRule.onNodeWithTag("shift_header").assertIsDisplayed()
    }
}
