# Testmuster

Alle Tests liegen in `app/src/test/` und laufen auf der JVM (`./gradlew testDebugUnitTest`), ohne Emulator.
Benennung: `` `abschnitt ist höchste offene dringlichkeit`() `` – Backtick-Namen auf Deutsch, Arrange/Act/Assert.

## Feste Uhr

```kotlin
class FixedShiftClock(var current: LocalDateTime = LocalDateTime.parse("2026-10-12T07:15:00")) : ShiftClock {
    override fun now(): LocalDateTime = current
    override val ticks: Flow<LocalDateTime> = flowOf(current)
}
```

## Unit (Domain)

```kotlin
class PrioritizerTest {
    private val now = LocalDateTime.parse("2026-10-12T07:15:00")

    @Test fun `vorschicht in bearbeitung bleibt unter jetzt handeln`() {
        val events = listOf(event("e018", residentId = "r12", severity = URGENT, status = IN_PROGRESS, assignedTo = NACHTDIENST))
        val sections = Prioritizer.sections(residents, events, currentShift = FRUEHDIENST, now = now)
        assertEquals(listOf("r12"), sections.actNow.map { it.resident.id })
    }
}
```

Testdaten mit kleinen Buildern (`event(id, severity = …)`) mit sinnvollen Defaults, keine kompletten Seed-Dateien.
Pflichtfälle: Abschnittszuordnung, Sortierung inkl. Zweitschlüssel (Zimmer, ID) bei gleichem Zeitstempel,
erlaubte/verbotene Übergänge, Pflichtnotiz, Undo-Snapshot, Wochentagsfilter (rt04 nur sonntags), ShiftClock-Anker.

## Integration (Room in-memory)

```kotlin
@RunWith(RobolectricTestRunner::class)
class LumaEventDaoTest {
    private lateinit var db: LumaDatabase

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), LumaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After fun tearDown() = db.close()

    @Test fun `bedingtes update greift nur bei erwartetem status`() = runTest {
        db.eventDao().insertAll(listOf(eventEntity("e001", status = "open")))
        assertEquals(1, db.eventDao().updateStatus("e001", expected = "open", new = "in_progress", assignedTo = "ich"))
        assertEquals(0, db.eventDao().updateStatus("e001", expected = "open", new = "in_progress", assignedTo = "ich"))
    }
}
```

SeedImporter: liest `docs/case/luma-seed.json` (als Test-Ressource bzw. aus `assets`), prüft Anzahl
(15 Bewohnende, 26 Events, 7 Routinen, 4 Aktivitäten), Idempotenz (zweiter Import ändert nichts) und
`seed_version` erst nach erfolgreichem Commit.

## E2E (Compose + Robolectric)

```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")   // Phone; Tablet-Variante: "w1280dp-h800dp-land-xhdpi"
class CoreFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun `anliegen übernehmen notieren abschließen und neustart überleben`() {
        compose.waitUntilExactlyOneExists(hasTestTag("person_card_r04"), timeoutMillis = 5_000)
        compose.onNode(hasTestTag("section_act_now")).assertExists()
        compose.onNodeWithTag("person_card_r04").performClick()
        compose.onNodeWithTag("action_take_over").performClick()
        compose.onNodeWithTag("action_resolve").performClick()
        compose.onNodeWithText("Arzt informiert").performClick()
        compose.onNodeWithTag("action_confirm_resolve").performClick()
        // zurück im Dashboard (Phone) bzw. Liste links (Tablet)
        compose.onNodeWithTag("person_card_r04").assertDoesNotExist() // Werner hat nur e005, ohne offene Events keine Karte mehr
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("person_card_r04").assertDoesNotExist()
    }
}
```

- `AppContainer` im Test mit In-Memory-DB und `FixedShiftClock` überschreiben (Test-`Application` oder
  austauschbarer Container), damit Import und Uhr deterministisch sind.
- Elemente über `testTag` und Semantik finden. Kein `Thread.sleep`, sondern `waitUntil…`.
