package com.mulkatz.lumacare.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.mulkatz.lumacare.R

val Manrope = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
)

private val Base = Typography()

private fun TextStyle.manrope() = copy(fontFamily = Manrope)

private fun TextStyle.luma(size: TextUnit, lineHeight: TextUnit, weight: FontWeight) =
    copy(fontFamily = Manrope, fontSize = size, lineHeight = lineHeight, fontWeight = weight)

/** Rollen laut Token-Tabelle; übrige Rollen nur mit Manrope. Uhrzeiten zusätzlich mit `tnum`. */
val LumaTypography = Typography(
    displayLarge = Base.displayLarge.manrope(),
    displayMedium = Base.displayMedium.manrope(),
    displaySmall = Base.displaySmall.manrope(),
    headlineLarge = Base.headlineLarge.manrope(),
    headlineMedium = Base.headlineMedium.manrope(),
    headlineSmall = Base.headlineSmall.luma(24.sp, 32.sp, FontWeight.Bold),
    titleLarge = Base.titleLarge.manrope(),
    titleMedium = Base.titleMedium.luma(18.sp, 24.sp, FontWeight.SemiBold),
    titleSmall = Base.titleSmall.luma(15.sp, 20.sp, FontWeight.SemiBold),
    bodyLarge = Base.bodyLarge.luma(16.sp, 24.sp, FontWeight.Normal),
    bodyMedium = Base.bodyMedium.luma(14.sp, 20.sp, FontWeight.Normal),
    bodySmall = Base.bodySmall.manrope(),
    labelLarge = Base.labelLarge.luma(14.sp, 20.sp, FontWeight.SemiBold),
    labelMedium = Base.labelMedium.luma(12.sp, 16.sp, FontWeight.SemiBold),
    labelSmall = Base.labelSmall.luma(12.sp, 16.sp, FontWeight.SemiBold),
)

/** Gleich breite Ziffern für Uhrzeiten. */
fun TextStyle.tabularNumbers(): TextStyle = copy(fontFeatureSettings = "tnum")
