package com.mulkatz.lumacare.data.seed

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Abbild von `docs/case/luma-seed.json`; Werte bleiben Strings, das Mapping macht der [SeedImporter]. */
@Serializable
data class SeedFile(
    val meta: SeedMeta,
    val residents: List<SeedResident>,
    @SerialName("luma_events") val lumaEvents: List<SeedEvent>,
    val routines: List<SeedRoutine>,
    val activities: List<SeedActivity>,
)

@Serializable
data class SeedMeta(
    val facility: String,
    val unit: String,
    val shift: SeedShift,
    val now: String,
)

@Serializable
data class SeedShift(
    val type: String,
    val start: String,
    val end: String,
)

@Serializable
data class SeedResident(
    val id: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    val age: Int,
    val room: String,
    @SerialName("care_level") val careLevel: Int,
    val biography: List<String> = emptyList(),
    val preferences: List<String> = emptyList(),
    val contact: SeedContact? = null,
)

@Serializable
data class SeedContact(
    val name: String,
    val relation: String,
)

@Serializable
data class SeedEvent(
    val id: String,
    @SerialName("resident_id") val residentId: String,
    val timestamp: String,
    val type: String,
    val severity: String,
    val status: String,
    val summary: String,
)

@Serializable
data class SeedRoutine(
    val id: String,
    @SerialName("resident_id") val residentId: String,
    val title: String,
    val time: String,
    val days: List<String>,
    @SerialName("luma_call") val lumaCall: Boolean,
)

@Serializable
data class SeedActivity(
    val id: String,
    val title: String,
    val start: String,
    val location: String,
    val registered: List<String> = emptyList(),
)
