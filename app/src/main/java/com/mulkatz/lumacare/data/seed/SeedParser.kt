package com.mulkatz.lumacare.data.seed

import kotlinx.serialization.json.Json

/** Fehler im Seed (ungültiges JSON oder unbekannter Wert); der Import bricht damit ab. */
class SeedFormatException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

object SeedParser {
    /** Name der Seed-Datei im (generierten) Asset-Verzeichnis. */
    const val ASSET_NAME = "luma-seed.json"

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): SeedFile =
        try {
            json.decodeFromString<SeedFile>(text)
        } catch (e: IllegalArgumentException) {
            // umfasst SerializationException
            throw SeedFormatException("Seed ist kein gültiges JSON im erwarteten Format: ${e.message}", e)
        }
}
