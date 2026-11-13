package com.example

import com.example.data.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Every grade in the catalogue must reach a cycle that carries hours, or be a grade where a time
 * would have to be invented. This is the guard that stopped "ENTER PER GRADE" standing in for a
 * soak time the specs can actually give.
 */
class CycleTimeAuditTest {

    private val mats = FastenerCatalog.MATERIALS.values.sortedBy { it.key }

    /** Wire preparation: a steel spheroidize cycle, or a non-ferrous anneal, or bought annealed. */
    @Test fun everyGradeReachesATimedWirePreparationCycleOrSaysWhyNot() {
        val uncovered = mutableListOf<String>()
        for (m in mats) {
            val spheroidize = AnnealCycleEngine.spheroidize(m) != null
            val nonFerrous = HeatTreatCycleEngine.softAnneal(m) != null
            val spec = SoftAnnealEngine.build(m)
            // Mill-annealed grades are bought ready to head: the in-house cycle does not exist, and the
            // published mill cycle is only quoted where a datasheet stated it.
            val boughtAnnealed = spec.route == AnnealRoute.MILL_ANNEALED || !spec.inHouseAnnealNeeded
            if (spheroidize || nonFerrous || boughtAnnealed) continue
            // Anything left must say in plain words that it has no cycle and why, and list it as unsourced.
            // Today that is MP35N: bought work-strengthened, so there is no anneal to time.
            val explained = spec.notes.any { it.contains("solution", true) || it.contains("anneal", true) } &&
                spec.unsourced.isNotEmpty()
            if (!explained) uncovered += m.key
        }
        assertTrue("no cycle and no explanation of why: $uncovered", uncovered.isEmpty())
    }

    /** Post-forming: every quench-and-temper steel must get hours worked out from its section. */
    @Test fun everyQuenchAndTemperSteelGetsATimedCycle() {
        val missing = mutableListOf<String>()
        for (m in mats) {
            if (PokaYokeEngine.materialFamily(m) != MaterialFamily.CARBON_ALLOY_STEEL) continue
            val aust = m.heatTreatmentSpec.austenitizingOrSolutionTempC
            val quenched = aust.isNotBlank() && !aust.contains("N/A", true)
            if (!quenched) continue
            val cycle = HeatTreatCycleEngine.quenchTemper(m, 10.0)
            if (cycle == null || cycle.totalLow <= 0.0) missing += m.key
        }
        assertTrue("quench-and-temper steels with no timed cycle: $missing", missing.isEmpty())
    }

    /** Every aluminium and copper grade must get the anneal the catalogue left blank. */
    @Test fun everyNonFerrousGradeGetsItsAnnealCycle() {
        val missing = mutableListOf<String>()
        for (m in mats) {
            val fam = PokaYokeEngine.materialFamily(m)
            if (fam != MaterialFamily.ALUMINUM && fam != MaterialFamily.COPPER_ALLOY) continue
            val c = HeatTreatCycleEngine.softAnneal(m)
            if (c == null || c.stages.isEmpty() || c.totalLow <= 0.0) missing += m.key
        }
        assertTrue("non-ferrous grades with no anneal cycle: $missing", missing.isEmpty())
    }

    /** Any cycle the app shows must have an hour figure on every single stage. */
    @Test fun noCycleAnywhereHasAnUntimedStage() {
        for (m in mats) {
            listOfNotNull(
                AnnealCycleEngine.spheroidize(m)?.stages,
                HeatTreatCycleEngine.quenchTemper(m, 10.0)?.stages,
                HeatTreatCycleEngine.softAnneal(m)?.stages
            ).flatten().forEach { st ->
                assertTrue("${m.key} / ${st.name} has no hours", st.hoursLow > 0.0)
                assertTrue("${m.key} / ${st.name} hours are reversed", st.hoursHigh >= st.hoursLow)
                assertTrue("${m.key} / ${st.name} has no detail", st.detail.isNotBlank())
            }
        }
    }
}
