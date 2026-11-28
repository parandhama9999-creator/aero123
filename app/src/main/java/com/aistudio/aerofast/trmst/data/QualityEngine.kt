package com.aistudio.aerofast.trmst.data

import kotlinx.serialization.Serializable

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

/** Process capability of individual measurements against a two-sided (or one-sided) specification. */
@Serializable
data class Capability(
    val n: Int,
    val mean: Double,
    val min: Double,
    val max: Double,
    /** Short-term sigma from the average moving range: MR̄ / d2, d2 = 1.128 for ranges of two. */
    val sigmaWithin: Double,
    /** Long-term sigma: sample standard deviation (n − 1). */
    val sigmaOverall: Double,
    val cp: Double?,
    val cpk: Double?,
    val pp: Double?,
    val ppk: Double?,
    /** Expected parts per million outside the limits, normal model on the overall sigma. */
    val ppmOutside: Double,
    val outOfSpecCount: Int
)

/** VDI 2230 Part 1 (2015) assembly preload and tightening torque for a metric bolt tightened to ν·Rp0.2. */
@Serializable
data class TorqueTension(
    val d2Mm: Double,
    val d3Mm: Double,
    val stressAreaMm2: Double,
    val dkmMm: Double,
    /** Assembly preload FM at utilisation ν of the minimum yield, kN. */
    val preloadKn: Double,
    /** Tightening torque MA, N·m. */
    val torqueNm: Double,
    /** Nut factor K = MA / (FM · d). */
    val nutFactorK: Double,
    /** Share of the torque spent on thread friction, head friction and preload (pitch). */
    val threadFrictionPct: Double,
    val headFrictionPct: Double,
    val pitchPct: Double
)

@Serializable
data class Oee(val availability: Double, val performance: Double, val quality: Double) {
    val oee get() = availability * performance * quality
}
