package com.aistudio.aerofast.trmst

import com.aistudio.aerofast.trmst.data.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Cycle checks against published data (ATI, Special Metals, AMS 2759/1, plant spheroidize practice). */
class HeatTreatAuditTest {
    private fun mat(k: String) = FastenerCatalog.MATERIALS.getValue(k)
    private fun ht(k: String, d: Double = 10.0, pc: PropertyClass? = null) =
        CalculationEngine.calculateDynamicHeatTreatment(mat(k), d, false, pc)

    @Test fun ti64IsSolutionTreatedBelowTheBetaTransusAndWaterQuenched() {
        val t = ht("TI_GR5")
        assertTrue(t.austenitizingOrSolutionTempC, t.austenitizingOrSolutionTempC.startsWith("913°C – 954°C"))
        assertTrue(t.quenchMedium.contains("Water"))
        assertTrue(t.temperingOrAgingTempC.contains("524°C – 552°C"))
        assertTrue(t.standardReference.contains("AMS 4965"))
    }

    @Test fun betaCAndTi3Al25VKeepTheirOwnCyclesNotTi64() {
        assertEquals(mat("TI_BETA_C").heatTreatmentSpec.austenitizingOrSolutionTempC, ht("TI_BETA_C").austenitizingOrSolutionTempC)
        assertEquals(mat("TI_3AL25V").heatTreatmentSpec.austenitizingOrSolutionTempC, ht("TI_3AL25V").austenitizingOrSolutionTempC)
    }

    @Test fun waspaloySolutionIsFourHours() {
        assertTrue(ht("WASPALOY").soakingTimeMinutes.startsWith("240 mins"))
    }

    @Test fun specCardAndCycleCardShowTheSameTimesAndTemper() {
        val spec = ht("4140", 10.0, PropertyClass.CLASS_10_9)
        assertTrue(spec.temperingOrAgingTimeHours, spec.temperingOrAgingTimeHours.startsWith("2.0 hrs"))
        assertTrue(spec.soakingTimeMinutes, spec.soakingTimeMinutes.startsWith("1.0 hrs"))
        val cycle = HeatTreatCycleEngine.quenchTemper(mat("4140"), 10.0, spec)!!
        assertEquals(2.0, cycle.stages.first { it.name.contains("Temper soak") }.hoursLow, 1e-9)
        // The cycle tempers at the class temperature, not the catalogue's generic one
        assertTrue(cycle.stages.any { it.detail.contains("500°C – 560°C") })
    }

    @Test fun coldFormedClassHasNoQuenchAndTemperCycle() {
        val spec = ht("10B21", 10.0, PropertyClass.CLASS_5_8)
        assertEquals(null, HeatTreatCycleEngine.quenchTemper(mat("10B21"), 10.0, spec))
    }

    @Test fun spheroidizeSoakKeepsAFifteenDegreeMarginUnderA1() {
        for (k in listOf("10B21", "4135", "4140", "4340")) {
            val ac1 = SteelCompositions.ac1For(mat(k))!!
            val top = Regex("""(\d+)\s*–\s*(\d+)""").find(AnnealCycleEngine.spheroidize(mat(k))!!.soakTempC)!!.groupValues[2].toDouble()
            assertTrue("$k soak top $top vs A1 $ac1", top <= ac1 - 14.5)
        }
        assertFalse(SoftAnnealEngine.build(mat("4140")).coolingRate.contains("transformation range"))
    }
}
