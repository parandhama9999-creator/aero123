package com.aistudio.aerofast.trmst.data

import kotlinx.serialization.Serializable

import kotlin.math.ceil

enum class GripStatus { THREADS_IN_JOINT, OK, EXCESS_GRIP }

@Serializable
data class GripCheck(
    val status: GripStatus,
    /** Shortest plain shank: L − b − runout, so no incomplete thread is counted as shank. */
    val gripMinMm: Double,
    /** Longest plain shank: L − b. */
    val gripMaxMm: Double,
    val stackMm: Double,
    /** Shortest length, in whole grip steps, that keeps every thread (runout included) out of the joint. */
    val suggestedLengthMm: Double,
    val stepMm: Double
) {
    val threadsInJointMm get() = (stackMm - gripMinMm).coerceAtLeast(0.0)
    val excessMm get() = (gripMinMm - stackMm).coerceAtLeast(0.0)
}
