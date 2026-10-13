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

    /** Rand der gewählten Karte im List-Detail (Tablet), Farbe `primary`. */
    val selectedBorder: Dp = 2.dp

    /** Breite der Liste (Dashboard) im List-Detail; Vorgabe 360–420 dp. */
    val listPaneWidth: Dp = 400.dp

    /** Icon in Chips, Metazeilen und Badges. */
    val iconSmall: Dp = 18.dp

    /** Icon in Abschnittsüberschriften. */
    val iconMedium: Dp = 20.dp

    /** Icon in leeren Zuständen und Fehlerzustand. */
    val iconLarge: Dp = 48.dp

    /** Mindesthöhe einer Auswahlzeile im Abschluss-Sheet (große Radio-Zeilen). */
    val optionRowMinHeight: Dp = 56.dp

    /** Platz, den die Snackbar über der Aktionsleiste im Detail freihält. */
    val actionBarReserve: Dp = 80.dp

    /** Mindesthöhe der Appbar auf Unterseiten (Schichtverlauf). */
    val appBarMinHeight: Dp = 56.dp

    /** Feste Zeitspalte in Verlaufslisten (bei 100 % Schrift; wächst mit der Schriftgröße). */
    val timeColumn: Dp = 64.dp

    // ---------- Schichtverlauf (Stundenblöcke) ----------

    /** Mindestbreite eines Stundenblocks. */
    val hourBlockMinWidth: Dp = 36.dp

    /** Höhe eines Stundenblocks; wächst bei großer Schrift. */
    val hourBlockHeight: Dp = 52.dp

    /** Abstand zwischen zwei Stundenblöcken. */
    val hourBlockGap: Dp = 2.dp

    /** Strichlänge und Lücke des gestrichelten Rands zukünftiger Stunden. */
    val dash: Dp = 4.dp

    /** Fläche unter dem Block für Routine-Ticks. */
    val tickAreaHeight: Dp = 14.dp

    /** Routine-Tick: Abstand oben, Länge und Breite. */
    val tickTop: Dp = 3.dp
    val tickLength: Dp = 10.dp
    val tickWidth: Dp = 2.dp

    /** Jetzt-Marker: Linienbreite, Überstand über den Block und Durchmesser des Punkts. */
    val nowMarkerWidth: Dp = 2.dp
    val nowMarkerOverhang: Dp = 6.dp
    val nowMarkerDot: Dp = 8.dp

    /** Legende im Schichtverlauf: Beispielblock links. */
    val legendSwatchWidth: Dp = 44.dp
    val legendSwatchHeight: Dp = 36.dp
}
