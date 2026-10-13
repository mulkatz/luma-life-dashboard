# Komponenten

Alle in `ui/components/`, zustandslos. `testTag`s in Klammern.

## Adaptives Layout (Phone und Tablet)

Nach `currentWindowAdaptiveInfo().windowSizeClass`, nie nach Gerätetyp oder Ausrichtung:

| Breite | Klasse | Layout |
|---|---|---|
| < 600 dp | Compact (Phone hochkant) | Eine Spalte. Dashboard → Detail als eigener Screen. Aktionen in der `BottomActionBar` |
| 600–840 dp | Medium (Phone quer, kleines Tablet, Foldable) | Eine Spalte, Inhalt max. 640 dp breit und zentriert. Navigation wie Compact |
| ≥ 840 dp | Expanded (Tablet) | List-Detail: Dashboard links (360–420 dp), Detail rechts. Auswahl hervorgehoben, Detail-Aktionen unten im Detailbereich |

- `NavigableListDetailPaneScaffold` (material3-adaptive) übernimmt Spalten und Zurück-Navigation.
- Gewählte Person im `SavedStateHandle`, überlebt Rotation und Größenwechsel.
- Expanded ohne Auswahl: Detailbereich zeigt `EmptyState` „Wähle links eine Person aus.“
- Keine feste Ausrichtung im Manifest. Text und Karten max. ca. 640 dp breit.

## ShiftHeader (`shift_header`)

Fläche `secondary`, Text `onSecondary`. Zeile 1: „Ahorn · Frühdienst“ (`titleSmall`).
Zeile 2: „07:15 · noch 6:45 h“ (`headlineSmall`, `tnum`). Nach 14:00: „Schicht beendet“.

## MetricRow (`metric_row`)

Horizontal scrollbare Chips: „2 dringend · 7 zeitnah · 2 aus Nachtdienst · 2× Luma nicht erreicht“.
Jeder Chip ist ein Filter (`FilterChip`, ≥ 48 dp hoch), Zahl in `labelLarge`, Statusfarbe nur als Icon.
Bei 0 wird der Chip ausgeblendet, nicht ausgegraut.

## SectionHeader(count) (`section_<key>`)

`titleSmall` + Anzahl als Badge: „Jetzt handeln · 2“. Abschnitte ohne Einträge entfallen ganz.

## PersonCard (`person_card_<residentId>`)

```
┌───────────────────────────────────────────┐
│▌ Werner Hoffmann            [⚠ Dringend]   │  titleMedium · SeverityBadge (höchster offener Status)
│▌ Zi. 2.03 · PG 3                           │  bodyMedium, muted
│▌ „Berichtet von Zittern und…“   · 06:30  │  1–3 Hinweise (EventItem kompakt), älteste zuerst
│▌ [Diabetes Typ 2] [Routine 07:00 Blutz.]   │  ContextChips, max. eine Zeile + „+2“
│▌                          [ Übernehmen ]   │  Schnellaktion, rechts unten
└───────────────────────────────────────────┘
```
- Streifen links (4 dp) nur in „Jetzt handeln“.
- Ganze Karte antippbar → Detail. Schnellaktion ist ein eigenes Touch-Ziel (≥ 48 dp).
- Mehr als 3 Hinweise: „+ 2 weitere Hinweise“.
- Semantik zusammengefasst: „Werner Hoffmann, Zimmer 2.03, dringend, 1 Hinweis, Diabetes Typ 2“.
- Zustände: Standard · gedrückt · ausgewählt (Tablet, `primary`-Rand 2 dp) · Vorschicht („Aus Nachtdienst“).

## SeverityBadge

Container + Icon + Label aus `LumaTheme.status`, Radius 8, `labelMedium`. Niemals nur ein Farbpunkt.

## ContextChip

Neutral (`surface` + `outline`-Rand), Icon optional, `labelMedium`. Nur Fakten aus den Daten:
`health_notes`, Routine heute ±60 min, Aktivität in der Schicht, weitere Events der Person, „Luma nicht erreicht“.

## EventItem

Kopfzeile: „Luma-Hinweis · Gespräch 06:30 · vor 45 Min.“ (`bodyMedium`, muted).
Inhalt: Originalaussage (`bodyLarge`). Rechts: Bearbeitungsstand. Keine Interpretation.

## BottomActionBar (`action_bar`)

Unten fixiert, über der Navigationsleiste (`WindowInsets.navigationBars`). Max. eine primäre Aktion
(gefüllt, `primary`) + eine sekundäre (umrandet). Primär je nach Zustand:
`open` → „Übernehmen“ · Vorschicht → „Übernahme bestätigen“ · `in_progress` (ich) → „Abschließen“.
Sekundär: „Notiz“ bzw. „Zurückgeben“.

## ResolutionSheet (`resolution_sheet`)

Modal Bottom Sheet, Radius 28 oben. Optionen als große Radio-Zeilen (≥ 56 dp):
„Erledigt“ · „Weitergeleitet an …“ (Bereiche: Haustechnik, Küche, Sozialdienst, Verwaltung) ·
„Kein Handlungsbedarf“. Darunter `NoteInput`. Button „Abschließen“ erst aktiv, wenn gültig.

## NoteInput (`note_input`)

Textbausteine als Chips (Tippen fügt ein), darunter optionales Freitextfeld.
Pflicht bei `health_hint` und `urgent`: Inline-Fehler „Bitte kurz notieren, was du getan hast.“
(leer oder < 3 Zeichen), kein Dialog. Entwurf liegt im `SavedStateHandle`.
Beispiel-Bausteine: „Gesehen, wohlauf“ · „Arzt informiert“ · „Werte gemessen“ · „Angehörige informiert“.

## TimelineItem

Verlauf aus `event_action`: Icon + „Übernommen von Anna K. · 07:20“ + Notiz. Neueste oben.

## Snackbar „Rückgängig“

Nach jeder Statusänderung: „Abgeschlossen · Rückgängig“, 6 s. Ersetzt Bestätigungsdialoge.

## EmptyState / LoadingState

- `LoadingState` während des Seed-Imports (`loading`): Skeleton-Karten, kein Spinner allein.
- `EmptyState` (`empty_state`) **nur** nach abgeschlossenem Laden: Icon `task_alt`,
  „Alles erledigt“ + „Gerade braucht niemand deine Aufmerksamkeit.“
