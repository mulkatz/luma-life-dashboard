# Planung – Luma Care Dashboard (Android)

> Arbeitsdokument aus der Vorbereitung. Entstanden im Dialog mit Claude Code.
> Grundlage war ein Entwurf von Codex, der geprüft und korrigiert wurde. Anschließend hat ein
> separater Review-Agent die Planung unabhängig geprüft; seine Ergebnisse sind eingearbeitet (§12).
> Kennzeichnung: **[R]** = belegt (Case, Seed, Recherche), **[A]** = eigene Annahme.

---

## 1. Entscheidungen

| Thema | Entscheidung | Begründung |
|---|---|---|
| Plattform | **Android: Kotlin, Jetpack Compose, Room** | Vorgabe „nativ“ [R] |
| Formfaktoren | **Phone und Tablet**, adaptiv über `WindowSizeClass` (Compact/Medium eine Spalte, Expanded List-Detail) | Ab Android 16 ignoriert das System auf großen Bildschirmen feste Ausrichtung und Größensperren [R]; Tablet im Stationszimmer für Übergabe [A] |
| Kern-Flow | **Von Luma gemeldetes Anliegen übernehmen → bearbeiten → abschließen** | Der tägliche Kernvorgang, den die Seed-Daten vollständig tragen |
| Zweiter Flow | **Routine anlegen mit Prüfung auf Überschneidungen**, nur skizziert | Optionaler Pluspunkt [R] |
| LLM-Call | **Nur konzeptionell** (Roadmap) | Das Budget von 4 h insgesamt [R] lässt keinen Raum dafür |
| Zeit | **3–4 h insgesamt**, inklusive Planung, Skills und Doku | Antwort von Marcel & Marvin [R]; ehrliches Zeitprotokoll |
| Präsentation | Kein Video, **Vorstellung im Videocall** | Antwort von Marcel & Marvin [R] |
| Native vs. Web | **Kanalstrategie**, kurz in `01-entscheidungen.md` | Konstruktiv, ohne die Vorgabe zu diskutieren |
| Commits | **Conventional Commits, nur lowercase, keine Emojis, ohne KI-Co-Author-Trailer** | Lesbare Historie; KI-Einsatz wird in `docs/02-ki-arbeitsweise.md` transparent gemacht |
| Repository | **`mulkatz/luma-life-dashboard`** (privat), Produktname „Luma Care“ | Eigenes Logo, abgeleitet aus der Luma-Bildmarke (§7) |

---

## 2. Kontext

### Luma Life [R]
- KI-Begleiter für **Menschen mit Demenz** (Avatar, Gespräche, Erinnerungen, Gedächtnisspiele).
- Angehörige und Pflegekräfte werden „bei auffälligen Veränderungen informiert“ (lumalife.de). Das Dashboard liegt also nah an der Produktvision.
- Tonalität: Du-Form, warm; „Entlastung, Sicherheit, Verbundenheit“.

### Arbeitsalltag der Pflegekräfte
| Erkenntnis | Quelle |
|---|---|
| Schichtbetrieb mit Übergabe; Frühdienst 06–14 Uhr | [R] Seed |
| Eine Pflegefachkraft, 10–20 Bewohnende, Smartphone im Gehen | [R] Case |
| Häufige Unterbrechungen, fragmentierte Aufgaben | [R] Pflegeforschung, hier nicht quantifiziert |
| Schwindel beim Aufstehen = Sturzrisiko (Expertenstandard Sturzprophylaxe, DNQP) | [R] |
| Viele fachfremde Aufgaben (Zeitung, Küche, Organisatorisches) landen bei der Pflege | [A] |
| Notizen in der App sind **Übergabe-Notizen, keine Pflegedokumentation** (keine Doppeldokumentation) | [A] |
| Tippen im Gehen ist unrealistisch → Textbausteine statt Freitext-Pflicht | [A] |
| Smartphone im Gehen, **Tablet im Stationszimmer** (Schichtbeginn, Übergabe) | [A] |
| „Ich“ = feste, fiktive Nutzerin („Anna K., Pflegefachkraft“), kein Login | [A] |

### Nutzungsszenarien
1. **Schichtbeginn:** Was ist seit dem letzten Dienst passiert, was hat der Nachtdienst offen gelassen?
2. **Unterwegs:** 10 Sekunden zwischen zwei Zimmern. Wer ist als Nächstes dran? Unterbrechen und später fortsetzen.
3. **Nach der Versorgung:** Abschließen mit kurzer Notiz oder an den zuständigen Bereich weiterleiten.

