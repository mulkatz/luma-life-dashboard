package com.mulkatz.lumacare.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Werte laut .claude/skills/luma-design-system/references/tokens.md (mit contrast.py geprüft).

private val Aubergine = Color(0xFF211E32)
private val Accent = Color(0xFFFACF39)

// Kopfbereich im Dark Mode: kräftigeres, helleres Aubergine, damit sich der Kopf von den Karten (surface) abhebt.
private val HeaderDark = Color(0xFF2F2A4A)

internal val LumaLightColorScheme = lightColorScheme(
    primary = Color(0xFF2E7F85),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Aubergine,
    onSecondary = Color(0xFFFFFFFF),
    // accent: nur „neu“-Punkt und sparsame Akzente, nie Status
    tertiary = Accent,
    onTertiary = Aubergine,
    background = Color(0xFFF7F7F9),
    onBackground = Aubergine,
    surface = Color(0xFFFFFFFF),
    onSurface = Aubergine,
    onSurfaceVariant = Color(0xFF5C5A66),
    outline = Color(0xFF8E8B99),
    // Material-Rollen, damit nichts auf das Baseline-Schema zurückfällt
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFFF0EFF4),
    surfaceVariant = Color(0xFFF0EFF4),
    // Liste im List-Detail (Tablet): etwas dunkler als background, Karten (surface) stehen darauf klar
    surfaceDim = Color(0xFFF0EFF4),
    primaryContainer = Color(0xFFD5EEF0),
    onPrimaryContainer = Color(0xFF17474B),
    secondaryContainer = Color(0xFFD5EEF0),
    onSecondaryContainer = Color(0xFF17474B),
    inverseSurface = Aubergine,
    inverseOnSurface = Color(0xFFECEAF4),
    inversePrimary = Color(0xFF4AAFB6),
    error = Color(0xFFB42318),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE4E2),
    onErrorContainer = Color(0xFFB42318),
    outlineVariant = Color(0xFFD9D8DF),
)

internal val LumaDarkColorScheme = darkColorScheme(
    primary = Color(0xFF4AAFB6),
    onPrimary = Aubergine,
    secondary = HeaderDark,
    onSecondary = Color(0xFFECEAF4),
    tertiary = Accent,
    onTertiary = Aubergine,
    background = Color(0xFF17151F),
    onBackground = Color(0xFFECEAF4),
    surface = Aubergine,
    onSurface = Color(0xFFECEAF4),
    onSurfaceVariant = Color(0xFFA9A6B6),
    outline = Color(0xFF807C92),
    surfaceContainerLowest = Color(0xFF17151F),
    surfaceContainerLow = Color(0xFF1C1A2A),
    surfaceContainer = Aubergine,
    surfaceContainerHigh = Color(0xFF2A2740),
    surfaceContainerHighest = Color(0xFF322F48),
    surfaceVariant = Color(0xFF322F48),
    // Liste im List-Detail (Tablet): etwas dunkler als background, Karten (surface) stehen darauf klar
    surfaceDim = Color(0xFF110F18),
    primaryContainer = Color(0xFF1F4A4E),
    onPrimaryContainer = Color(0xFFCDEEF0),
    secondaryContainer = Color(0xFF1F4A4E),
    onSecondaryContainer = Color(0xFFCDEEF0),
    inverseSurface = Color(0xFFECEAF4),
    inverseOnSurface = Aubergine,
    inversePrimary = Color(0xFF256A70),
    error = Color(0xFFFDA29B),
    onError = Color(0xFF3A1210),
    errorContainer = Color(0xFF3A1210),
    onErrorContainer = Color(0xFFFDA29B),
    // nur dekorative Trenner
    outlineVariant = Color(0xFF3D3A52),
)

internal val LumaLightStatusColors = LumaStatusColors(
    urgent = StatusColor(content = Color(0xFFB42318), container = Color(0xFFFEE4E2)),
    attention = StatusColor(content = Color(0xFF93370D), container = Color(0xFFFEF0C7)),
    info = StatusColor(content = Color(0xFF256A70), container = Color(0xFFE3F4F5)),
    done = StatusColor(content = Color(0xFF067647), container = Color(0xFFDCFAE6)),
)

internal val LumaDarkStatusColors = LumaStatusColors(
    urgent = StatusColor(content = Color(0xFFFDA29B), container = Color(0xFF3A1210)),
    attention = StatusColor(content = Color(0xFFFEC84B), container = Color(0xFF33230A)),
    info = StatusColor(content = Color(0xFF7FD3D9), container = Color(0xFF10282A)),
    done = StatusColor(content = Color(0xFF75E0A7), container = Color(0xFF0B2A1B)),
)
