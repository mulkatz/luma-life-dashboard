package com.mulkatz.lumacare.domain

import com.mulkatz.lumacare.domain.EventCommand.AddNote
import com.mulkatz.lumacare.domain.EventCommand.ConfirmHandover
import com.mulkatz.lumacare.domain.EventCommand.Release
import com.mulkatz.lumacare.domain.EventCommand.Resolve
import com.mulkatz.lumacare.domain.EventCommand.TakeOver
import com.mulkatz.lumacare.domain.EventStatus.DONE
import com.mulkatz.lumacare.domain.EventStatus.IN_PROGRESS
import com.mulkatz.lumacare.domain.EventStatus.OPEN
import com.mulkatz.lumacare.domain.ShiftType.EARLY
import com.mulkatz.lumacare.domain.ShiftType.NIGHT
import com.mulkatz.lumacare.domain.TransitionError.FORWARD_TARGET_REQUIRED
import com.mulkatz.lumacare.domain.TransitionError.NOTE_REQUIRED
import com.mulkatz.lumacare.domain.TransitionError.NOTE_TOO_SHORT
import com.mulkatz.lumacare.domain.TransitionError.NOT_ALLOWED
import org.junit.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** Fixtures nach dem Seed: Frühdienst, Typ und Dringlichkeit wie im Seed. Alle Daten fiktiv. */
class EventTransitionsTest {
    private val time = LocalDateTime.parse("2026-10-12T06:30:00")

    private fun event(
        id: String,
        type: EventType,
        severity: Severity,
        status: EventStatus = OPEN,
        assignedTo: ShiftType? = null,
        resolution: Resolution? = null,
    ) = LumaEvent(id, "r00", time, type, severity, status, "Fiktiver Hinweis $id.", assignedTo, resolution, time)

    /** Wie im Seed: urgent health_hint, offen. */
    private val e005 = event("e005", EventType.HEALTH_HINT, Severity.URGENT)

    /** Wie im Seed: urgent health_hint, vom Nachtdienst in Bearbeitung. */
    private val e018 = event("e018", EventType.HEALTH_HINT, Severity.URGENT, IN_PROGRESS, NIGHT)

    /** Wie im Seed: meal_wish info, offen. */
    private val e004 = event("e004", EventType.MEAL_WISH, Severity.INFO)

    /** Wie im Seed: request attention, offen. */
    private val e015 = event("e015", EventType.REQUEST, Severity.ATTENTION)

    private fun LumaEvent.own() = copy(status = IN_PROGRESS, assignedTo = EARLY)
    private fun LumaEvent.done() = copy(status = DONE, assignedTo = EARLY, resolution = Resolution.DONE)

    private fun apply(event: LumaEvent, command: EventCommand) = EventTransitions.apply(event, EARLY, command)

    private fun assertAllowed(event: LumaEvent, command: EventCommand): TransitionResult.Allowed =
        assertIs<TransitionResult.Allowed>(apply(event, command), "$command auf ${event.state}")

    private fun assertRejected(error: TransitionError, event: LumaEvent, command: EventCommand) =
        assertEquals(TransitionResult.Rejected(error), apply(event, command), "$command auf ${event.state}")

    @Test
    fun `tabelle erlaubter und verbotener übergänge je zuständigkeit`() {
        // Neutraler Typ (mood, attention), gültige Notiz: hier zählt nur der Ausgangszustand.
        val base = event("e100", EventType.MOOD, Severity.ATTENTION)
        val states = mapOf(
            "open" to base,
            "vorschicht" to base.copy(status = IN_PROGRESS, assignedTo = NIGHT),
            "eigen" to base.own(),
            "done" to base.done(),
        )
        val commands = mapOf(
            "TakeOver" to TakeOver,
            "ConfirmHandover" to ConfirmHandover,
            "Release" to Release,
            "AddNote" to AddNote("Gespräch geführt"),
            "Resolve" to Resolve(Resolution.DONE, note = "Gespräch geführt"),
        )
        val allowed = setOf(
            "TakeOver" to "open",
            "ConfirmHandover" to "vorschicht",
            "Release" to "eigen",
            "AddNote" to "open",
            "AddNote" to "vorschicht",
            "AddNote" to "eigen",
            "Resolve" to "open",
            "Resolve" to "eigen",
        )

        for ((commandName, command) in commands) {
            for ((stateName, event) in states) {
                val result = apply(event, command)
                if (commandName to stateName in allowed) {
                    assertIs<TransitionResult.Allowed>(result, "$commandName aus $stateName")
                } else {
                    assertEquals(TransitionResult.Rejected(NOT_ALLOWED), result, "$commandName aus $stateName")
                }
            }
        }
    }