---

## 3. Analyse der Seed-Daten [R]

`meta.now` = **Mo, 12.10.2026, 07:15**, Frühdienst 06:00–14:00, 15 Bewohnende, 26 Events.

| Person | Signal (verknüpft) | Konsequenz |
|---|---|---|
| **Werner Hoffmann** 2.03 | e005 `urgent` (Zittern/Schwitzen, nicht gefrühstückt) · „Diabetes Typ 2“ steht in *preferences* · Routine „Blutzucker messen“ 07:00 | Kontext am Event: `Diabetes Typ 2` · `Routine 07:00 Blutzucker`. **Keine Behauptung „überfällig“**, weil Durchführungsdaten fehlen |
| **Rudolf Schmidt** 2.12, PG 5 | e018 `urgent` Brustschmerz 23:15, `in_progress` („Nachtdienst informiert“) · e019 Morgengespräch 2× nicht angenommen | Vorgang aus der Vorschicht → **Übernahme bestätigen**; mehrere Signale bei einer Person |
| **Elfriede Schulz** 2.05 | e006 nachts unruhig (`in_progress`) · e007 Morgengespräch 06:45 verpasst | Gleiches Muster |
| **Gertrud Brandt** 2.04 | e001 Schwindel beim Aufstehen · Sitzgymnastik 10:30 | Sturzrisiko vor der nächsten Mobilisation |
| **Margarete Fischer** 2.09 | e014 Hörgeräte pfeifen · Routine „Hörgeräte einsetzen“ 07:30 · e013 Tochter kommt Samstag | Fällige Routine mit dem Hinweis verknüpfen |
| **Lieselotte Zimmermann** 2.16 | e024 müder als sonst · Vorliebe „Ruhe am Vormittag“ · Singkreis 11:15 | Konflikt sichtbar machen |
| **Hannelore Krüger** 2.07 | e009 niedergeschlagen (Sohn 3 Wochen nicht da) · e010 möchte in den Park · Singkreis 11:15, mag Singen | Sozial; der Singkreis ist ein positiver Anknüpfungspunkt |
| **Heinz Petersen** 2.01 | e003 möchte Sohn anrufen, kennt die Nummer nicht · Erinnerungscafé Hafen (Biografie: Hafenmeister) | `attention`, aber nicht pflegerisch → **weiterleiten** |
| **Horst Meyer** 2.10 | e015 Zeitung fehlt seit 2 Tagen · Routine „Nachrichten vorlesen“ 08:00 · Vorliebe „Klare Absprachen“ | `attention`, nicht pflegerisch → weiterleiten, verbindlich antworten |
| Essenswünsche, Angehörige, Heimbeirat | `info` | Organisatorisch, weiterleiten |

Weitere Beobachtungen:
- **Wochentagsfilter:** rt04 (nur `sun`) darf am Montag nicht erscheinen.
- Spielenachmittag 15:00 und Erinnerungscafé 16:30 liegen **nach Schichtende**.
- **Gleicher Zeitstempel:** e004/e023 und weitere Paare → deterministische Zweitsortierung nötig.
- Kein Geschlechtsfeld → keine Anrede, sondern voller Name und Zimmer. Kein Zimmer 2.13.
- Positive Stimmungen (Segeltour 1972, Backstube) → Idee „Gesprächsimpulse“ (Roadmap).

---

## 4. Produktkonzept

**Leitsatz:** Ein Arbeitsassistent für Pflegekräfte, den Luma mit Informationen versorgt.
Er beantwortet: *Wer braucht jetzt meine Aufmerksamkeit, und warum?*

**Prinzipien**
1. **Personen statt Event-Typen.** Granularität ist die **Personenkarte** mit allen offenen Events der Person.
2. **Zwei Achsen.** Dringlichkeit (`urgent/attention/info`) ≠ Zuständigkeit/Status. Dringendes verschwindet nie unter „in Bearbeitung“.
3. **Hinweis, keine Diagnose.** „Luma-Hinweis · Gespräch 06:30“ + Originalaussage. Nur Fakten aus den Daten, keine abgeleiteten Behauptungen.
4. **Kurze Zeit bis zur Handlung.** Designziel: in 5 Sekunden erkennen, was als Nächstes wichtig ist (bewusst gesetzt).
5. **Unterbrechungen tolerieren.** Status liegt sofort in der DB, Entwürfe überleben Rotation und Prozess-Tod, Rückgängig statt Bestätigungsdialog.

