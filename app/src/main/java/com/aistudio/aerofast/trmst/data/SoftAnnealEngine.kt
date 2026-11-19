package com.aistudio.aerofast.trmst.data

/** How the wire is softened before cold heading. */
enum class AnnealRoute(val label: String, val purpose: String) {
    SPHEROIDIZE(
        "Sub-critical spheroidize anneal",
        "Convert lamellar pearlite to globular cementite so the wire can be headed without cracking"
    ),
    SOLUTION_ANNEAL(
        "Solution anneal + rapid cool",
        "Dissolve carbides and restore ductile austenite; rapid cool prevents sensitisation"
    ),
    O_TEMPER(
        "Full anneal to O temper",
        "Fully soften to the annealed (O) condition"
    ),
    MILL_ANNEALED(
        "Mill-annealed — supplied ready to head",
        "Wire is bought in the annealed / Condition A state; no in-house anneal"
    ),
    ANNEAL_REQUIRED(
        "Anneal required — wire supplied hard",
        "Catalogue condition is cold drawn / half-hard, which is not a headable state"
    )
}

/**
 * Wire-preparation anneal that happens BEFORE cut-off and heading.
 *
 * Everything here is derived from data already in the material catalogue — the supplied-wire
 * condition string and the post-forming heat-treatment cycle. Any value that could not be
 * sourced from the catalogue is left blank and listed in [unsourced] rather than invented.
 */
data class SoftAnnealSpec(
    val route: AnnealRoute,
    val inHouseAnnealNeeded: Boolean,
    val suppliedCondition: String,
    val temperatureC: String,
    val soakTime: String,
    val coolingRate: String,
    val atmosphere: String,
    val targetUtsMpa: String,
    val targetHardnessHb: String,
    val targetHardnessHrb: String,
    val structureTarget: String,
    val standardReference: String,
    /** Annealed UTS / post-forging UTS. Low values mean the wire is much softer than the finished part. */
    val softeningRatio: Double?,
    /** Fields the catalogue could not supply; the user fills these per grade. */
    val unsourced: List<String>,
    val notes: List<String>,
    /** Where the anneal cycle came from, when it is not derived from the catalogue. */
    val cycleSource: String = "",
    val cycleConfidence: String = ""
)

/** A published mill / softening anneal cycle, with the datasheet it came from. */
data class MillAnnealCycle(
    val temperatureC: String,
    val hold: String,
    val cooling: String,
    val source: String,
    val confidence: String,
    val note: String = ""
)

object SoftAnnealEngine {

    private const val HIGH = "High — producer / supplier datasheet"
    private const val MEDIUM = "Medium — consistent secondary sources"
    private const val DISPUTED = "Medium — sources disagree"
    private const val LOW = "Low — verify against GOST / TU before use"

