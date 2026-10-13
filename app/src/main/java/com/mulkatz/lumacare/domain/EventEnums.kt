package com.mulkatz.lumacare.domain

/** Bearbeitungsstand eines Luma-Hinweises. */
enum class EventStatus { OPEN, IN_PROGRESS, DONE }

/** Dringlichkeit, wie Luma sie im Seed vergibt. */
enum class Severity { URGENT, ATTENTION, INFO }

/** Art des Hinweises, entspricht den Typen im Seed. */
enum class EventType {
    HEALTH_HINT,
    MOOD,
    REQUEST,
    MEAL_WISH,
    ACTIVITY_SIGNUP,
    FAMILY_MESSAGE,
    MISSED_CALL,
}

/**
 * Abschlussart eines erledigten Hinweises. [ACKNOWLEDGED] ist „Zur Kenntnis“: ein `info`-Hinweis ohne
 * Handlungsbedarf, direkt aus der Karte abgeschlossen. Eigener Wert, damit der Verlauf nur zeigt, was geschah.
 */
enum class Resolution { DONE, FORWARDED, NO_ACTION_NEEDED, ACKNOWLEDGED }

/** Schicht, der ein Hinweis zugeordnet ist. */
enum class ShiftType { EARLY, NIGHT }

/** Art eines Verlaufseintrags (`event_action`). */
enum class EventActionType { ASSIGN, CONFIRM_HANDOVER, RELEASE, RESOLVE, NOTE }

/** Ziel einer Weiterleitung (Abschlussart [Resolution.FORWARDED]). */
enum class ForwardTarget { KITCHEN, MAINTENANCE, SOCIAL_SERVICE, ADMINISTRATION }
