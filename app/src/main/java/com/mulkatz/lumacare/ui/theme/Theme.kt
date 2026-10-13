package com.mulkatz.lumacare.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/** Luma-Theme ohne Dynamic Color, damit die Markenfarben stabil bleiben. */
@Composable
fun LumaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalLumaStatusColors provides if (darkTheme) LumaDarkStatusColors else LumaLightStatusColors,
        LocalLumaSpacing provides LumaSpacing(),
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) LumaDarkColorScheme else LumaLightColorScheme,
            typography = LumaTypography,
            shapes = LumaShapes,
            content = content,
        )
    }
}

object LumaTheme {
    val status: LumaStatusColors
        @Composable get() = LocalLumaStatusColors.current

    val spacing: LumaSpacing
        @Composable get() = LocalLumaSpacing.current
}
