package com.aistudio.aerofast.trmst

import com.aistudio.aerofast.trmst.data.AnnealRoute
import com.aistudio.aerofast.trmst.data.FastenerCatalog
import com.aistudio.aerofast.trmst.data.MaterialFamily
import com.aistudio.aerofast.trmst.data.PokaYokeEngine
import com.aistudio.aerofast.trmst.data.SoftAnnealEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResearchedMaterialsTest {
    private val added = listOf(
        "20MNB4", "23MNB4", "32CRB4", "36CRB4", "34CRMO4", "41CR4", "SCM435", "18CRMO4", "1022",
        "MP159", "INCONEL_X750", "TI_BETA_C", "TI_3AL25V", "AL2117", "AL7050", "AL5056"
    )
    private fun m(k: String) = FastenerCatalog.MATERIALS.getValue(k)

    @Test fun all16GradesAreInTheCatalogue_andBaseGradesAreUntouched() {
        added.forEach { assertNotNull(it, FastenerCatalog.MATERIALS[it]) }
        // base grades keep no estimate marker
        assertEquals(null, m("10B21").estimatedFrom)
        assertEquals(1040.0, m("10B21").uts, 0.0)
    }

    @Test fun everyNewGrade_namesItsBaseGradeAndItsSources() {
        added.forEach {
            val g = m(it)
            assertNotNull("$it estimatedFrom", g.estimatedFrom)
            assertTrue("$it base ${g.estimatedFrom} exists", FastenerCatalog.MATERIALS.containsKey(g.estimatedFrom))
            assertTrue("$it has no sources", g.dataSources.isNotEmpty())
        }
    }

    @Test fun boltSteels_carryIso898ClassMinima() {
        listOf("23MNB4", "32CRB4", "34CRMO4", "41CR4", "SCM435").forEach {
            assertEquals(it, 1040.0, m(it).postForgingTargetUtsMpa, 0.0)
            assertEquals(it, 940.0, m(it).yieldStrengthMpa, 0.0)
            assertTrue(m(it).heatTreatmentSpec.hydrogenBakingRequired)
        }
        assertEquals(800.0, m("20MNB4").postForgingTargetUtsMpa, 0.0)
        assertEquals(52.0, m("20MNB4").reductionOfAreaZPct, 0.0)
    }

    @Test fun sourcedHeatTreatCyclesAreThePublishedOnes() {
        assertTrue(m("34CRMO4").heatTreatmentSpec.austenitizingOrSolutionTempC.startsWith("830°C – 870°C"))
        assertTrue(m("23MNB4").heatTreatmentSpec.austenitizingOrSolutionTempC.startsWith("880°C"))
        assertTrue(m("36CRB4").heatTreatmentSpec.austenitizingOrSolutionTempC.contains("estimated from 32CrB4"))
        assertEquals("649°C – 677°C (1200 – 1250°F)", m("MP159").heatTreatmentSpec.temperingOrAgingTempC)
        assertEquals(1793.0, m("MP159").postForgingTargetUtsMpa, 0.0)
        assertEquals(2.83, m("AL7050").density, 0.0)
    }

    @Test fun familiesRouteCorrectly() {
        assertEquals(MaterialFamily.NICKEL_SUPERALLOY, PokaYokeEngine.materialFamily(m("MP159")))
        assertEquals(MaterialFamily.NICKEL_SUPERALLOY, PokaYokeEngine.materialFamily(m("INCONEL_X750")))
        assertEquals(MaterialFamily.TITANIUM, PokaYokeEngine.materialFamily(m("TI_BETA_C")))
        assertEquals(MaterialFamily.ALUMINUM, PokaYokeEngine.materialFamily(m("AL2117")))
        assertEquals(MaterialFamily.CARBON_ALLOY_STEEL, PokaYokeEngine.materialFamily(m("34CRMO4")))
    }

    @Test fun rivetAlloys_areDrivenAsReceived_notAnnealed() {
        listOf("AL2117", "AL7050", "AL5056").forEach {
            val s = SoftAnnealEngine.build(m(it))
            assertEquals(it, AnnealRoute.O_TEMPER, s.route)
            assertTrue("$it should not need an in-house anneal", !s.inHouseAnnealNeeded)
        }
    }

    @Test fun yieldNeverExceedsTensile() {
        added.forEach { assertTrue(it, m(it).yieldStrengthMpa <= m(it).postForgingTargetUtsMpa) }
    }
}
