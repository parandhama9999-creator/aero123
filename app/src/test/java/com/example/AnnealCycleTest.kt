package com.example

import com.example.data.*
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
        assertEquals((698.0 - 550.0) / 20.0, slow.hoursLow, 1e-9)
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

    /** The card used to say "enter per grade" for the soak; it must now point at the cycle. */
    @Test fun theSoakIsNoLongerAnUnsourcedBlank() {
        val spec = SoftAnnealEngine.build(mat("10B21"))
        assertEquals(AnnealRoute.SPHEROIDIZE, spec.route)
        assertFalse(spec.unsourced.any { it.contains("Soak hours") })
        assertTrue(spec.soakTime.isNotBlank())
    }
}
