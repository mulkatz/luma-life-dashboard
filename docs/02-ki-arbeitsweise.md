# KI-Arbeitsweise

Luma Care ist mit Claude Code entstanden. Dieses Dokument zeigt, wie die Arbeit gesteuert wurde,
wo die KI korrigiert werden musste und was das gekostet hat. Die Steuerungsdateien liegen im
Repository: [CLAUDE.md](../CLAUDE.md), [.claude/skills/](../.claude/skills/), [.claude/agents/](../.claude/agents/).

## Aufbau der Steuerung

| Ebene | Inhalt | Zweck |
|---|---|---|
| [CLAUDE.md](../CLAUDE.md) | Befehle, Architekturregeln, Arbeitsprinzipien, harte Regeln, Commit-Konvention; rund 50 Zeilen | Wird in jeder Sitzung geladen und bleibt deshalb schlank. Keine Zeitpläne, keine Designdetails. |
| Skill `luma-design-system` | Tokens, Typografie, Komponenten-Anatomie, Dichte-Regeln, Sprache, Barrierefreiheit; Skript zur Kontrastprüfung | Lädt bei UI-Arbeit automatisch |
| Skill `android-feature` | Reihenfolge Domain → DAO → Repository → ViewModel → Screen, Room- und Compose-Regeln, Testmuster, Definition of Done | Lädt bei Feature-Arbeit automatisch |
| Skill `pflege-ux-check` | Heuristiken aus Sicht einer Pflegefachkraft, arbeitet mit Emulator-Screenshots | Nur manuell, in eigenem Kontext |
| Agent `researcher` | Versionen und Doku aus Primärquellen | Hält Recherche aus dem Hauptkontext |
| Agent `implementer` | Setzt ein Arbeitspaket mit Ziel, Umfang und Abnahmekriterium um | Hält Code und Build-Logs aus dem Hauptkontext |
| Agent `reviewer` | Prüft den Diff unabhängig gegen Konventionen und Produktregeln, ändert nichts | Zweite Meinung ohne Eigeninteresse am Code |
| Agent `verifier` | Build, Tests, Lint; meldet nur das verdichtete Ergebnis | Frischer Lauf, unabhängig vom Implementer |

**Rollen:** Die Hauptsitzung plant, entscheidet, integriert und committet. Die Agenten arbeiten
nacheinander am selben Arbeitsbaum, parallel nur auf getrennten Dateien.

## Arbeitszyklus pro Paket

1. **Auftrag** an den Implementer mit Ziel, Umfang und prüfbarem Abnahmekriterium.
2. **Implementer** setzt um, schreibt Tests und prüft mit Gradle.
3. **Reviewer** prüft den Diff gegen `CLAUDE.md`, Skills und Planung.
4. **Korrekturen** durch den Implementer.
5. **Verifier** baut und testet in einem frischen Lauf.
6. **Emulator-Blick** durch die Hauptsitzung, Phone und bei Layout-Änderungen Tablet.
7. **Atomare Conventional Commits**, je ein Thema pro Commit (siehe `git log --oneline`).

## Konzept vor Code

Vor dem Dashboard-Code entstand ein statisches HTML-Zielbild für Dashboard, Detail und Schichtverlauf.
Eine UX-Analyse fand darin neun Befunde, unter anderem: Nur eine der zwei dringenden Karten lag über
der Falz, 6-px-Marker waren nicht unterscheidbar, 12-px-Text, „Neu“ stand überall. Daraus entstand
das [Zielbild v2](zielbild/zielbild.html) (v1 liegt in der Git-Historie). Die Regeln gingen ins
Designsystem, mit dem Grundsatz: **Das Zielbild gibt Inhalte vor, nicht die Dichte.**

## Korrekturmomente

Momente, in denen der Plan, das Zielbild oder generierter Code nicht stimmten. Die Korrekturen aus
der Planungsphase (Codex-Entwurf, erster Review-Agent) stehen in [Planung §12](00-planung.md).
„Von wem“ nennt die Quelle nur, wo sie festgehalten ist; „—“ heißt: nicht zugeordnet. „Emulator“ heißt:
beim Blick der Hauptsitzung auf das laufende Gerät gefunden.

