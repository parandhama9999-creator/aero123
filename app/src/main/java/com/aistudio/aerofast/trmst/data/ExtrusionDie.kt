package com.aistudio.aerofast.trmst.data

import java.util.Locale
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan

/**
 * Published guideline values for forward extrusion of steel, as quoted by the sources listed in [SOURCES].
 * These are handbook / industry guidelines, not an ISO standard: no standard sets extrusion die geometry.
 */
object ExtrusionData {
    /** Free (open-die) forward extrusion of solid bars: 30 – 35 % reduction in area. */
    const val OPEN_LOW = 30.0
    const val OPEN_HIGH = 35.0
    /** Billet fully enclosed in the die (trapped / closed): 70 – 75 %. */
    const val TRAPPED_LOW = 70.0
    const val TRAPPED_HIGH = 75.0

    /** Die opening angle 2α for forward extrusion of solids: 24° – 70°. */
    const val ANGLE_SOLID_LOW = 24.0
    const val ANGLE_SOLID_HIGH = 70.0

    /** Die land for steel forward extrusion: common practice 1.5 – 4.5 mm; another source 0.75 – 3.25 mm. */
    const val LAND_LOW = 1.5
    const val LAND_HIGH = 4.5
    const val LAND_ALT_LOW = 0.75
    const val LAND_ALT_HIGH = 3.25

    /** Practical limits: punch pressure about 2400 MPa, die internal pressure about 1900 MPa. */
    const val PUNCH_LIMIT_MPA = 2400.0
    const val DIE_INTERNAL_LIMIT_MPA = 1900.0
    /** Conventional HSS punches (M2, M42) 1800 – 2200 MPa; PM-HSS about 2500 MPa. */
    const val HSS_LOW = 1800.0
    const val HSS_HIGH = 2200.0
    const val PM_HSS = 2500.0

    /** Johnson extrusion pressure p = σ̄ (a + b ln R): a ≈ 0.8, b ≈ 1.2 – 1.5. */
    const val JOHNSON_A = 0.8
    const val JOHNSON_B_LOW = 1.2
    const val JOHNSON_B_HIGH = 1.5

    /** Approximate maximum extrusion ratios published per material (type 305 stainless, 3.5, is not in the catalogue). */
    private val MAX_RATIO = mapOf("1018" to 5.0)
    fun maxRatio(m: MaterialInfo): Double? = MAX_RATIO[m.key.uppercase(Locale.ROOT)]

    val SOURCES = listOf(
        "Open 30 – 35 %, trapped 70 – 75 %, 2α 24° – 70° for solids, punch ≈ 2400 MPa / die ≈ 1900 MPa, max extrusion ratio ≈ 5 for AISI 1018 and 3.5 for type 305 stainless: IspatGuru, Cold Extrusion Process for Steel (ispatguru.com)",
        "Trapped shank extrusion up to 75 % vs the 30 % ground rule for open extrusion: Heading Hints (cold-forging guide)",
        "Extrusion angle should not exceed 30° (convention not stated): Carpenter, Heading Hints — A Guide to Cold Forming Specialty Alloys",
        "Reductions approaching 70 – 75 % by trapping the material: Machine Design, Banging out tiny parts (machinedesign.com)",
        "Die land 1.5 – 4.5 mm common practice; 0.75 – 3.25 mm in another source: cold extrusion handbook text (search excerpt)",
        "M2 / M42 punches 1800 – 2200 MPa, PM-HSS about 2500 MPa: MDPI J. Manuf. Mater. Process. 10(2):70",
        "p = σ̄ (a + b ln R), a ≈ 0.8, b ≈ 1.2 – 1.5 (Johnson): extrusion analysis lecture notes (MSU ME477; extrusion force calculators)"
    )
}

/** Forward-extrusion die for one station, from the reduction it makes and the material's flow curve. */
object ExtrusionDie {
    private fun f(v: Double, dp: Int = 2) = String.format(Locale.US, "%.${dp}f", v)

