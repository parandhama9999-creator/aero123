package com.example.data

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
    fun quenchTemper(material: MaterialInfo, sectionMm: Double): FurnaceCycle? {
        if (PokaYokeEngine.materialFamily(material) != MaterialFamily.CARBON_ALLOY_STEEL) return null
        // Maraging steel is never quenched and tempered: it air-cools to a soft nickel martensite
        // and is hardened by ageing. The quench-and-temper stages and their cautions — transfer
        // without delay, untempered martensite cracks — are simply false for it.
        if (!SteelCompositions.canSpheroidise(material)) return solutionTreatAndAge(material, sectionMm)
        val ht = material.heatTreatmentSpec
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
            else -> null
        }
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
