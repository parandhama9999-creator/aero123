package com.aistudio.aerofast.trmst.data

/**
 * A furnace cycle with a time on every stage, for the routes that are not the spheroidize anneal
 * (that one is [AnnealCycleEngine]). Same shape as [AnnealCycle] so both render the same way.
 */
data class FurnaceCycle(
    val title: String,
    val stages: List<AnnealStage>,
    val basis: String,
    val sources: List<String>,
    val cautions: List<String>
) {
    val totalLow: Double get() = stages.sumOf { it.hoursLow }
    val totalHigh: Double get() = stages.sumOf { it.hoursHigh }
    val totalText: String get() = String.format(java.util.Locale.US, "%.1f – %.1f h", totalLow, totalHigh)
}

/**
 * Timing for the post-forming heat treatment and for the non-ferrous anneals.
 *
 * Steel quench-and-temper times come from the section size, which is the rule the specs actually
 * use — the catalogue says "per section size" for these grades and never worked out the hours.
 * For a fastener the ruling section is the shank diameter, not the head.
 */
object HeatTreatCycleEngine {

    /** AMS 2759/1: temper soak is 2 h minimum, plus 1 h for each 25 mm of section over the first 25 mm. */
    fun temperHours(sectionMm: Double): Double {
        val extraInches = kotlin.math.ceil(((sectionMm - 25.4) / 25.4).coerceAtLeast(0.0))
        return 2.0 + extraInches
    }

    /** Austenitize / solution soak: the shop rule is 1 h per 25 mm of section, never under 1 h. */
    fun soakHours(sectionMm: Double): Double =
        kotlin.math.max(1.0, kotlin.math.ceil(sectionMm / 25.4 * 2.0) / 2.0)

    private fun f(v: Double, dp: Int = 1) = String.format(java.util.Locale.US, "%.${dp}f", v)

    private val QT_SOURCES = listOf(
        "AMS 2759/1: tempering soak not less than 2 h, plus 1 h for each inch (25 mm) of thickness or part thereof over the first inch",
        "AMS 2759: thickness is the minimum dimension of the heaviest section of the part or nested load; with load thermocouples the soak runs from the load, and is never under 1 h",
        "Shop rule for austenitizing: 1 h per inch (25 mm) of section, minimum 1 h, timed from when the load reaches temperature",
        "ISO 4042 / ASTM F1940: electroplated parts at or above 1000 MPa are baked within 4 h of plating"
    )

