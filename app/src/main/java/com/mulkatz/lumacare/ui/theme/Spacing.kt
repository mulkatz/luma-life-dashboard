package com.mulkatz.lumacare.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Abstände im 4-dp-Raster. Seitenrand l · Kartenabstand m · Innenabstand Karte l · Abschnitte xl. */
@Immutable
data class LumaSpacing(
    val xs: Dp = 4.dp,
    val s: Dp = 8.dp,
    val m: Dp = 12.dp,
    val l: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
)

internal val LocalLumaSpacing = staticCompositionLocalOf { LumaSpacing() }
