package com.mulkatz.lumacare.ui.components

import android.content.Context
import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Duration
import java.time.LocalDateTime
import kotlin.test.assertEquals

/** Zeitangaben mit den echten String-Ressourcen; `now` = Mo 12.10.2026, 07:15. */
@RunWith(RobolectricTestRunner::class)
class TimeFormatTest {
    private val res: Resources = ApplicationProvider.getApplicationContext<Context>().resources
    private val now = LocalDateTime.parse("2026-10-12T07:15:00")

    private fun at(text: String) = LocalDateTime.parse(text)

    @Test
    fun `alter in minuten, ab einer stunde abgerundet in stunden`() {
        assertEquals("gerade eben", TimeFormat.age(res, at("2026-10-12T07:15:00"), now))
        assertEquals("vor 45 Min.", TimeFormat.age(res, at("2026-10-12T06:30:00"), now))
        assertEquals("vor 1 Std.", TimeFormat.age(res, at("2026-10-12T06:15:00"), now))
        assertEquals("vor 8 Std.", TimeFormat.age(res, at("2026-10-11T23:15:00"), now))
    }

    @Test
    fun `uhrzeit heute, gestern und älter`() {
        assertEquals("06:30", TimeFormat.clockLabel(res, at("2026-10-12T06:30:00"), now))
        assertEquals("gestern 23:15", TimeFormat.clockLabel(res, at("2026-10-11T23:15:00"), now))
        assertEquals("10.10. 23:15", TimeFormat.clockLabel(res, at("2026-10-10T23:15:00"), now))
    }

    @Test
    fun `metazeile kombiniert alter und uhrzeit`() {
        assertEquals("vor 45 Min. · 06:30", TimeFormat.ageAndClock(res, at("2026-10-12T06:30:00"), now))
        assertEquals("vor 8 Std. · gestern 23:15", TimeFormat.ageAndClock(res, at("2026-10-11T23:15:00"), now))
    }

    @Test
    fun `abstand bis zum termin rechnet auf ganzen minuten`() {
        assertEquals("in 15 Min.", TimeFormat.until(res, at("2026-10-12T07:30:00"), now))
        assertEquals("in 15 Min.", TimeFormat.until(res, at("2026-10-12T07:30:00"), at("2026-10-12T07:15:40")))
        assertEquals("jetzt", TimeFormat.until(res, at("2026-10-12T07:15:00"), now))
    }

    @Test
    fun `restzeit kurz zählt angefangene minuten voll`() {
        assertEquals("6:45 h", TimeFormat.remainingShort(res, Duration.ofHours(6).plusMinutes(45)))
        assertEquals("6:45 h", TimeFormat.remainingShort(res, Duration.ofHours(6).plusMinutes(44).plusSeconds(30)))
        assertEquals("0:05 h", TimeFormat.remainingShort(res, Duration.ofMinutes(5)))
    }

    @Test
    fun `restzeit für talkback mit plurals`() {
        assertEquals("6 Stunden 45 Minuten", TimeFormat.remainingSpoken(res, Duration.ofHours(6).plusMinutes(45)))
        assertEquals("1 Stunde 1 Minute", TimeFormat.remainingSpoken(res, Duration.ofMinutes(61)))
        assertEquals("2 Stunden", TimeFormat.remainingSpoken(res, Duration.ofHours(2)))
        assertEquals("45 Minuten", TimeFormat.remainingSpoken(res, Duration.ofMinutes(45)))
        assertEquals("1 Minute", TimeFormat.remainingSpoken(res, Duration.ofSeconds(20)))
    }

    @Test
    fun `wohnbereich kurzform nach dem gedankenstrich, sonst unverändert`() {
        assertEquals("Ahorn", shortUnitName("Wohnbereich 2 – Ahorn"))
        assertEquals("Wohnbereich Linde", shortUnitName("Wohnbereich Linde"))
        assertEquals("Wohnbereich 3 –", shortUnitName("Wohnbereich 3 –"))
    }
}
