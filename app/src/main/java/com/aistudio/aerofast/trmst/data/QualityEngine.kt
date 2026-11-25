package com.aistudio.aerofast.trmst.data

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

/** Process capability of individual measurements against a two-sided (or one-sided) specification. */
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

data class Oee(val availability: Double, val performance: Double, val quality: Double) {
    val oee get() = availability * performance * quality
}

object QualityEngine {

    /** Reads numbers separated by spaces, commas, semicolons or new lines; anything else is ignored. */
    fun parseValues(text: String): List<Double> =
        text.split(Regex("[\\s,;]+")).mapNotNull { it.trim().replace("−", "-").toDoubleOrNull() }

    fun capability(values: List<Double>, lsl: Double?, usl: Double?): Capability? {
        if (values.size < 2) return null
        val n = values.size
        val mean = values.average()
        val sOverall = sqrt(values.sumOf { (it - mean).pow(2) } / (n - 1))
        val mrBar = values.zipWithNext { a, b -> abs(b - a) }.average()
        val sWithin = mrBar / 1.128
        fun cp(s: Double) = if (lsl != null && usl != null && s > 0) (usl - lsl) / (6 * s) else null
        fun cpk(s: Double): Double? {
            if (s <= 0) return null
            val upper = usl?.let { (it - mean) / (3 * s) }
            val lower = lsl?.let { (mean - it) / (3 * s) }
            return listOfNotNull(upper, lower).minOrNull()
        }
        val ppm = if (sOverall > 0) {
            val above = usl?.let { 1 - normalCdf((it - mean) / sOverall) } ?: 0.0
            val below = lsl?.let { normalCdf((it - mean) / sOverall) } ?: 0.0
            (above + below) * 1e6
        } else 0.0
        val oos = values.count { (usl != null && it > usl) || (lsl != null && it < lsl) }
        return Capability(n, mean, values.min(), values.max(), sWithin, sOverall, cp(sWithin), cpk(sWithin), cp(sOverall), cpk(sOverall), ppm, oos)
    }

    /** Standard normal CDF (Abramowitz & Stegun 7.1.26 erf, |error| < 1.5e-7). */
    fun normalCdf(z: Double): Double {
        val x = abs(z) / sqrt(2.0)
        val t = 1 / (1 + 0.3275911 * x)
        val erf = 1 - (((((1.061405429 * t - 1.453152027) * t) + 1.421413741) * t - 0.284496736) * t + 0.254829592) * t * exp(-x * x)
        return if (z >= 0) 0.5 * (1 + erf) else 0.5 * (1 - erf)
    }

    /** Inverse standard normal CDF (Acklam's rational approximation, relative error < 1.2e-9). */
    fun normalInv(p: Double): Double {
        require(p > 0 && p < 1)
        val a = doubleArrayOf(-3.969683028665376e+01, 2.209460984245205e+02, -2.759285104469687e+02, 1.383577518672690e+02, -3.066479806614716e+01, 2.506628277459239e+00)
        val b = doubleArrayOf(-5.447609879822406e+01, 1.615858368580409e+02, -1.556989798598866e+02, 6.680131188771972e+01, -1.328068155288572e+01)
        val c = doubleArrayOf(-7.784894002430293e-03, -3.223964580411365e-01, -2.400758277161838e+00, -2.549732539343734e+00, 4.374664141464968e+00, 2.938163982698783e+00)
        val d = doubleArrayOf(7.784695709041462e-03, 3.224671290700398e-01, 2.445134137142996e+00, 3.754408661907416e+00)
        val pLow = 0.02425
        return when {
            p < pLow -> {
                val q = sqrt(-2 * ln(p))
                (((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5]) / ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1)
            }
            p <= 1 - pLow -> {
                val q = p - 0.5
                val r = q * q
                (((((a[0] * r + a[1]) * r + a[2]) * r + a[3]) * r + a[4]) * r + a[5]) * q / (((((b[0] * r + b[1]) * r + b[2]) * r + b[3]) * r + b[4]) * r + 1)
            }
            else -> {
                val q = sqrt(-2 * ln(1 - p))
                -(((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5]) / ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1)
            }
        }
    }

    /** Six Sigma level from defects per million opportunities, with the conventional 1.5 σ long-term shift. */
    fun sigmaLevel(dpmo: Double): Double? =
        if (dpmo <= 0 || dpmo >= 1e6) null else normalInv(1 - dpmo / 1e6) + 1.5