    /**
     * Researched mill / softening anneal cycles for the grades bought mill-annealed (see MILL_ANNEAL_CYCLES.md).
     * Kept apart from heatTreatmentSpec on purpose: for titanium that field is the solution-treat temperature
     * used for hardening, roughly 200 °C above the anneal. Blank hold / cooling means the source did not state it.
     */
    val MILL_ANNEAL: Map<String, MillAnnealCycle> = mapOf(
        "TI_GR5" to MillAnnealCycle("704 – 788 °C (1300 – 1450 °F)", "2 h", "Air cool",
            "Rolled Alloys 6Al-4V datasheet", HIGH),
        "TI_GR23" to MillAnnealCycle("704 – 732 °C (1300 – 1350 °F)", "1 – 8 h", "Air cool",
            "ATI 6-4 ELI datasheet (AMS 4930, annealed)", HIGH),
        "TI_6242" to MillAnnealCycle("704 – 843 °C (1300 – 1550 °F)", "", "Air cool",
            "TIMET 6242 datasheet", HIGH,
            "Duplex anneal alternative: 899 °C 30 min AC, then 788 °C 15 min AC."),
        "TI_GR1" to MillAnnealCycle("649 – 760 °C (1200 – 1400 °F)", "6 min – 2 h", "Air cool",
            "Corrosion Materials / Rolled Alloys CP Ti datasheets", HIGH),
        "VT16" to MillAnnealCycle("770 – 790 °C", "2 h", "Furnace cool 1 – 5 °C/min to 500 °C, then air",
            "TsKBA standard process", HIGH),
        "INCONEL718" to MillAnnealCycle("927 – 1010 °C (1700 – 1850 °F), 980 °C typical", "", "Air cool or faster",
            "HPAlloys; Total Materia", MEDIUM),
        "A286" to MillAnnealCycle("982 °C (1800 °F); 899 °C (1650 °F) for more ductility", "1 h", "Water or oil quench",
            "Aircraft Materials; UPMET (AMS 5731 / 5732 / 5737)", MEDIUM),
        "WASPALOY" to MillAnnealCycle("1049 °C (1920 °F)", "4 h", "Air cool",
            "Alloy Wire; Haynes", MEDIUM,
            "Heat through 650 – 870 °C as fast as possible to avoid strain-age cracking."),
        "RENE41" to MillAnnealCycle("1066 – 1080 °C (1950 – 1975 °F)", "4 h", "Air cool",
            "Alfa Chemistry; High Temp Metals", DISPUTED,
            "Heat fast through 650 – 870 °C (strain-age cracking). Catalogue hardening cycle tops out at 1120 °C, above these sources."),
        "INCONEL625" to MillAnnealCycle("982 °C typical (Grade 1, 871 °C min)", "", "Air cool",
            "Langley Alloys; Rickard Metals", DISPUTED,
            "Grade 1 anneal for cold forming. The catalogue's 1090 – 1150 °C is the Grade 2 solution, which coarsens grain."),
        "INCONEL_600" to MillAnnealCycle("1010 °C (1850 °F)", "15 min", "",
            "Total Materia (citing Special Metals); Austral Wright", MEDIUM,
            "Brief exposure to 1040 °C also softens without coarse grain."),
        "HASTELLOY_C276" to MillAnnealCycle("1121 °C (2050 °F)", "", "Water quench (fast air only below 10 mm)",
            "HPAlloys; AZoM", MEDIUM,
            "Slow cooling precipitates carbides and destroys corrosion resistance."),
        "MONEL_K500" to MillAnnealCycle("982 – 1038 °C (1800 – 1900 °F)", "", "Water quench",
            "High Temp Metals; Metal Suppliers Online", MEDIUM),
        "MP159" to MillAnnealCycle("1038 – 1052 °C (1900 – 1925 °F)", "4 – 8 h", "Water quench",
            "Aircraft Materials / UPMET MP159 (AMS 5842)", MEDIUM,
            "Solution-treated stock is the soft condition; strength comes from work-strengthening plus aging."),
        "INCONEL_X750" to MillAnnealCycle("982 °C ± 14 °C (1800 °F)", "", "Air cool or faster",
            "Total Materia Inconel X-750", MEDIUM),
        "TI_BETA_C" to MillAnnealCycle("788 – 927 °C (1450 – 1700 °F) solution treat", "", "",
            "Total Materia Ti-3Al-8V-6Cr-4Mo-4Zr", MEDIUM,
            "Beta alloy: the solution-treated condition is the soft, cold-formable one. Re-solution between blows, age after forming."),
        "10KH11N23T3MR" to MillAnnealCycle("≈ 1000 – 1050 °C (unconfirmed)", "", "Air",
            "Garbled table in a TsKBA amendment only", LOW)
    )

    /** Eutectoid temperature for plain carbon steel. Alloying shifts this, so it is only a baseline. */
    private const val A1_PLAIN_CARBON_C = 723.0

    /** Brinell from tensile strength for steel (ASTM A370 / SAE J417 basis): HB = Rm / 3.38. */
    internal const val HB_PER_MPA = 3.38

