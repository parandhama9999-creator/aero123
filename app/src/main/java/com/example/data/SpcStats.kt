package com.example.data

import kotlin.math.sqrt

data class SpcResult(
    val n: Int,
    val mean: Double,
    val sigma: Double,
    val cp: Double,
    val cpk: Double,
    val ucl: Double,
    val lcl: Double,
    val outOfSpec: Int
)

/** Process capability from measured readings (sample standard deviation, n − 1). */
object SpcStats {
    /** Parses "8.95, 8.96 8.94;8.97" style input; ignores anything that is not a number. */
    fun parse(text: String): List<Double> =
        text.split(Regex("[\\s,;]+")).mapNotNull { it.trim().replace(',', '.').toDoubleOrNull() }

    fun compute(readings: List<Double>, lsl: Double, usl: Double): SpcResult? {
        if (readings.size < 2 || usl <= lsl) return null
        val mean = readings.average()
        val sigma = sqrt(readings.sumOf { (it - mean) * (it - mean) } / (readings.size - 1))
        if (sigma <= 0.0) return null
        return SpcResult(
            n = readings.size,
            mean = mean,
            sigma = sigma,
            cp = (usl - lsl) / (6 * sigma),
            cpk = minOf(usl - mean, mean - lsl) / (3 * sigma),
            ucl = mean + 3 * sigma,
            lcl = mean - 3 * sigma,
            outOfSpec = readings.count { it < lsl || it > usl }
        )
    }
}
