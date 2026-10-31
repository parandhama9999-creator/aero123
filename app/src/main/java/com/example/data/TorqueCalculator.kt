package com.example.data

import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sqrt

data class TorqueResult(
    val preloadN: Double,
    val torqueNm: Double,
    val pitchTorqueNm: Double,
    val threadFrictionTorqueNm: Double,
    val bearingFrictionTorqueNm: Double,
    val nutFactorK: Double,
    val bearingDiaDkmMm: Double,
    val axialStressMpa: Double,
    val torsionStressMpa: Double,
    val vonMisesStressMpa: Double,
    /** σred / Rp0.2 during tightening; VDI 2230 limits this to 0.9. */
    val yieldUtilization: Double
)

/** VDI 2230 / ISO 16047 tightening torque with the assembly (tension + torsion) stress check. */
object TorqueCalculator {

    // ISO 273 medium clearance hole dh, by nominal diameter
    private val CLEARANCE_HOLE = mapOf(
        1.6 to 1.8, 2.0 to 2.4, 2.5 to 2.9, 3.0 to 3.4, 4.0 to 4.5, 5.0 to 5.5, 6.0 to 6.6, 8.0 to 9.0,
        10.0 to 11.0, 12.0 to 13.5, 14.0 to 15.5, 16.0 to 17.5, 20.0 to 22.0, 24.0 to 26.0, 30.0 to 33.0, 36.0 to 39.0
    )

    /** Mean bearing diameter DKm = (dw + dh) / 2, with dw from the ISO 4014 table (DimensionTables). */
    fun bearingDiameter(d: Double, dwOverride: Double? = null): Double {
        val dh = CLEARANCE_HOLE.entries.firstOrNull { kotlin.math.abs(it.key - d) < 0.01 }?.value ?: (1.08 * d)
        val dw = dwOverride ?: DimensionTables.forPart(FastenerType.HEX_BOLT, UnitSystem.METRIC, d).bearingDiaDw
        return (dw + dh) / 2.0
    }

    /** Typical friction (μG thread, μK head) for the finish; real values must come from ISO 16047 tests. */
    fun typicalFriction(coating: CoatingType, family: MaterialFamily): Pair<Double, Double> {
        if (family == MaterialFamily.STAINLESS || family == MaterialFamily.TITANIUM) return 0.20 to 0.20 // galling-prone, unlubricated
        return when (coating) {
            CoatingType.GEOMET, CoatingType.DACROMET, CoatingType.ZN_NI, CoatingType.MAGNI_565 -> 0.12 to 0.12 // with integrated lubricant / top coat
            CoatingType.PTFE_XYLAN -> 0.08 to 0.08
            CoatingType.PHOSPHATE, CoatingType.PHOSPHATE_MN, CoatingType.BLACK_OXIDE -> 0.14 to 0.14 // oiled
            CoatingType.HOT_DIP_GALV -> 0.16 to 0.16 // waxed nut assumed
            CoatingType.ZINC_BLUE, CoatingType.ZINC_YELLOW, CoatingType.ZN_FE, CoatingType.TIN -> 0.16 to 0.16 // no lubricant
            else -> if (coating.name.contains("CAD")) 0.09 to 0.09 else 0.14 to 0.14
        }
    }

    fun compute(
        d: Double,
        pitch: Double,
        stressAreaMm2: Double,
        yieldMpa: Double,
        preloadN: Double,
        muThread: Double,
        muBearing: Double,
        bearingDiaDw: Double? = null
    ): TorqueResult {
        val d2 = d - 0.649519 * pitch
        val d3 = d - 1.226869 * pitch
        val d0 = (d2 + d3) / 2.0
        val dkm = bearingDiameter(d, bearingDiaDw)

        val pitchTorque = preloadN * 0.16 * pitch / 1000.0
        val threadFriction = preloadN * 0.58 * d2 * muThread / 1000.0
        val bearingFriction = preloadN * muBearing * dkm / 2.0 / 1000.0
        val total = pitchTorque + threadFriction + bearingFriction

        val sigma = preloadN / stressAreaMm2
        val tau = (pitchTorque + threadFriction) * 1000.0 / (PI / 16.0 * d0.pow(3))
        val sigmaRed = sqrt(sigma * sigma + 3.0 * tau * tau)

        return TorqueResult(
            preloadN = preloadN,
            torqueNm = total,
            pitchTorqueNm = pitchTorque,
            threadFrictionTorqueNm = threadFriction,
            bearingFrictionTorqueNm = bearingFriction,
            nutFactorK = if (preloadN > 0) total * 1000.0 / (preloadN * d) else 0.0,
            bearingDiaDkmMm = dkm,
            axialStressMpa = sigma,
            torsionStressMpa = tau,
            vonMisesStressMpa = sigmaRed,
            yieldUtilization = sigmaRed / yieldMpa
        )
    }
}