    /** ASTM A370 steel HB -> HRB, piecewise over the annealed range. Approximate by nature. */
    internal fun hbToHrb(hb: Double): Double? {
        val pts = listOf(
            100.0 to 56.2, 120.0 to 66.7, 140.0 to 75.0, 160.0 to 83.0,
            180.0 to 89.0, 200.0 to 93.0, 220.0 to 97.0
        )
        if (hb < pts.first().first || hb > pts.last().first) return null
        for (i in 0 until pts.size - 1) {
            val (x0, y0) = pts[i]
            val (x1, y1) = pts[i + 1]
            if (hb in x0..x1) return y0 + (hb - x0) / (x1 - x0) * (y1 - y0)
        }
        return null
    }

    /** Pulls "480 – 560" out of "480 – 560 MPa (Spheroidized Annealed Wire, >85% globular)". */
    internal fun parseUtsRange(s: String): Pair<Double, Double>? {
        val nums = Regex("""(\d+(?:\.\d+)?)""").findAll(s).map { it.value.toDouble() }.toList()
        val range = nums.filter { it >= 100.0 }
        return when {
            range.size >= 2 -> range[0] to range[1]
            range.size == 1 -> range[0] to range[0]
            else -> null
        }
    }

    private fun fmt(v: Double, decimals: Int = 0) =
        String.format(java.util.Locale.US, "%.${decimals}f", v)