### Dashboard (von oben nach unten)
1. **Kopfbereich:** Ahorn · Frühdienst · 07:15 · noch 6:45 h.
2. **Kennzahlen-Zeile** (antippbar als Filter): `2 dringend · 7 zeitnah · 2 aus Nachtdienst · 2× Luma nicht erreicht`.
3. **Jetzt handeln:** Personen mit offenem `urgent`.
4. **Demnächst** (nächste 60 min): Routinen und Aktivitäten, Konflikte markiert (z. B. Hörgeräte 07:30).
5. **Zeitnah prüfen:** Personen mit `attention` als höchster offener Dringlichkeit.
6. **Organisatorisches:** Personen nur mit offenen `info`-Events, kompakt.
7. **Leerer Zustand** „Alles erledigt“ erscheint **nur nach** dem Laden, nie während des Imports.

**Tablet (Expanded, ≥ 840 dp):** Dashboard links, Detail der gewählten Person rechts (List-Detail).
Phone und kleine Fenster: eine Spalte, Detail als eigener Screen.

### Priorisierungsregeln (Domain, getestet)
- Abschnitt einer Person = höchste Dringlichkeit ihrer **offenen** Events (`status != done`).
- „Vorschicht“ = `in_progress && assigned_to != aktuelle Schicht`. Der Import setzt `assigned_to = nachtdienst`, wenn ein `in_progress`-Event vor `shift.start` liegt. Die Regel hängt also an der Zuständigkeit, nicht am Zeitstempel.
- Sortierung innerhalb eines Abschnitts: 1. `open` (unbeachtet) → 2. Vorschicht ohne Übernahme → 3. eigene → jeweils ältestes zuerst → Zweitschlüssel Zimmer und ID.
- Kontext-Chips nur aus Daten: `health_notes`, Routine heute ±60 min, Aktivität in der Schicht, weitere Events der Person, „Luma nicht erreicht“.
- Events mit Typ `request`, `meal_wish` oder `family_message` bekommen die Aktion **Weiterleiten**, unabhängig von der Dringlichkeit.

### UX-Regeln
Siehe Designsystem (§7). Kurz: eine Hand, Aktionen unten, 48 dp, Farbe nie allein, Dark Mode, große Schrift, positive leere Zustände.

---

## 5. Kern-Flow: Anliegen übernehmen → bearbeiten → abschließen

```
Dashboard ─tap Person─▶ Detail (Person, Hinweise, Kontext, Verlauf)
  [Übernehmen]               [Übernehmen] / [Übernahme bestätigen] (Vorschicht)
                             [Notiz]  Textbausteine-Chips + optional Freitext
                             [Abschließen ▾] Erledigt | Weitergeleitet an … | Kein Handlungsbedarf
◀── reaktiv aktualisiert ── Snackbar „Rückgängig“
```

**Übergänge**
| Von → Nach | Regel |
|---|---|
| `open → in_progress` | Übernehmen (`assigned_to = ich`) |
| `in_progress (Vorschicht) → in_progress (ich)` | Übernahme bestätigen |
| `in_progress → open` | Zurückgeben |
| `open → done` | `info`: direkt („Zur Kenntnis“); `attention`: mit Notiz |
| `in_progress → done` | Abschlussart wählen; **Notiz Pflicht** bei `health_hint` und `urgent`; „Kein Handlungsbedarf“ bei `urgent` nur mit Notiz |
| **Rückgängig** | Stellt den **vorherigen Snapshot** (status, assigned_to, resolution) aus `event_action` wieder her und entfernt die Aktion |

**Robustheit**
- **Bedingtes Update:** `UPDATE … WHERE id = ? AND status = :expected`; die Anzahl betroffener Zeilen wird ausgewertet. Das deckt doppeltes Tippen und veraltete Zustände ab.
- Notizentwurf über `SavedStateHandle`.
- Validierung: Pflichtnotiz leer oder < 3 Zeichen → Inline-Fehler, keine Dialoge.

---

## 6. Technik

