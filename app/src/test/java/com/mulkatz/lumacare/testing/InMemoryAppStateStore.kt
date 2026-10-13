package com.mulkatz.lumacare.testing

import com.mulkatz.lumacare.domain.AppStateStore

/** Fake für Tests: hält Seed-Version und Clock-Anker nur im Speicher. */
class InMemoryAppStateStore(
    override var seedVersion: Int? = null,
    override var clockAnchorMillis: Long? = null,
) : AppStateStore
