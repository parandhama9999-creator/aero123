package com.aistudio.aerofast.trmst.data

import kotlinx.serialization.Serializable

/** A tensile strength band from the customer drawing (e.g. "1000–1240 MPa") instead of a property class. */
@Serializable
data class TensileRange(val minMpa: Double, val maxMpa: Double) {
    val label: String get() = "Rm ${minMpa.toInt()}–${maxMpa.toInt()} MPa (drawing)"

    /** Q&T alloy steel ratios as in ISO 898-1 class 10.9: Rp0.2 ≈ 0.9 Rm, proof stress ≈ 0.8 Rm. */
    val strength: ClassStrength get() = ClassStrength(minMpa, 0.9 * minMpa, 0.8 * minMpa, false)

    companion object {
        fun of(min: Double?, max: Double?): TensileRange? {
            if (min == null || min <= 0.0) return null
            val hi = max?.takeIf { it >= min } ?: min
            return TensileRange(min, hi)
        }

        /** Reads "1000-1240 MPa", "1000 – 1240MPa", "160-180 KSI", "TENSILE STRENGTH = 1000-1240MPa" or a single minimum. */
        fun parse(text: String): TensileRange? = NativeEngine.run("TensileRange.parse", NativeEngine.a(text))
    }
}