### Stack (Stand 13.10.2026, beim Setup gegen maven.google.com verifizieren) [R]
| Komponente | Version |
|---|---|
| Android Studio | **Rabbit 1 (2026.2.1)**; installiert ist 2026.1 → Update nötig |
| JDK | **25 (LTS)**, festgelegt über `gradle/gradle-daemon-jvm.properties` (kein lokaler Pfad) |
| AGP / Gradle | 9.4 / 9.6 |
| Kotlin | 2.4.20 (KSP passend dazu) |
| Compose | BOM 2026.09.00 (Material 3) |
| Room | **2.8.4** (KSP). Bewusst nicht Room 3.0: weniger Setup-Risiko im knappen Budget; Repository-Schicht macht einen späteren Wechsel lokal |
| Serialisierung | kotlinx.serialization |
| minSdk / target | 26 / 37 (java.time ohne Desugaring) |

### Struktur (ein Modul, manuelle DI)
```
data/        local (Entities, DAOs, DB) · seed (DTOs, Importer) · repository
domain/      Prioritizer · EventTransitions · ShiftClock · Modelle für Event & Resident
ui/          theme (Tokens) · components · dashboard · detail
AppContainer
```

### Wichtige Entscheidungen
- **Zeitmodell:** durchgängig `LocalDateTime` ohne Zone, gespeichert als ISO-String (sortierbar). `days` wird auf `DayOfWeek` abgebildet.
- **ShiftClock:** `now = meta.now + (Systemzeit − Anker)`. Der Anker wird beim ersten Start **persistiert**, damit die Uhr Neustarts übersteht und nie rückwärts läuft. Zustand „Schicht beendet“ nach 14:00. Zurücksetzen: App-Daten löschen (README).
- **Zeit-Reaktivität:** Room-Flow `combine` Minuten-Ticker, damit „07:30 fällig“ live umspringt.
- **Seed-Import:** in **einer Transaktion**; `seed_version` erst nach dem Commit; Ladezustand bis dahin. Mapping-Regel im Importer: Gesundheitsangaben aus `preferences` → `health_notes`. Die Seed-Datei bleibt unverändert.
- **Datenmodell-Erweiterungen:** `resident.health_notes`, `luma_event.assigned_to / resolution / updated_at`, Tabelle `event_action` (Verlauf + Undo-Snapshot). Anmeldungen als Liste per Converter.
- **Repository-Schicht** als austauschbare Grenze für eine spätere API (Pluspunkt [R]).

---

## 7. Designsystem

Die vollständige Spezifikation liegt im Skill `luma-design-system` (`references/`). Hier die Eckpunkte.

### Farben – Marke [R, lumalife.de]
| Token | Light | Dark | Hinweis |
|---|---|---|---|
| `primary` | `#2E7F85` (4,68 : 1 auf Weiß) | `#4AAFB6` | Markenfarbe `#4AAFB6` hat auf Weiß nur 2,59 : 1 → im Light Mode dunklere Stufe. Als Textfarbe nur auf `surface` (auf `background` nur 4,37 : 1) |
| `onPrimary` | `#FFFFFF` | `#211E32` | `#4AAFB6` auf Aubergine: 6,25 : 1 |
| `secondary` / Kopfbereich | `#211E32` | `#211E32` | Aubergine |
| `accent` | `#FACF39` | `#FACF39` | sparsam, **nie** als Statusfarbe |
| `background` / `surface` | `#F7F7F9` / `#FFFFFF` | `#17151F` / `#211E32` | |
| `onSurface` / `muted` | `#211E32` / `#5C5A66` (6,75) | `#ECEAF4` (15,2) / `#A9A6B6` (7,58) | |
| `outline` | `#8E8B99` (3,33, UI ≥ 3 : 1) | `#807C92` (4,02) | |

### Farben – Status (Text/Icon auf Container, geprüft)
| Status | Light | Kontrast | Dark | Kontrast |
|---|---|---|---|---|
| `urgent` | `#B42318` auf `#FEE4E2` | 5,45 | `#FDA29B` auf `#3A1210` | 8,49 |
| `attention` | `#93370D` auf `#FEF0C7` | 6,62 | `#FEC84B` auf `#33230A` | 9,80 |
| `info` | `#256A70` auf `#E3F4F5` | 5,49 | `#7FD3D9` auf `#10282A` | 8,99 |
| `done` | `#067647` auf `#DCFAE6` | 5,11 | `#75E0A7` auf `#0B2A1B` | 9,53 |

Status wird immer mit **Icon + Label + Farbe** dargestellt (z. B. ⚠ „Dringend“).

