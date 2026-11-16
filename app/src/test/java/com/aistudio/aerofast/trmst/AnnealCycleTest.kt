package com.aistudio.aerofast.trmst

import com.aistudio.aerofast.trmst.data.*
import org.junit.Assert.*
import org.junit.Test

/** The anneal card has to give a complete cycle with timing, not just a temperature. */
class AnnealCycleTest {
    private fun mat(k: String) = FastenerCatalog.MATERIALS[k]!!

    @Test fun everyStageIsTimedAndTheTotalIsTheSumOfTheStages() {
        val c = AnnealCycleEngine.spheroidize(mat("10B21"))!!
        assertEquals(7, c.stages.size)
        for (s in c.stages) {
            assertTrue("${s.name} has no time", s.hoursLow > 0.0 && s.hoursHigh >= s.hoursLow)
            assertTrue("${s.name} has no detail", s.detail.isNotBlank())
            assertTrue("${s.name} has no hours text", s.hoursText.contains("h"))
        }
        assertEquals(c.stages.sumOf { it.hoursLow }, c.totalLow, 1e-9)
        assertEquals(c.stages.sumOf { it.hoursHigh }, c.totalHigh, 1e-9)
    }

    /** A bell-furnace spheroidize cycle for wire runs roughly a day; flag anything far outside that. */
    @Test fun theTotalCycleIsARealisticFurnaceBooking() {
        val c = AnnealCycleEngine.spheroidize(mat("10B21"), chargeTonnes = 10.0)!!
        assertTrue("low ${c.totalLow} h", c.totalLow in 18.0..30.0)
        assertTrue("high ${c.totalHigh} h", c.totalHigh in 24.0..40.0)
        assertTrue(c.totalText.contains("h"))
        assertTrue(c.hoursPerTonne.endsWith("h/t"))
    }

    @Test fun aHeavierChargeTakesLongerButLessTimePerTonne() {
        val small = AnnealCycleEngine.spheroidize(mat("10B21"), chargeTonnes = 2.0)!!
        val big = AnnealCycleEngine.spheroidize(mat("10B21"), chargeTonnes = 30.0)!!
        assertTrue("heat-up must scale with the charge", big.totalLow > small.totalLow)
        fun perTonne(c: AnnealCycle) = (c.totalLow + c.totalHigh) / 2.0 / c.chargeTonnes
        assertTrue("a full furnace must be cheaper per tonne", perTonne(big) < perTonne(small))
    }

    @Test fun alloySteelSoaksLongerThanPlainCarbon() {
        fun soak(k: String) = AnnealCycleEngine.spheroidize(mat(k))!!.stages.first { it.name.contains("Soak") }
        assertTrue(soak("4140").hoursHigh > soak("10B21").hoursHigh)
        assertTrue(soak("10B21").hoursHigh > soak("1008").hoursHigh)
    }

    /** The slow leg through transformation is what keeps the carbide globular: it must not be skipped. */
    @Test fun theControlledCoolIsInTheCycle() {
        val c = AnnealCycleEngine.spheroidize(mat("10B21"))!!
        val slow = c.stages.first { it.detail.contains("20 °C/h") }
        // The soak band follows this grade's own A1, so derive the leg rather than pin a number
        val soakLo = SteelCompositions.ac1For(mat("10B21"))!! - 25.0
        assertEquals((soakLo - 550.0) / 20.0, slow.hoursLow, 1e-9)
        assertTrue(c.stages.any { it.detail.contains("80 °C/h") })
    }

    @Test fun theCycleCarriesItsSourcesAndItsCaveats() {
        val c = AnnealCycleEngine.spheroidize(mat("10B21"))!!
        assertTrue(c.sources.size >= 4)
        assertTrue(c.cautions.any { it.contains("A1") })
        assertTrue(c.cautions.any { it.contains("coil mass") })
        assertTrue(c.basis.contains("10.0 t"))
    }

    /** Only the spheroidize route is modelled; a stainless or titanium anneal is a different cycle. */
    @Test fun onlySteelGetsASpheroidizeCycle() {
        assertNull(AnnealCycleEngine.spheroidize(mat("SS304")))
        assertNull(AnnealCycleEngine.spheroidize(mat("TI_GR5")))
        assertNull(AnnealCycleEngine.spheroidize(mat("AL7075")))
        assertNotNull(AnnealCycleEngine.spheroidize(mat("4140")))
    }

    @Test fun thickerWireSoaksLonger() {
        fun soakHi(dia: Double) = AnnealCycleEngine.spheroidize(mat("10B21"), wireDiaMm = dia)!!
            .stages.first { it.name.contains("Soak") }.hoursHigh
        assertTrue("20 mm must soak longer than 3 mm", soakHi(20.0) > soakHi(3.0))
        assertTrue("10 mm must soak longer than 3 mm", soakHi(10.0) > soakHi(3.0))
    }

    @Test fun diameterAndGradeCombineByTakingTheHigher() {
        // 4135 at 3.5 mm: grade (Cr+Mo=1.15, high alloy) dominates over small diameter
        val crMoSmall = AnnealCycleEngine.spheroidize(mat("4135"), wireDiaMm = 3.5)!!
            .stages.first { it.name.contains("Soak") }
        assertTrue("Cr-Mo grade at small dia should be >= 8 h", crMoSmall.hoursLow >= 8.0)

        // 1008 at 20 mm: diameter dominates over low-alloy grade
        val plainBig = AnnealCycleEngine.spheroidize(mat("1008"), wireDiaMm = 20.0)!!
            .stages.first { it.name.contains("Soak") }
        assertTrue("Plain carbon at 20 mm should be >= 12 h", plainBig.hoursLow >= 12.0)
    }

    @Test fun diameterSoakRangesMatchPublishedTable() {
        val (lo2, hi2) = SteelCompositions.soakHoursForDiameter(3.0)
        assertEquals(6.0, lo2, 1e-9); assertEquals(7.0, hi2, 1e-9)
        val (lo5, hi5) = SteelCompositions.soakHoursForDiameter(6.0)
        assertEquals(7.0, lo5, 1e-9); assertEquals(9.0, hi5, 1e-9)
        val (lo10, hi10) = SteelCompositions.soakHoursForDiameter(12.0)
        assertEquals(9.0, lo10, 1e-9); assertEquals(12.0, hi10, 1e-9)
        val (lo18, hi18) = SteelCompositions.soakHoursForDiameter(20.0)
        assertEquals(12.0, lo18, 1e-9); assertEquals(16.0, hi18, 1e-9)
    }

    /** The card used to say "enter per grade" for the soak; it must now point at the cycle. */
    @Test fun theSoakIsNoLongerAnUnsourcedBlank() {
        val spec = SoftAnnealEngine.build(mat("10B21"))
        assertEquals(AnnealRoute.SPHEROIDIZE, spec.route)
        assertFalse(spec.unsourced.any { it.contains("Soak hours") })
        assertTrue(spec.soakTime.isNotBlank())
    }
}
