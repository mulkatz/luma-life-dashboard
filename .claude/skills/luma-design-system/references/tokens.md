# Tokens

Alle Werte mit `scripts/contrast.py` geprüft. Bei Änderungen Tabelle **und** `PAIRS` im Skript anpassen.

## Farben – Marke

| Token | Light | Dark | Hinweis |
|---|---|---|---|
| `primary` | `#2E7F85` | `#4AAFB6` | Markenfarbe `#4AAFB6` hat auf Weiß nur 2,59 : 1, deshalb im Light Mode eine dunklere Stufe |
| `onPrimary` | `#FFFFFF` (4,68) | `#211E32` (6,25) | |
| `secondary` | `#211E32` | `#211E32` | Aubergine; Kopfbereich (`ShiftHeader`) |
| `onSecondary` | `#FFFFFF` (16,2) | `#ECEAF4` | |
| `accent` | `#FACF39` | `#FACF39` | Nur „neu“-Punkt und Akzente, **nie Status**. Text darauf: `#211E32` (10,8) |
| `background` | `#F7F7F9` | `#17151F` | |
| `surface` | `#FFFFFF` | `#211E32` | Karten, Sheets |
| `onSurface` | `#211E32` (16,2) | `#ECEAF4` (13,6 auf surface, 15,2 auf background) | |
| `muted` (`onSurfaceVariant`) | `#5C5A66` (6,75) | `#A9A6B6` (6,80 auf surface, 7,58 auf background) | Metadaten, Zeitangaben |
| `outline` | `#8E8B99` (3,33) | `#807C92` (4,02) | Ränder, nur UI (≥ 3 : 1) |

**Einschränkung Light Mode:** `primary` als **Textfarbe** nur auf `surface` (4,68). Auf `background`
erreicht es nur 4,37 : 1. Textlinks und Text-Buttons deshalb auf Karten oder Sheets setzen, nicht direkt
auf den Seitenhintergrund.

## Farben – Material-Rollen

Alle übrigen Rollen im `ColorScheme` explizit gesetzt, damit nichts auf das lila Baseline-Schema
zurückfällt. Karten setzen ihre Farbe explizit auf `surface` (nicht M3-Default
`surfaceContainerHighest`).

| Rolle | Light | Dark | Hinweis |
|---|---|---|---|
| `surfaceContainerLowest` | `#FFFFFF` | `#17151F` | |
| `surfaceContainerLow` | `#FFFFFF` | `#1C1A2A` | |
| `surfaceContainer` | `#FFFFFF` | `#211E32` | |
| `surfaceContainerHigh` | `#FFFFFF` | `#2A2740` | |
| `surfaceContainerHighest` | `#F0EFF4` | `#322F48` | `muted` darauf 5,90 / 5,39, `onSurface` 14,2 / 10,8 |
| `surfaceVariant` | `#F0EFF4` | `#322F48` | |
| `onSurfaceVariant` | = `muted` `#5C5A66` | = `muted` `#A9A6B6` | |
| `primaryContainer` / `secondaryContainer` | `#D5EEF0` | `#1F4A4E` | |
| `onPrimaryContainer` / `onSecondaryContainer` | `#17474B` (8,49) | `#CDEEF0` (7,95) | |
| `inverseSurface` | `#211E32` | `#ECEAF4` | Snackbar |
| `inverseOnSurface` | `#ECEAF4` (13,6) | `#211E32` (13,6) | |
| `inversePrimary` | `#4AAFB6` (6,25) | `#256A70` (5,23) | Aktion in der Snackbar |
| `error` / `onError` | `#B42318` / `#FFFFFF` (6,57) | `#FDA29B` / `#3A1210` (8,49) | |
| `errorContainer` / `onErrorContainer` | `#FEE4E2` / `#B42318` (5,45) | `#3A1210` / `#FDA29B` (8,49) | |
| `outlineVariant` | `#D9D8DF` | `#3D3A52` | Nur dekorative Trenner, kein Kontrastanspruch |

## Farben – Status

Text/Icon (`on…`) auf Container. Status immer mit Icon + Label + Farbe.