    /**
     * The quench-and-temper cycle for a steel fastener of this section. Returns null for a grade the
     * catalogue does not quench and temper (austenitic stainless, aluminium, solid-solution nickel).
     */
    fun quenchTemper(
        material: MaterialInfo,
        sectionMm: Double,
        /** The part's own spec (class-based temper, cold-formed classes N/A); defaults to the catalogue card. */
        spec: HeatTreatmentSpec = material.heatTreatmentSpec
    ): FurnaceCycle? {
        if (PokaYokeEngine.materialFamily(material) != MaterialFamily.CARBON_ALLOY_STEEL) return null
        // Maraging steel is never quenched and tempered: it air-cools to a soft nickel martensite
        // and is hardened by ageing. The quench-and-temper stages and their cautions — transfer
        // without delay, untempered martensite cracks — are simply false for it.
        if (!SteelCompositions.canSpheroidise(material)) return solutionTreatAndAge(material, sectionMm)
        val ht = spec
        val aust = ht.austenitizingOrSolutionTempC
        if (aust.isBlank() || aust.contains("N/A", true) || aust.contains("Not applicable", true)) return null

        val soak = soakHours(sectionMm)
        val temper = temperHours(sectionMm)
        val stages = mutableListOf<AnnealStage>()

        if (ht.preheatTempC.isNotBlank() && !ht.preheatTempC.contains("N/A", true)) {
            stages += AnnealStage(
                "1. Preheat", "${ht.preheatTempC} — equalise before the final heat so a heavy section does not crack",
                0.5, 1.0
            )
        }
        stages += AnnealStage(
            "${stages.size + 1}. Heat to austenitize",
            "$aust${if (ht.atmosphere.isNotBlank()) " • ${ht.atmosphere}" else " • protective or neutral atmosphere"}",
            0.5, 1.5
        )
        stages += AnnealStage(
            "${stages.size + 1}. Austenitize soak",
            "${f(soak)} h at temperature = 1 h per 25 mm of ${f(sectionMm)} mm section, minimum 1 h. " +
                "Time it from the load thermocouple, not the setpoint",
            soak, soak
        )
        stages += AnnealStage(
            "${stages.size + 1}. Quench",
            "${ht.quenchMedium.ifBlank { "Per the grade's quenchant" }} — transfer without delay; " +
                "a slow transfer loses hardness at the surface",
            0.1, 0.2
        )
        stages += AnnealStage(
            "${stages.size + 1}. Temper within 1 h of the quench",
            "${ht.temperingOrAgingTempC.ifBlank { "Per class" }} — untempered martensite cracks if it is left to stand",
            0.3, 1.0
        )
        stages += AnnealStage(
            "${stages.size + 1}. Temper soak",
            "${f(temper)} h = AMS 2759/1: 2 h minimum, plus 1 h per 25 mm over the first 25 mm " +
                "(${f(sectionMm)} mm section)",
            temper, temper
        )
        if (ht.isDoubleTemper) {
            stages += AnnealStage(
                "${stages.size + 1}. Second temper",
                "Cool to room temperature between tempers, then repeat — the second temper conditions " +
                    "the martensite formed on cooling from the first",
                temper, temper
            )
        }
        stages += AnnealStage(
            "${stages.size + 1}. Cool & unload",
            "${ht.coolingMethod.ifBlank { "Still air" }}",
            0.5, 1.5
        )
        if (ht.hydrogenBakingRequired) {
            stages += AnnealStage(
                "${stages.size + 1}. Hydrogen bake (after plating)",
                "${ht.bakingTempC.ifBlank { "190 °C – 220 °C" }}, ${ht.bakingSoakTime.ifBlank { "8 to 24 h" }}, " +
                    "started within 4 h of plating (ISO 4042 / ASTM F1940). Not part of the furnace cycle above — " +
                    "it runs after the coating line",
                8.0, 24.0
            )
        }

        return FurnaceCycle(
            title = "Quench & temper — complete cycle",
            stages = stages,
            basis = "Soak and temper worked out for a ${f(sectionMm)} mm ruling section (the shank Ø, not the head)",
            sources = QT_SOURCES,
            cautions = listOf(
                "Section size sets the hours. For a fastener the ruling section is the shank; a flange or a " +
                    "heavy head soaks longer than the shank does.",
                "Soak time starts when the LOAD reaches temperature. A furnace at setpoint with a cold charge " +
                    "in it has not started soaking.",
                "Temper temperature, not time, sets the final hardness. Do not shorten the temper to hit a " +
                    "hardness: drop the temperature instead."
            )
        )
    }

    // ---------------------------------------------------------------------------------------------
    // Non-ferrous anneal (the O temper), which the catalogue left blank
    // ---------------------------------------------------------------------------------------------

    private val ALUMINIUM_SOURCES = listOf(
        "6061 supplier procedure: heat to 413 °C (775 °F), hold 2 – 3 h, then cool no faster than 28 °C/h (50 °F/h) to 260 °C (500 °F)",
        "Same 413 °C / 2 – 3 h / 28 °C per h cycle is published for 7075; a 7075 plate datasheet instead asks for 10 °C/h, so confirm against the spec you work to",
        "7xxx patent describing the conventional MIL route: 2 h at 413 °C, then 232 °C for 6 h; under 30 °C/h gives a full O condition",
        "5xxx are not heat treatable: the anneal only recrystallises, and the cooling rate does not matter"
    )

    private fun aluminiumSeries(key: String): Char? =
        key.uppercase().removePrefix("AL").firstOrNull()?.takeIf { it.isDigit() }