### Typografie
**Manrope** (Markenschrift [R]) als gebündelte TTF (400/500/600/700), keine Downloadable Fonts.
| Rolle | Größe/Zeile | Gewicht | Einsatz |
|---|---|---|---|
| `headlineSmall` | 24/32 | 700 | Begrüßung, Detail-Titel |
| `titleMedium` | 18/24 | 600 | Personenname auf der Karte |
| `titleSmall` | 15/20 | 600 | Abschnittsüberschriften |
| `bodyLarge` | 16/24 | 400 | Luma-Aussage |
| `bodyMedium` | 14/20 | 400 | Kontext, Metadaten |
| `labelLarge` | 14/20 | 600 | Buttons |
| `labelMedium` | 12/16 | 600 | Badges, Chips |
Mindestgröße 12 sp; getestet bei Schriftgröße 130 % und 200 %.

### Raster, Formen, Bewegung
- **Abstände:** 4-dp-Raster: 4 · 8 · 12 · 16 · 24 · 32. Seitenrand 16, Kartenabstand 12.
- **Radien:** Chips/Badges 8 · Karten 16 · Bottom Sheets 28 · Buttons voll gerundet.
- **Höhe:** flach, tonale Flächen statt Schatten; Hervorhebung über Farbe und Rand.
- **Icons:** Material Symbols *Rounded* (passt zur Rundung von Manrope).
- **Adaptives Layout:** Breakpoints über `WindowSizeClass`: < 600 dp eine Spalte · 600–840 dp eine Spalte, max. 640 dp breit · ≥ 840 dp List-Detail (`NavigableListDetailPaneScaffold`). Nie nach Gerätetyp oder Ausrichtung verzweigen; Auswahl und Entwurf überleben den Wechsel.
- **Bewegung:** 150–250 ms, nur für Statuswechsel und Listen-Umsortierung; respektiert „Animationen entfernen“.

### Komponenten (Anatomie im Skill)
`ShiftHeader` · `MetricRow` · `SectionHeader(count)` · **`PersonCard`** (Name, Zimmer, PG, höchster Status, 1–3 Hinweise, Kontext-Chips, Schnellaktion) · `SeverityBadge` · `ContextChip` · `EventItem` · `BottomActionBar` · `ResolutionSheet` · `NoteInput` (Chips + Freitext) · `TimelineItem` · `EmptyState` · `LoadingState`.
Jede Komponente hat eine Preview in Light und Dark, mit Zuständen (Standard/gedrückt/deaktiviert/Fehler).

### Sprache und Ton
Du-Form gegenüber der Pflegekraft; Verben auf Buttons („Übernehmen“, „Abschließen“); Hinweis statt Diagnose; Zeit relativ + absolut („vor 45 Min. · 06:30“); Bewohnende mit vollem Namen.

### Barrierefreiheit
Text ≥ 4,5 : 1, UI ≥ 3 : 1 · Touch-Flächen ≥ 48 dp · TalkBack: zusammengefasste Semantik pro Karte, sinnvolle `contentDescription` · Farbe nie allein.

### Logo [A]
Eigene Bildmarke „Luma Care“, abgeleitet aus der Luma-Bildmarke: abgerundete Kachel, monolinearer offener Ring, Punkt als Person. Statt der Hand: zwei Listenzeilen (priorisierte Hinweise); gelber Punkt in der Ringöffnung = neuer Hinweis. Petrol-Kachel grenzt die professionelle Pflege-Variante von der gelben Consumer-App ab. Wortmarke in Manrope 700, als Pfade gesetzt. Dateien: `docs/assets/logo*.svg`.

### Umsetzung in Compose
`LumaTheme` = Material-3-`ColorScheme`/`Typography`/`Shapes` + `LumaStatusColors` per `CompositionLocal`. Tokens nur in `ui/theme/`; **keine Hex-Werte außerhalb von `theme/`**.

---

## 8. Teststrategie

