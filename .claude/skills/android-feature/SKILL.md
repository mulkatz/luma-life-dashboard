---
name: android-feature
description: Konventionen für Feature-Arbeit in Luma Care – Schichtreihenfolge Entität/DAO/Repository/Domain/ViewModel/Screen, Room- und Compose-Regeln, Testmuster (Unit, Room, Robolectric-E2E), Definition of Done. Verwenden beim Anlegen oder Ändern von Datenmodell, Datenzugriff, Geschäftslogik, ViewModels oder Screens.
---

# Android-Feature

## Reihenfolge

Von innen nach außen, jede Schicht mit Test:

1. **Domain** (`domain/`): reine Kotlin-Funktionen/Klassen, kein Android-Import. Unit-Test.
2. **Entität + DAO** (`data/local/`): Room-Entity, DAO mit `Flow` für Lesen, `suspend` für Schreiben. Integrationstest mit In-Memory-DB.
3. **Repository** (`data/repository/`): Interface + Room-Implementierung. Bildet Entities auf Domain-Modelle ab. Einziger Zugang der UI zu Daten.
4. **ViewModel** (`ui/<feature>/`): ein `StateFlow<UiState>`, Events als Funktionen. Factory über `AppContainer`.
5. **Screen** (`ui/<feature>/`): `XxxRoute` (holt ViewModel, sammelt State) + zustandsloser `XxxScreen(state, onEvent…)` mit Previews.
6. **Verdrahten** in `AppContainer` und Navigation.

## Regeln

- **Zeit:** zeitabhängige Flows mit dem Minuten-Ticker der `ShiftClock` kombinieren; Tests mit fester Uhr.
- **Statuswechsel:** nur über `EventTransitions` (prüft erlaubte Übergänge, Pflichtnotiz) und im DAO als
  bedingtes Update `UPDATE … WHERE id = :id AND status = :expected`; Rückgabewert (Anzahl Zeilen) auswerten.
  0 Zeilen = veralteter Zustand → kein Verlaufseintrag, UI zeigt den aktuellen Stand.
- **Verlauf und Undo:** jede Statusänderung schreibt in derselben Transaktion (`withTransaction`) eine
  `event_action` mit dem vorherigen Snapshot (`status`, `assigned_to`, `resolution`). Undo stellt den Snapshot her und löscht die Aktion.
- **Room:** Zeiten als ISO-String (`LocalDateTime.toString()`, sortierbar) über `TypeConverter`;
  Listen (Anmeldungen) per Converter, keine Join-Tabelle. Schema-Export an (`room.schemaLocation`).
- **ViewModel:** `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)`; der Initialzustand
  ist `Loading`, nie eine leere Liste (sonst blitzt „Alles erledigt“ auf).
- **Eingaben,** die eine Unterbrechung überleben müssen (Notizentwurf, gewählte Person), gehören in den `SavedStateHandle`.
- **Compose:** State hoisten, Lambdas statt ViewModel in Komponenten, `LazyColumn` mit stabilen `key`s,
  `collectAsStateWithLifecycle()`. UI nach `luma-design-system`.
- **Adaptiv:** Screens hängen nur von `WindowSizeClass` ab (Compact/Medium eine Spalte, Expanded List-Detail),
  nie von Gerätetyp oder Ausrichtung.
- **testTag** an allem, was ein Test findet: Karten `person_card_<id>`, Aktionen `action_<name>`.
- Keine neuen Abhängigkeiten ohne Rückfrage. Versionen nur im Version Catalog (`gradle/libs.versions.toml`).

Codegerüste für Tests (Unit, Room in-memory, Robolectric-E2E, feste Uhr): `references/testmuster.md`.

## Definition of Done

1. `./gradlew assembleDebug testDebugUnitTest lint` grün.
2. Neue Logik hat Tests; der E2E-Test des Kern-Flows läuft weiterhin.
3. Ergebnis im Emulator angesehen (Screenshot), bei Layout-Änderungen auch auf dem Tablet.
