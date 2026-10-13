# Case Study – Product Builder

## Warum dieser Case?

Wir suchen jemanden, der aus Anforderungen und User Stories eigenständig ein funktionierendes Produkt baut: durchdachte UX, sauberes Frontend und eine solide Datenbasis – für eine Zielgruppe, die von den meisten Produkten übersehen wird.

Wir arbeiten selbst KI-gestützt und erwarten das auch von dir. Uns interessiert, wie schnell und wie gut du mit Tools wie Claude Code, Cursor oder Copilot von einer Idee zu einer laufenden App kommst – und wo du die KI steuerst, statt ihr nur zu folgen.

Der folgende Case ist bewusst **kein** Teil unserer aktuellen Roadmap. Unser Fokus liegt derzeit auf B2C. Das Szenario stammt aus einer echten Anfrage, die uns erreicht hat, und dient uns ausschließlich als realistische Spielwiese, um deine Arbeitsweise kennenzulernen. Wir werden dein Ergebnis nicht produktiv verwenden.

## Ausgangslage

Ein Alters- und Pflegeheim führt ein Innovationsprojekt durch und fragt sich, wie ein digitaler Bewohner-Assistent der nächsten Generation aussehen könnte – deutlich mehr als eine klassische Bewohner-App mit Menüplan und Aktivitätenkalender.

Die Vorstellung: ein persönlicher, KI-gestützter Assistent (bei uns: **Luma**), der Bewohnende im Alltag begleitet und Selbstbestimmung, Teilhabe und Lebensqualität fördert. Was sich das Pflegeheim vorstellt, das Luma können sollte:

- Kennt Biografie, Vorlieben und Gewohnheiten der Bewohnenden
- Führt natürliche Gespräche und fördert soziale Interaktion
- Unterstützt Orientierung und Selbstständigkeit im Alltag
- Beantwortet Fragen rund um Heim, Termine, Aktivitäten und Angebote
- Fragt proaktiv nach Wohlbefinden, Wünschen und Bedürfnissen
- Unterstützt bei Menüwahlen und Anmeldungen zu Aktivitäten
- Bindet Angehörige ein und ermöglicht transparenten Informationsfluss
- Leitet Anliegen an Mitarbeitende oder Fachbereiche weiter
- Unterstützt Biografiearbeit, Erinnerungspflege und Aktivierung
- Funktioniert möglichst einfach über Sprache und natürliche Dialoge

Luma spricht also täglich mit den Bewohnenden. Die Frage, um die es in diesem Case geht: **Was davon kommt bei den Pflegekräften an – und wie?**

## Deine Aufgabe

Baue eine **lauffähige mobile App für professionelle Pflegekräfte** in einer stationären Einrichtung. Stell dir eine Pflegefachperson vor, die 10–20 Bewohnende betreut, im Schichtbetrieb arbeitet und die App im Gehen auf dem Smartphone nutzt.

Du entscheidest, ob du für **iOS** oder **Android** entwickelst. Bitte entwickle nativ; Cross-Platform-Frameworks wie Flutter oder React Native sind für diesen Case nicht vorgesehen. Ein separates Figma-Design ist nicht nötig – die App ist dein High-Fidelity-Ergebnis. Wenn du vorher skizzieren möchtest, darfst du das natürlich.

### 1. Dashboard (Hauptteil)

Ein hochwertig gestaltetes, funktionierendes Dashboard als Einstiegspunkt der App. Überleg dir, welche Metriken, Informationen, Benachrichtigungen und Funktionen für eine Pflegekraft wirklich relevant sind – und wie du sie priorisierst. Die Inhalte kommen aus der lokalen Datenbank, nicht hart codiert aus dem UI.

### 2. Ein funktionsfähiger Flow

Baue ausgehend vom Dashboard **einen** Flow, der durchgängig funktioniert: Eingabe, Speichern in der Datenbank, sichtbare Wirkung im Dashboard oder in der Detailansicht. Zum Beispiel:

