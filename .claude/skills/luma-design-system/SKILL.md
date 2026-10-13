---
name: luma-design-system
description: Designsystem von Luma Care – Farb- und Status-Tokens, Typografie, Abstände, Komponenten-Anatomie, adaptives Layout, Sprache, Barrierefreiheit. Verwenden bei jeder UI-Arbeit – Screens, Komponenten, Theme, UI-Texte, Farben, Icons, Previews.
---

# Luma Design System

Ziel: Eine Pflegekraft erkennt im Gehen, mit einer Hand, in 5 Sekunden, was als Nächstes wichtig ist.

## Regeln, die immer gelten

1. **Tokens statt Werte:** `MaterialTheme.colorScheme/typography/shapes`, `LumaTheme.status`, `LumaTheme.spacing`.
2. **Farbe nie allein.** Status immer als Icon + Label + Farbe (`SeverityBadge`).
3. **Gelb (`accent`) nie als Statusfarbe.** Nur für den „neu“-Punkt und sparsame Akzente.
4. **Touch-Flächen ≥ 48 dp**, primäre Aktionen unten (`BottomActionBar`), im Daumenbereich.
5. **Kontrast:** Text ≥ 4,5 : 1, UI-Elemente ≥ 3 : 1. Neue Farbpaare mit dem Skript prüfen.
6. **Flach:** tonale Flächen und Ränder statt Schatten.
7. **Bewegung** 150–250 ms, nur für Statuswechsel und Umsortierung; „Animationen entfernen“ respektieren.
8. **Phone und Tablet:** Layout nach `WindowSizeClass` (siehe `references/components.md`).
9. **Previews:** Light und Dark, relevante Zustände, `fontScale = 2f`; Screens zusätzlich als Tablet
   (`device = Devices.TABLET`).

## Sprache

- Du-Form, warm und knapp.
- Buttons sind Verben: „Übernehmen“, „Übernahme bestätigen“, „Abschließen“, „Weiterleiten“, „Rückgängig“.
- **Hinweis statt Diagnose:** „Luma-Hinweis · Gespräch 06:30“ und darunter die Originalaussage.
  Nie „Hypoglykämie?“, „Sturzgefahr!“ o. ä.
- Zeit relativ und absolut: „vor 45 Min. · 06:30“.
- Bewohnende mit vollem Namen und Zimmer („Werner Hoffmann · 2.03“), keine Anrede (kein Geschlechtsfeld).

## Barrierefreiheit

- `PersonCard` als eine Ansage (`semantics(mergeDescendants = true)`).
- Rein dekorative Icons: `contentDescription = null`; Icons mit Bedeutung bekommen einen Text.
- Schriftgröße 200 %: nichts abgeschnitten, Karten wachsen in der Höhe.

## Referenzen

- `references/tokens.md`: alle Werte und `LumaTheme`. Vor Arbeit an `ui/theme/` lesen.
- `references/components.md`: Anatomie, Zustände, adaptives Layout. Vor Arbeit an Komponenten lesen.
- `scripts/contrast.py`: ohne Argumente alle Token-Paare, mit `"#FG" "#BG"` ein Paar.
