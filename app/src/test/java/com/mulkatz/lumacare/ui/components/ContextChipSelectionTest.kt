package com.mulkatz.lumacare.ui.components

import com.mulkatz.lumacare.domain.Activity
import com.mulkatz.lumacare.domain.ContextChip
import com.mulkatz.lumacare.domain.Routine
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.test.assertEquals

class ContextChipSelectionTest {
    private val now = LocalDateTime.parse("2026-10-12T07:15:00")
    private val health = ContextChip.HealthNote("Diabetes Typ 2, achtet auf Zucker")
    private val routine = ContextChip.RoutineNearby(
        Routine("rt06", "r12", "Morgengespräch auf Plattdeutsch", LocalTime.of(6, 30), DayOfWeek.entries.toSet(), true),
        now.withHour(6).withMinute(30),
    )
    private val activity = ContextChip.ActivityAhead(Activity("a02", "Singkreis", now.withHour(11), "Wintergarten", listOf("r05")))

    @Test
    fun `die ersten zwei chips in modellreihenfolge, rest als zahl`() {
        val selection = selectContextChips(listOf(health, routine, ContextChip.LumaNotReached, activity))

        assertEquals(listOf(health, routine), selection.shown)
        assertEquals(2, selection.hiddenCount)
    }

    @Test
    fun `ui sortiert nicht neu, weitere hinweise nie als chip`() {
        val selection = selectContextChips(listOf(activity, ContextChip.FurtherEvents(1), ContextChip.LumaNotReached))

        assertEquals(listOf(activity, ContextChip.LumaNotReached), selection.shown)
        assertEquals(0, selection.hiddenCount)
    }
}
