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
- Gewählte Person im speicherbaren Zustand des Navigators (Key `person:<id>` bzw. `shift_history`),
  überlebt Rotation, Größenwechsel und Prozess-Tod. Zurück: einspaltig Person → Verlauf → Dashboard,
  zweispaltig verlässt Zurück die App (kein Durchblättern früherer Auswahlen).
- Insets einheitlich über `WindowInsets.safeDrawing` (inkl. Titelleiste im Desktop-Fenster), nie nur `statusBars`.
- Expanded ohne Auswahl: Detailbereich zeigt `EmptyState` „Wähle links eine Person aus.“
- Keine feste Ausrichtung im Manifest. Text und Karten max. ca. 640 dp breit.

## ShiftHeader (`shift_header`)

Fläche `secondary`, Text `onSecondary`. Zeile 1: „Ahorn · Frühdienst“ (`titleSmall`).
Zeile 2: „07:15 · noch 6:45 h“ (`headlineSmall`, `tnum`). Nach 14:00: „Schicht beendet“.

## Dichte (gilt für das Dashboard)

Das Zielbild gibt Inhalte vor, nicht die Dichte. Auf dem Phone (411 × 891 dp, Schrift 100 %) sind beide
Karten unter „Jetzt handeln“ ohne Scrollen sichtbar. Auf dem Dashboard nichts unter 14 sp.
Abstände: 16 dp innerhalb von Karten, 12 dp zwischen Karten, 32 dp zwischen Abschnitten.
Kein „Neu“-Marker; markiert wird nur, was abweicht („Aus Nachtdienst“, später „In Bearbeitung“).

## MetricRow: Roadmap

Entfällt, bis es Filter gibt. Ohne Filter doppelt sie nur die Zählung der Abschnitte.

## SectionHeader(count) (`section_<key>`)

`titleMedium` mit Status-Icon + Anzahl: „Jetzt handeln · 2“. Abschnitte ohne Einträge entfallen ganz.
Reihenfolge: Jetzt handeln · Demnächst · Schichtverlauf · Zeitnah prüfen · Organisatorisches.

## PersonCard (`person_card_<residentId>`)

```
┌───────────────────────────────────────────┐
│▌ Werner Hoffmann · 2.03     [⚠ Dringend]   │  titleMedium, eine Zeile (lange Namen umbrechen) · SeverityBadge
│▌ vor 45 Min. · 06:30                      │  Metazeile bodyMedium, muted (bzw. „Aus Nachtdienst · gestern 23:15“)
│▌ Berichtet von Zittern und Schwitzen, hat │  NUR der maßgebliche Hinweis, bodyLarge 16 sp, max. 3 Zeilen (bei Schrift > 100 % unbegrenzt)
│▌ laut eigener Aussage noch nicht gefrüh…  │
│▌ + 1 weiterer Hinweis                     │  weitere Hinweise nur als Anzahl
│▌ [Diabetes Typ 2…] [⟳ 07:00 Blutz…] +1   │  max. 2 ContextChips (labelLarge 14 sp, einzeilig) + „+n“
│▌                          [ Übernehmen ]   │  Schnellaktion, rechts unten
└───────────────────────────────────────────┘
```
- Streifen links (4 dp) und SeverityBadge nur in „Jetzt handeln“. In „Zeitnah prüfen“ trägt die
  Abschnittsüberschrift den Status; die Karte nennt ihn nur in der TalkBack-Ansage.
- Chip-Priorität (legt der Prioritizer fest, die UI sortiert nicht): `health_notes` · Routine ±60 min ·
  „Luma nicht erreicht“ (ohne Zahl) · Aktivität. Pflegegrad nur in TalkBack-Ansage und Detail.
- Ganze Karte antippbar → Detail. Schnellaktion ist ein eigenes Touch-Ziel (≥ 48 dp).
- Semantik zusammengefasst: „Werner Hoffmann, Zimmer 2.03, dringend, 1 Hinweis, Diabetes Typ 2“.
- Zustände: Standard · gedrückt · ausgewählt (Tablet, `primary`-Rand 2 dp) · Vorschicht („Aus Nachtdienst“).

## SeverityBadge

Container + Icon + Label aus `LumaTheme.status`, Radius 8, `labelLarge` (14 sp). Niemals nur ein Farbpunkt.

## UpcomingRow (`section_soon`)

Uhrzeit (`titleMedium`, `tnum`) · Titel · „Name · Zimmer“ · „in 15 Min.“. Hat die Person einen offenen
Hinweis: eine Zeile „Offener Hinweis bei Margarete Fischer“ (Tap → Person), Text nicht wiederholen,
nie „Konflikt“.

## ShiftTimeline (`shift_timeline`)

Karte „Schichtverlauf ›“, als Ganzes ein Tap-Ziel (öffnet den Schichtverlauf), einzelne Blöcke nicht antippbar.
Nacht (20:00–06:00) als ein Block, danach ein Block pro Stunde bis Schichtende, je ≥ 36 dp breit.
Zahl im Block = alle Hinweise der Stunde (auch erledigte); Tönung und Icon nach höchster **offener**
Dringlichkeit, vollständig erledigte Stunden neutral. Darunter Routinen als Ticks, Jetzt-Marker.
Keine Legende auf dem Dashboard. Der Jetzt-Marker liegt hinter den Kacheln (nur Punkt darüber und Strich
in der Tick-Fläche sichtbar), nie über Zahl oder Icon. Semantik am klickbaren Knoten: eine Zusammenfassung
plus `Role.Button`, z. B. „Schichtverlauf, 2 dringende Hinweise, nächste Routine 07:30 Hörgeräte einsetzen“.

## SubpageAppBar

Appbar für Detail und Schichtverlauf: Fläche `secondary`/`onSecondary`, Zurück 48 dp („Zurück“), Titel optional.
Im zweispaltigen Detail ohne Zurück, nur als Abstand unter `safeDrawing`.

## ShiftHistory (`shift_history`)

Eigener Screen, Appbar als `secondary`-Fläche wie der ShiftHeader (Zurück 48 dp). Oben die Blockreihe mit
Legende, darunter chronologisch, jüngste oben, gruppiert (`history_group_current`, `history_group_previous`).
Eintrag: Uhrzeit · Dringlichkeit (Icon + Text) · Name · Zimmer · Originaltext 16 sp ungekürzt ·
„Erledigt“ bzw. „Aus Nachtdienst“ (gleiche Regel wie die PersonCard). Routinen bis jetzt als „geplant“,
keine Aktivitäten. Öffnet beim erneuten Aufruf oben bei „jetzt“.

## ContextChip

Neutral (`surface` + `outline`-Rand), Icon optional, `labelLarge` (14 sp). Nur Fakten aus den Daten:
`health_notes`, Routine heute ±60 min, Aktivität in der Schicht, weitere Events der Person, „Luma nicht erreicht“.

## EventItem

Kopfzeile: „Luma-Hinweis · Gespräch 06:30 · vor 45 Min.“ (`bodyMedium`, muted). Quelle nach Typ:
`missed_call` → „Anruf“, `family_message` → „Nachricht“, sonst „Gespräch“.
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
- `ErrorState` (`error_state`) wenn der Import scheitert: „Daten konnten nicht geladen werden“ + „Erneut versuchen“ (`action_retry`).
- `EmptyState` (`empty_state`) **nur** nach abgeschlossenem Laden: Icon `task_alt`,
  „Alles erledigt“ + „Gerade braucht niemand deine Aufmerksamkeit.“
