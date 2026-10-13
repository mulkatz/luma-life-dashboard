package com.mulkatz.lumacare.data.local

import android.content.SharedPreferences
import androidx.core.content.edit
import com.mulkatz.lumacare.domain.AppStateStore

/** [AppStateStore] auf SharedPreferences; schreibt synchron, damit Anker und Seed-Version sofort sicher sind. */
class SharedPreferencesAppStateStore(private val prefs: SharedPreferences) : AppStateStore {
    override var seedVersion: Int?
        get() = if (prefs.contains(KEY_SEED_VERSION)) prefs.getInt(KEY_SEED_VERSION, 0) else null
        set(value) = prefs.edit(commit = true) {
            if (value == null) remove(KEY_SEED_VERSION) else putInt(KEY_SEED_VERSION, value)
        }

    override var clockAnchorMillis: Long?
        get() = if (prefs.contains(KEY_CLOCK_ANCHOR)) prefs.getLong(KEY_CLOCK_ANCHOR, 0L) else null
        set(value) = prefs.edit(commit = true) {
            if (value == null) remove(KEY_CLOCK_ANCHOR) else putLong(KEY_CLOCK_ANCHOR, value)
        }

    companion object {
        const val PREFS_NAME = "luma_state"
        private const val KEY_SEED_VERSION = "seed_version"
        private const val KEY_CLOCK_ANCHOR = "clock_anchor_millis"
    }
}