    /**
     * The soft-condition cycle for anything that is not spheroidize annealed: the O temper for
     * aluminium, the soft anneal for copper alloys, and the solution anneal for stainless and PH
     * grades. Null for carbon steel, which goes through [AnnealCycleEngine], and for the grades
     * bought ready to head.
     */
    fun softAnneal(material: MaterialInfo, sectionMm: Double = 10.0): FurnaceCycle? {
        val family = PokaYokeEngine.materialFamily(material)
        return when (family) {
            MaterialFamily.ALUMINUM -> aluminium(material)
            MaterialFamily.COPPER_ALLOY -> copper(material)
            MaterialFamily.STAINLESS -> when (PokaYokeEngine.stainlessType(material)) {
                // Quenching a martensitic from temperature is its hardening treatment, so the
                // solution-anneal cycle would harden the wire it is meant to soften.
                PokaYokeEngine.StainlessType.MARTENSITIC,
                PokaYokeEngine.StainlessType.FERRITIC -> subCriticalStainlessAnneal(material, sectionMm)
                else -> solutionAnneal(material, sectionMm)
            }
            // Maraging steel heads in the solution-annealed state; there is no spheroidize cycle
            // because at 0.02 % carbon there is nothing to ball up.
            MaterialFamily.CARBON_ALLOY_STEEL ->
                if (!SteelCompositions.canSpheroidise(material)) maragingSolutionAnneal(material, sectionMm) else null
            MaterialFamily.NICKEL_SUPERALLOY -> superalloyAnneal(material, sectionMm)
            else -> null
        }
    }

    /** Re-anneal between blows for a nickel superalloy, from its researched mill-anneal cycle; null when none is on file. */
    private fun superalloyAnneal(material: MaterialInfo, sectionMm: Double): FurnaceCycle? {
        val mill = SoftAnnealEngine.MILL_ANNEAL[material.key] ?: return null
        val soak = soakHours(sectionMm)
        return FurnaceCycle(
            title = "Solution (mill) anneal — complete cycle",
            stages = listOf(
                AnnealStage("1. Charge & heat", "${mill.temperatureC} • vacuum or argon, or the surface oxidises", 0.5, 1.5),
                AnnealStage("2. Soak", "${mill.hold.ifBlank { "Hold not stated in the source" }} — ${f(soak)} h by the shop rule of 1 h per 25 mm of ${f(sectionMm)} mm section, minimum 1 h", soak, soak),
                AnnealStage("3. Cool", mill.cooling.ifBlank { "Cooling not stated in the source" }, 0.1, 0.5)
            ),
            basis = "Mill anneal of ${material.name} for a ${f(sectionMm)} mm section, to soften the wire between blows",
            sources = listOf("${mill.source} (${mill.confidence})"),
            cautions = listOf(
                "This is a solution anneal: the softening comes from dissolving the hardening phases and cooling fast " +
                    "enough to keep them dissolved. A slow or furnace cool does the opposite.",
                "Never age before heading: the age goes on the finished part."
            )
        )
    }

    /**
     * Inconel 718 solution treatment and precipitation hardening (AMS 5662 solution + AMS 5663 age): 718 °C for 8 h,
     * furnace cool at 55 °C/h to 621 °C, hold until the total ageing time reaches 18 h, air cool. Null for other grades.
     */
    fun precipitationHarden(material: MaterialInfo, sectionMm: Double): FurnaceCycle? = when (material.key) {
        "INCONEL718" -> inconel718(sectionMm)
        "A286" -> a286(sectionMm)
        "WASPALOY" -> waspaloy(sectionMm)
        else -> null
    }

    /** A286: solution 982 °C (AMS 5731/5732) or 899 °C (AMS 5737) about 1 h, oil / water quench, age 718 °C 16 h, air cool. */
    private fun a286(sectionMm: Double) = FurnaceCycle(
        title = "Solution treat & age — complete cycle",
        stages = listOf(
            AnnealStage("1. Charge & heat", "982 °C (1800 °F) for AMS 5731 / 5732, or 899 °C (1650 °F) for AMS 5737 • vacuum, argon or protective gas", 0.5, 1.5),
            AnnealStage("2. Solution soak", "About 1 h at temperature (${f(sectionMm)} mm section)", 1.0, 1.0),
            AnnealStage("3. Quench", "Oil or water", 0.1, 0.3),
            AnnealStage("4. Age 718 °C (1325 °F)", "Heat to 718 °C and hold 16 h (AMS window 704 – 760 °C, at least 16 h)", 16.5, 17.0),
            AnnealStage("5. Air cool", "To room temperature", 0.5, 1.0)
        ),
        basis = "A286 solution treatment and ageing for a ${f(sectionMm)} mm section",
        sources = listOf(
            "Aircraft Materials A286 (AMS 5731 / 5732 / 5737): 1800 °F or 1650 °F, about 1 h, water or oil quench",
            "High Temp Metals A286: solution 1800 °F 1 h oil quench, age 1325 °F 16 h air cool",
            "Rolled Alloys A-286: 1800 °F gives the best creep-rupture strength, 1650 °F better ductility and room-temperature strength"
        ),
        cautions = listOf(
            "Pick the solution temperature from the drawing: 982 °C and 899 °C give different properties (AMS 5732 vs AMS 5737).",
            "Hardness acceptance values differ between sources (248 – 341 HB and 277 – 363 HB quoted): use the AMS sheet on the drawing."
        )
    )

