package com.aistudio.aerofast.trmst

import com.aistudio.aerofast.trmst.data.HeadingProgression
import com.aistudio.aerofast.trmst.data.RawMaterialPlanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs

class RawMaterialPlannerTest {

    private val steel = 7.85
    // An M10 x 60 hex bolt comes to roughly this once the 4 % allowance is on
    private val volMm3 = 6200.0

    @Test
    fun kgPerMetreMatchesTheMillFigureForPlainWire() {
        // 6 mm steel wire is 0.222 kg/m; 8 mm is 0.395 kg/m
        val plan = RawMaterialPlanner.plan(volMm3, steel, 1000, listOf(6.0, 8.0))!!
        assertEquals(0.2219, plan.options.first { it.wireDiaMm == 6.0 }.kgPerMetre, 0.0005)
        assertEquals(0.3946, plan.options.first { it.wireDiaMm == 8.0 }.kgPerMetre, 0.0005)
    }

    @Test
    fun batchWeightDoesNotDependOnTheDiameterChosen() {
        val plan = RawMaterialPlanner.plan(volMm3, steel, 25_000, listOf(5.5, 6.0, 7.0, 8.0))!!
        val weights = plan.options.map { it.batchWeightKg }.distinct()
        assertEquals("upsetting moves metal, it does not create it", 1, weights.size)
        // 6200 mm^3 x 7.85 g/cm^3 = 48.67 g a piece; x 25000 = 1216.75 kg
        assertEquals(1216.75, plan.batchWeightKg, 0.1)
    }

    @Test
    fun lengthTimesKgPerMetreComesBackToTheBatchWeight() {
        val plan = RawMaterialPlanner.plan(volMm3, steel, 10_000, listOf(5.0, 6.5, 9.0))!!
        for (o in plan.options) {
            assertEquals(
                "Ø${o.wireDiaMm}: length x kg/m must equal the batch weight",
                plan.batchWeightKg, o.batchLengthM * o.kgPerMetre, 0.5
            )
        }
    }

    @Test
    fun cutOffLengthFallsWithTheSquareOfDiameter() {
        val plan = RawMaterialPlanner.plan(volMm3, steel, 100, listOf(5.0, 10.0))!!
        val short = plan.options.first { it.wireDiaMm == 10.0 }.cutOffLengthMm
        val long = plan.options.first { it.wireDiaMm == 5.0 }.cutOffLengthMm
        assertEquals("doubling the diameter must quarter the length", 4.0, long / short, 0.001)
        // Volume / area, checked independently
        assertEquals(volMm3 / (PI / 4.0 * 25.0), long, 0.01)
    }

    @Test
    fun thinWireThatCannotGatherTheHeadIsFlaggedNotSilentlyOffered() {
        // A big head on deliberately thin wire pushes L/D past the 3-blow limit
        val plan = RawMaterialPlanner.plan(
            grossVolumeMm3 = volMm3, densityGPerCm3 = steel, batchQty = 1000,
            wireDiameters = listOf(3.0, 10.0), headVolumeMm3 = 2500.0
        )!!
        val thin = plan.options.first { it.wireDiaMm == 3.0 }
        assertTrue("Ø3 should not be offered as workable", !thin.feasible)
        assertTrue(thin.cautions.any { it.contains("3-blow limit") })
        assertTrue("Ø10 should be fine", plan.options.first { it.wireDiaMm == 10.0 }.feasible)
    }

    @Test
    fun shankLargerThanTheWireIsRejected() {
        val plan = RawMaterialPlanner.plan(volMm3, steel, 500, listOf(4.0), shankDiaMm = 6.0)!!
        assertTrue(plan.options.single().cautions.any { it.contains("cannot be formed") })
    }

    @Test
    fun overDeepExtrusionIsFlagged() {
        // Ø10 wire down to a Ø7 shank removes 51 % of area
        val plan = RawMaterialPlanner.plan(volMm3, steel, 500, listOf(10.0), shankDiaMm = 7.0)!!
        val o = plan.options.single()
        assertEquals(51.0, o.extrusionReductionPct!!, 0.5)
        assertTrue(o.cautions.any { it.contains("30 %") })
    }

    @Test
    fun recommendsTheShortestWorkableCutOff() {
        val plan = RawMaterialPlanner.plan(
            grossVolumeMm3 = volMm3, densityGPerCm3 = steel, batchQty = 1000,
            wireDiameters = listOf(3.0, 6.0, 8.0), headVolumeMm3 = 2500.0
        )!!
        // Ø3 is infeasible, so the thickest workable wire wins on length
        assertEquals(8.0, plan.recommended!!.wireDiaMm, 0.001)
    }

    @Test
    fun piecesPerTonneAndCoilCount() {
        val plan = RawMaterialPlanner.plan(volMm3, steel, 25_000, listOf(6.0))!!
        assertEquals(20_546.0, plan.piecesPerTonne.toDouble(), 50.0)
        // 1216.75 kg at 1000 kg a coil is two coils, not one and a bit
        assertEquals(2, RawMaterialPlanner.coilsNeeded(plan.batchWeightKg, 1000.0))
    }

