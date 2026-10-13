package com.mulkatz.lumacare.domain

import java.time.Duration
import java.time.LocalDateTime

/** Lage eines Stundenblocks relativ zu „jetzt“. */
enum class HourPhase { PAST, CURRENT, FUTURE }

/** Ein Block der Zeitleiste, halboffen [start, end). */
data class HourBlock(
    val start: LocalDateTime,
    val end: LocalDateTime,
    /** Sammelblock der Vorschicht: [20:00 des Vortags, Schichtbeginn). */
    val isNight: Boolean,
    /** Alle Hinweise im Block, auch erledigte. */
    val hintCount: Int,
    /** Höchste Dringlichkeit der offenen Hinweise; `null`, wenn keiner offen ist. */
    val openSeverity: Severity?,
    val phase: HourPhase,
    /** Routinetermine im Block als Anteil 0..1 ab [start], aufsteigend. */
    val routineTicks: List<Float>,
)

/** Position von „jetzt“: Block und Anteil 0..1 innerhalb des Blocks. */
data class NowMarker(val blockIndex: Int, val fraction: Float)

/** Zeitleiste als Stundenblöcke: Nacht als ein Block, danach je ein Block pro Stunde bis Schichtende. */
data class ShiftHours(
    val blocks: List<HourBlock>,
    /** `null`, wenn „jetzt“ außerhalb der Zeitleiste liegt (z. B. nach Schichtende). */
    val nowMarker: NowMarker?,
) {
    companion object {
        private val HOUR: Duration = Duration.ofHours(1)

        /**
         * Reine Funktion aus [timeline] und [now]. Die Schichtgrenzen kommen aus der [Timeline]
         * ([Timeline.shiftStart], [Timeline.end]); die letzte Stunde wird bei Bedarf am Schichtende gekappt.
         */
        fun from(timeline: Timeline, now: LocalDateTime): ShiftHours {
            val bounds = buildList {
                add(timeline.start to timeline.shiftStart)
                var hour = timeline.shiftStart
                while (hour.isBefore(timeline.end)) {
                    val next = minOf(hour + HOUR, timeline.end)
                    add(hour to next)
                    hour = next
                }
            }
            val blocks = bounds.mapIndexed { index, (start, end) ->
                val events = timeline.events.map { it.event }.filter { it.timestamp.isWithin(start, end) }
                HourBlock(
                    start = start,
                    end = end,
                    isNight = index == 0,
                    hintCount = events.size,
                    openSeverity = events.filter { it.isOpen }.minOfOrNull { it.severity },
                    phase = when {
                        !now.isBefore(end) -> HourPhase.PAST
                        now.isBefore(start) -> HourPhase.FUTURE
                        else -> HourPhase.CURRENT
                    },
                    routineTicks = timeline.routines
                        .map { it.time }
                        .filter { it.isWithin(start, end) }
                        .map { fraction(start, end, it) }
                        .sorted(),
                )
            }
            val current = blocks.indexOfFirst { it.phase == HourPhase.CURRENT }
            val marker = if (current < 0) null else blocks[current].let { NowMarker(current, fraction(it.start, it.end, now)) }
            return ShiftHours(blocks, marker)
        }

        private fun fraction(start: LocalDateTime, end: LocalDateTime, time: LocalDateTime): Float =
            Duration.between(start, time).toMillis().toFloat() / Duration.between(start, end).toMillis()

        /** Halboffenes Intervall [from, to). */
        private fun LocalDateTime.isWithin(from: LocalDateTime, to: LocalDateTime): Boolean =
            !isBefore(from) && isBefore(to)
    }
}
