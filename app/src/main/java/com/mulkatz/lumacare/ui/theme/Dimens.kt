package com.mulkatz.lumacare.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Feste Maße neben den Abständen ([LumaSpacing]): Tap-Ziele, Inhaltsbreite, Ränder, Icons. */
object LumaSizes {
    /** Mindestgröße für Tap-Ziele. */
    val minTouchTarget: Dp = 48.dp

    /** Mindesthöhe einer Listenzeile (z. B. Organisatorisches). */
    val listRowMinHeight: Dp = 56.dp

    /** Maximale Breite von Text und Karten. */
    val contentMaxWidth: Dp = 640.dp

    /** Streifen links an Karten unter „Jetzt handeln“. */
    val stripe: Dp = 4.dp

    /** Rand von Karten und Chips. */
    val border: Dp = 1.dp

    /** Icon in Chips, Metazeilen und Badges. */
    val iconSmall: Dp = 18.dp

    /** Icon in Abschnittsüberschriften. */
    val iconMedium: Dp = 20.dp

    /** Icon in leeren Zuständen und Fehlerzustand. */
    val iconLarge: Dp = 48.dp
}