    /** Waspaloy, Special Metals heat treatment B: solution 996 – 1038 °C, oil quench, stabilize 845 °C 4 h AC, age 760 °C 16 h AC. */
    private fun waspaloy(sectionMm: Double) = FurnaceCycle(
        title = "Solution treat, stabilize & age — complete cycle",
        stages = listOf(
            AnnealStage("1. Charge & heat", "996 – 1038 °C (1825 – 1900 °F), 1010 °C typical • vacuum or argon. Heat through 650 – 870 °C fast", 0.5, 1.5),
            AnnealStage("2. Solution soak", "4 h (Special Metals heat treatment B; Haynes uses 2 h, Upmet 1 h)", 4.0, 4.0),
            AnnealStage("3. Oil quench", "Or fast gas / water", 0.1, 0.3),
            AnnealStage("4. Stabilize 845 °C (1550 °F)", "4 h, then air cool", 4.5, 5.0),
            AnnealStage("5. Age 760 °C (1400 °F)", "16 h, then air cool", 16.5, 17.0)
        ),
        basis = "Waspaloy heat treatment for room- and high-temperature tensile properties, ${f(sectionMm)} mm section",
        sources = listOf(
            "Special Metals Waspaloy, heat treatment B: 995 – 1035 °C 4 h oil quench; 845 °C 4 h AC; 760 °C 16 h AC; 34 – 44 HRC",
            "Haynes Waspaloy: 1825 °F 2 h, then 1550 °F 4 h and 1400 °F 16 h",
            "Upmet AMS 5706: supplied solution treated 1825 – 1900 °F for 1 h"
        ),
        cautions = listOf(
            "For creep / stress-rupture parts Special Metals uses a different route (1975 °F 4 h AC, 1550 °F 24 h, 1400 °F 16 h): follow the drawing.",
            "Waspaloy is prone to strain-age cracking: heat cold-worked parts through 650 – 870 °C as fast as possible."
        )
    )

    private fun inconel718(sectionMm: Double): FurnaceCycle {
        val soak = soakHours(sectionMm)
        val fcHours = (718.0 - 621.0) / 55.0
        val hold621 = 18.0 - 8.0 - fcHours
        return FurnaceCycle(
            title = "Solution treat & age — complete cycle",
            stages = listOf(
                AnnealStage("1. Charge & heat", "927 – 1010 °C (1700 – 1850 °F), 955 – 980 °C typical • vacuum or argon", 0.5, 1.5),
                AnnealStage("2. Solution soak", "Time commensurate with section (AMS 5662) — ${f(soak)} h by the shop rule for a ${f(sectionMm)} mm section", soak, soak),
                AnnealStage("3. Rapid cool", "Water, or fast gas / air (air cool or faster). The faster the cool, the stronger the ageing response", 0.1, 0.3),
                AnnealStage("4. Age 718 °C (1325 °F)", "Heat to 718 °C and hold 8 h", 8.5, 9.0),
                AnnealStage("5. Furnace cool", "55 °C/h (100 °F/h) from 718 °C to 621 °C (1150 °F)", fcHours, fcHours),
                AnnealStage("6. Hold 621 °C (1150 °F)", "Until the total ageing time reaches 18 h (≈ ${f(hold621)} h here)", hold621, hold621),
                AnnealStage("7. Air cool", "To room temperature", 0.5, 1.0)
            ),
            basis = "AMS 5662 solution treatment + AMS 5663 precipitation treatment for a ${f(sectionMm)} mm section",
            sources = listOf(
                "Special Metals INCONEL 718: solution 1700 – 1850 °F, rapid cool usually in water; age 1325 °F 8 h, furnace cool to 1150 °F, hold for a total ageing time of 18 h, air cool",
                "ASTM A1014: precipitation treatment at 1325 °F (718 °C) followed by a furnace-cooled step to 1150 °F (621 °C)",
                "NASA PRC-2003: a controlled 100 °F/h cool may replace the furnace cool",
                "voestalpine 718: about 28 HRC max annealed, about 36 – 43 HRC aged"
            ),
            cautions = listOf(
                "The 18 h is the total precipitation time, including the furnace cool from 718 to 621 °C.",
                "Do not furnace cool from the solution temperature: slow cooling precipitates δ / γ″ in the solution step and weakens the age.",
                "AMS 5664 is a different cycle with a higher solution temperature: use it only when the drawing calls for it.",
                "Aerospace 718 bolts are usually thread rolled after this treatment: check the drawing / bolt specification."
            )
        )
    }

