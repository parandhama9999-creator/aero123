package com.example

import com.example.data.*
import org.junit.Assert.*
import org.junit.Test

/** The quench-and-temper and non-ferrous cycles must also carry hours, not just temperatures. */
class HeatTreatCycleTest {
    private fun mat(k: String) = FastenerCatalog.MATERIALS[k]!!

    /** AMS 2759/1: 2 h minimum, plus 1 h per 25 mm of section over the first 25 mm. */
    @Test fun temperTimeFollowsTheSpecRule() {
        assertEquals(2.0, HeatTreatCycleEngine.temperHours(6.0), 1e-9)
        assertEquals(2.0, HeatTreatCycleEngine.temperHours(25.4), 1e-9)
        assertEquals(3.0, HeatTreatCycleEngine.temperHours(30.0), 1e-9)
        assertEquals(3.0, HeatTreatCycleEngine.temperHours(50.8), 1e-9)
        assertEquals(4.0, HeatTreatCycleEngine.temperHours(60.0), 1e-9)
    }

    /** Austenitize: 1 h per 25 mm, never under an hour. */
    @Test fun soakTimeIsOneHourPerInchWithAnHourFloor() {
        assertEquals(1.0, HeatTreatCycleEngine.soakHours(5.0), 1e-9)
        assertEquals(1.0, HeatTreatCycleEngine.soakHours(25.4), 1e-9)
        assertTrue(HeatTreatCycleEngine.soakHours(50.8) >= 2.0)
        assertTrue(HeatTreatCycleEngine.soakHours(76.2) >= 3.0)
    }

    @Test fun aQuenchAndTemperSteelGetsAFullyTimedCycle() {
        val c = HeatTreatCycleEngine.quenchTemper(mat("10B21"), 10.0)!!
        assertTrue(c.stages.size >= 6)
        for (s in c.stages) {
            assertTrue("${s.name} has no time", s.hoursLow > 0.0 && s.hoursHigh >= s.hoursLow)
            assertTrue("${s.name} has no detail", s.detail.isNotBlank())
        }
        assertTrue(c.stages.any { it.name.contains("Austenitize soak") })
        assertTrue(c.stages.any { it.name.contains("Temper soak") })
        assertTrue(c.stages.any { it.name.contains("Quench") })
        assertEquals(c.stages.sumOf { it.hoursLow }, c.totalLow, 1e-9)
        assertTrue(c.sources.any { it.contains("AMS 2759/1") })
        assertTrue(c.cautions.any { it.contains("LOAD") })
    }

    @Test fun aThickerSectionSoaksAndTempersLonger() {
        val thin = HeatTreatCycleEngine.quenchTemper(mat("4140"), 8.0)!!
        val thick = HeatTreatCycleEngine.quenchTemper(mat("4140"), 60.0)!!
        assertTrue(thick.totalLow > thin.totalLow)
    }

    /** A plated high-strength part must show the bake, because it is a real cycle step with real hours. */
    @Test fun aHighStrengthGradeShowsTheHydrogenBake() {
        val c = HeatTreatCycleEngine.quenchTemper(mat("4140"), 10.0)!!
        assertTrue(c.stages.any { it.name.contains("Hydrogen bake") })
    }

    @Test fun austeniticAndNonFerrousGradesGetNoQuenchAndTemperCycle() {
        assertNull(HeatTreatCycleEngine.quenchTemper(mat("SS304"), 10.0))
        assertNull(HeatTreatCycleEngine.quenchTemper(mat("AL7075"), 10.0))
        assertNull(HeatTreatCycleEngine.quenchTemper(mat("TI_GR5"), 10.0))
    }

    /** Aluminium's O-temper cycle was blank in the catalogue; the slow cool is the whole point of it. */
    @Test fun aluminiumGetsTheFourStageOTemperCycle() {
        val c = HeatTreatCycleEngine.softAnneal(mat("AL7075"))!!
        assertTrue(c.stages.any { it.detail.contains("413") })
        val slow = c.stages.first { it.name.contains("Controlled cool") }
        assertEquals((413.0 - 230.0) / 28.0, slow.hoursLow, 0.01)   // 7xxx cools to 230 °C
        assertTrue(c.cautions.any { it.contains("HARDER") })
        assertTrue(c.cautions.any { it.contains("10 °C/h") })        // the datasheet conflict is surfaced
        val six = HeatTreatCycleEngine.softAnneal(mat("AL6061"))!!
        assertEquals((413.0 - 260.0) / 28.0, six.stages.first { it.name.contains("Controlled cool") }.hoursLow, 0.01)
    }

    @Test fun nonHeatTreatableAluminiumGetsTheRecrystallisationAnnealInstead() {
        val c = HeatTreatCycleEngine.softAnneal(mat("AL5052"))!!
        assertTrue(c.stages.any { it.detail.contains("343") })
        assertFalse(c.stages.any { it.name.contains("Controlled cool") })
    }

    @Test fun copperGetsTheGenericEnvelopeAndSaysSo() {
        val c = HeatTreatCycleEngine.softAnneal(mat("BRASS_C360"))!!
        assertTrue(c.basis.contains("Generic"))
        assertTrue(c.cautions.any { it.contains("ASTM B601") })
        val be = HeatTreatCycleEngine.softAnneal(mat("CUBE2"))!!
        assertTrue(be.stages.any { it.name.contains("Water quench") })
    }

    @Test fun carbonSteelGetsNoSoftAnnealHere_itGoesThroughTheSpheroidizeRoute() {
        assertNull(HeatTreatCycleEngine.softAnneal(mat("10B21")))
        assertNull(HeatTreatCycleEngine.softAnneal(mat("4140")))
    }

    /** Stainless is solution annealed: the quench is the softening step, not the slow cool. */
    @Test fun stainlessGetsATimedSolutionAnneal() {
        val c = HeatTreatCycleEngine.softAnneal(mat("SS304"), 10.0)!!
        assertTrue(c.title.contains("Solution"))
        assertTrue(c.stages.any { it.name.contains("Solution soak") })
        assertTrue(c.stages.any { it.name.contains("Rapid quench") })
        assertTrue(c.cautions.any { it.contains("sensitises") })
        // A PH grade is headed in Condition A and aged after forming
        val ph = HeatTreatCycleEngine.softAnneal(mat("17_4PH"), 10.0)!!
        assertTrue(ph.stages.any { it.name.contains("AFTER forming") })
    }

    /** The two bronzes were falling through to carbon steel and getting steel rules. */
    @Test fun bronzesAreClassifiedAsCopperAlloys() {
        for (k in listOf("PHOS_BRONZE_C510", "SIL_BRONZE_C655", "CUNI_C715", "BRASS_C360")) {
            assertEquals(k, MaterialFamily.COPPER_ALLOY, PokaYokeEngine.materialFamily(mat(k)))
            assertNull("$k must not get a steel spheroidize cycle", AnnealCycleEngine.spheroidize(mat(k)))
        }
    }
}
