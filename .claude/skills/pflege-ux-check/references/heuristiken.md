# Heuristiken

Referenzdaten: `meta.now` = Mo 12.10.2026, 07:15, Frühdienst.

## 1. 5-Sekunden-Test

- Erkennt man ohne Scrollen, **wer jetzt Aufmerksamkeit braucht und warum**?
- Erwartung mit den Seed-Daten: Werner Hoffmann (2.03, dringend, Zittern/Schwitzen) und Rudolf Schmidt
  (2.12, dringend, Brustschmerz aus dem Nachtdienst) stehen ganz oben unter „Jetzt handeln“.
- Ist der Grund auf der Karte lesbar (Originalaussage), oder muss man erst tippen?
- Konkurriert etwas visuell mit „Jetzt handeln“ (Gelb, große Kennzahlen, Dekoration)?

## 2. Eine Hand, im Gehen

- Primäre Aktionen im unteren Drittel, mit dem Daumen erreichbar?
- Touch-Flächen ≥ 48 dp, genug Abstand zwischen „Abschließen“ und „Zurückgeben“?
- Kommt man ohne Tippen von Freitext durch den Flow (Textbausteine)?
- Keine Gesten als einziger Weg (Wischen zum Erledigen nur zusätzlich).

## 3. Unterbrechung

- Was passiert bei Rotation, App-Wechsel, Prozess-Tod mitten in der Notiz? Entwurf noch da?
- Sieht man nach der Rückkehr sofort, wo man war (gewählte Person, offene Sheets)?
- Statusänderung sofort gespeichert, mit „Rückgängig“ statt Bestätigungsdialog?
- Doppeltes Tippen auf „Übernehmen“ erzeugt keinen doppelten Verlaufseintrag.

## 4. Priorität und zwei Achsen

- Dringlichkeit und Bearbeitungsstand getrennt: Verschwindet ein dringender Vorgang „in Bearbeitung“
  (Rudolf, Brustschmerz) **nicht** aus „Jetzt handeln“?
- Vorgänge aus dem Nachtdienst als solche erkennbar, Übernahme ausdrücklich?
- Reihenfolge innerhalb eines Abschnitts nachvollziehbar (unbeachtet → Vorschicht → eigene, älteste zuerst)?
- Routine „Blutzucker messen 07:00“ und „Hörgeräte 07:30“ unter „Demnächst“; Sonntags-Routine (rt04) fehlt am Montag.
- Aktivitäten nach Schichtende (15:00, 16:30) nicht als „Demnächst“.

## 5. Sprache: Hinweis statt Diagnose

- Nur Fakten aus den Daten: „Luma-Hinweis · Gespräch 06:30“ + Originalaussage.
- Keine Diagnosen oder Wertungen („Unterzucker?“, „Sturzgefahr!“, „überfällig“).
- Du-Form, Verben auf Buttons, Zeit relativ + absolut, voller Name + Zimmer, keine Anrede.
- Fachfremdes (Zeitung, Essenswunsch, Anruf beim Sohn) als „Weiterleiten“ angeboten, nicht als Pflegeaufgabe.

## 6. Edge Cases und Zustände

- Ladezustand beim ersten Start; „Alles erledigt“ erscheint **nicht** während des Imports.
- Leerer Abschnitt entfällt, statt „0“ anzuzeigen.
- Pflichtnotiz leer oder zu kurz → Inline-Fehler, kein Dialog, Eingabe bleibt erhalten.
- Lange Namen, viele Hinweise bei einer Person („+ 2 weitere“), Schicht beendet nach 14:00.
- Gleiche Zeitstempel: stabile Reihenfolge über Neustarts.

## 7. Barrierefreiheit

- Status nie nur über Farbe (Icon + Label vorhanden?).
- Kontrast Text ≥ 4,5 : 1, UI ≥ 3 : 1 (im Zweifel mit
  `.claude/skills/luma-design-system/scripts/contrast.py` nachrechnen).
- Schriftgröße 200 %: nichts abgeschnitten, keine überlappenden Elemente, Buttons noch erreichbar.
- Dark Mode: Statusfarben unterscheidbar, kein grelles Weiß.
- TalkBack: Karte als eine sinnvolle Ansage, Icons mit Bedeutung beschriftet.

## 8. Phone und Tablet

- Phone (Compact): eine Spalte, nichts horizontal abgeschnitten, Querformat brauchbar.
- Tablet (Expanded): List-Detail, gewählte Person in der Liste markiert, Detail ohne Auswahl mit Hinweis.
- Keine über die volle Tablet-Breite gestreckten Karten oder Textzeilen.
- Wechsel der Fenstergröße bzw. Rotation verliert weder Auswahl noch Notizentwurf.
- Tablet-Szenario Übergabe: Lässt sich am Schichtbeginn in einem Blick erfassen, was aus dem Nachtdienst offen ist?