    /**
     * The soft anneal for a martensitic or ferritic stainless.
     *
     * The temperature in these grades' heat-treatment spec — 970 to 1020 °C with an oil quench and
     * a temper — is the HARDENING treatment, run after forming. Annealing is the opposite leg:
     * a martensitic holds below the critical range and cools slowly so carbide precipitates and
     * coarsens; a ferritic only recrystallises, and is cooled briskly through 540 – 400 °C to miss
     * 475 °C embrittlement.
     */
    private fun subCriticalStainlessAnneal(material: MaterialInfo, sectionMm: Double): FurnaceCycle {
        val type = PokaYokeEngine.stainlessType(material)
        val soak = soakHours(sectionMm)
        val id = (material.key + " " + material.name).uppercase()
        // 431-type carries 1.5 – 2.5 % nickel, which keeps austenite stable: a full anneal does not
        // soften it properly, so the sub-critical hold is the published route and the floor is higher.
        val nickelBearing = id.contains("431")
        return if (type == PokaYokeEngine.StainlessType.FERRITIC) FurnaceCycle(
            title = "Ferritic anneal — complete cycle",
            stages = listOf(
                AnnealStage("1. Charge & heat", "Ramp to 705 – 790 °C under protective atmosphere", 0.5, 1.5),
                AnnealStage(
                    "2. Anneal soak",
                    "${f(soak)} h = 1 h per 25 mm of ${f(sectionMm)} mm section, minimum 1 h — " +
                        "recrystallises the cold work; a ferritic does not transformation-harden",
                    soak, soak
                ),
                AnnealStage(
                    "3. Air cool",
                    "Cool briskly through 540 – 400 °C: holding or creeping through that band causes " +
                        "475 °C embrittlement in a 17 % Cr ferritic",
                    0.5, 1.0
                )
            ),
            basis = "Ferritic stainless anneal for a ${f(sectionMm)} mm section",
            sources = listOf(
                "AISI 430 anneal: 705 – 790 °C, air cool; the grade does not harden by quenching",
                "475 °C embrittlement: 17 % Cr ferritics embrittle on slow cooling or holding at 400 – 540 °C"
            ),
            cautions = listOf(
                "Do not furnace-cool slowly through 540 – 400 °C — that is the 475 °C embrittlement band.",
                "The spec card's 970 – 1000 °C figure is not for this grade's softening: a ferritic gains " +
                    "nothing there but grain growth."
            )
        ) else FurnaceCycle(
            title = "Martensitic soft anneal — complete cycle",
            stages = if (nickelBearing) listOf(
                AnnealStage("1. Charge & heat", "Ramp to 620 – 660 °C under protective atmosphere", 0.5, 1.5),
                AnnealStage(
                    "2. Sub-critical soak",
                    "${f(soak + 1)} – ${f(soak + 3)} h — the nickel keeps austenite stable, so a 431-type " +
                        "never anneals fully soft: expect about 230 – 270 HB, not a carbon-steel softness",
                    soak + 1, soak + 3
                ),
                AnnealStage("3. Air cool", "Still air; the sub-critical hold does not re-austenitise", 0.5, 1.0)
            ) else listOf(
                AnnealStage("1. Charge & heat", "Ramp to 845 – 885 °C under protective atmosphere", 0.5, 1.5),
                AnnealStage(
                    "2. Anneal soak",
                    "${f(soak)} h = 1 h per 25 mm of ${f(sectionMm)} mm section, minimum 1 h",
                    soak, soak
                ),
                AnnealStage(
                    "3. Furnace cool ≤ 30 °C/h to 600 °C",
                    "The slow leg IS the anneal: carbide precipitates and coarsens on the way down. " +
                        "Air cooling from temperature re-hardens the wire instead",
                    (865.0 - 600.0) / 30.0, (865.0 - 600.0) / 20.0
                ),
                AnnealStage("4. Air cool below 600 °C", "The rate no longer matters", 0.5, 1.5)
            ),
            basis = (if (nickelBearing) "Sub-critical anneal for a nickel-bearing martensitic (431-type)"
            else "Full anneal for a 410/420-type martensitic") + ", ${f(sectionMm)} mm section",
            sources = listOf(
                "AISI 410 / 420 full anneal: 830 – 885 °C, furnace cool not over about 30 °C/h to roughly 600 °C, then air — lowest hardness",
                "Process (sub-critical) anneal: 650 – 760 °C air cool where partial softening is enough",
                "AISI 431: full annealing is not effective because of the nickel; sub-critical treatment is the published route"
            ),
            cautions = listOf(
                "The 970 – 1020 °C + oil quench on this grade's spec card is the HARDENING treatment, run " +
                    "after forming. Quenching from temperature is how this grade is made hard, not soft.",
                "Do not confuse this with an austenitic solution anneal: here the slow cool softens, and a " +
                    "rapid quench hardens — the exact opposite."
            )
        )
    }

