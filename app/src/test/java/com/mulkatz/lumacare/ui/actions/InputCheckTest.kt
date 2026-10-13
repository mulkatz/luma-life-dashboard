package com.mulkatz.lumacare.ui.actions

import com.mulkatz.lumacare.domain.EventCommand
import com.mulkatz.lumacare.domain.EventStatus
import com.mulkatz.lumacare.domain.EventType
import com.mulkatz.lumacare.domain.ForwardTarget
import com.mulkatz.lumacare.domain.LumaEvent
import com.mulkatz.lumacare.domain.Resolution
import com.mulkatz.lumacare.domain.Severity
import com.mulkatz.lumacare.domain.ShiftType
import org.junit.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals

/** Sheet-Prüfung nach EventTransitions: Typ und Dringlichkeit wie im Seed, Daten fiktiv. */
class InputCheckTest {
    private val time = LocalDateTime.parse("2026-10-12T06:30:00")
    private val ownUrgentHint = LumaEvent(
        "e005", "r04", time, EventType.HEALTH_HINT, Severity.URGENT, EventStatus.IN_PROGRESS,
        "Fiktiver Hinweis.", ShiftType.EARLY, null, time,
    )
    private val openMealWish = ownUrgentHint.copy(
        id = "e004", type = EventType.MEAL_WISH, severity = Severity.INFO, status = EventStatus.OPEN, assignedTo = null,
    )

    @Test
    fun `pflichtnotiz fehlt oder ist zu kurz, mit baustein gültig`() {
        fun check(note: String?) = InputCheck.resolve(ownUrgentHint, ShiftType.EARLY, EventCommand.Resolve(Resolution.DONE, note))

        assertEquals(InputCheck(valid = false, noteRequired = true, noteInvalid = true), check(null))
        assertEquals(InputCheck(valid = false, noteRequired = true, noteInvalid = true), check("ok"))
        assertEquals(InputCheck(valid = true, noteRequired = true, noteInvalid = false), check("Werte gemessen"))
    }

    @Test
    fun `weiterleiten braucht ein ziel, die notiz ist bei info optional`() {
        fun check(target: ForwardTarget?, note: String? = null) =
            InputCheck.resolve(openMealWish, ShiftType.EARLY, EventCommand.Resolve(Resolution.FORWARDED, note, target))

        assertEquals(InputCheck(valid = false, noteRequired = false, noteInvalid = false), check(null))
        assertEquals(InputCheck(valid = true, noteRequired = false, noteInvalid = false), check(ForwardTarget.KITCHEN))
        assertEquals(InputCheck(valid = false, noteRequired = false, noteInvalid = true), check(ForwardTarget.KITCHEN, "ok"))
    }

    @Test
    fun `notiz ist immer pflicht`() {
        assertEquals(InputCheck(valid = false, noteRequired = true, noteInvalid = true), InputCheck.note(openMealWish, ShiftType.EARLY, ""))
        assertEquals(InputCheck(valid = true, noteRequired = true, noteInvalid = false), InputCheck.note(openMealWish, ShiftType.EARLY, "Küche informiert"))
    }
}
