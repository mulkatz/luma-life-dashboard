package com.mulkatz.lumacare.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Chips/Badges 8 dp (`small`) · Karten 16 dp (`medium`) · Bottom Sheets 28 dp (`extraLarge`).
 * Buttons sind in Material 3 bereits voll gerundet.
 */
val LumaShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
