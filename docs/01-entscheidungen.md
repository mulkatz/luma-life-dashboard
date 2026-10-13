# Entscheidungen

Verdichtete Begründungen zu Produkt, Daten und Technik. Die ausführliche Herleitung steht in der
[Planung](00-planung.md); dieses Dokument wiederholt sie nicht, sondern nennt das Ergebnis und den Grund.
Kennzeichnung: **[R]** = belegt (Case, Seed, Recherche), **[A]** = eigene Annahme.

## Produkt und UX

**Leitfrage:** *Wer braucht jetzt meine Aufmerksamkeit, und warum?* Die App ist ein Arbeitsassistent
für die Pflegefachkraft, den Luma mit Informationen versorgt. Herleitung: [Planung §2–§4](00-planung.md).

| Entscheidung | Begründung |
|---|---|
| Kern-Flow „Anliegen übernehmen → bearbeiten → abschließen“ | Der tägliche Kernvorgang. Die Seed-Daten tragen ihn vollständig (`severity`, `status`, Vorgänge aus dem Nachtdienst). |
| Personen statt Events | Die Pflegekraft geht zu Menschen, nicht zu Ereignistypen. Eine Karte bündelt alle offenen Hinweise einer Person; der Abschnitt richtet sich nach der höchsten offenen Dringlichkeit. |
| Dringlichkeit ≠ Zuständigkeit | Ein dringender Fall aus dem Nachtdienst (Rudolf Schmidt, `in_progress`) bleibt unter *Jetzt handeln*, markiert mit „Aus Nachtdienst“, bis die Frühschicht ihn mit „Übernahme bestätigen“ übernimmt. |
| Hinweis statt Diagnose | Die App zeigt „Luma-Hinweis · Gespräch 06:30“ und die Originalaussage. Keine klinische Bewertung, keine abgeleiteten Behauptungen. |
| Nur Fakten aus den Daten | Kein „überfällig“ bei Routinen, weil Durchführungsdaten fehlen; vergangene Routinen heißen neutral „geplant“. „Offener Hinweis bei …“ statt „Konflikt“. Bei Angehörigen-Nachrichten „Nachricht“ statt „Gespräch“. Chips ohne Zählwert („Luma nicht erreicht“), weil sich „zweimal“ nur aus dem Freitext ergibt. |
| Übernahme pro Hinweis | Status und Verlauf liegen pro Event; eine Person kann einen übernommenen und einen offenen Hinweis haben. |
| Rückgängig statt Bestätigungsdialog | Dialoge kosten im Gehen Zeit. Jede Aktion wirkt sofort, die Snackbar bietet „Rückgängig“. |
| Pflichtnotiz nur, wo nötig | Bei dringenden und Gesundheitshinweisen sowie beim Weiterleiten dringender Hinweise. Textbausteine („Gesehen, wohlauf“, „Arzt informiert“ …) machen die Notiz im Gehen möglich. Regeln: [Planung §5](00-planung.md). |
| Ruhe vor Vollständigkeit | Nach der UX-Analyse des Zielbilds: eine Karte zeigt nur den maßgeblichen Hinweis, höchstens zwei Chips, nichts unter 14 sp. Abnahmekriterium: Beide Karten unter *Jetzt handeln* sind auf dem Phone (411 × 891 dp) ohne Scrollen sichtbar. |
| Kennzahlen-Zeile entfällt | Ohne Filter doppelt sie nur die Zählung der Abschnitte. Sie kommt mit Filtern zurück (Roadmap). |
| Schichtverlauf als Stundenblöcke | Nutzer-Einwand: „Alles erledigt“ reicht nicht, die Schicht muss nachvollziehbar sein. Die Karte zeigt die Verteilung, der Screen die chronologische Liste. |
| Zustände ausdrücklich | „Alles erledigt“ erst nach dem Import, nie während des Ladens. Scheitert der Import, erscheint „Daten konnten nicht geladen werden“ mit „Erneut versuchen“ statt eines Absturzes. |
| Tablet als List-Detail | Ab 840 dp Breite Dashboard links, Detail rechts; verzweigt nur nach `WindowSizeClass`. Zurück verlässt auf dem Tablet die App, statt durch frühere Auswahlen zu blättern. |
| Designsystem aus der Marke | Markenfarbe Petrol, im Light Mode eine dunklere Stufe für ausreichenden Kontrast. `primary` als Textfarbe nur auf `surface` (auf `background` nur 4,37 : 1). Status immer als Icon + Label + Farbe. Manrope gebündelt. Details: [Planung §7](00-planung.md), Skill `luma-design-system`. |

## Annahmen