    fun build(material: MaterialInfo): SoftAnnealSpec {
        val supplied = material.rawMaterialSourcingUtsMpa
        val cond = supplied.uppercase()
        val family = PokaYokeEngine.materialFamily(material)
        val solutionTemp = material.heatTreatmentSpec.austenitizingOrSolutionTempC

        val suppliedHard = cond.contains("COLD DRAWN") || cond.contains("HALF-HARD") || cond.contains("HALF HARD")
        val route = when {
            suppliedHard && family == MaterialFamily.CARBON_ALLOY_STEEL -> AnnealRoute.ANNEAL_REQUIRED
            suppliedHard -> AnnealRoute.ANNEAL_REQUIRED
            // Maraging steel is iron-nickel martensite at about 0.02 % C. There is no cementite to
            // ball up, so a spheroidize anneal is meaningless: it is solution annealed instead.
            family == MaterialFamily.CARBON_ALLOY_STEEL && !SteelCompositions.canSpheroidise(material) ->
                AnnealRoute.SOLUTION_ANNEAL
            family == MaterialFamily.CARBON_ALLOY_STEEL -> AnnealRoute.SPHEROIDIZE
            family == MaterialFamily.STAINLESS -> AnnealRoute.SOLUTION_ANNEAL
            family == MaterialFamily.ALUMINUM || cond.contains("CONDITION O") -> AnnealRoute.O_TEMPER
            family == MaterialFamily.TITANIUM || family == MaterialFamily.NICKEL_SUPERALLOY -> AnnealRoute.MILL_ANNEALED
            family == MaterialFamily.COPPER_ALLOY -> AnnealRoute.O_TEMPER
            else -> AnnealRoute.MILL_ANNEALED
        }

        val uts = parseUtsRange(supplied)
        val unsourced = mutableListOf<String>()
        val notes = mutableListOf<String>()

        // Hardness: derived from the catalogue's annealed UTS. Steel correlation only.
        val isSteel = family == MaterialFamily.CARBON_ALLOY_STEEL || family == MaterialFamily.STAINLESS
        var hbText = ""
        var hrbText = ""
        if (uts != null && isSteel) {
            val hbLo = uts.first / HB_PER_MPA
            val hbHi = uts.second / HB_PER_MPA
            hbText = "${fmt(hbLo)} – ${fmt(hbHi)} HB (derived: Rm / $HB_PER_MPA)"
            val lo = hbToHrb(hbLo)
            val hi = hbToHrb(hbHi)
            hrbText = if (lo != null && hi != null) {
                "≈ ${fmt(lo)} – ${fmt(hi)} HRB (ASTM A370 conversion)"
            } else {
                unsourced += "Target hardness in HRB — derived HB falls outside the A370 HRB range"
                ""
            }
        } else {
            unsourced += "Target hardness — no steel Rm/HB correlation for this family; enter per grade"
        }

        val softening = if (uts != null && material.postForgingTargetUtsMpa > 0.0) {
            ((uts.first + uts.second) / 2.0) / material.postForgingTargetUtsMpa
        } else null

        val spec = when (route) {
            AnnealRoute.SPHEROIDIZE -> {
                val comp = SteelCompositions.forMaterial(material)
                val ac1 = comp?.let { SteelCompositions.ac1(it) }
                if (ac1 != null) {
                    notes += "A1 = ${fmt(ac1)} °C for this grade, from its nominal composition by Andrews " +
                        "(Mn ${comp.manganese}, Ni ${comp.nickel}, Si ${comp.silicon}, Cr ${comp.chromium} %). " +
                        "Plain carbon sits at ${fmt(A1_PLAIN_CARBON_C)} °C: nickel and manganese pull A1 down, " +
                        "silicon and chromium push it up. Confirm against the mill certificate — a heat at the far " +
                        "end of its range moves A1 by a few degrees."
                } else {
                    notes += "A1 baseline is ${fmt(A1_PLAIN_CARBON_C)} °C for plain carbon steel; this grade's " +
                        "composition is not on file. Cr, Mo and Si raise A1; Mn and Ni lower it — confirm A1 " +
                        "for this grade before setting the furnace."
                    unsourced += "Exact A1 for this grade"
                }
                val a1 = ac1 ?: A1_PLAIN_CARBON_C
                SoftAnnealSpec(
                    route = route,
                    inHouseAnnealNeeded = true,
                    suppliedCondition = supplied,
                    temperatureC = "${fmt(a1 - 40)} – ${fmt(a1 - 15)} °C (below A1 with a 15 °C margin; A1 = ${fmt(a1)} °C)",
                    soakTime = "See the full cycle below — soak is coil-mass driven, not part Ø",
                    coolingRate = "Slow furnace cool ≈ 20 °C/h to 550 °C, then still air (published wire practice)",
                    atmosphere = "Protective N₂ or endothermic gas — prevent decarburisation of the wire surface",
                    targetUtsMpa = supplied,
                    targetHardnessHb = hbText,
                    targetHardnessHrb = hrbText,
                    structureTarget = if (cond.contains("GLOBULAR")) {
                        supplied.substringAfter("(").substringBeforeLast(")")
                    } else "Spheroidised cementite — globular fraction per EN 10263",
                    standardReference = "EN 10263-2 / EN 10263-4 (steel rod & wire for cold heading)",
                    softeningRatio = softening,
                    unsourced = unsourced,
                    notes = notes
                )
            }

            AnnealRoute.SOLUTION_ANNEAL -> {
                val ssType = PokaYokeEngine.stainlessType(material)
                when (ssType) {
                    // The spec card's high temperature on these grades is the HARDENING treatment;
                    // their softening is a sub-critical hold, and the cooling logic is the reverse.
                    PokaYokeEngine.StainlessType.MARTENSITIC -> {
                        val is431 = (material.key + " " + material.name).uppercase().contains("431")
                        notes += "Martensitic stainless: the $solutionTemp on the spec card is the hardening " +
                            "treatment, run after forming. The anneal holds below the critical range and cools " +
                            "slowly — quenching from temperature makes this grade HARD, not soft."
                        if (is431) notes += "A 431-type never anneals fully soft: its nickel keeps austenite " +
                            "stable, so expect about 230 – 270 HB from the sub-critical treatment."
                        SoftAnnealSpec(
                            route = route,
                            inHouseAnnealNeeded = true,
                            suppliedCondition = supplied,
                            temperatureC = if (is431) "620 – 660 °C (sub-critical; full anneal is not effective on a 431-type)"
                            else "845 – 885 °C (full anneal; hardening at $solutionTemp comes after forming)",
                            soakTime = "See the full cycle below",
                            coolingRate = if (is431) "Still air — the sub-critical hold does not re-austenitise"
                            else "Furnace cool ≤ 30 °C/h to 600 °C, then air — the slow leg is the anneal",
                            atmosphere = "Protective atmosphere or vacuum to avoid scaling",
                            targetUtsMpa = supplied,
                            targetHardnessHb = hbText,
                            targetHardnessHrb = hrbText,
                            structureTarget = "Ferrite with coarse spheroidal carbide — the soft, headable structure",
                            standardReference = material.heatTreatmentSpec.standardReference,
                            softeningRatio = softening,
                            unsourced = unsourced,
                            notes = notes
                        )
                    }
                    PokaYokeEngine.StainlessType.FERRITIC -> {
                        notes += "Ferritic stainless does not transformation-harden: the anneal only " +
                            "recrystallises the cold work. Cool briskly through 540 – 400 °C — creeping " +
                            "through that band causes 475 °C embrittlement in a 17 % Cr ferritic."
                        SoftAnnealSpec(
                            route = route,
                            inHouseAnnealNeeded = true,
                            suppliedCondition = supplied,
                            temperatureC = "705 – 790 °C (recrystallisation anneal)",
                            soakTime = "See the full cycle below",
                            coolingRate = "Air cool; pass 540 – 400 °C briskly (475 °C embrittlement)",
                            atmosphere = "Protective atmosphere to keep the surface bright",
                            targetUtsMpa = supplied,
                            targetHardnessHb = hbText,
                            targetHardnessHrb = hrbText,
                            structureTarget = "Recrystallised ferrite, fine equiaxed grain",
                            standardReference = material.heatTreatmentSpec.standardReference,
                            softeningRatio = softening,
                            unsourced = unsourced,
                            notes = notes
                        )
                    }
                    else -> {
                        notes += "Austenitic grades are solution annealed, not spheroidised — the goal is the opposite: " +
                            "dissolve carbides and cool fast enough to keep them in solution."
                        SoftAnnealSpec(
                            route = route,
                            inHouseAnnealNeeded = true,
                            suppliedCondition = supplied,
                            temperatureC = solutionTemp,
                            soakTime = "Hold until fully solutioned, then cool without delay",
                            coolingRate = "Rapid — water quench or forced gas; slow cooling sensitises the grain boundaries",
                            atmosphere = "Bright-anneal (H₂/N₂) or vacuum to avoid scaling",
                            targetUtsMpa = supplied,
                            targetHardnessHb = hbText,
                            targetHardnessHrb = hrbText,
                            structureTarget = "Carbide-free austenite, uniform grain size",
                            standardReference = material.heatTreatmentSpec.standardReference,
                            softeningRatio = softening,
                            unsourced = unsourced,
                            notes = notes
                        )
                    }
                }
            }

            AnnealRoute.O_TEMPER -> {
                unsourced += "Anneal temperature and soak — not in the catalogue for this family"
                notes += "Non-ferrous anneal runs far below steel temperatures. Take the cycle from the supplier datasheet."
                SoftAnnealSpec(
                    route = route,
                    inHouseAnnealNeeded = !cond.contains("CONDITION O") && !cond.contains("NO ANNEAL"),
                    suppliedCondition = supplied,
                    temperatureC = "",
                    soakTime = "",
                    coolingRate = "Controlled slow cool to the fully soft condition",
                    atmosphere = "Protective or air, per supplier",
                    targetUtsMpa = supplied,
                    targetHardnessHb = hbText,
                    targetHardnessHrb = hrbText,
                    structureTarget = "Fully recrystallised, O temper",
                    standardReference = "Supplier datasheet",
                    softeningRatio = softening,
                    unsourced = unsourced,
                    notes = notes
                )
            }

            AnnealRoute.MILL_ANNEALED -> {
                notes += "Bought already annealed — verify the condition on the mill certificate. " +
                    "The cycle shown is the mill anneal, for re-annealing between blows."
                val mill = MILL_ANNEAL[material.key]
                if (mill == null) {
                    unsourced += "Mill-anneal cycle — not researched for this grade"
                } else {
                    if (mill.hold.isBlank()) unsourced += "Hold time — not stated in the source"
                    if (mill.cooling.isBlank()) unsourced += "Cooling — not stated in the source"
                    if (mill.confidence == LOW) unsourced += "Confirm the anneal cycle against the GOST / TU for this grade"
                    if (mill.note.isNotBlank()) notes += mill.note
                }
                val isTi = family == MaterialFamily.TITANIUM
                SoftAnnealSpec(
                    route = route,
                    inHouseAnnealNeeded = false,
                    suppliedCondition = supplied,
                    temperatureC = mill?.temperatureC ?: "N/A — supplied annealed",
                    soakTime = mill?.hold ?: "N/A",
                    coolingRate = mill?.cooling ?: "N/A",
                    atmosphere = if (mill == null) "N/A" else if (isTi) {
                        "Vacuum or argon — air above ~600 °C forms alpha-case, which cracks on heading"
                    } else "Vacuum, argon or protective gas",
                    cycleSource = mill?.source ?: "",
                    cycleConfidence = mill?.confidence ?: "",
                    targetUtsMpa = supplied,
                    targetHardnessHb = hbText,
                    targetHardnessHrb = hrbText,
                    structureTarget = "As supplied — confirm against the mill certificate",
                    standardReference = material.heatTreatmentSpec.standardReference,
                    softeningRatio = softening,
                    unsourced = unsourced,
                    notes = notes
                )
            }

            AnnealRoute.ANNEAL_REQUIRED -> {
                notes += if (family == MaterialFamily.CARBON_ALLOY_STEEL)
                    "The catalogue lists this wire as cold drawn / half-hard, which is not a headable condition. " +
                        "Either source spheroidize-annealed wire, or anneal before heading."
                else
                    "The catalogue lists this wire in the cold-drawn condition. For ${material.name} that is the " +
                        "work-strengthened delivery state, not a headable condition, and it is NOT spheroidize " +
                        "annealed: " +
                        "the soft condition is the solution-treated one. Source solution-treated stock, or take the " +
                        "solution cycle from the alloy's own AMS specification before forming."
                unsourced += "Anneal cycle — none in the catalogue, because this grade is listed in the hard condition"
                SoftAnnealSpec(
                    route = route,
                    inHouseAnnealNeeded = true,
                    suppliedCondition = supplied,
                    temperatureC = "",
                    soakTime = "",
                    coolingRate = "",
                    atmosphere = "",
                    targetUtsMpa = supplied,
                    targetHardnessHb = hbText,
                    targetHardnessHrb = hrbText,
                    structureTarget = "Spheroidised cementite before heading",
                    standardReference = "EN 10263",
                    softeningRatio = softening,
                    unsourced = unsourced,
                    notes = notes
                )
            }
        }

        // Flow-stress sanity check: the tonnage model is driven by K, which must reflect the ANNEALED wire,
        // not the finished hardened part. Flag when the two are far apart.
        if (softening != null && softening < 0.75 && material.flowStressCoeffK >= material.postForgingTargetUtsMpa) {
            return spec.copy(
                notes = spec.notes + ("Flow-stress check: wire anneals to ${fmt(softening * 100)} % of the finished " +
                    "UTS, but K (${fmt(material.flowStressCoeffK)} MPa) is at or above the finished UTS " +
                    "(${fmt(material.postForgingTargetUtsMpa)} MPa). Forging tonnage may be overstated — " +
                    "verify K was measured on annealed wire.")
            )
        }
        return spec
    }
}

