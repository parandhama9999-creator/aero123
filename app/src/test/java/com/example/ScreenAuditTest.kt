package com.example

import com.example.data.CalculationEngine
import com.example.data.CoatingType
import com.example.data.CustomMaterialInput
import com.example.data.CustomMaterialStore
import com.example.data.FastenerCalculationResult
import com.example.data.FastenerCatalog
import com.example.data.FastenerType
import com.example.data.ProductStandard
import com.example.data.PropertyClass
import com.example.data.SpcStats
import com.example.data.UnitSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Checks the fatigue, nut G-code, SPC, heat-treatment and custom-material fixes. */
class ScreenAuditTest {

    private val m10 = FastenerCatalog.METRIC_SIZES.indexOfFirst { it.d == 10.0 }

    private fun calc(
        type: FastenerType = FastenerType.HEX_BOLT,
        standard: ProductStandard = ProductStandard.ISO_4014,
        threadClass: String = "6g",
        material: String = "10B21",
        pc: PropertyClass? = PropertyClass.CLASS_10_9
    ): FastenerCalculationResult = CalculationEngine.calculate(
        fastenerType = type, unitSystem = UnitSystem.METRIC, standard = standard, nominalSizeIndex = m10,
        pitchValue = 1.5, lengthMm = 60.0, threadClass = threadClass,
        material = FastenerCatalog.MATERIALS[material]!!, coating = CoatingType.GEOMET, batchQty = 1000,
        propertyClass = pc
    )

    // ---------- Bolt fatigue (VDI 2230: σASV = 0.85·(150/d + 45)) ----------
    @Test fun fatigue_isThreadEnduranceNotHalfUts() {
        val r = calc()
        assertEquals(0.85 * (150.0 / 10.0 + 45.0), r.fatigueEnduranceLimitSeMpa, 0.2) // ≈ 51 MPa, not ~450
        assertEquals(51.0 * 58.0 / 1.2 / 1000.0, r.allowableAlternatingForceKn, 0.1)
    }

    // ---------- G-code ----------
    @Test fun nutProgram_boresNutHeightNotBoltLength() {
        val code = calc(type = FastenerType.HEX_NUT, standard = ProductStandard.ISO_4032, threadClass = "6H").fanucGCode
        assertTrue(code.contains("Z-8.9")) // 0.84·10 + 0.5
        assertFalse(code.contains("Z-60."))
    }

    // ---------- SPC ----------
    @Test fun spc_computesFromReadings() {
        val readings = SpcStats.parse("8.93, 8.94 8.92;8.93 8.94")
        val s = SpcStats.compute(readings, 8.862, 8.994)!!
        assertEquals(5, s.n)
        assertEquals(8.932, s.mean, 1e-6)
        assertEquals(0.008367, s.sigma, 1e-5)
        assertEquals((8.994 - 8.862) / (6 * s.sigma), s.cp, 1e-6)
        assertTrue(s.cpk < s.cp)
    }

    // ---------- Heat treatment ----------
    @Test fun aluminium_neverGetsSteelQuenchCycle() {
        val ht = calc(material = "AL7075", pc = PropertyClass.AERO_AL).heatTreatmentSpec
        assertFalse(ht.austenitizingOrSolutionTempC.contains("8"+"60"))
        assertTrue(ht.austenitizingOrSolutionTempC.contains("470"))
    }

    @Test fun tempering_respectsIso898Minimum_andClassHardness() {
        val ht109 = calc().heatTreatmentSpec
        assertTrue(ht109.temperingOrAgingTempC.startsWith("425"))
        assertTrue(ht109.targetHardnessHrc.startsWith("32–39"))
        assertTrue(calc(pc = PropertyClass.CLASS_8_8).heatTreatmentSpec.targetHardnessHrc.startsWith("22–32"))
        assertTrue(calc(material = "1008", pc = PropertyClass.CLASS_4_8).heatTreatmentSpec.austenitizingOrSolutionTempC.contains("no hardening"))
        assertTrue(ht109.atmosphere.contains("0.20%–0.25%"))
    }

    @Test fun gradedClasses_stateHardnessInHrcOrHrb() {
        mapOf(
            PropertyClass.CLASS_4_6 to "67–95 HRB", PropertyClass.CLASS_5_6 to "79–95 HRB", PropertyClass.CLASS_5_8 to "82–95 HRB",
            PropertyClass.CLASS_6_8 to "89–99.5 HRB", PropertyClass.CLASS_9_8 to "28–37 HRC", PropertyClass.CLASS_12_9 to "39–44 HRC",
            PropertyClass.SAE_GRADE_5 to "25–34 HRC", PropertyClass.SAE_GRADE_8 to "33–39 HRC", PropertyClass.ASTM_A574 to "39–45 HRC"
        ).forEach { (pc, hardness) ->
            val text = calc(pc = pc).heatTreatmentSpec.targetHardnessHrc
            assertTrue("$pc: $text", text.startsWith(hardness))
        }
    }

    // ---------- Custom materials ----------
    @Test fun customMaterial_validation() {
        val ok = CustomMaterialInput("MY_ALLOY", "My alloy", 1100.0, 950.0, 7.85, 0.02, 100.0, false, "", "AMS 1234")
        assertTrue(CustomMaterialStore.validate(ok).isEmpty())
        assertFalse(CustomMaterialStore.validate(ok.copy(key = "10B21")).isEmpty())   // cannot overwrite built-in
        assertFalse(CustomMaterialStore.validate(ok.copy(yieldMpa = 1200.0)).isEmpty()) // yield above UTS
        assertFalse(CustomMaterialStore.validate(ok.copy(utsMpa = -1.0)).isEmpty())     // unparsable number
        assertFalse(CustomMaterialStore.validate(ok.copy(density = 0.0)).isEmpty())
    }
}