    /**
     * Maraging steel, both as wire preparation and after forming. It is solution annealed and
     * AIR cooled — the nickel martensite that forms is soft and headable, about 30 HRC — and all
     * of its strength comes from ageing at 480 °C after the part is formed.
     */
    private fun maragingSolutionAnneal(material: MaterialInfo, sectionMm: Double): FurnaceCycle {
        val soak = soakHours(sectionMm)
        return FurnaceCycle(
            title = "Solution anneal — complete cycle",
            stages = listOf(
                AnnealStage("1. Charge & heat", "Ramp to 815 – 830 °C, protective atmosphere or vacuum", 0.5, 1.5),
                AnnealStage(
                    "2. Solution soak",
                    "${f(soak)} h = 1 h per 25 mm of ${f(sectionMm)} mm section, minimum 1 h",
                    soak, soak
                ),
                AnnealStage(
                    "3. Air cool",
                    "No quench. The steel air-cools to a soft, tough nickel martensite around 30 HRC — " +
                        "that IS the headable condition",
                    0.5, 1.5
                ),
                AnnealStage(
                    "4. Age AFTER forming",
                    "${material.heatTreatmentSpec.temperingOrAgingTempC.ifBlank { "480 °C, 3 – 6 h" }} — " +
                        "ageing is where all the strength comes from, and it is done to the finished part",
                    3.0, 6.0
                )
            ),
            basis = "Maraging solution treatment for a ${f(sectionMm)} mm section; the age is listed because it belongs after forming",
            sources = listOf(
                "AMS 6512 / maraging practice: solution anneal about 815 °C for 1 h per 25 mm, air cool; age 480 °C for 3 – 6 h",
                "Maraging martensite is soft and ductile as cooled — hardening is entirely by precipitation during the age"
            ),
            cautions = listOf(
                "There is no quench and no tempering. The quench-and-temper stage list for the other steels " +
                    "does not apply to a maraging grade.",
                "Head and machine in the solution-annealed state; age only the finished part."
            )
        )
    }

    /** The post-forming cycle for maraging steel: the same solution treat, then the age itself. */
    private fun solutionTreatAndAge(material: MaterialInfo, sectionMm: Double): FurnaceCycle {
        val base = maragingSolutionAnneal(material, sectionMm)
        return base.copy(
            title = "Solution treat & age — complete cycle",
            basis = "Maraging post-forming treatment for a ${f(sectionMm)} mm section — ageing replaces quench and temper"
        )
    }

