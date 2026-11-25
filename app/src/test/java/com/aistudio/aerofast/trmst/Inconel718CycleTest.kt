package com.aistudio.aerofast.trmst

import com.aistudio.aerofast.trmst.data.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Inconel 718 against Special Metals / AMS 5662 / AMS 5663 / ASTM A1014. */
class Inconel718CycleTest {
    private val in718 = FastenerCatalog.MATERIALS["INCONEL718"]!!

    private fun calc(): FastenerCalculationResult {
        val m6 = FastenerCatalog.METRIC_SIZES.indexOfFirst { it.d == 6.0 }
        return CalculationEngine.calculate(fastenerType = FastenerType.TWELVE_POINT, unitSystem = UnitSystem.METRIC,
            standard = PokaYokeEngine.defaultStandard(FastenerType.TWELVE_POINT, UnitSystem.METRIC), nominalSizeIndex = m6,
            pitchValue = 1.0, lengthMm = 30.0, threadClass = "6g", material = in718, coating = CoatingType.AERO_IVD,
            batchQty = 1000, propertyClass = PropertyClass.AERO_1380)
    }

    @Test fun ageing_isAms5663_18hTotal() {
        val c = HeatTreatCycleEngine.precipitationHarden(in718, 6.0)!!
        val age = c.stages.first { it.name.startsWith("4.") }
        val fc = c.stages.first { it.name.startsWith("5.") }
        val hold = c.stages.first { it.name.startsWith("6.") }
        assertTrue(age.name.contains("718 °C"))
        assertEquals(97.0 / 55.0, fc.hoursLow, 1e-9)
        assertEquals(18.0, 8.0 + fc.hoursLow + hold.hoursLow, 1e-9)
        assertTrue(c.stages.first { it.name.startsWith("3.") }.detail.contains("Water"))
        assertNull(HeatTreatCycleEngine.precipitationHarden(FastenerCatalog.MATERIALS["WASPALOY"]!!, 6.0))
    }

    @Test fun anneal_cycleShown_withRapidCool() {
        val a = HeatTreatCycleEngine.softAnneal(in718, 6.0)
        assertNotNull(a)
        val cool = a!!.stages.last().detail
        assertTrue(cool, cool.contains("water", ignoreCase = true) && cool.contains("Never furnace cool"))
        assertTrue(a.stages.first().detail.contains("927 – 1010 °C"))
    }

    @Test fun spec_citesTheRightStandards_andNoPreheat() {
        val ht = calc().heatTreatmentSpec
        assertTrue(ht.standardReference, ht.standardReference.contains("AMS 5663") && ht.standardReference.contains("AMS 2774"))
        assertFalse(ht.standardReference, ht.standardReference.contains("5664") || ht.standardReference.contains("2759"))
        assertTrue(ht.preheatTempC.isBlank())
        assertTrue(ht.temperingOrAgingTempC.contains("621 °C") && ht.temperingOrAgingTempC.contains("18 h"))
        assertTrue(ht.targetHardnessHrc.startsWith("36 – 43 HRC"))
        assertFalse(in718.heatTreatmentSpec.standardReference.contains("2759"))
    }

    @Test fun forgingAdvice_andSequence() {
        val r = calc()
        assertFalse(r.forgingRecommendations.any { it.contains("650°C–720°C") })
        assertFalse(r.forgingRisks.any { it.contains("catastrophic") })
        assertTrue(r.forgingRecommendations.any { it.contains("RAPID cool") })
        val seq = ProductionReport.processSequence(r)
        assertTrue(seq.any { it.contains("AFTER heat treatment") })
        assertTrue(seq.any { it.contains("; age Age 718") || it.contains("; age ") })
        assertFalse(seq.any { it.contains("decarburisation") })
        assertFalse(QualityDocs.controlPlan(r).any { it.characteristic.contains("decarburisation") })
    }
}