    @Test
    fun `übernehmen ordnet der aktuellen schicht zu`() {
        val result = assertAllowed(e005, TakeOver)

        assertEquals(EventState(IN_PROGRESS, EARLY, null), result.newState)
        assertEquals(EventActionType.ASSIGN, result.action)
        assertEquals(null, result.note)
    }

    @Test
    fun `vorschicht e018 erst bestätigen, dann abschließen`() {
        assertRejected(NOT_ALLOWED, e018, Resolve(Resolution.DONE, note = "Arzt informiert"))
        assertRejected(NOT_ALLOWED, e018, Release)
        assertRejected(NOT_ALLOWED, e018, TakeOver)

        val confirmed = assertAllowed(e018, ConfirmHandover)
        assertEquals(EventState(IN_PROGRESS, EARLY, null), confirmed.newState)
        assertEquals(EventActionType.CONFIRM_HANDOVER, confirmed.action)

        assertAllowed(e018.own(), Resolve(Resolution.DONE, note = "Arzt informiert"))
    }

    @Test
    fun `eigener fall der nachtschicht ist aus sicht der nacht eigen`() {
        val result = EventTransitions.apply(e018, NIGHT, Release)

        assertEquals(TransitionResult.Allowed(EventState(OPEN, null, null), EventActionType.RELEASE, null, null), result)
    }

    @Test
    fun `zurückgeben macht den eigenen fall wieder offen`() {
        val result = assertAllowed(e005.own(), Release)

        assertEquals(EventState(OPEN, null, null), result.newState)
        assertEquals(EventActionType.RELEASE, result.action)
    }

    @Test
    fun `urgent e005 lässt sich aus open nicht abschließen, auch nicht mit notiz`() {
        for (resolution in listOf(Resolution.DONE, Resolution.NO_ACTION_NEEDED)) {
            assertRejected(NOT_ALLOWED, e005, Resolve(resolution, note = "Werte gemessen"))
        }
    }

    @Test
    fun `e005 eigen braucht eine notiz von mindestens drei zeichen`() {
        assertRejected(NOTE_REQUIRED, e005.own(), Resolve(Resolution.DONE))
        assertRejected(NOTE_REQUIRED, e005.own(), Resolve(Resolution.DONE, note = "   "))
        assertRejected(NOTE_TOO_SHORT, e005.own(), Resolve(Resolution.DONE, note = " ok "))

        val result = assertAllowed(e005.own(), Resolve(Resolution.DONE, note = "  Werte gemessen "))
        assertEquals(EventState(DONE, EARLY, Resolution.DONE), result.newState)
        assertEquals(EventActionType.RESOLVE, result.action)
        assertEquals("Werte gemessen", result.note)
        assertEquals(null, result.forwardedTo)
    }

    @Test
    fun `drei zeichen nach trim reichen`() {
        assertAllowed(e005.own(), Resolve(Resolution.DONE, note = "  abc  "))
    }

    @Test
    fun `kein handlungsbedarf bei urgent nur mit notiz, auch ohne health_hint`() {
        val urgentMood = event("e101", EventType.MOOD, Severity.URGENT).own()

        assertRejected(NOTE_REQUIRED, urgentMood, Resolve(Resolution.NO_ACTION_NEEDED))
        val result = assertAllowed(urgentMood, Resolve(Resolution.NO_ACTION_NEEDED, note = "Gesehen, wohlauf"))
        assertEquals(Resolution.NO_ACTION_NEEDED, result.newState.resolution)
    }

    @Test
    fun `health_hint braucht beim abschließen immer eine notiz`() {
        val attention = event("e102", EventType.HEALTH_HINT, Severity.ATTENTION).own()
        val infoOpen = event("e103", EventType.HEALTH_HINT, Severity.INFO)

        assertRejected(NOTE_REQUIRED, attention, Resolve(Resolution.DONE))
        assertRejected(NOTE_REQUIRED, attention, Resolve(Resolution.NO_ACTION_NEEDED))
        assertRejected(NOTE_REQUIRED, infoOpen, Resolve(Resolution.DONE))
        assertAllowed(infoOpen, Resolve(Resolution.DONE, note = "Gesehen, wohlauf"))
    }