    private val SOLUTION_SOURCES = listOf(
        "Shop rule for solution annealing: 1 h per inch (25 mm) of section, minimum 30 min at temperature — " +
            "hold only as long as needed, because austenitic grain grows the whole time",
        "ISO 3506 / ASTM A276: the austenitic grades are supplied solution annealed; strength comes from cold work, not heat treatment",
        "AMS 2759: with load thermocouples the soak runs from the load, never under 1 h"
    )

    /**
     * Solution anneal for stainless. The aim is the opposite of a spheroidize anneal: dissolve the
     * carbides and cool fast enough to keep them dissolved. A slow cool through 850 – 450 °C
     * sensitises the grain boundaries and the part fails intergranular corrosion.
     */
    private fun solutionAnneal(material: MaterialInfo, sectionMm: Double): FurnaceCycle? {
        val ht = material.heatTreatmentSpec
        val temp = ht.austenitizingOrSolutionTempC
        if (temp.isBlank() || temp.contains("N/A", true)) return null
        val soak = kotlin.math.max(0.5, kotlin.math.ceil(sectionMm / 25.4 * 2.0) / 2.0)
        val isPh = material.key.uppercase().contains("PH") || material.key.uppercase() == "CUSTOM_455" ||
            material.key.uppercase() == "AM350"

        val stages = mutableListOf(
            AnnealStage("1. Charge & heat", "$temp — bright anneal (H₂/N₂) or vacuum, or the surface scales and has to be pickled", 0.5, 1.5),
            AnnealStage(
                "2. Solution soak",
                "${f(soak)} h = 1 h per 25 mm of ${f(sectionMm)} mm section, minimum 30 min. Do not overhold: " +
                    "every extra hour coarsens the grain and the wire heads worse, not better",
                soak, soak
            ),
            AnnealStage(
                "3. Rapid quench",
                "${ht.quenchMedium.ifBlank { "Water or forced gas" }} — through 850 – 450 °C as fast as the section " +
                    "allows. A slow cool here precipitates chromium carbide and sensitises the grain boundaries",
                0.1, 0.3
            )
        )
        if (isPh && ht.temperingOrAgingTempC.isNotBlank() && !ht.temperingOrAgingTempC.contains("N/A", true)) {
            stages += AnnealStage(
                "4. Age AFTER forming",
                "${ht.temperingOrAgingTempC} for ${ht.temperingOrAgingTimeHours.ifBlank { "the grade's aging time" }} — " +
                    "a precipitation-hardening grade is headed in the solution-treated (Condition A) state and aged afterwards",
                1.0, 4.0
            )
        }
        return FurnaceCycle(
            title = "Solution anneal — complete cycle",
            stages = stages,
            basis = "Solution soak worked out for a ${f(sectionMm)} mm ruling section" +
                if (isPh) "; the age is listed because it belongs after forming, not before" else "",
            sources = SOLUTION_SOURCES,
            cautions = listOfNotNull(
                "This is not an anneal in the steel sense. Cooling slowly does not soften a stainless — it " +
                    "sensitises it. The quench IS the softening step.",
                "Hold the minimum that fully solutions the section. Unlike a spheroidize anneal, longer is worse.",
                if (isPh) "Condition A is the headable state. Age the formed part; ageing the wire first makes it uncrackable only on paper." else null
            )
        )
    }

