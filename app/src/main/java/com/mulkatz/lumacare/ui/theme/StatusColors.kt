package com.mulkatz.lumacare.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Text/Icon ([content]) auf [container]. Status immer mit Icon + Label + Farbe zeigen. */
@Immutable
data class StatusColor(val content: Color, val container: Color)

@Immutable
data class LumaStatusColors(
    val urgent: StatusColor,
    val attention: StatusColor,
    val info: StatusColor,
    val done: StatusColor,
)

internal val LocalLumaStatusColors = staticCompositionLocalOf { LumaLightStatusColors }