    @Test
    fun `eigener fall ohne health_hint und urgent braucht keine notiz`() {
        val result = assertAllowed(e015.own(), Resolve(Resolution.DONE))

        assertEquals(null, result.note)
    }

    @Test
    fun `info e004 direkt zur kenntnis ohne notiz`() {
        val result = assertAllowed(e004, Resolve(Resolution.DONE))

        assertEquals(EventState(DONE, EARLY, Resolution.DONE), result.newState)
        assertAllowed(e004, Resolve(Resolution.NO_ACTION_NEEDED))
    }

    @Test
    fun `optionale notiz darf nicht zu kurz sein`() {
        assertRejected(NOTE_TOO_SHORT, e004, Resolve(Resolution.DONE, note = "ok"))
    }

    @Test
    fun `attention e015 aus open nur mit notiz`() {
        assertRejected(NOTE_REQUIRED, e015, Resolve(Resolution.DONE))
        assertRejected(NOTE_REQUIRED, e015, Resolve(Resolution.NO_ACTION_NEEDED))

        assertAllowed(e015, Resolve(Resolution.DONE, note = "Zeitung nachbestellt"))
    }

    @Test
    fun `e004 an die küche weiterleiten, notiz optional`() {
        val result = assertAllowed(e004, Resolve(Resolution.FORWARDED, forwardedTo = ForwardTarget.KITCHEN))

        assertEquals(EventState(DONE, EARLY, Resolution.FORWARDED), result.newState)
        assertEquals(EventActionType.RESOLVE, result.action)
        assertEquals(ForwardTarget.KITCHEN, result.forwardedTo)
        assertEquals(null, result.note)
    }

    @Test
    fun `weiterleiten aus open und eigenem fall, nicht aus vorschicht`() {
        val forward = Resolve(Resolution.FORWARDED, note = "Bitte prüfen", forwardedTo = ForwardTarget.ADMINISTRATION)

        assertEquals("Bitte prüfen", assertAllowed(e015, forward).note)
        assertAllowed(e015.own(), forward)
        assertRejected(NOT_ALLOWED, e015.copy(status = IN_PROGRESS, assignedTo = NIGHT), forward)
        assertRejected(NOT_ALLOWED, e015.done(), forward)
    }

    @Test
    fun `weiterleiten nur für request, meal_wish und family_message`() {
        val forward = Resolve(Resolution.FORWARDED, forwardedTo = ForwardTarget.SOCIAL_SERVICE)

        for (type in EventType.entries) {
            val event = event("e104", type, Severity.ATTENTION).own()
            if (type in setOf(EventType.REQUEST, EventType.MEAL_WISH, EventType.FAMILY_MESSAGE)) {
                assertAllowed(event, forward)
            } else {
                assertRejected(NOT_ALLOWED, event, forward)
            }
        }
        assertRejected(NOT_ALLOWED, e005.own(), forward.copy(note = "Werte gemessen"))
    }

    @Test
    fun `weiterleiten bei urgent nur mit notiz`() {
        val urgentRequest = event("e105", EventType.REQUEST, Severity.URGENT)
        val forward = Resolve(Resolution.FORWARDED, forwardedTo = ForwardTarget.MAINTENANCE)

        assertRejected(NOTE_REQUIRED, urgentRequest, forward)
        assertRejected(NOTE_REQUIRED, urgentRequest.own(), forward.copy(note = "  "))
        assertRejected(NOTE_TOO_SHORT, urgentRequest, forward.copy(note = "ok"))

        val result = assertAllowed(urgentRequest, forward.copy(note = "Haustechnik angerufen"))
        assertEquals(EventState(DONE, EARLY, Resolution.FORWARDED), result.newState)
        assertEquals("Haustechnik angerufen", result.note)
    }

