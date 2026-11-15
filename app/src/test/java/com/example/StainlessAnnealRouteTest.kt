package com.example

import com.example.data.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Stainless is not one family for softening. An austenitic is solution annealed and quenched; for
 * a martensitic that same quench from temperature is the HARDENING treatment, and its anneal is a
 * sub-critical hold with a slow cool; a ferritic only recrystallises and must pass 540 – 400 °C
 * briskly. Maraging steel is never quenched and tempered at all.
 */
class StainlessAnnealRouteTest {

    private fun mat(k: String) = FastenerCatalog.MATERIALS[k]!!
    private fun cycle(k: String) = HeatTreatCycleEngine.softAnneal(mat(k), 10.0)

    @Test fun martensiticGradesAreNotQuenchedSoft() {
        for (k in listOf("SS410", "20X13", "30X13")) {
            val c = cycle(k)!!
            assertTrue("$k must get the martensitic anneal", c.title.contains("Martensitic"))
            assertTrue("$k must have NO rapid quench — that would harden it",
                c.stages.none { it.name.contains("quench", true) })
            assertTrue("$k softens on the slow furnace leg",
                c.stages.any { it.name.contains("Furnace cool") })
            assertTrue("$k must say the spec-card temperature is the hardening treatment",
                c.cautions.any { it.contains("HARDENING") })
        }
    }

    @Test fun nickelBearing431TypeGetsTheSubCriticalHold() {
        for (k in listOf("SS431", "14KH17N2")) {
            val c = cycle(k)!!
            assertTrue("$k holds sub-critical", c.stages.any { it.detail.contains("620 – 660") })
            assertTrue("$k never fully softens and the cycle says so",
                c.stages.any { it.detail.contains("230 – 270 HB") })
            assertTrue(c.stages.none { it.name.contains("Furnace cool") })
        }
    }

    @Test fun ferritic430RecrystallisesAndMissesTheEmbrittlementBand() {
        val c = cycle("SS430")!!
        assertTrue(c.title.contains("Ferritic"))
        assertTrue(c.stages.any { it.detail.contains("705 – 790") })
        assertTrue("475 °C embrittlement must be named",
            (c.stages.map { it.detail } + c.cautions).any { it.contains("475 °C") })
        assertTrue(c.stages.none { it.name.contains("quench", true) })
        assertTrue("no slow furnace leg either — that is the martensitic cycle",
            c.stages.none { it.name.contains("Furnace cool") })
    }

    @Test fun austeniticDuplexAndPhKeepTheTrueSolutionAnneal() {
        for (k in listOf("SS304", "SS316", "NITRONIC60", "DUPLEX_2205", "SUPER_DUPLEX_2507", "17_4PH")) {
            val c = cycle(k)!!
            assertTrue("$k is solution annealed and quenched", c.stages.any { it.name.contains("Rapid quench") })
        }
        assertTrue("a PH grade still ages after forming",
            cycle("17_4PH")!!.stages.any { it.name.contains("Age AFTER forming") })
    }

    @Test fun everyStainlessGradeGetsTheCycleItsSubfamilyNeeds() {
        var n = 0
        FastenerCatalog.MATERIALS.values.forEach { m ->
            val t = PokaYokeEngine.stainlessType(m) ?: return@forEach
            val c = HeatTreatCycleEngine.softAnneal(m, 10.0)
            assertNotNull("${m.key} must get a softening cycle", c)
            val quenched = c!!.stages.any { it.name.contains("Rapid quench") }
            val shouldQuench = t != PokaYokeEngine.StainlessType.MARTENSITIC &&
                t != PokaYokeEngine.StainlessType.FERRITIC
            assertEquals("${m.key} ($t): quench only where it softens", shouldQuench, quenched)
            n++
        }
        assertTrue("the sweep has to cover the whole stainless family", n >= 20)
    }

    @Test fun theCardNoLongerTellsAMartensiticToQuenchItselfSoft() {
        val m410 = SoftAnnealEngine.build(mat("SS410"))
        assertTrue(m410.temperatureC.contains("845 – 885"))
        assertTrue(m410.coolingRate.contains("Furnace cool"))
        val m431 = SoftAnnealEngine.build(mat("SS431"))
        assertTrue(m431.temperatureC.contains("620 – 660"))
        val f430 = SoftAnnealEngine.build(mat("SS430"))
        assertTrue(f430.temperatureC.contains("705 – 790"))
        // and the austenitic card is untouched
        val a304 = SoftAnnealEngine.build(mat("SS304"))
        assertTrue(a304.coolingRate.contains("Rapid"))
    }

    // --- maraging ---------------------------------------------------------------------------------

    @Test fun maragingIsSolutionTreatedAndAgedNeverQuenchedAndTempered() {
        val c = HeatTreatCycleEngine.quenchTemper(mat("MARAGING_250"), 10.0)!!
        assertTrue(c.title.contains("Solution treat & age"))
        assertTrue("no quench stage", c.stages.none { it.name.contains("Quench") })
        assertTrue("no untempered-martensite caution — maraging martensite is soft",
            c.stages.none { it.detail.contains("untempered martensite") })
        assertTrue("air cool is the point", c.stages.any { it.name.contains("Air cool") })
        assertTrue("the age carries hours", c.stages.any { it.name.contains("Age") && it.hoursHigh >= 3.0 })
    }

    @Test fun maragingWirePreparationNowHasItsOwnTimedCycle() {
        val c = HeatTreatCycleEngine.softAnneal(mat("MARAGING_250"), 10.0)!!
        assertTrue(c.title.contains("Solution anneal"))
        assertTrue(c.stages.any { it.detail.contains("815") })
        assertTrue(c.stages.none { it.name.contains("Quench") })
        assertTrue(c.totalLow > 0.0)
    }

    @Test fun ordinarySteelsStillGetTheRealQuenchAndTemper() {
        val c = HeatTreatCycleEngine.quenchTemper(mat("4140"), 10.0)!!
        assertTrue(c.title.contains("Quench & temper"))
        assertTrue(c.stages.any { it.name.contains("Quench") })
    }
}
