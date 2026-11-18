package com.aistudio.aerofast.trmst.data

/** A tensile strength band from the customer drawing (e.g. "1000–1240 MPa") instead of a property class. */
data class TensileRange(val minMpa: Double, val maxMpa: Double) {
    val label: String get() = "Rm ${minMpa.toInt()}–${maxMpa.toInt()} MPa (drawing)"
    val midMpa: Double get() = (minMpa + maxMpa) / 2.0

    /** Q&T alloy steel ratios as in ISO 898-1 class 10.9: Rp0.2 ≈ 0.9 Rm, proof stress ≈ 0.8 Rm. */
    val strength: ClassStrength get() = ClassStrength(minMpa, 0.9 * minMpa, 0.8 * minMpa, false)

    companion object {
        fun of(min: Double?, max: Double?): TensileRange? {
            if (min == null || min <= 0.0) return null
            val hi = max?.takeIf { it >= min } ?: min
            return TensileRange(min, hi)
        }

        /** Reads "1000-1240 MPa", "1000 – 1240MPa", "160-180 KSI", "TENSILE STRENGTH = 1000-1240MPa" or a single minimum. */
        fun parse(text: String): TensileRange? {
            val t = text.uppercase().replace(',', '.')
            val ksi = "KSI" in t
            if (!ksi && "MPA" !in t && "N/MM" !in t) return null
            val nums = Regex("""\d+(\.\d+)?""").findAll(t).map { it.value.toDouble() }
                .filter { if (ksi) it in 40.0..400.0 else it in 300.0..2500.0 }.toList()
            if (nums.isEmpty()) return null
            val f = if (ksi) 6.894757 else 1.0
            return of(nums.first() * f, nums.getOrNull(1)?.times(f))
        }
    }
}

/** Steel hardness for a tensile strength, from the ISO 898-1 class pairs (8.8 → 12.9) extended by ISO 18265. */
object HardnessConversion {
    private val points = listOf(800.0 to 22.0, 1040.0 to 32.0, 1220.0 to 39.0, 1420.0 to 44.0, 1650.0 to 48.0, 1950.0 to 53.0)

    fun hrcFor(rmMpa: Double): Double {
        if (rmMpa <= points.first().first) return points.first().second
        points.zipWithNext().forEach { (a, b) ->
            if (rmMpa <= b.first) return a.second + (rmMpa - a.first) / (b.first - a.first) * (b.second - a.second)
        }
        return points.last().second
    }
}

/** Typical tempering response of oil-quenched alloy steels: a starting temperature for the first trial temper. */
object TemperGuide {
    // AISI 4340, oil quenched from 845 °C, small section (ASM Heat Treater's Guide): temper °C to HRC
    private val curves = mapOf(
        "4340" to listOf(205.0 to 53.0, 315.0 to 49.5, 425.0 to 46.0, 540.0 to 39.0, 650.0 to 31.0)
    )

    fun startTempC(materialKey: String, targetHrc: Double): Double? {
        val c = curves[materialKey.uppercase()] ?: return null
        if (targetHrc >= c.first().second) return c.first().first
        if (targetHrc <= c.last().second) return c.last().first
        c.zipWithNext().forEach { (a, b) ->
            if (targetHrc <= a.second && targetHrc >= b.second)
                return a.first + (a.second - targetHrc) / (a.second - b.second) * (b.first - a.first)
        }
        return null
    }
}
