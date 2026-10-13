package com.mulkatz.lumacare.data.local

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Zeiten als ISO-String mit festen Sekunden (`2026-10-12T07:15:00`), damit sie als Text sortierbar sind.
 * Listen und Wochentage als JSON-Array. Enums speichert Room selbst als Namen.
 */
class Converters {
    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime): String = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(value)

    @TypeConverter
    fun toLocalDateTime(value: String): LocalDateTime = LocalDateTime.parse(value)

    @TypeConverter
    fun fromLocalTime(value: LocalTime): String = DateTimeFormatter.ISO_LOCAL_TIME.format(value)

    @TypeConverter
    fun toLocalTime(value: String): LocalTime = LocalTime.parse(value)

    @TypeConverter
    fun fromStringList(value: List<String>): String = Json.encodeToString(value)

    @TypeConverter
    fun toStringList(value: String): List<String> = Json.decodeFromString(value)

    @TypeConverter
    fun fromDays(value: Set<DayOfWeek>): String = Json.encodeToString(value.sorted().map { it.name })

    @TypeConverter
    fun toDays(value: String): Set<DayOfWeek> =
        Json.decodeFromString<List<String>>(value).mapTo(sortedSetOf()) { DayOfWeek.valueOf(it) }
}
