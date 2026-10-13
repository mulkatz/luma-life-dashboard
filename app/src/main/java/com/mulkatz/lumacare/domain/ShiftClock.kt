package com.mulkatz.lumacare.domain

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Duration
import java.time.LocalDateTime

/**
 * Einzige Quelle für „jetzt“.
 *
 * Beim ersten Start wird die Systemzeit als Anker gespeichert. Danach gilt
 * `now() = seedNow + (Systemzeit − Anker)`. Eine negative Differenz (Systemzeit zurückgestellt)
 * zählt als 0, die Uhr läuft also nie vor `seedNow` zurück.
 */
class ShiftClock(
    private val seedNow: LocalDateTime,
    val shiftStart: LocalDateTime,
    val shiftEnd: LocalDateTime,
    anchorStore: AppStateStore,
    private val currentTimeMillis: () -> Long,
) {
    private val anchorMillis: Long =
        anchorStore.clockAnchorMillis ?: currentTimeMillis().also { anchorStore.clockAnchorMillis = it }

    fun now(): LocalDateTime {
        val elapsed = (currentTimeMillis() - anchorMillis).coerceAtLeast(0L)
        return seedNow.plus(Duration.ofMillis(elapsed))
    }

    /** Restzeit bis Schichtende, nie negativ. */
    fun remaining(now: LocalDateTime): Duration =
        Duration.between(now, shiftEnd).takeUnless { it.isNegative } ?: Duration.ZERO

    /** Ab Schichtende `true`. */
    fun isShiftOver(now: LocalDateTime): Boolean = !now.isBefore(shiftEnd)

    /** Emittiert sofort [now] und danach an jeder vollen Minute. */
    val ticks: Flow<LocalDateTime> = flow {
        while (true) {
            emit(now())
            // nach emit neu lesen: der Sammler kann Zeit verbraucht haben
            delay(millisUntilNextMinute(now()))
        }
    }

    private fun millisUntilNextMinute(time: LocalDateTime): Long {
        val intoMinute = time.second * 1_000L + time.nano / 1_000_000L
        return MINUTE_MILLIS - intoMinute
    }

    private companion object {
        const val MINUTE_MILLIS = 60_000L
    }
}