| Phase | Was gefunden | Von wem | Folge |
|---|---|---|---|
| Setup | `primary` auf `background` nur 4,37 : 1; `outline` fehlte im Dark Mode | Kontrastskript | Regel „`primary`-Text nur auf `surface`“; `outline` im Dark Mode ergänzt |
| Setup | Plan-Versionen veraltet (Gradle 9.6 → 9.7.1, Kotlin 2.4.21, Room 2.8.5); Risiko „KGP 2.4 nur bis AGP 9.3 getestet“ | Researcher | Versionen korrigiert, Fallback definiert, nicht gebraucht |
| Setup | Fehlende Material-Farbrollen (Rückfall auf das lila Baseline-Schema), `labelSmall` 11 sp unter dem Minimum, unnötige Abhängigkeiten | Reviewer | Farbrollen ergänzt, `labelSmall` auf 12 sp, Abhängigkeiten bereinigt |
| Setup | `inversePrimary` im Dark Mode fiel durch (3,93 : 1) | Kontrastskript | Neuer Wert `#256A70` |
| Setup | Robolectric 4.17 braucht auf SDK 37 Espresso 3.7.0 und unter JDK 25 zusätzliche JVM-Argumente | Implementer | Build-Konfiguration angepasst |
| Setup | Agenten, die während einer Sitzung angelegt werden, lädt Claude Code erst in der nächsten Sitzung | Hauptsitzung | Neue Agenten sind erst ab der nächsten Sitzung nutzbar |
| Room/Seed | Ein `in_progress`-Event nach Schichtbeginn wäre still der aktuellen Schicht zugeordnet worden; die Daten geben das nicht her | Reviewer | Import lehnt den Fall ab |
| Room/Seed | Der Rollback-Test wäre auch grün gewesen, wenn das Leeren außerhalb der Transaktion läge | Reviewer | Test „erst gültig importieren, dann fehlerhaft neu importieren“ |
| Room/Seed | Plan und Code widersprachen sich bei „Uhr läuft nie rückwärts“ | Reviewer | Plan präzisiert |
| Room/Seed | Begründete Abweichung vom Skill: Zeitformat mit Sekunden | Implementer | Skill angepasst |
| Prioritizer | Das Zielbild leitete „2× Luma nicht erreicht“ aus dem Freitext „zweimal“ ab, die Logik zählte 1 Event | Hauptsitzung (Abgleich Zielbild ↔ Prioritizer), Reviewer | Kennzahlen zählen Personen, Chips ohne Zahl |
| Prioritizer | „Konflikt“ war eine Bewertung, die die Daten nicht hergeben | Reviewer | Neutral „Offener Hinweis bei …“ |
| Prioritizer | Das Live-Umspringen mit der Uhr war ungetestet (konstante Uhr im Test) | Reviewer | Tick-Test |
| Prioritizer | Kein Fehlerzustand: Ein Importfehler hätte die App abstürzen lassen | Implementer (als offen gemeldet), Reviewer | Fehlerzustand mit „Erneut versuchen“ |
| Konzept | Einwand: „Alles erledigt reicht nicht, die Historie muss nachvollziehbar sein“ | Nutzer | HTML-Zielbild, Zeitleiste, Schichtverlauf, Personenverlauf |
| Konzept | Einwand: „Zielbild zu dicht“ | Nutzer | UX-Analyse mit neun Befunden, Zielbild v2, Dichte-Regeln im Designsystem; Kennzahlen-Zeile entfällt |
| Dashboard | Robolectric rechnet ohne Systemleisten; das Falz-Kriterium war nur im Test erfüllt (848 von 891 dp) | Reviewer, bestätigt im Emulator | Test zieht die Systemleisten ab |
| Dashboard | Der Jetzt-Marker lief durch Zahl und Icon | Emulator | Marker hinter die Kachel |
| Dashboard | TalkBack-Rolle „Schaltfläche“ fehlte | Hauptsitzung (uiautomator), Reviewer | Rolle ergänzt |
| Detail/Tablet | Im Desktop-Fenster des Tablets verdeckte die Titelleiste den Namen; nur im Emulator sichtbar | Emulator | `safeDrawing` und ein Regressionstest, der mit dem alten Code fehlschlägt |
| Detail/Tablet | „Gespräch“ bei Angehörigen-Nachrichten wäre eine falsche Behauptung gewesen | Reviewer | „Nachricht“ |
| Detail/Tablet | Ein Platzhaltersatz war faktisch falsch | Reviewer | Entfernt |
| Detail/Tablet | Zurück blätterte auf dem Tablet durch alle früheren Auswahlen | Reviewer | Zurück verlässt die App |
| Flow-Logik | Doppeltipp ließ sich ohne den gesehenen Zustand nicht als „veraltet“ erkennen | Implementer | Optimistisches Sperren plus bedingtes Update |
| Flow-Logik | Sortierung nach Zeit ist unsicher, weil die Schichtuhr bei zurückgestellter Systemzeit rückwärts laufen kann | Implementer (Hinweis), Hauptsitzung (Entscheidung); zweite Stelle: Reviewer | Sortierung nach ID an beiden Stellen |
| Flow-Logik | Weiterleiten bei `urgent` ohne Notiz widersprach Planung §5 | Reviewer | Pflichtnotiz |
| Flow-Logik | Regeln standen nur im Code | Reviewer | In die Planung übernommen |
| Flow-UI | „Rückgängig“ in der Snackbar wirkte im Emulator nicht, obwohl die Repository-Logik getestet war | Emulator | Unter Robolectric nicht nachstellbar, die genaue Ursache ist daher nicht belegt. Anfällig war, dass Undo nur lief, solange eine Coroutine auf das Ergebnis der Snackbar wartete. Ersetzt durch eine Snackbar aus dem ViewModel-Zustand, Undo direkt im Klick, Rückmeldung bei Fehlschlag; auf dem Gerät für Übernehmen und Abschließen bestätigt, im E2E-Test abgedeckt |
| Flow-UI | Im Test-Quellbaum lagen Kopien von Produktionsdateien (teils veralteter Stand) aus einer Worktree-Prüfung des Implementers | Reviewer (prüft den echten Arbeitsbaum, nicht den Bericht) | Kopien entfernt, Herkunft per Byte-Vergleich belegt |
| Abschluss | UX-Check (Skill `pflege-ux-check`) mit Emulator-Screenshots auf Phone, Tablet, Dark Mode und 200 % Schrift: 12 Befunde, u. a. bedienbare Ränder nur 1,4 : 1, „Demnächst“ bricht bei großer Schrift mitten im Wort, roter Streifen von der Kartenrundung abgeschnitten, sieben gefüllte Buttons konkurrieren mit „Jetzt handeln“, Sheet öffnet sofort rot | Agent mit dem Skill | Alle behoben; Ränder in `outline`, Button-Hierarchie, neutraler Pflichthinweis |
| Abschluss | Liste und Detail auf dem Tablet nicht unterscheidbar, Lücke und Stufe zwischen den Kopfbereichen, beim Start niemand ausgewählt | Nutzer | Getönte Liste mit Trennlinie, bündige Köpfe mit Namen rechts, oberste Person automatisch geöffnet |
| Prozess | CI blockiert durch das GitHub-Actions-Budget | Nutzer (Actions-Tab), Hauptsitzung (Diagnose) | Abrechnung, nicht der Workflow; geprüft wird lokal mit dem Verifier |
| Prozess | Sitzungslimits unterbrachen Agenten | Hauptsitzung | Mit Kontext fortgesetzt |
| Prozess | Fokus auf den Abschluss | Nutzer | Bewusste Entscheidung: außer dem E2E-Test keine weiteren automatisierten Tests |
| Prozess | Granularität der Commits | Nutzer | Atomare Commits als Regel |