- Eine Routine bzw. Erinnerung mit Luma-Anruf für eine bestimmte Bewohnerin einrichten
- Den Essensplan hinterlegen, sodass Luma Fragen zum Essen beantworten und Essenswünsche aufnehmen kann
- Biografieinhalte zu einer Person ergänzen, damit Luma persönlicher sprechen kann
- Ein von Luma gemeldetes Anliegen übernehmen, bearbeiten und abschließen

Du kannst dir auch einen eigenen Flow überlegen. Externe Aktionen wie einen echten Luma-Anruf, Push-Benachrichtigungen oder das Weiterleiten an andere Fachbereiche musst du nicht umsetzen – Speichern und sichtbares Anzeigen in der App genügt. Denk an Edge Cases wie leere Zustände, fehlerhafte Eingaben oder Überschneidungen.

### 3. Daten

Luma selbst musst du **nicht** bauen. Was Luma aus den Gesprächen mit den Bewohnenden liefert, bekommst du als Seed-Daten im JSON-Format (siehe Anhang). Importiere sie beim ersten App-Start in eine lokale Datenbank und arbeite ab dann mit der Datenbank. Du darfst das Datenmodell erweitern, wenn dein Konzept es braucht.

Es geht ausdrücklich um einen fokussierten, sauberen Ausschnitt – nicht um ein fertiges Produkt. Lieber ein Dashboard und ein Flow, die richtig gut sind, als viel Halbfertiges.

## Technischer Rahmen

Die Pflichtteile halten den Case vergleichbar, die optionalen Punkte sind Gelegenheiten zu zeigen, was du darüber hinaus kannst.

|  | Pflicht | Optional (Pluspunkt) |
| --- | --- | --- |
| Plattform | iOS (Swift/SwiftUI) oder Android (Kotlin/Jetpack Compose) | – |
| Daten | Lokale Datenbank, z. B. SwiftData/Core Data oder Room/SQLite, befüllt aus dem Seed-JSON | Datenzugriff hinter einer eigenen Schicht (z. B. Repository), sodass später eine echte API angebunden werden kann |
| Funktion | Dashboard mit echten Daten aus der DB, ein durchgängiger Flow mit Persistenz | Zweiter Flow, gern auch nur skizziert |
| Backend | Nicht nötig | Echtes Backend, z. B. Supabase oder eine kleine API |
| KI im Produkt | Nicht nötig | Ein echter LLM-Call, z. B. eine Schichtübergabe-Zusammenfassung aus den Luma-Events |
| Qualität | Läuft im Simulator/Emulator, nachvollziehbare Projektstruktur | Tests für die Kernlogik |

KI-Tools sind ausdrücklich erlaubt und erwünscht. Wenn du magst, leg Prompts, Projekt-Instruktionen (z. B. eine `CLAUDE.md`) oder Chat-Ausschnitte mit ins Repository – das hilft uns, deine Arbeitsweise zu verstehen.

## Abgabe

Du schickst uns einen Link zum Repository (GitHub, GitLab o. Ä., mit Lesezugriff).

Leg ins Repository bitte eine kurze README mit den Schritten, um die App zu starten: benötigte Version von Xcode bzw. Android Studio, Ziel-Betriebssystem und ggf. Besonderheiten beim ersten Start.

## Rahmen

- **Zeitaufwand:** ca. 3–4 Stunden. Bitte investiere nicht mehr – wenn die Zeit um ist, zeig uns, was du hast, und erzähl im Video, was noch fehlt.
- **Abgabe:** Repository-Link an marcel@lumalife.de und marvin@lumalife.de, innerhalb von 10 Tagen nach Erhalt des Cases (gerne früher)
- **Daten:** Verwende ausschließlich fiktive Daten. Das Seed-JSON ist komplett erfunden; bitte ergänze keine echten Personen- oder Gesundheitsdaten.
- **Annahmen:** Wo Informationen fehlen, triff eigene Annahmen und mach sie im Video sichtbar. Es gibt keine Musterlösung – uns interessiert, wie du von Anforderungen zu einem Produkt kommst.

