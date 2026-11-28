package com.aistudio.aerofast.trmst.data

import kotlinx.serialization.Serializable

/**
 * A furnace cycle with a time on every stage, for the routes that are not the spheroidize anneal
 * (that one is [AnnealCycleEngine]). Same shape as [AnnealCycle] so both render the same way.
 */
@Serializable
data class FurnaceCycle(
    val title: String,
    val stages: List<AnnealStage>,
    val basis: String,
    val sources: List<String>,
    val cautions: List<String>
) {
    val totalLow: Double get() = stages.sumOf { it.hoursLow }
    val totalHigh: Double get() = stages.sumOf { it.hoursHigh }
    val totalText: String get() = String.format(java.util.Locale.US, "%.1f – %.1f h", totalLow, totalHigh)
}
