# Luma Care

Android-App (Kotlin, Compose, Room) für Pflegefachkräfte: priorisierte Luma-Hinweise und der Flow
„Anliegen übernehmen → bearbeiten → abschließen“. Phone und Tablet.
UI-Texte, Kommentare, Doku: Deutsch. Bezeichner: Englisch.

## Befehle

```sh
./gradlew assembleDebug        # bauen
./gradlew testDebugUnitTest    # alle Tests (Unit, Room in-memory, Robolectric-E2E), ohne Emulator
./gradlew lint                 # Android Lint
```

## Architektur

- Ein Modul, Schichten `data/` (local, seed, repository) → `domain/` → `ui/`. UI kennt nur Repositories und Domain-Modelle.
- Manuelle DI über `AppContainer`, kein Hilt/Koin.
- Farben, Typografie, Formen, Abstände als Tokens in `ui/theme/`; keine Hex-Werte außerhalb davon.
- „Jetzt“ kommt nur aus `ShiftClock`, nie aus `LocalDateTime.now()`. Zeiten sind `LocalDateTime` ohne Zone.
- Statusänderungen nur über `EventTransitions` und bedingte Updates (`WHERE status = :expected`).

## Arbeitsprinzipien

1. Annahmen und Unklarheiten zuerst nennen, nicht still entscheiden.
2. Die einfachste Lösung, die die Anforderung erfüllt. Keine spekulativen Abstraktionen.
3. Nur ändern, was für die Aufgabe nötig ist. Kein Umbau nebenbei.
4. Ziel als prüfbaren Test formulieren; fertig ist es, wenn Build und Tests grün sind.

## Harte Regeln

- Nur fiktive Daten. Keine echten Personen- oder Gesundheitsdaten ergänzen.
- Hinweis statt Diagnose: zeigen, was Luma gehört hat, keine klinischen Bewertungen.
- Nur Fakten aus den Daten. Nichts behaupten, was die Daten nicht hergeben (z. B. „überfällig“).
- `docs/case/luma-seed.json` bleibt unverändert; Mapping passiert im Importer.

## Commits

Conventional Commits, alles lowercase, keine Emojis, kein Co-Author-Trailer:
`<type>(<scope>): <beschreibung>`, Typen `feat fix test refactor docs build ci chore style`.

## Mehr

Konventionen in `.claude/skills/` (UX-Review manuell: `/pflege-ux-check`).
Produktkonzept und Priorisierungsregeln: `docs/00-planung.md`.
