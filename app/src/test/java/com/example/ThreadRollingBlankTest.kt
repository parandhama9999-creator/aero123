package com.example

import com.example.data.CalculationEngine
import com.example.data.CoatingType
import com.example.data.FastenerCatalog
import com.example.data.FastenerType
import com.example.data.ProductStandard
import com.example.data.PropertyClass
import com.example.data.ThreadRollingBlank
import com.example.data.UnitSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A rolled thread is formed at constant volume, so the blank is set by the balance between the
 * metal taken out of the root and the metal raised into the crest. MJ and UNJ carry a mandatory
 * large root radius, which makes the root shallower, so less metal is displaced and the blank has
 * to be larger. These tests pin that difference and guard the plain form against drifting with it.
 */
class ThreadRollingBlankTest {

    private val steel = FastenerCatalog.MATERIALS["10B21"]!!

    private fun blankFor(threadClass: String, dMm: Double = 10.0, pitch: Double = 1.5): Double {
        val idx = FastenerCatalog.sizeIndexFor(UnitSystem.METRIC, dMm)
        return CalculationEngine.calculate(
            FastenerType.HEX_BOLT, UnitSystem.METRIC, ProductStandard.ISO_4014, idx,
            pitch, 60.0, threadClass, steel, CoatingType.ZN_NI, 100,
            propertyClass = PropertyClass.CLASS_8_8
        ).blankTargetDia
    }

    // --- the geometry itself -------------------------------------------------------------------

    @Test
    fun theVolumeBalancePutsThePlainBlankJustAboveThePitchDiameter() {
        // Balancing the form by hand gives blank = d2 + 0.01054P for the plain root and
        // d2 + 0.02406P at the shallow end of the J band; the difference is what we apply.
        assertEquals(0.01352, ThreadRollingBlank.controlledRootDelta(1.0, ThreadRollingBlank.ROOT_DEPTH_J_SHALLOW), 0.00005)
        assertEquals(0.00195, ThreadRollingBlank.controlledRootDelta(1.0, ThreadRollingBlank.ROOT_DEPTH_J_DEEP), 0.00005)
        assertEquals("mid of the allowed root radius", 0.00747, ThreadRollingBlank.controlledRootDelta(1.0), 0.00005)
    }

    @Test
    fun theCorrectionScalesWithPitchAndIsAlwaysPositive() {
        for (p in listOf(0.5, 1.0, 1.25, 1.5, 1.75, 2.0, 2.5)) {
            val delta = ThreadRollingBlank.controlledRootDelta(p)
            assertEquals("linear in pitch", ThreadRollingBlank.controlledRootDelta(1.0) * p, delta, 1e-9)
            assertTrue("a J blank is never smaller than a plain one", delta > 0.0)
        }
    }

    @Test
    fun theCorrectionBandBracketsTheDesignValue() {
        val r = ThreadRollingBlank.controlledRootDeltaRange(1.5)
        val mid = ThreadRollingBlank.controlledRootDelta(1.5)
        assertTrue("deep root needs least", r.start < mid)
        assertTrue("shallow root needs most", r.endInclusive > mid)
        // A shallower root displaces less metal, so it always demands the larger blank
        assertTrue(
            ThreadRollingBlank.controlledRootDelta(1.5, ThreadRollingBlank.ROOT_DEPTH_J_SHALLOW) >
                ThreadRollingBlank.controlledRootDelta(1.5, ThreadRollingBlank.ROOT_DEPTH_J_DEEP)
        )
    }

    // --- wired into the engine -----------------------------------------------------------------

    @Test
    fun mjBlankIsLargerThanTheSameClassWithoutTheJRoot() {
        val plain = blankFor("4h6h")
        val mj = blankFor("4h6h-MJ")
        assertEquals(
            "the whole difference is the root-form correction",
            ThreadRollingBlank.controlledRootDelta(1.5), mj - plain, 0.0015
        )
        assertTrue("M10x1.5 MJ must sit above plain 4h6h", mj > plain)
    }

    @Test
    fun theCorrectionIsAQuarterOfWhatA4hBandAllows() {
        // Grade 4 is 0.63 of the grade 6 band and position h has no allowance, so a systematic
        // offset this size is not something the tolerance can absorb.
        val idx = FastenerCatalog.sizeIndexFor(UnitSystem.METRIC, 10.0)
        val r = CalculationEngine.calculate(
            FastenerType.HEX_BOLT, UnitSystem.METRIC, ProductStandard.ISO_4014, idx,
            1.5, 60.0, "4h6h-MJ", steel, CoatingType.ZN_NI, 100, propertyClass = PropertyClass.CLASS_8_8
        )
        val band = r.pitchDiaMax - r.pitchDiaMin
        val delta = ThreadRollingBlank.controlledRootDelta(1.5)
        assertTrue("the correction must be a real fraction of the band", delta / band > 0.10)
        assertEquals("position h leaves d2max at basic", 10.0 - 0.649519 * 1.5, r.pitchDiaMax, 0.0005)
    }

    @Test
    fun plainMetricBlanksAreUnchangedByTheFix() {
        // Regression guard: the plain-form figure is calibrated to practice, not to the first-order
        // geometry above, and must not move when the J correction is added.
        //   blank = d2Target − 0.014P + km·P,  d2Target = d2Min + 0.55·Td2
        for ((d, p) in listOf(6.0 to 1.0, 10.0 to 1.5, 16.0 to 2.0)) {
            val idx = FastenerCatalog.sizeIndexFor(UnitSystem.METRIC, d)
            val r = CalculationEngine.calculate(
                FastenerType.HEX_BOLT, UnitSystem.METRIC, ProductStandard.ISO_4014, idx,
                p, 60.0, "6g", steel, CoatingType.ZN_NI, 100, propertyClass = PropertyClass.CLASS_8_8
            )
            val d2Target = r.pitchDiaMin + 0.55 * (r.pitchDiaMax - r.pitchDiaMin)
            val expected = d2Target - 0.014 * p + steel.km * p
            assertEquals("M$d×$p plain blank must not move", expected, r.blankTargetDia, 0.0015)
        }
    }

    @Test
    fun unjInchBlankIsLargerThanUnr() {
        val idx = FastenerCatalog.sizeIndexFor(UnitSystem.INCH, 0.375 * 25.4)
        fun inchBlank(cls: String) = CalculationEngine.calculate(
            FastenerType.HEX_BOLT, UnitSystem.INCH, ProductStandard.ASME_B18_2_1, idx,
            16.0, 50.0, cls, steel, CoatingType.ZN_NI, 100, propertyClass = PropertyClass.SAE_GRADE_5
        ).blankTargetDia
        // Same tolerance class on both sides, so the only difference left is the root form
        val unr = inchBlank("3A")
        val unj = inchBlank("3A-UNJ")
        val pitchMm = 25.4 / 16.0
        assertTrue("the UNJ root is shallower, so its blank is larger", unj > unr)
        assertEquals(
            "and larger by exactly the root-form correction",
            ThreadRollingBlank.controlledRootDelta(pitchMm), unj - unr, 0.0015
        )
    }
}