| Ebene | Was | Werkzeug | Läuft |
|---|---|---|---|
| **Unit** | Prioritizer (Abschnitte, Sortierung, Zweitschlüssel), EventTransitions (erlaubt/verboten, Pflichtnotiz, Undo), ShiftClock (Anker, Neustart, Schichtende), Wochentagsfilter | JUnit + kotlin.test | JVM, Sekunden |
| **Integration** | SeedImporter (vollständig, idempotent, transaktional), bedingtes Update im DAO (betroffene Zeilen) | Room in-memory | JVM |
| **E2E (App-Ebene)** | Kern-Flow durch die echte UI: Start → Import → Werner unter „Jetzt handeln“ → Detail → Übernehmen → Notiz → Abschließen → Werner nicht mehr unter „Jetzt handeln“ → Activity neu erzeugen → Zustand bleibt | Compose UI Test + **Robolectric**, In-Memory-DB, feste Uhr; als Phone und Tablet (`@Config(qualifiers)`) | JVM, ohne Emulator |
| **CI** | `lint`, `testDebugUnitTest`, `assembleDebug` bei jedem Push | GitHub Actions | automatisch |
| *Optional* | Black-Box-Smoke-Test auf dem Emulator | Maestro (YAML) | Roadmap |

Grundsatz: Logik testen, nicht Pixel. Der E2E-Test sichert genau den einen Flow, der laut Case durchgängig funktionieren muss. Er findet Elemente über `testTag` und Semantik, was gleichzeitig der Barrierefreiheit hilft.

---

## 9. KI-Arbeitsweise

### CLAUDE.md (schlank, ca. 40 Zeilen)
- Ein Satz zum Projekt.
- Befehle: build, test, lint.
- Architekturregeln (5 Zeilen): Schichten, manuelle DI, Tokens nur in `theme/`, Zeit nur über `ShiftClock`.
- Arbeitsprinzipien in 4 Zeilen (nach Karpathy): erst Annahmen nennen · einfachste Lösung · nur nötige Änderungen · Ziel als Test.
- Harte Regeln: keine echten Daten · Hinweis statt Diagnose · nur Fakten aus den Daten.
- Commits: Conventional Commits, nur lowercase, keine Emojis.
- Verweis auf die Skills.

**Nicht** in der CLAUDE.md: Zeitpläne, Produktbegründungen, Designdetails, Glossar (→ Skills/Doku).

### Skills (`.claude/skills/`, Frontmatter `name` + `description`: „was + wann“, Details in `references/`)
| Skill | Inhalt | Aufruf |
|---|---|---|
| **`luma-design-system`** | Tokens, Typo, Abstände, Komponenten-Anatomie, Sprache, a11y-Regeln; `references/tokens.md`, `components.md`; **Skript zur Kontrastprüfung** | automatisch bei UI-Arbeit |
| **`pflege-ux-check`** | Heuristiken: 5-Sekunden-Test, eine Hand, Unterbrechung, Edge Cases, Sprache (keine Diagnosen), a11y; arbeitet **mit Screenshot** vom Emulator; liefert eine priorisierte Ergebnisliste | **manuell** (`/pflege-ux-check`), `disable-model-invocation`, in eigenem Kontext (unabhängiger Review) |
| **`android-feature`** | Wie Entität/DAO/Repository/ViewModel/Screen angelegt werden; Room-/Compose-Konventionen; Testmuster für Unit/Integration/E2E; Definition of Done: build + Tests grün + Screenshot angesehen | automatisch bei Feature-Arbeit |
| Eingebaut: `/code-review`, `/simplify` | Kritische Prüfung des generierten Codes | vor Commit-Blöcken |

### Sichtbarkeit der Steuerung
- Kleine, thematisch saubere Commits (ohne KI-Trailer; Transparenz über die Doku).
- `docs/02-ki-arbeitsweise.md`: **echte** Momente, in denen die KI korrigiert wurde, laufend notiert. Beispiele aus der Planung: Codex-Entwurf (Achsen vermischt, Video, LLM-Pluspunkt), Review-Agent (Undo-Semantik, Vorschicht-Regel, unbelegte Behauptung „überfällig“).
- Zeitprotokoll.

### Commit-Konvention
`<type>(<scope>): <beschreibung>`, alles lowercase, keine Emojis.
Typen: `feat` · `fix` · `test` · `refactor` · `docs` · `build` · `ci` · `chore` · `style`.
Beispiele: `build: set up compose, room and gradle daemon jvm 25` · `feat(dashboard): group open events by resident` · `test(domain): cover status transitions and undo`.

---

## 10. Zeitplan

Gesamtbudget **4 h**. Die Planung ist bereits erfolgt (Ist-Zeit: unter 45 min).

