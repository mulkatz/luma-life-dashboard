package com.mulkatz.lumacare.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mulkatz.lumacare.R
import com.mulkatz.lumacare.domain.EventActionType
import com.mulkatz.lumacare.domain.ForwardTarget
import com.mulkatz.lumacare.domain.LoggedAction
import com.mulkatz.lumacare.domain.Resolution
import com.mulkatz.lumacare.domain.ShiftType

@get:StringRes
val ShiftType.labelRes: Int
    get() = when (this) {
        ShiftType.EARLY -> R.string.shift_early
        ShiftType.NIGHT -> R.string.shift_night
    }

@get:StringRes
val ForwardTarget.labelRes: Int
    get() = when (this) {
        ForwardTarget.KITCHEN -> R.string.forward_target_kitchen
        ForwardTarget.MAINTENANCE -> R.string.forward_target_maintenance
        ForwardTarget.SOCIAL_SERVICE -> R.string.forward_target_social_service
        ForwardTarget.ADMINISTRATION -> R.string.forward_target_administration
    }

/** Abschlussart als Wort, z. B. für „Abgeschlossen: Erledigt“. */
@get:StringRes
val Resolution.labelRes: Int
    get() = when (this) {
        Resolution.DONE -> R.string.resolution_done
        Resolution.FORWARDED -> R.string.resolution_forwarded_option
        Resolution.NO_ACTION_NEEDED -> R.string.resolution_no_action_needed
        Resolution.ACKNOWLEDGED -> R.string.resolution_acknowledged
    }

/**
 * Ergebnis eines Abschlusses als Satz: „Abgeschlossen: Erledigt“, „Weitergeleitet an Küche“,
 * „Zur Kenntnis genommen“. Nur was gespeichert ist, ohne Bewertung.
 */
@Composable
fun resolutionText(resolution: Resolution, forwardedTo: ForwardTarget?): String = when (resolution) {
    Resolution.FORWARDED ->
        if (forwardedTo != null) {
            stringResource(R.string.history_forwarded, stringResource(forwardedTo.labelRes))
        } else {
            stringResource(R.string.resolution_forwarded_option)
        }
    Resolution.ACKNOWLEDGED -> stringResource(R.string.resolution_acknowledged)
    Resolution.DONE, Resolution.NO_ACTION_NEEDED ->
        stringResource(R.string.history_resolved, stringResource(resolution.labelRes))
}

/** Titel eines Verlaufseintrags der Pflege, z. B. „Übernommen (Frühdienst)“. Ohne Namen, es gibt kein Login. */
@Composable
fun LoggedAction.title(): String = when (record.type) {
    EventActionType.ASSIGN -> {
        val shift = after.assignedTo
        if (shift != null) {
            stringResource(R.string.history_assigned, stringResource(shift.labelRes))
        } else {
            stringResource(R.string.snackbar_taken_over)
        }
    }
    EventActionType.CONFIRM_HANDOVER -> stringResource(R.string.history_handover_confirmed)
    EventActionType.RELEASE -> stringResource(R.string.history_released)
    EventActionType.NOTE -> stringResource(R.string.history_note)
    EventActionType.RESOLVE -> {
        val resolution = after.resolution
        if (resolution != null) resolutionText(resolution, record.forwardedTo) else stringResource(R.string.snackbar_resolved)
    }
}

/** Icon eines Verlaufseintrags der Pflege. */
@get:DrawableRes
val LoggedAction.iconRes: Int
    get() = when (record.type) {
        EventActionType.ASSIGN, EventActionType.CONFIRM_HANDOVER -> R.drawable.ic_person
        EventActionType.RELEASE -> R.drawable.ic_back
        EventActionType.NOTE -> R.drawable.ic_note
        EventActionType.RESOLVE ->
            if (after.resolution == Resolution.FORWARDED) R.drawable.ic_forward else R.drawable.ic_check_circle
    }
