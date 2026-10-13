package com.mulkatz.lumacare.data.repository

import com.mulkatz.lumacare.domain.CareData
import kotlinx.coroutines.flow.Flow

/** Einziger Datenzugang der UI. Liefert Domain-Modelle, keine Room-Entities. */
interface CareRepository {
    /** Importiert den Seed, falls noch nicht geschehen. Läuft nicht auf dem Main-Thread. */
    suspend fun ensureSeeded()

    /** Bewohnende, Hinweise, Routinen und Aktivitäten als ein Stand; emittiert bei jeder Änderung neu. */
    fun observeCareData(): Flow<CareData>
}