/** One stage of a furnace cycle, with the time it takes. */
data class AnnealStage(
    val name: String,
    val detail: String,
    val hoursLow: Double,
    val hoursHigh: Double
) {
    val hoursText: String
        get() = if (kotlin.math.abs(hoursHigh - hoursLow) < 0.05) "${fmtH(hoursLow)} h"
        else "${fmtH(hoursLow)} – ${fmtH(hoursHigh)} h"

    private fun fmtH(v: Double) = String.format(java.util.Locale.US, "%.1f", v)
}

/**
 * A complete furnace cycle: every stage from charging to unloading, with door-to-door hours.
 *
 * The stage times are the published figures for spheroidize annealing of cold-heading wire in a
 * bell furnace (see [sources]), scaled for the charge mass where the source says the stage is
 * charge-driven. They are a starting schedule for a furnace trial, not a substitute for the
 * furnace's own thermocouple chart: a bell furnace's ramp depends on coil weight, stack height,
 * and where a coil sits in the stack.
 */
data class AnnealCycle(
    val stages: List<AnnealStage>,
    val chargeTonnes: Double,
    val soakTempC: String,
    val basis: String,
    val sources: List<String>,
    val cautions: List<String>
) {
    val totalLow: Double get() = stages.sumOf { it.hoursLow }
    val totalHigh: Double get() = stages.sumOf { it.hoursHigh }
    val totalText: String
        get() = String.format(java.util.Locale.US, "%.1f – %.1f h", totalLow, totalHigh)
    /** Door-to-door hours per tonne of charge: what the cycle really costs the furnace. */
    val hoursPerTonne: String
        get() = if (chargeTonnes <= 0.0) "" else
            String.format(java.util.Locale.US, "%.1f h/t", (totalLow + totalHigh) / 2.0 / chargeTonnes)
}