    @Test
    fun `weiterleiten braucht ein ziel und ein ziel nur beim weiterleiten`() {
        assertRejected(FORWARD_TARGET_REQUIRED, e004, Resolve(Resolution.FORWARDED))
        assertRejected(NOTE_TOO_SHORT, e004, Resolve(Resolution.FORWARDED, note = "x", forwardedTo = ForwardTarget.KITCHEN))
        assertRejected(NOT_ALLOWED, e004, Resolve(Resolution.DONE, forwardedTo = ForwardTarget.KITCHEN))
    }

    @Test
    fun `notiz ändert den zustand nicht und ist pflicht`() {
        assertRejected(NOTE_REQUIRED, e005, AddNote(""))
        assertRejected(NOTE_TOO_SHORT, e005, AddNote(" ab "))

        val result = assertAllowed(e018, AddNote(" Angehörige informiert "))
        assertEquals(e018.state, result.newState)
        assertEquals(EventActionType.NOTE, result.action)
        assertEquals("Angehörige informiert", result.note)
    }

    @Test
    fun `done ist final`() {
        val commands = listOf(
            TakeOver,
            ConfirmHandover,
            Release,
            AddNote("Werte gemessen"),
            Resolve(Resolution.DONE, note = "Werte gemessen"),
            Resolve(Resolution.FORWARDED, forwardedTo = ForwardTarget.KITCHEN),
        )
        for (event in listOf(e005.done(), e004.done(), e018.copy(status = DONE, resolution = Resolution.DONE))) {
            commands.forEach { assertRejected(NOT_ALLOWED, event, it) }
        }
    }

    @Test
    fun `status nach der aktion für rückgängig`() {
        assertEquals(IN_PROGRESS, EventTransitions.statusAfter(EventActionType.ASSIGN, OPEN))
        assertEquals(IN_PROGRESS, EventTransitions.statusAfter(EventActionType.CONFIRM_HANDOVER, IN_PROGRESS))
        assertEquals(OPEN, EventTransitions.statusAfter(EventActionType.RELEASE, IN_PROGRESS))
        assertEquals(DONE, EventTransitions.statusAfter(EventActionType.RESOLVE, OPEN))
        assertEquals(OPEN, EventTransitions.statusAfter(EventActionType.NOTE, OPEN))
        assertEquals(IN_PROGRESS, EventTransitions.statusAfter(EventActionType.NOTE, IN_PROGRESS))
    }

    @Test
    fun `status nach der aktion passt zu jedem erlaubten übergang`() {
        val cases = listOf(
            e005 to TakeOver,
            e018 to ConfirmHandover,
            e005.own() to Release,
            e005.own() to AddNote("Werte gemessen"),
            e004 to Resolve(Resolution.FORWARDED, forwardedTo = ForwardTarget.KITCHEN),
        )
        for ((event, command) in cases) {
            val result = assertAllowed(event, command)
            assertEquals(result.newState.status, EventTransitions.statusAfter(result.action, event.status), "$command")
        }
    }

    @Test
    fun `zur kenntnis nur für info ohne health_hint, notiz optional`() {
        val mood = event("e017", EventType.MOOD, Severity.INFO)
        val missed = event("e007", EventType.MISSED_CALL, Severity.INFO)

        val acknowledged = assertAllowed(missed, Resolve(Resolution.ACKNOWLEDGED))
        assertEquals(EventState(DONE, EARLY, Resolution.ACKNOWLEDGED), acknowledged.newState)
        assertEquals(EventActionType.RESOLVE, acknowledged.action)
        assertAllowed(mood.own(), Resolve(Resolution.ACKNOWLEDGED, note = "Gesehen, wohlauf"))
        assertRejected(NOTE_TOO_SHORT, mood, Resolve(Resolution.ACKNOWLEDGED, note = "ok"))

        assertRejected(NOT_ALLOWED, event("e026", EventType.HEALTH_HINT, Severity.INFO), Resolve(Resolution.ACKNOWLEDGED))
        assertRejected(NOT_ALLOWED, e015, Resolve(Resolution.ACKNOWLEDGED))
        assertRejected(NOT_ALLOWED, e018, Resolve(Resolution.ACKNOWLEDGED))
        assertRejected(NOT_ALLOWED, mood.copy(status = IN_PROGRESS, assignedTo = NIGHT), Resolve(Resolution.ACKNOWLEDGED))
        assertRejected(NOT_ALLOWED, mood, Resolve(Resolution.ACKNOWLEDGED, forwardedTo = ForwardTarget.KITCHEN))
    }
}