    fun dpmo(defects: Double, units: Double, opportunitiesPerUnit: Double): Double? =
        if (units <= 0 || opportunitiesPerUnit <= 0 || defects < 0) null else defects / (units * opportunitiesPerUnit) * 1e6

    /** OEE = availability × performance × quality (times in minutes, ideal cycle time in seconds per piece). */
    fun oee(plannedMin: Double, downtimeMin: Double, idealCycleSec: Double, totalCount: Double, goodCount: Double): Oee? {
        val run = plannedMin - downtimeMin
        if (plannedMin <= 0 || run <= 0 || idealCycleSec <= 0 || totalCount <= 0 || goodCount < 0 || goodCount > totalCount) return null
        return Oee(run / plannedMin, (idealCycleSec * totalCount / 60.0) / run, goodCount / totalCount)
    }

    /** ISO 273 medium-series clearance hole dh for a metric thread, or null outside the table. */
    fun clearanceHoleMedium(d: Double): Double? = mapOf(
        1.6 to 1.8, 2.0 to 2.4, 2.5 to 2.9, 3.0 to 3.4, 4.0 to 4.5, 5.0 to 5.5, 6.0 to 6.6, 8.0 to 9.0, 10.0 to 11.0,
        12.0 to 13.5, 14.0 to 15.5, 16.0 to 17.5, 18.0 to 20.0, 20.0 to 22.0, 22.0 to 24.0, 24.0 to 26.0, 27.0 to 30.0,
        30.0 to 33.0, 33.0 to 36.0, 36.0 to 39.0
    ).entries.firstOrNull { abs(it.key - d) < 1e-6 }?.value

    /**
     * VDI 2230 Part 1, eq. (R2/1) and (R13): FM = A0·ν·Rp0.2 / √(1 + 3·[1.5·(d2/d0)·(P/(π·d2) + 1.155·µG)]²) with
     * A0 = As and d0 = ds (the stress diameter), and MA = FM·(0.16·P + 0.58·d2·µG + Dkm/2·µK), Dkm = (dw + dh)/2.
     */
    fun torqueTension(d: Double, p: Double, rp02Mpa: Double, muThread: Double, muHead: Double, dwMm: Double, dhMm: Double, utilisation: Double = 0.9): TorqueTension? {
        if (d <= 0 || p <= 0 || rp02Mpa <= 0 || muThread < 0 || muHead < 0 || dwMm <= 0 || dhMm <= 0 || dwMm <= dhMm || utilisation <= 0 || utilisation > 1) return null
        val d2 = d - 0.649519 * p
        val d3 = d - 1.226869 * p
        val ds = (d2 + d3) / 2
        val asMm2 = PI / 4 * ds * ds
        val torsion = 1.5 * (d2 / ds) * (p / (PI * d2) + 1.155 * muThread)
        val fmN = asMm2 * utilisation * rp02Mpa / sqrt(1 + 3 * torsion * torsion)
        val dkm = (dwMm + dhMm) / 2
        val pitchPart = 0.16 * p
        val threadPart = 0.58 * d2 * muThread
        val headPart = dkm / 2 * muHead
        val sum = pitchPart + threadPart + headPart
        val maNm = fmN * sum / 1000.0
        return TorqueTension(d2, d3, asMm2, dkm, fmN / 1000.0, maNm, maNm * 1000.0 / (fmN * d),
            threadPart / sum * 100, headPart / sum * 100, pitchPart / sum * 100)
    }

    /** ISO 286-1 standard tolerance grades IT13, IT14, IT15 (mm) for nominal sizes up to 315 mm. */
    fun itGrade(grade: Int, size: Double): Double? {
        val ranges = doubleArrayOf(3.0, 6.0, 10.0, 18.0, 30.0, 50.0, 80.0, 120.0, 180.0, 250.0, 315.0)
        val table = mapOf(
            13 to doubleArrayOf(0.14, 0.18, 0.22, 0.27, 0.33, 0.39, 0.46, 0.54, 0.63, 0.72, 0.81),
            14 to doubleArrayOf(0.25, 0.30, 0.36, 0.43, 0.52, 0.62, 0.74, 0.87, 1.00, 1.15, 1.30),
            15 to doubleArrayOf(0.40, 0.48, 0.58, 0.70, 0.84, 1.00, 1.20, 1.40, 1.60, 1.85, 2.10)
        )
        val row = table[grade] ?: return null
        val i = ranges.indexOfFirst { size <= it }
        return if (size <= 0 || i < 0) null else row[i]
    }
}
