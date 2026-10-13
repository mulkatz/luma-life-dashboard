package com.mulkatz.lumacare.domain

/**
 * Kleiner persistenter Zustand außerhalb der Datenbank: Seed-Version und Anker der [ShiftClock].
 * `null` heißt: noch nie gesetzt.
 */
interface AppStateStore {
    var seedVersion: Int?
    var clockAnchorMillis: Long?
}