/**
 * Timing for the sub-critical spheroidize anneal. The temperature was always on the card; this adds
 * how long each stage takes, which is what sets furnace booking and cost per tonne.
 */
object AnnealCycleEngine {

    const val DEFAULT_CHARGE_TONNES = 10.0

    private fun fmt(v: Double) = String.format(java.util.Locale.US, "%.0f", v)

    private val SOURCES = listOf(
        "Lin et al., AISI 1022 cold-heading wire, Taguchi study: 695 – 705 °C, 7 – 8 h soak, 7.5 – 8.5 h furnace cool (optimum 700 °C, 8 h cool)",
        "CN102876859A (screw wire): 710 – 730 °C for 5 – 8 h under inert gas, cool ≈ 20 °C/h to ≈ 550 °C, then ≈ 80 °C/h to ≈ 400 °C, then air",
        "CN109593929B (cold-forging steel): 650 – 690 °C held 6 – 20 h, hold set by section thickness",
        "Singh et al. (AIST), SCM435 wire rod in a bell furnace: whole cycle ≈ 21 – 22 h; alloy grades soak up to 10 h",
        "Bell-furnace practice: a coil stack takes 13 – 25 h to reach 720 °C depending on coil weight, height and position in the stack"
    )

    /**
     * Cr, Mo and high carbon spheroidise more slowly, so they hold longer.
     *
     * This used to match on the grade's name, which quietly dropped 300M, H11/H13, 30KHGSA and
     * 16KHGSN into the plain-low-carbon bucket because their names carry no "CrMo" spelling.
     * Carbide-former content decides it now.
     */
    private fun soakHours(material: MaterialInfo, wireDiaMm: Double = 0.0): Pair<Double, Double> =
        SteelCompositions.soakHours(material, wireDiaMm)

