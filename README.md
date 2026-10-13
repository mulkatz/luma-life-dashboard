<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="docs/assets/logo-dark.svg">
    <img src="docs/assets/logo.svg" alt="Luma Care" height="72">
  </picture>
</p>

<p align="center">
  <strong>Was Luma im Gespräch mit Bewohnenden erfährt – priorisiert, nachvollziehbar und im Gehen bedienbar.</strong><br>
  Native Android-App für Pflegefachkräfte in der stationären Langzeitpflege.
</p>

<p align="center">
  Case Study „Product Builder“ · Luma Life · Status: <em>in Umsetzung</em>
</p>

---

## Überblick

Luma ist ein KI-Begleiter, der täglich mit den Bewohnenden spricht. Luma Care übersetzt diese
Gespräche in das, was eine Pflegefachkraft während der Schicht wirklich braucht: **Wer braucht jetzt
meine Aufmerksamkeit – und warum?**

- **Personen statt Ereignislisten.** Hinweise werden pro Bewohner·in gebündelt und mit Kontext
  angereichert (Routinen, Aktivitäten, gesundheitsrelevante Angaben).
- **Dringlichkeit und Zuständigkeit getrennt.** Dringendes verschwindet nie unter „in Bearbeitung“;
  offene Vorgänge aus dem Nachtdienst werden ausdrücklich übernommen.
- **Hinweis, keine Diagnose.** Die App zeigt, was Luma gehört hat, und trifft keine klinischen Bewertungen.

## Umfang

| Bereich | Inhalt |
|---|---|
| Dashboard | Schichtkontext, Kennzahlen, priorisierte Personenkarten (*Jetzt handeln*, *Demnächst*, *Zeitnah prüfen*, *Organisatorisches*) |
| Kern-Flow | Von Luma gemeldetes Anliegen übernehmen → bearbeiten → abschließen oder weiterleiten, mit Verlauf und Rückgängig |
| Daten | Import der Seed-Daten beim ersten Start in eine lokale Room-Datenbank; Zugriff über Repositories |
| Qualität | Unit-, Integrations- und End-to-End-Tests der Kernlogik; CI über GitHub Actions |

## Technik

Kotlin · Jetpack Compose (Material 3) · Room · Coroutines/Flow · kotlinx.serialization ·
Robolectric · Gradle mit festgelegter Daemon-JVM (JDK 25)

## Projekt starten

Voraussetzungen: Android Studio Rabbit 1 (2026.2.1) oder neuer. Für `./gradlew` im Terminal genügt
ein beliebiges lokales JDK ≥ 17 (z. B. die JBR von Android Studio); JDK 25 für den Build lädt Gradle
bei Bedarf selbst (`gradle/gradle-daemon-jvm.properties`). Die App läuft ab Android 8.0 (minSdk 26),
targetSdk ist 37; Phone und Tablet.

```sh
./gradlew assembleDebug        # Debug-APK bauen
./gradlew testDebugUnitTest    # alle Tests, ohne Emulator (Robolectric)
./gradlew lint                 # Android Lint
./gradlew installDebug         # auf Emulator oder Gerät installieren
```

Beim ersten Start importiert die App die Seed-Daten in die lokale Datenbank (folgt). Zurücksetzen:
in den Android-Einstellungen bei „Luma Care“ die App-Daten löschen.

## Dokumentation

| Dokument | Inhalt |
|---|---|
| [Planung](docs/00-planung.md) | Analyse, Produktkonzept, Designsystem, Architektur, Teststrategie, Zeitplan |
| [Aufgabenstellung](docs/case/case-study.md) | Original-Case von Luma Life inkl. [Seed-Daten](docs/case/luma-seed.json) |

## Arbeitsweise

Die App entsteht KI-gestützt mit Claude Code. Projekt-Instruktionen (`CLAUDE.md`) und
projektspezifische Skills (`.claude/skills/`) liegen im Repository; wo und wie die KI gesteuert und
korrigiert wurde, ist in der Dokumentation festgehalten.

## Lizenz

Alle Rechte vorbehalten – Details und Inhalte Dritter siehe [LICENSE](LICENSE).
Alle Personen- und Gesundheitsdaten sind fiktiv.