Bei Fragen melde dich jederzeit unter marcel@lumalife.de – Nachfragen sind ausdrücklich erwünscht.

## Worauf wir schauen

- **Produktdenken:** Priorisierung, Verständnis für den Arbeitsalltag der Zielgruppe, sinnvolle Reduktion
- **UX & Flow:** Nachvollziehbare Informationsarchitektur, saubere Nutzerführung, Umgang mit Edge Cases
- **Visuelle Qualität:** Ansprechendes, konsistentes UI mit Blick für Details – direkt im Code umgesetzt
- **Technische Umsetzung:** Sinnvolles Datenmodell, funktionierende Persistenz, nachvollziehbare Struktur
- **Arbeit mit KI:** Tempo und Qualität, kritischer Umgang mit generiertem Code, bewusste Steuerung
- **Kommunikation:** Wie klar erklärst und begründest du deine Entscheidungen im Gespräch?

## Anhang: Seed-Daten

Die Datei `luma-seed.json` liegt diesem Case bei. Sie enthält einen Wohnbereich mit 15 fiktiven Bewohnenden und die Ereignisse, die Luma in den letzten 24 Stunden aus Gesprächen gemeldet hat. Der Zeitpunkt „jetzt“ ist `meta.now`; berechne relative Zeiten davon ausgehend.

| Bereich | Inhalt |
| --- | --- |
| `meta` | Einrichtung, Wohnbereich, aktuelle Schicht, Referenzzeitpunkt |
| `residents` | Stammdaten, Zimmer, Pflegegrad, Biografie-Stichpunkte, Vorlieben, Kontaktperson |
| `luma_events` | Was Luma meldet: Stimmung, Anliegen, Gesundheitshinweis, Essenswunsch, Aktivitätsanmeldung, Angehörigen-Nachricht, verpasster Anruf |
| `routines` | Bestehende Routinen und Erinnerungen mit Luma-Anruf |
| `activities` | Heutige Angebote im Haus mit Anmeldungen |

Jedes Event hat eine `severity` (`info`, `attention`, `urgent`) und einen `status` (`open`, `in_progress`, `done`). Wie du diese Informationen gewichtest und darstellst, ist Teil deiner Aufgabe.

Ausschnitt aus der Struktur:

```json
{
  "meta": {
    "facility": "Haus am Lindenpark",
    "unit": "Wohnbereich 2 – Ahorn",
    "shift": { "type": "early", "start": "2026-10-12T06:00:00", "end": "2026-10-12T14:00:00" },
    "now": "2026-10-12T07:15:00"
  },
  "residents": [
    {
      "id": "r01",
      "first_name": "Gertrud",
      "last_name": "Brandt",
      "age": 87,
      "room": "2.04",
      "care_level": 3,
      "biography": ["War 30 Jahre Grundschullehrerin", "Hat zwei Kinder, Tochter Sabine besucht sonntags"],
      "preferences": ["Tee statt Kaffee", "Hört gern Schlager"],
      "contact": { "name": "Sabine Brandt", "relation": "Tochter" }
    }
  ],
  "luma_events": [
    {
      "id": "e001",
      "resident_id": "r01",
      "timestamp": "2026-10-12T06:40:00",
      "type": "health_hint",
      "severity": "attention",
      "status": "open",
      "summary": "Hat im Morgengespräch zweimal über Schwindel beim Aufstehen geklagt."
    }
  ],
  "routines": [
    { "id": "rt01", "resident_id": "r01", "title": "Erinnerung Trinken", "time": "10:00", "days": ["mon","tue","wed","thu","fri","sat","sun"], "luma_call": true }
  ],
  "activities": [
    { "id": "a01", "title": "Sitzgymnastik", "start": "2026-10-12T10:30:00", "location": "Gemeinschaftsraum", "registered": ["r01"] }
  ]
}
```