| Annahme | Art | Folge |
|---|---|---|
| Eine Pflegefachkraft betreut 10–20 Bewohnende, nutzt das Smartphone im Gehen | [R] Case | Einhandbedienung, Aktionen unten, 48 dp |
| Frühdienst 06:00–14:00, „jetzt“ = Mo, 12.10.2026, 07:15 | [R] Seed | Uhr startet dort und läuft ab dem ersten Start mit |
| Durchführungsdaten zu Routinen fehlen | [R] Seed | Kein „überfällig“, nur „geplant“ |
| Ab Android 16 ignoriert das System auf großen Bildschirmen feste Ausrichtung | [R] Recherche | Adaptives Layout statt Hochformat-Sperre |
| Im Stationszimmer liegt ein Tablet für Schichtbeginn und Übergabe | [A] | List-Detail ab 840 dp |
| Kein Login; Zuständigkeit wird als Schicht gespeichert (Frühdienst, Nachtdienst) | [A] | Der Verlauf nennt Schichten, keine Namen |
| `in_progress` vor Schichtbeginn gehört dem Nachtdienst | [A] | Import setzt `assigned_to = nachtdienst`; „Aus Nachtdienst“ hängt an der Zuständigkeit, nicht am Zeitstempel |
| `in_progress` nach Schichtbeginn: Zuständigkeit unbekannt | [A] | Der Import lehnt den Fall ab, statt ihn still der laufenden Schicht zuzuordnen |
| „Diabetes Typ 2“ unter `preferences` ist eine Gesundheitsangabe | [A] | Importer verschiebt sie nach `health_notes`; die Seed-Datei bleibt unverändert |
| Notizen sind Übergabe-Notizen, keine Pflegedokumentation | [A] | Kurz, mit Textbausteinen, ab 3 Zeichen gültig |
| Weiterleiten betrifft Anliegen, Essenswünsche und Angehörigen-Nachrichten | [A] | Ziele Küche, Haustechnik, Sozialdienst, Verwaltung; gespeichert und angezeigt, nicht versendet |
| Der Schichtverlauf beginnt um 20:00 des Vortags | [A] | Nacht als ein Block, danach ein Block pro Stunde bis Schichtende |

## Datenmodell

Der Seed wird beim Build unverändert als Asset gebündelt und beim ersten Start in **einer Transaktion**
importiert; die Seed-Version wird erst nach dem Commit gesetzt. Danach liest die App nur aus Room.

| Tabelle | Inhalt | Erweiterung gegenüber dem Seed |
|---|---|---|
| `resident` | Stammdaten, Biografie, Vorlieben, Kontakt | `health_notes` (aus `preferences` abgeleitet) |
| `luma_event` | Luma-Hinweise mit `severity` und `status` | `assigned_to` (Schicht), `resolution`, `updated_at` |
| `routine` | Routinen; `days` als Menge von Wochentagen | – |
| `activity` | Angebote; Anmeldungen als Liste per Converter | – |
| `event_action` | Verlauf jeder Aktion mit Notiz und Weiterleitungsziel | neu; enthält den vorherigen Zustand (`prev_status`, `prev_assigned_to`, `prev_resolution`) für Rückgängig |

- **Zeiten** als `LocalDateTime` ohne Zone, gespeichert als ISO-String mit Sekunden (sortierbar).
- **Schreibpfad:** optimistisches Sperren (die UI übergibt den gesehenen Zustand) plus bedingtes Update
  im DAO; die Anzahl betroffener Zeilen entscheidet über Erfolg oder „Wurde inzwischen geändert“.
  Das deckt Doppeltipp und veraltete Ansichten ab.
- **Reihenfolge** von Aktionen nach ID, nicht nach Zeit: Die Schichtuhr kann bei zurückgestellter
  Systemzeit rückwärts laufen, die ID steigt immer.
- **Migration:** Schema v2 ergänzt `event_action.forwarded_to` per Auto-Migration; Schemas liegen in `app/schemas/`.
- **Room 2.8.5 statt 3.0:** weniger Setup-Risiko im knappen Budget. Die Repository-Schicht hält einen
  späteren Wechsel und eine echte API lokal.

## Kanalstrategie Native und Web

1. Die Pflegekraft im Gehen bekommt eine native App: Einhandbedienung, lokale Datenbank, später Push.
2. Dieselbe App deckt das Tablet im Stationszimmer ab; das Layout passt sich der Fensterbreite an.
3. Eine Web-Oberfläche ergänzt das für Rollen am Schreibtisch, etwa Wohnbereichsleitung oder die Empfänger von Weiterleitungen.
4. Gemeinsame Grenze ist die Repository-Schicht: Dort würde eine API angebunden, die beide Kanäle bedient.
5. Ein Angehörigen-Kanal ist ein eigener Kanal mit eigener Zielgruppe und nicht Teil von Luma Care (Roadmap).

## Roadmap

Bewusst nicht im Umfang, in grober Reihenfolge:

1. **Login und Rollen**, danach Namen im Verlauf statt nur der Schicht.
2. **Synchronisation** zwischen Geräten und Schichten über eine API hinter der Repository-Schicht.
3. **Schichtübergabe per LLM** aus den Luma-Hinweisen und dem Verlauf, mit Datenschutzkonzept
   (Pseudonymisierung ist keine Anonymisierung).
4. **Kennzahlen-Zeile mit Filtern** und „Alle Bewohnenden“ als eigene Liste.
5. **Zweiter Flow:** Routine mit Luma-Anruf anlegen, mit Prüfung auf Überschneidungen.
6. **Echte Weiterleitung** an Fachbereiche, Angehörigen-Kanal, Push-Benachrichtigungen, Gesprächsimpulse aus positiven Stimmungen.
7. **Qualität:** Smoke-Test auf dem Emulator (Maestro), Screenshot-Tests, Lücke oben im Tablet-Layout schließen.
8. **Regulatorische Einordnung** prüfen (Medizinprodukte-Software bei Priorisierung von Gesundheitshinweisen).
9. **Nutzertests** mit Pflegefachkräften.