    @Test
    fun parsesTheDiameterFieldHoweverItIsTyped() {
        assertEquals(listOf(5.5, 6.0, 6.5), RawMaterialPlanner.parseDiameters("6.5, 5.5 6"))
        assertEquals(listOf(5.5, 6.0), RawMaterialPlanner.parseDiameters("5.5; 6; 5.5; junk"))
        assertTrue(RawMaterialPlanner.parseDiameters("").isEmpty())
    }

    @Test
    fun refusesNonsenseInputRatherThanReturningZeroes() {
        assertEquals(null, RawMaterialPlanner.plan(0.0, steel, 100, listOf(6.0)))
        assertEquals(null, RawMaterialPlanner.plan(volMm3, steel, 0, listOf(6.0)))
        assertEquals(null, RawMaterialPlanner.plan(volMm3, steel, 100, emptyList()))
    }
}

class WireWindowAndStageClearanceTest {

    private val steel = 7.85
    private val volMm3 = 1400.0            // about an M6 x 30 screw
    private val m6RollingBlank = 5.35      // pitch dia of M6 x 1.0 is 5.3505
    private val m6ShankWire = 5.90

    @Test
    fun theWireWindowRunsFromRollingBlankToShank() {
        val w = RawMaterialPlanner.suggestedDiameters(m6RollingBlank, m6ShankWire)
        assertEquals(4, w.size)
        assertEquals("the pitch-diameter end", 5.35, w.first(), 0.001)
        assertEquals("the shank end", 5.90, w.last(), 0.001)
        assertTrue("wire is drawn in 0.05 steps", w.all { abs(it / 0.05 - Math.round(it / 0.05)) < 1e-6 })
    }

    @Test
    fun aDiameterBelowTheRollingBlankStillGivesACutOffButIsFlagged() {
        // The shop wants to run Ø5.20 on an M6: the length is needed either way, the risk is named.
        val plan = RawMaterialPlanner.plan(
            grossVolumeMm3 = volMm3, densityGPerCm3 = steel, batchQty = 50_000,
            wireDiameters = listOf(5.20, 5.35, 5.90), rollingBlankDiaMm = m6RollingBlank
        )!!
        val thin = plan.options.first { it.wireDiaMm == 5.20 }
        assertEquals(volMm3 / (PI / 4.0 * 5.20 * 5.20), thin.cutOffLengthMm, 0.01)
        assertTrue("a length must still be given", thin.cutOffLengthMm > 0.0)
        assertTrue(thin.cautions.any { it.contains("rolling blank") })

        assertTrue("Ø5.35 is the blank itself", plan.options.first { it.wireDiaMm == 5.35 }.feasible)
        assertEquals("thread rolling blank", plan.options.first { it.wireDiaMm == 5.35 }.role)
    }

    @Test
    fun theShortestCutOffIsTheThickestWire() {
        val plan = RawMaterialPlanner.plan(
            grossVolumeMm3 = volMm3, densityGPerCm3 = steel, batchQty = 10_000,
            wireDiameters = listOf(5.35, 5.90), rollingBlankDiaMm = m6RollingBlank
        )!!
        val onBlank = plan.options.first { it.wireDiaMm == 5.35 }
        val onShank = plan.options.first { it.wireDiaMm == 5.90 }
        assertTrue("blank wire runs longer per piece", onBlank.cutOffLengthMm > onShank.cutOffLengthMm)
        assertEquals("but the run weighs the same", onBlank.batchWeightKg, onShank.batchWeightKg, 0.001)
        assertTrue("and needs more metres", onBlank.batchLengthM > onShank.batchLengthM)
    }

    @Test
    fun stageClearanceHoldsTheStatedFloorAndCeiling() {
        assertEquals(0.02, HeadingProgression.stageClearance(3.0), 0.0005)
        assertEquals(0.09, HeadingProgression.stageClearance(24.0), 0.0005)
        // clamped outside the range rather than running off
        assertEquals(0.02, HeadingProgression.stageClearance(1.0), 0.0005)
        assertEquals(0.09, HeadingProgression.stageClearance(40.0), 0.0005)
    }

    @Test
    fun stageClearanceGrowsWithDiameterAndLandsOnDieShopSteps() {
        assertEquals(0.03, HeadingProgression.stageClearance(6.0), 0.0005)
        assertEquals(0.05, HeadingProgression.stageClearance(12.0), 0.0005)
        assertEquals(0.08, HeadingProgression.stageClearance(20.0), 0.0005)
        var last = 0.0
        for (d in 3..24) {
            val c = HeadingProgression.stageClearance(d.toDouble())
            assertTrue("clearance must not fall as diameter rises", c >= last)
            assertTrue("every value is a 0.01 step", abs(c / 0.01 - Math.round(c / 0.01)) < 1e-6)
            assertTrue("never outside the shop rule", c >= 0.02 - 1e-9 && c <= 0.09 + 1e-9)
            last = c
        }
    }

    @Test
    fun theFirstDieBoreIsTheWirePlusItsClearance() {
        val plan = RawMaterialPlanner.plan(volMm3, steel, 1000, listOf(6.0, 20.0))!!
        assertEquals(6.03, plan.options.first { it.wireDiaMm == 6.0 }.firstDieBoreMm, 0.0005)
        assertEquals(20.08, plan.options.first { it.wireDiaMm == 20.0 }.firstDieBoreMm, 0.0005)
    }
}
