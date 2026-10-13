package com.mulkatz.lumacare.ui.components

import android.content.res.Resources
import com.mulkatz.lumacare.R
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Zeitangaben für die Oberfläche. Reine Funktionen: gleiche Eingabe, gleiche Ausgabe; „jetzt“ kommt immer
 * als Parameter (aus der ShiftClock). Gerechnet wird auf ganzen Minuten, passend zur Uhrzeit im Kopfbereich.
 */
object TimeFormat {
    private val CLOCK: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val DATE_CLOCK: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM. HH:mm")

    /** „07:15“ */
    fun clock(time: LocalDateTime): String = time.format(CLOCK)

    /** Uhrzeit relativ zum Tag von [now]: „06:30“, „gestern 23:15“, sonst „10.10. 23:15“. */
    fun clockLabel(res: Resources, time: LocalDateTime, now: LocalDateTime): String =
        when (ChronoUnit.DAYS.between(time.toLocalDate(), now.toLocalDate())) {
            0L -> clock(time)
            1L -> res.getString(R.string.time_yesterday, clock(time))
            else -> time.format(DATE_CLOCK)
        }

    /** Alter: „gerade eben“, „vor 45 Min.“, ab einer Stunde abgerundet „vor 8 Std.“. */
    fun age(res: Resources, time: LocalDateTime, now: LocalDateTime): String {
        val minutes = minutesBetween(time, now)
        return when {
            minutes < 1 -> res.getString(R.string.time_just_now)
            minutes < 60 -> res.getQuantityString(R.plurals.time_minutes_ago, minutes.toInt(), minutes.toInt())
            else -> res.getString(R.string.time_hours_ago, minutes / 60)
        }
    }

    /** „vor 45 Min. · 06:30“ */
    fun ageAndClock(res: Resources, time: LocalDateTime, now: LocalDateTime): String =
        res.getString(R.string.hint_meta, age(res, time, now), clockLabel(res, time, now))

    /** Abstand bis zu einem Termin: „in 15 Min.“, bei 0 oder weniger „jetzt“. */
    fun until(res: Resources, time: LocalDateTime, now: LocalDateTime): String {
        val minutes = minutesBetween(now, time)
        return if (minutes <= 0) res.getString(R.string.time_now) else res.getQuantityString(R.plurals.time_in_minutes, minutes.toInt(), minutes.toInt())
    }

    /** Restzeit kurz: „6:45 h“. Angefangene Minuten zählen voll. */
    fun remainingShort(res: Resources, remaining: Duration): String {
        val minutes = ceilMinutes(remaining)
        return res.getString(R.string.time_remaining_short, minutes / 60, minutes % 60)
    }

    /** Restzeit für TalkBack: „6 Stunden 45 Minuten“, „1 Stunde“, „45 Minuten“. */
    fun remainingSpoken(res: Resources, remaining: Duration): String {
        val total = ceilMinutes(remaining)
        val hours = (total / 60).toInt()
        val minutes = (total % 60).toInt()
        val hoursText = res.getQuantityString(R.plurals.time_hours, hours, hours)
        val minutesText = res.getQuantityString(R.plurals.time_minutes, minutes, minutes)
        return when {
            hours == 0 -> minutesText
            minutes == 0 -> hoursText
            else -> res.getString(R.string.time_hours_and_minutes, hoursText, minutesText)
        }
    }

    private fun minutesBetween(from: LocalDateTime, to: LocalDateTime): Long =
        Duration.between(from.truncatedTo(ChronoUnit.MINUTES), to.truncatedTo(ChronoUnit.MINUTES)).toMinutes()

    private fun ceilMinutes(duration: Duration): Long {
        val positive = if (duration.isNegative) Duration.ZERO else duration
        val whole = positive.toMinutes()
        return if (positive > Duration.ofMinutes(whole)) whole + 1 else whole
    }
}