    /** Length of the reduction cone for an included die angle 2α. */
    fun coneLengthMm(fromMm: Double, toMm: Double, includedAngleDeg: Double): Double =
        (fromMm - toMm) / 2.0 / tan(includedAngleDeg / 2.0 * PI / 180.0)

    /** Mean flow stress over 0..φ for σ = K φⁿ: K φⁿ / (n + 1). */
    fun meanFlowStress(m: MaterialInfo, strain: Double): Double =
        if (strain <= 0.0) 0.0 else m.flowStressCoeffK * strain.pow(m.strainHardeningN) / (m.strainHardeningN + 1.0)

    /** Johnson die pressure band (MPa) for this reduction; container friction is not included. */
    fun pressureMpa(m: MaterialInfo, fromMm: Double, toMm: Double): Pair<Double, Double> {
        val lnR = ln((fromMm / toMm).pow(2.0))
        val s = meanFlowStress(m, lnR)
        return s * (ExtrusionData.JOHNSON_A + ExtrusionData.JOHNSON_B_LOW * lnR) to s * (ExtrusionData.JOHNSON_A + ExtrusionData.JOHNSON_B_HIGH * lnR)
    }

    fun items(ex: StationExtrusion, m: MaterialInfo, setup: MachineSetup): Pair<List<ToolItem>, List<String>> {
        val items = mutableListOf<ToolItem>()
        val problems = mutableListOf<String>()
        val trapped = ex.reductionPct > setup.openLimit
        val container = ex.fromMm + HeadingProgression.stageClearance(ex.fromMm)

        items += ToolItem(if (trapped) "Container bore (trap)" else "Die entry bore", "Ø${f(container)} mm (billet Ø${f(ex.fromMm)} + transfer clearance)", ToolBasis.CALCULATED)
        if (trapped) items += ToolItem("Trap length", if (ex.heldLengthMm > 0.0) "Billet held ${f(ex.heldLengthMm, 1)} mm in the container before the reduction starts" else "Hold the billet in the container before the reduction starts", ToolBasis.CALCULATED)
        items += ToolItem("Land bore", "Ø${f(ex.toMm)} mm (extruded shank)", ToolBasis.CALCULATED)

        val angle = setup.dieAngleDeg
        if (angle != null) {
            items += ToolItem("Die opening angle 2α", "${f(angle, 0)}° (your setting); guideline for solids ${f(ExtrusionData.ANGLE_SOLID_LOW, 0)}° – ${f(ExtrusionData.ANGLE_SOLID_HIGH, 0)}°", ToolBasis.MACHINE)
            items += ToolItem("Reduction cone length", "${f(coneLengthMm(ex.fromMm, ex.toMm, angle))} mm at 2α = ${f(angle, 0)}°", ToolBasis.CALCULATED)
            if (angle < ExtrusionData.ANGLE_SOLID_LOW || angle > ExtrusionData.ANGLE_SOLID_HIGH)
                problems += "die angle 2α ${f(angle, 0)}° is outside the ${f(ExtrusionData.ANGLE_SOLID_LOW, 0)}° – ${f(ExtrusionData.ANGLE_SOLID_HIGH, 0)}° guideline for solid forward extrusion"
        } else {
            items += ToolItem("Die opening angle 2α", "${f(ExtrusionData.ANGLE_SOLID_LOW, 0)}° – ${f(ExtrusionData.ANGLE_SOLID_HIGH, 0)}° for solid forward extrusion; a cold-heading guide says the extrusion angle should not exceed 30° (it does not say whether included or half angle)", ToolBasis.PUBLISHED)
            items += ToolItem("Reduction cone length",
                "${f(coneLengthMm(ex.fromMm, ex.toMm, ExtrusionData.ANGLE_SOLID_HIGH))} mm at 2α = ${f(ExtrusionData.ANGLE_SOLID_HIGH, 0)}° … ${f(coneLengthMm(ex.fromMm, ex.toMm, ExtrusionData.ANGLE_SOLID_LOW))} mm at 2α = ${f(ExtrusionData.ANGLE_SOLID_LOW, 0)}°",
                ToolBasis.CALCULATED)
        }
        items += ToolItem("Land length", setup.landLengthMm?.let { "${f(it, 1)} mm (your setting)" }
            ?: "${f(ExtrusionData.LAND_LOW, 1)} – ${f(ExtrusionData.LAND_HIGH, 1)} mm common practice (another source ${f(ExtrusionData.LAND_ALT_LOW)} – ${f(ExtrusionData.LAND_ALT_HIGH)} mm)",
            if (setup.landLengthMm != null) ToolBasis.MACHINE else ToolBasis.PUBLISHED)
        items += ToolItem("Back relief after the land", "Relief diameter and length", ToolBasis.TOOLMAKER)

        val matLimit = ExtrusionData.maxRatio(m)?.let { r -> (1.0 - 1.0 / r) * 100.0 }
        items += ToolItem("Reduction limit for ${m.key}",
            (matLimit?.let { "Max extrusion ratio ≈ ${f(ExtrusionData.maxRatio(m)!!, 1)} for ${m.key} (≈ ${f(it, 0)} %); " } ?: "No grade-specific limit published in the sources; ") +
                "open ${f(ExtrusionData.OPEN_LOW, 0)} – ${f(ExtrusionData.OPEN_HIGH, 0)} %, trapped ${f(ExtrusionData.TRAPPED_LOW, 0)} – ${f(ExtrusionData.TRAPPED_HIGH, 0)} %. This station: ${f(ex.reductionPct, 1)} %",
            ToolBasis.PUBLISHED)
        if (matLimit != null && ex.reductionPct > matLimit) problems += "reduction ${f(ex.reductionPct, 0)} % is over the published ≈ ${f(matLimit, 0)} % for ${m.key}"

        val (pLo, pHi) = pressureMpa(m, ex.fromMm, ex.toMm)
        val areaMm2 = PI / 4.0 * container * container
        items += ToolItem("Extrusion pressure (estimate)",
            "${f(pLo, 0)} – ${f(pHi, 0)} MPa → punch load ≈ ${f(pLo * areaMm2 / 1000.0, 0)} – ${f(pHi * areaMm2 / 1000.0, 0)} kN. " +
                "Johnson p = σ̄(0.8 + 1.2…1.5 ln R) with σ̄ from the ${m.key} flow curve (K ${f(m.flowStressCoeffK, 0)} MPa, n ${f(m.strainHardeningN)}); container friction not included",
            ToolBasis.ESTIMATE)
        items += ToolItem("Tool pressure limits",
            "Punch ≈ ${f(ExtrusionData.PUNCH_LIMIT_MPA, 0)} MPa, die internal ≈ ${f(ExtrusionData.DIE_INTERNAL_LIMIT_MPA, 0)} MPa; M2 / M42 punch ${f(ExtrusionData.HSS_LOW, 0)} – ${f(ExtrusionData.HSS_HIGH, 0)} MPa, PM-HSS ≈ ${f(ExtrusionData.PM_HSS, 0)} MPa",
            ToolBasis.PUBLISHED)
        when {
            pHi > ExtrusionData.PUNCH_LIMIT_MPA -> problems += "estimated pressure up to ${f(pHi, 0)} MPa is over the ≈ ${f(ExtrusionData.PUNCH_LIMIT_MPA, 0)} MPa punch limit: reduce the reduction per station or expect much shorter tool life"
            pHi > ExtrusionData.HSS_LOW -> items += ToolItem("Punch steel", "Up to ${f(pHi, 0)} MPa is above the ${f(ExtrusionData.HSS_LOW, 0)} MPa low end for M2 / M42: PM-HSS punch preferred", ToolBasis.PUBLISHED)
        }
        return items to problems
    }
}