## Was funktioniert hat und was nicht

**Funktioniert:**
- Reviewer finden Regelverstöße, die Tests nicht finden: falsche Behauptungen in UI-Texten,
  ein Test, der auch bei falschem Code grün gewesen wäre, ein ungetestetes Live-Verhalten.
- Harte Regeln in `CLAUDE.md` („nur Fakten aus den Daten“) machen solche Befunde überhaupt erst prüfbar.
- Agenten halten Logs, Dateiinhalte und Recherche aus dem Hauptkontext; die Hauptsitzung bleibt bei Entscheidungen.
- Ein Zielbild vor dem Code machte Dichteprobleme sichtbar, bevor sie in Compose umgesetzt waren.

**Nicht oder nur teilweise:**
- Robolectric ersetzt keinen Blick auf das Gerät. Die Titelleiste im Desktop-Fenster, der Jetzt-Marker
  und die Snackbar-Aktion fielen erst im Emulator auf; das Falz-Kriterium war nur im Test erfüllt,
  weil Robolectric ohne Systemleisten rechnet.
- Große Pakete brauchen mehrere Schleifen aus Review und Korrektur. Kleinere Aufträge wären schneller gewesen.
- Der Plan veraltet während der Umsetzung. Regeln, die nur im Code entstehen, müssen aktiv zurück in die Planung.

## Zeitprotokoll

Die Umsetzung (Setup bis Abgabe) hat knapp 4 Stunden gedauert und liegt damit im vorgesehenen Rahmen.
Die Planung lag davor ([Planung §10](00-planung.md)). Geplante Blöcke:

| Block | Plan |
|---|---|
| CLAUDE.md + 3 Skills | 15 min |
| Setup: Gradle, Versionen, Manrope, Theme, CI | 20 min |
| Room, Seed-Import, ShiftClock + Tests | 25 min |
| Prioritizer + Tests, Dashboard-UI (inkl. Zielbild und UX-Analyse) | 45 min |
| Detail + Flow + Undo + Verlauf | 40 min |
| E2E-Test + `/pflege-ux-check` + Korrekturen | 20 min |
| README + Doku | 15 min |