    private fun aluminium(material: MaterialInfo): FurnaceCycle {
        val series = aluminiumSeries(material.key)
        val heatTreatable = series == '2' || series == '6' || series == '7'
        val coolTo = if (series == '7') 230.0 else 260.0
        val stages = if (heatTreatable) listOf(
            AnnealStage("1. Charge & heat", "Ramp to 413 °C (775 °F). Aluminium has a narrow window: 30 °C over and the surface starts to melt", 0.5, 1.5),
            AnnealStage("2. Soak", "2 – 3 h at 413 °C ± 8 °C — dissolves and coarsens the precipitate so the alloy goes fully soft", 2.0, 3.0),
            AnnealStage(
                "3. Controlled cool to ${f(coolTo, 0)} °C",
                "≤ 28 °C/h (50 °F/h). This slow leg IS the anneal: cool faster and the alloy part-solution-treats " +
                    "and comes out harder than it went in",
                (413.0 - coolTo) / 28.0, (413.0 - coolTo) / 28.0
            ),
            AnnealStage("4. Air cool", "Below ${f(coolTo, 0)} °C the rate no longer matters", 0.5, 1.5)
        ) else listOf(
            AnnealStage("1. Charge & heat", "Ramp to 343 °C (650 °F)", 0.5, 1.0),
            AnnealStage("2. Soak", "2 h at 343 °C — 5xxx is not heat treatable, so this only recrystallises the cold work", 2.0, 2.0),
            AnnealStage("3. Cool", "Still air; the cooling rate does not affect the temper", 0.5, 1.0)
        )
        return FurnaceCycle(
            title = "O-temper anneal — complete cycle",
            stages = stages,
            basis = if (heatTreatable) "Published 413 °C O-temper cycle for ${series}xxx aluminium"
            else "Recrystallisation anneal for 5xxx (not heat treatable)",
            sources = ALUMINIUM_SOURCES,
            cautions = listOfNotNull(
                "The controlled cool is the whole point. Quenching from the anneal gives a part-solution-treated " +
                    "part that is HARDER than before and keeps age-hardening on the shelf for days.",
                if (series == '7') "Sources disagree on the cooling rate for 7xxx: 28 °C/h is the common figure, " +
                    "one plate datasheet asks for 10 °C/h. Use the slower rate if the spec does not say." else null,
                "Head within the annealed window: 2xxx and 7xxx start natural ageing as soon as they are cool."
            )
        )
    }

    private fun copper(material: MaterialInfo): FurnaceCycle {
        val isBeryllium = material.key.uppercase().contains("CUBE") || material.name.uppercase().contains("BERYLLIUM")
        return if (isBeryllium) FurnaceCycle(
            title = "Solution anneal — complete cycle",
            stages = listOf(
                AnnealStage("1. Charge & heat", "Ramp to the grade's solution temperature under protective gas", 0.5, 1.5),
                AnnealStage("2. Solution soak", "15 min – 1 h for thin sections, 1 – 3 h for heavy sections", 0.25, 3.0),
                AnnealStage("3. Water quench", "Immediate — beryllium copper is precipitation hardening, so the soft state is the quenched state", 0.1, 0.2),
                AnnealStage("4. Age after forming", "Age only once the part is formed; ageing before heading defeats the purpose", 2.0, 3.0)
            ),
            basis = "Precipitation-hardening copper: the solution-treated condition is the formable one",
            sources = listOf("Materion copper-beryllium heat treatment guidance: 15 min – 1 h thin sections, 1 – 3 h heavy sections"),
            cautions = listOf("Beryllium copper dust and fume are a health hazard — handle per the supplier's safety data sheet.")
        ) else FurnaceCycle(
            title = "Soft anneal — complete cycle",
            stages = listOf(
                AnnealStage("1. Charge & purge", "Protective gas; nitrogen is enough for copper alloys and keeps the surface bright", 0.3, 0.6),
                AnnealStage("2. Heat & soak", "0.5 – 3 h in the alloy's soft-anneal band (300 – 650 °C for the coppers). " +
                    "Take the band for THIS alloy from ASTM B601 or the supplier datasheet before setting the furnace", 0.5, 3.0),
                AnnealStage("3. Cool", "Cooling rate is not critical for the solid-solution copper alloys", 0.5, 1.5)
            ),
            basis = "Generic copper soft-anneal envelope — this alloy's own band is not in the catalogue",
            sources = listOf("Deutsches Kupferinstitut: soft anneal 300 – 650 °C for 0.5 – 3 h (Cu-ETP), 425 – 650 °C (Cu-OF), under protective gas"),
            cautions = listOf(
                "This is the generic copper envelope, not this alloy's cycle. Brass, phosphor bronze, silicon " +
                    "bronze and cupronickel each anneal in a different band — get it from ASTM B601 or the mill.",
                "Soft annealing runs well above the recrystallisation temperature; a light anneal sits just above it."
            )
        )
    }
}
