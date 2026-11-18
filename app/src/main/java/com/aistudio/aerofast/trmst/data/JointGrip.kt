package com.aistudio.aerofast.trmst.data

import kotlin.math.ceil

enum class GripStatus { THREADS_IN_JOINT, OK, EXCESS_GRIP }

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

/**
 * Aerospace joint grip (AC 43.13-1B ch.7): the plain shank must carry the whole joint so no thread is in
 * shear or bearing; any grip beyond the stack is taken up with washers under the nut.
 */
object JointGrip {
    /** AN / NAS / Hi-Lok grips step in 1/16"; metric aerospace grips are taken in 1 mm steps. */
    fun stepMm(unit: UnitSystem) = if (unit == UnitSystem.INCH) 25.4 / 16 else 1.0

    fun appliesTo(type: FastenerType) =
        PokaYokeEngine.isAerospaceType(type) && !type.isNut && !type.isWasher && !type.isUnthreaded && !type.isStud

    /** [runoutMm] is the incomplete-thread allowance beyond b (2P, as on the Sizing card). */
    fun check(unit: UnitSystem, lengthMm: Double, threadLengthMm: Double, runoutMm: Double, stackMm: Double): GripCheck {
        val step = stepMm(unit)
        val gripMax = (lengthMm - threadLengthMm).coerceAtLeast(0.0)
        val gripMin = (gripMax - runoutMm).coerceAtLeast(0.0)
        // Small tolerance so a stack typed to the same value as the grip is not flagged by rounding
        val eps = 1e-6
        val suggested = ceil(stackMm / step - eps).coerceAtLeast(0.0) * step + threadLengthMm + runoutMm
        val status = when {
            gripMin + eps < stackMm -> GripStatus.THREADS_IN_JOINT
            gripMin - stackMm > step + eps -> GripStatus.EXCESS_GRIP
            else -> GripStatus.OK
        }
        return GripCheck(status, gripMin, gripMax, stackMm, suggested, step)
    }
}
