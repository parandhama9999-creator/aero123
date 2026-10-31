package com.example.data

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
    private const val HB_PER_MPA = 3.38

    /** ASTM A370 steel HB -> HRB, piecewise over the annealed range. Approximate by nature. */
    private fun hbToHrb(hb: Double): Double? {
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
    private fun parseUtsRange(s: String): Pair<Double, Double>? {
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
                notes += "A1 baseline is ${fmt(A1_PLAIN_CARBON_C)} °C for plain carbon steel. Cr, Mo and Si raise A1; " +
                    "Mn and Ni lower it — confirm A1 for this grade before setting the furnace."
                unsourced += "Exact A1 for this grade"
                unsourced += "Soak hours — driven by coil mass and furnace load, not part diameter"
                SoftAnnealSpec(
                    route = route,
                    inHouseAnnealNeeded = true,
                    suppliedCondition = supplied,
                    temperatureC = "${fmt(A1_PLAIN_CARBON_C - 25)} – ${fmt(A1_PLAIN_CARBON_C - 5)} °C " +
                        "(just below A1; baseline A1 = ${fmt(A1_PLAIN_CARBON_C)} °C)",
                    soakTime = "Coil-mass driven — set from furnace load, not part Ø",
                    coolingRate = "Slow furnace cool through the transformation range, then still air",
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
                notes += "The catalogue lists this wire as cold drawn / half-hard, which is not a headable condition. " +
                    "Either source spheroidize-annealed wire, or anneal before heading."
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