| Status | Label | Icon (Material Symbols Rounded) | Light | Dark |
|---|---|---|---|---|
| `urgent` | „Dringend“ | `warning` | `#B42318` auf `#FEE4E2` (5,45) | `#FDA29B` auf `#3A1210` (8,49) |
| `attention` | „Zeitnah“ | `schedule` | `#93370D` auf `#FEF0C7` (6,62) | `#FEC84B` auf `#33230A` (9,80) |
| `info` | „Info“ | `info` | `#256A70` auf `#E3F4F5` (5,49) | `#7FD3D9` auf `#10282A` (8,99) |
| `done` | „Erledigt“ | `check_circle` | `#067647` auf `#DCFAE6` (5,11) | `#75E0A7` auf `#0B2A1B` (9,53) |

Bearbeitungsstand (zweite Achse, unabhängig von der Dringlichkeit) als neutraler Text mit Icon in `muted`:
`open` → „Neu“ + gelber Punkt (`accent`) · `in_progress` (ich) → „Bei dir“ ·
`in_progress` (Vorschicht) → „Aus Nachtdienst“ · `done` → Status `done`.

## Typografie

**Manrope**, gebündelt als TTF in `res/font/` (400, 500, 600, 700). Keine Downloadable Fonts.

| Rolle | Größe/Zeile (sp) | Gewicht | Einsatz |
|---|---|---|---|
| `headlineSmall` | 24/32 | 700 | Begrüßung, Detail-Titel |
| `titleMedium` | 18/24 | 600 | Personenname auf der Karte |
| `titleSmall` | 15/20 | 600 | Abschnittsüberschriften |
| `bodyLarge` | 16/24 | 400 | Luma-Aussage |
| `bodyMedium` | 14/20 | 400 | Kontext, Metadaten |
| `labelLarge` | 14/20 | 600 | Buttons |
| `labelMedium` | 12/16 | 600 | Badges, Chips |
| `labelSmall` | 12/16 | 600 | Wie `labelMedium`; Material-Default (11 sp) wäre zu klein |

Mindestgröße 12 sp. Ziffern für Uhrzeiten mit `fontFeatureSettings = "tnum"` (gleich breit).

## Abstände (`LumaSpacing`, 4-dp-Raster)

`xs 4` · `s 8` · `m 12` · `l 16` · `xl 24` · `xxl 32`.
Seitenrand 16 · Kartenabstand 12 · Innenabstand Karte 16 · Abstand zwischen Abschnitten 24.

## Formen

Chips/Badges 8 dp · Karten 16 dp · Bottom Sheets 28 dp (oben) · Buttons voll gerundet (`CircleShape`).

## Höhe und Bewegung

- Flach: Karten mit `surface` auf `background` und 1 dp `outline`-Rand nur bei Hervorhebung, keine Schatten.
- Hervorhebung „Jetzt handeln“: 4 dp breiter Streifen links in der Statusfarbe + Badge.
- Animation 150–250 ms (`animateItem()` für Umsortierung, `animateContentSize()`), aus bei
  `Settings.Global.ANIMATOR_DURATION_SCALE == 0`.

## Umsetzung in Compose

```kotlin
// ui/theme/
LumaTheme(darkTheme = isSystemInDarkTheme()) { … }
  ├─ MaterialTheme(colorScheme = lightScheme/darkScheme, typography = LumaTypography, shapes = LumaShapes)
  └─ CompositionLocalProvider(LocalLumaStatusColors provides …, LocalLumaSpacing provides …)

object LumaTheme {
    val status: LumaStatusColors @Composable get() = LocalLumaStatusColors.current
    val spacing: LumaSpacing @Composable get() = LocalLumaSpacing.current
}

@Immutable data class StatusColor(val content: Color, val container: Color)
@Immutable data class LumaStatusColors(val urgent: StatusColor, val attention: StatusColor,
                                       val info: StatusColor, val done: StatusColor)
```

`accent` liegt im `ColorScheme` auf `tertiary`, Text darauf auf `onTertiary` (`#211E32`).
`tertiary` nie als Statusfarbe.

Kein Dynamic Color (Material You): Die Markenfarben sollen stabil bleiben.
