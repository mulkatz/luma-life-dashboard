package com.mulkatz.lumacare.domain

import com.mulkatz.lumacare.testing.InMemoryAppStateStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShiftClockTest {
    private val seedNow = LocalDateTime.parse("2026-10-12T07:15:00")
    private val shift = Shift(
        type = ShiftType.EARLY,
        start = LocalDateTime.parse("2026-10-12T06:00:00"),
        end = LocalDateTime.parse("2026-10-12T14:00:00"),
        unit = "Wohnbereich 2 – Ahorn",
    )
    private val base = 1_800_000_000_000L

    private var systemTime = base
    private val store = InMemoryAppStateStore()

    private fun clock(currentTimeMillis: () -> Long = { systemTime }) =
        ShiftClock(seedNow, shift, store, currentTimeMillis)

    @Test
    fun `beim anker gilt seed-zeit und der anker wird sofort gespeichert`() {
        val clock = clock()

        assertEquals(seedNow, clock.now())
        assertEquals(base, store.clockAnchorMillis)
    }

    @Test
    fun `dreissig minuten später zeigt die uhr 07_45`() {
        val clock = clock()

        systemTime += Duration.ofMinutes(30).toMillis()

        assertEquals(LocalDateTime.parse("2026-10-12T07:45:00"), clock.now())
    }

    @Test
    fun `neue instanz mit demselben store läuft ohne neuen anker weiter`() {
        clock()
        systemTime += Duration.ofMinutes(50).toMillis()

        val restarted = clock()

        assertEquals(base, store.clockAnchorMillis)
        assertEquals(LocalDateTime.parse("2026-10-12T08:05:00"), restarted.now())
    }

    @Test
    fun `zurückgestellte systemzeit läuft nicht vor den anker`() {
        val clock = clock()

        systemTime -= Duration.ofHours(2).toMillis()

        assertEquals(seedNow, clock.now())
    }

    @Test
    fun `restzeit um 07_15 ist sechs stunden fünfundvierzig minuten`() {
        val clock = clock()

        assertEquals(Duration.ofHours(6).plusMinutes(45), clock.remaining(clock.now()))
        assertFalse(clock.isShiftOver(clock.now()))
    }

    @Test
    fun `um 14 uhr ist die schicht vorbei und die restzeit null`() {
        val clock = clock()
        val end = LocalDateTime.parse("2026-10-12T14:00:00")

        assertTrue(clock.isShiftOver(end))
        assertEquals(Duration.ZERO, clock.remaining(end))
        assertEquals(Duration.ZERO, clock.remaining(end.plusHours(1)))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `ticks emittiert sofort und dann an jeder vollen minute`() = runTest {
        val clock = clock { base + testScheduler.currentTime }
        advanceTimeBy(20_500)

        val emissions = mutableListOf<Pair<LocalDateTime, Long>>()
        clock.ticks.take(3).collect { emissions += it to currentTime }

        // sofort mit der aktuellen Zeit, danach genau an den Minutengrenzen (nicht starr alle 60 s)
        assertEquals(
            listOf(
                LocalDateTime.parse("2026-10-12T07:15:20.500") to 20_500L,
                LocalDateTime.parse("2026-10-12T07:16:00") to 60_000L,
                LocalDateTime.parse("2026-10-12T07:17:00") to 120_000L,
            ),
            emissions,
        )
    }
}