    /**
     * Heat-up and unload scale with the charge: a bell furnace takes 13 – 25 h to bring a coil stack
     * to temperature depending on its mass, so the ramp is interpolated across that published band.
     */
    private fun heatUpHours(tonnes: Double): Pair<Double, Double> {
        val t = tonnes.coerceIn(1.0, 40.0)
        val low = (4.0 + 0.30 * t).coerceIn(4.0, 16.0)
        val high = (6.0 + 0.45 * t).coerceIn(6.0, 25.0)
        return low to high
    }

    /**
     * The cycle for a grade that is spheroidize annealed. Returns null for any other route: a
     * solution anneal or a non-ferrous O temper is a different cycle and is not modelled here.
     */
    fun spheroidize(material: MaterialInfo, chargeTonnes: Double = DEFAULT_CHARGE_TONNES, wireDiaMm: Double = 0.0): AnnealCycle? {
        if (PokaYokeEngine.materialFamily(material) != MaterialFamily.CARBON_ALLOY_STEEL) return null
        // Maraging steel is iron-nickel martensite with almost no carbon: nothing to spheroidise.
        if (!SteelCompositions.canSpheroidise(material)) return null

        // Just below THIS grade's A1. Nickel and manganese depress it, silicon and chromium raise
        // it, and across these grades that spans more than 140 °C.
        val ac1 = SteelCompositions.ac1For(material) ?: SteelCompositions.A1_PLAIN_CARBON_C
        // 15 °C margin under A1 for furnace uniformity and heat-to-heat scatter; published plant soaks sit
        // 20 – 60 °C under A1 (SCM435 680 – 720 °C, AISI 1022 695 – 705 °C)
        val soakLo = ac1 - 40.0
        val soakHi = ac1 - 15.0
        val (heatLo, heatHi) = heatUpHours(chargeTonnes)
        val (holdLo, holdHi) = soakHours(material, wireDiaMm)

        // Controlled cool: 20 °C/h through the transformation range, then 80 °C/h to 400 °C.
        val slowCoolTo = 550.0
        val fastCoolTo = 400.0
        val slowHours = (soakLo - slowCoolTo) / 20.0
        val fastHours = (slowCoolTo - fastCoolTo) / 80.0
        // Unloading a hot stack is charge-driven too, but it is a short tail on the cycle.
        val unloadLo = (1.0 + 0.08 * chargeTonnes).coerceIn(1.0, 5.0)
        val unloadHi = (2.0 + 0.12 * chargeTonnes).coerceIn(2.0, 7.0)

        val stages = listOf(
            AnnealStage(
                "1. Charge & purge",
                "Load the stack, seal the bell, purge to the protective atmosphere before any heat goes in",
                0.5, 1.0
            ),
            AnnealStage(
                "2. Heat to soak",
                "Ramp to ${fmt(soakLo)} – ${fmt(soakHi)} °C under N₂ / endothermic gas. Charge-driven: " +
                    "a full stack takes 13 – 25 h, a light one far less",
                heatLo, heatHi
            ),
            AnnealStage(
                "3. Equalise",
                "Hold until the coldest coil in the stack reaches temperature — the control thermocouple " +
                    "leads the charge, so time from the load couple, not the setpoint",
                1.0, 2.0
            ),
            AnnealStage(
                "4. Soak (spheroidise)",
                "Hold just below A1 to break the lamellar pearlite into globular cementite. This is the " +
                    "stage that decides the structure: too short leaves lamellae, too long coarsens the carbide" +
                    if (wireDiaMm > 0.0) ". Soak adjusted for Ø ${String.format(java.util.Locale.US, "%.1f", wireDiaMm)} mm wire" else "",
                holdLo, holdHi
            ),
            AnnealStage(
                "5. Controlled cool to ${fmt(slowCoolTo)} °C",
                "≈ 20 °C/h, as in the published wire cycles. A soak below A1 forms no austenite, so this leg is the " +
                    "safeguard: any coil that ran over A1 re-transforms to globular carbide instead of new lamellar pearlite",
                slowHours, slowHours
            ),
            AnnealStage(
                "6. Cool to ${fmt(fastCoolTo)} °C",
                "≈ 80 °C/h below 550 °C; nothing is gained by holding the charge hot",
                fastHours, fastHours
            ),
            AnnealStage(
                "7. Cool under cover & unload",
                "Stay under the protective atmosphere below ${fmt(fastCoolTo)} °C until safe to strip, then still air. " +
                    "Opening hot scales and decarburises the wire surface",
                unloadLo, unloadHi
            )
        )

        return AnnealCycle(
            stages = stages,
            chargeTonnes = chargeTonnes,
            soakTempC = "${fmt(soakLo)} – ${fmt(soakHi)} °C",
            basis = "Published bell-furnace cycles for cold-heading wire, with the charge-driven stages scaled " +
                "to ${String.format(java.util.Locale.US, "%.1f", chargeTonnes)} t",
            sources = SOURCES,
            cautions = listOf(
                "A starting schedule for a furnace trial, not a released process. Prove it on the furnace's own " +
                    "load thermocouple before running production.",
                "Soak and ramp are set by coil mass, stack height and position in the stack — not by the part " +
                    "diameter. Two coils in one stack do not see the same cycle.",
                "The soak band is set 40 to 15 °C under this grade's own A1, worked out from its nominal " +
                    "composition. A mill certificate at the far end of the range shifts A1 by a few degrees, so " +
                    "check it: soaking above A1 austenitises and undoes the anneal."
            )
        )
    }
}