| Block | Dauer | Fertig, wenn … |
|---|---|---|
| CLAUDE.md + 3 Skills | 15 min | Dateien liegen im Repo, ich habe sie gelesen und gekürzt |
| Setup: Gradle, Versionen, Manrope, Theme, CI | 20 min | App startet im Luma-Theme, CI grün |
| Room, Seed-Import, ShiftClock + Tests | 25 min | Import- und Clock-Tests grün |
| Prioritizer + Tests, Dashboard-UI | 45 min | Werner und Rudolf oben, Kontext stimmt |
| Detail + Flow + Undo + Verlauf | 40 min | Flow persistiert, Dashboard reagiert |
| E2E-Test + `/pflege-ux-check` + Korrekturen | 20 min | E2E grün, Ergebnisse des UX-Checks bearbeitet |
| README + Doku + Zeitprotokoll | 15 min | Frischer Checkout baut und startet |
| **Summe** | **3:00 h** | + Planung ≈ 4 h |

**Kürzungen, falls die Planung länger als 60 min gedauert hat:**
1. Verlauf nur als Liste ohne Gestaltung.
2. Kennzahlen-Zeile ohne Filterfunktion.
3. Dark Mode nur über Tokens vorbereitet, nicht feinjustiert.
4. Tablet ohne List-Detail: auf allen Breiten eine Spalte, max. 640 dp breit und zentriert.

### Bewusst nicht im Umfang (Roadmap)
- Schichtübergabe per LLM (mit Datenschutzkonzept; Pseudonymisierung ist keine Anonymisierung)
- Push-Benachrichtigungen, Login/Rollen, Team-Synchronisation
- echte Weiterleitung, Angehörigen-Kanal, Gesprächsimpulse
- zweiter Flow (Routine anlegen)
- Maestro, Screenshot-Tests
- Prüfung der regulatorischen Einordnung (Medizinprodukte-Software bei Priorisierung von Gesundheitshinweisen)
- Nutzertests mit Pflegekräften

---

## 11. Dokumentation

```
README.md                  Executive Summary (5 Sätze) · Screenshots · Start: Android Studio Rabbit 1,
                           JDK 25 (automatisch via Gradle), minSdk 26/targetSdk 37, Seed-Import beim
                           ersten Start, Zurücksetzen über „App-Daten löschen“ · Tests ausführen
docs/00-planung.md         dieses Dokument
docs/01-entscheidungen.md  Produkt & UX, Annahmen [R]/[A], Datenmodell, Kanalstrategie (Native/Web, 5 Zeilen), Roadmap
docs/02-ki-arbeitsweise.md Skills, Steuerung mit echten Beispielen, Zeitprotokoll
CLAUDE.md · .claude/skills/
```

**Roter Faden für den Videocall:** Leitidee → Dashboard anhand von Werner und Rudolf → Flow live →
Datenmodell, Uhr, Tests → KI-Steuerung mit zwei Beispielen → Was fehlt und wie es weitergeht.

---

## 12. Review-Protokoll

**Codex-Entwurf, korrigiert:** Dringlichkeit und Status vermischt (Brustschmerz-Fall wäre verschwunden) ·
LLM-Pluspunkt übersehen · Luma nicht recherchiert · Discovery als Skill · Seed-Daten nicht analysiert ·
README-Pflichtangaben fehlten.

**Review-Agent, übernommen:**
- Zeitbudget ging nicht auf → gekürzt, LLM gestrichen
- Granularität Person vs. Event war unklar → Personenkarten
- Vorschicht-Regel hing am Zeitstempel → hängt jetzt an `assigned_to`
- Undo-Semantik → Snapshot
- unbelegtes „überfällig“ → gestrichen
- Uhr nach Neustart → persistierter Anker
- Ladezustand, Ticker, bedingtes Update, Zweitsortierung, Kennzahlen-Zeile, Textbausteine, `SavedStateHandle`, Wochentagsbeispiel rt04, Manrope gebündelt, `onPrimary` Aubergine

**Review-Agent, bewusst anders entschieden:**
- **`android-feature` bleibt als Skill.** Er hält Konventionen und Testmuster *aus* der CLAUDE.md heraus. Das ist genau das Ziel „schlanker Kontext“.
- **Kanalstrategie bleibt dokumentiert**, aber auf 5 Zeilen gekürzt.
- **`event_action` bleibt.** Sie trägt Verlauf *und* Undo.

**Gestrichen:** `contact_phone`, Edge Case „gehört jemand anderem“, Join-Tabelle für Anmeldungen, Rundgang-Sortierung.
