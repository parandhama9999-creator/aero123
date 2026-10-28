package com.example

import com.example.data.CalculationEngine
import com.example.data.CoatingType
import com.example.data.FastenerCalculationResult
import com.example.data.FastenerCatalog
import com.example.data.FastenerType
import com.example.data.PokaYokeSeverity
import com.example.data.ProductStandard
import com.example.data.PropertyClass
import com.example.data.UnitSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Checks the engine against published ISO 965-1, ISO 898-1 and ASME B1.1 table values. */
class FastenerEngineeringTest {

    private val m10Index = FastenerCatalog.METRIC_SIZES.indexOfFirst { it.d == 10.0 }

    private fun calc(
        type: FastenerType = FastenerType.HEX_BOLT,
        unit: UnitSystem = UnitSystem.METRIC,
        standard: ProductStandard = ProductStandard.ISO_4014,
        sizeIndex: Int = m10Index,
        pitch: Double = 1.5,
        length: Double = 60.0,
        threadClass: String = "6g",
        material: String = "10B21",
        coating: CoatingType = CoatingType.GEOMET,
        pc: PropertyClass? = PropertyClass.CLASS_10_9
    ): FastenerCalculationResult = CalculationEngine.calculate(
        fastenerType = type, unitSystem = unit, standard = standard, nominalSizeIndex = sizeIndex,
        pitchValue = pitch, lengthMm = length, threadClass = threadClass,
        material = FastenerCatalog.MATERIALS[material]!!, coating = coating, batchQty = 1000,
        propertyClass = pc
    )

    private fun FastenerCalculationResult.criticalTitles() =
        pokaYokeIssues.filter { it.severity == PokaYokeSeverity.CRITICAL }.map { it.title }

    // ---------- ISO 965-1: M10×1.5-6g external ----------
    @Test fun m10_6g_externalLimits_matchIso965() {
        val r = calc()
        assertEquals(9.968, r.majorDiaMax, 0.002)
        assertEquals(9.732, r.majorDiaMin, 0.005)
        assertEquals(8.994, r.pitchDiaMax, 0.002)
        assertEquals(8.862, r.pitchDiaMin, 0.005)
        assertEquals(8.128, r.minorDiaMax, 0.003) // d3 max
    }

    // ---------- ISO 965-1: M10×1.5-6H internal ----------
    @Test fun m10_6H_nutLimits_matchIso965() {
        val r = calc(type = FastenerType.HEX_NUT, standard = ProductStandard.ISO_4032, threadClass = "6H")
        assertEquals(9.026, r.pitchDiaMin, 0.002)
        assertEquals(9.206, r.pitchDiaMax, 0.006)
        assertEquals(8.376, r.minorDiaMin, 0.002)
        assertEquals(8.676, r.minorDiaMax, 0.008)
    }

    // ---------- ISO 898-1 Table 5/7: M10 proof & breaking loads ----------
    @Test fun m10_stressArea_andClassLoads_matchIso898() {
        assertEquals(58.0, calc().stressAreaAsMm2, 0.1)
        calc(pc = PropertyClass.CLASS_8_8).let {
            assertEquals(33.7, it.proofLoadKn, 0.15); assertEquals(46.4, it.tensileBreakingLoadKn, 0.15)
        }
        calc(pc = PropertyClass.CLASS_10_9).let {
            assertEquals(48.1, it.proofLoadKn, 0.15); assertEquals(60.3, it.tensileBreakingLoadKn, 0.15)
        }
        calc(material = "4140", pc = PropertyClass.CLASS_12_9).let {
            assertEquals(56.3, it.proofLoadKn, 0.15); assertEquals(70.8, it.tensileBreakingLoadKn, 0.15)
        }
    }

    // ---------- ASME B1.1: 3/8-16 UNC-2A ----------
    @Test fun threeEighths16_2A_matchesAsmeB11() {
        val idx = FastenerCatalog.INCH_SIZES.indexOfFirst { kotlin.math.abs(it.dInch - 0.375) < 1e-6 }
        val r = calc(unit = UnitSystem.INCH, standard = ProductStandard.ASME_B18_2_1, sizeIndex = idx, pitch = 16.0,
            length = 50.8, threadClass = "2A", pc = PropertyClass.CLASS_8_8)
        assertEquals(0.3331 * 25.4, r.pitchDiaMax, 0.004)
        assertEquals(0.2970 * 25.4, r.minorDiaMax, 0.004)
        assertEquals(0.0775 * 645.16, r.stressAreaAsMm2, 0.3) // As = 0.0775 in²
    }

    // ---------- Thread length b (ISO 4014 vs full-thread ISO 4017) ----------
    @Test fun threadLength_followsProductStandard() {
        assertEquals(26.0, calc(length = 60.0).threadLengthMm, 0.01)
        assertEquals(60.0, calc(type = FastenerType.HEX_BOLT_FULL, standard = ProductStandard.ISO_4017).threadLengthMm, 0.01)
    }

    // ---------- Poka-yoke rules ----------
    @Test fun validSpec_hasNoCriticalIssue() {
        val r = calc(coating = CoatingType.GEOMET)
        assertTrue("unexpected: ${r.criticalTitles()}", r.criticalTitles().isEmpty())
        assertFalse(r.isReleaseBlocked)
    }

    @Test fun stainlessWith10_9_isBlocked() {
        assertTrue(calc(material = "SS304", coating = CoatingType.PASSIVATION).isReleaseBlocked)
    }

    @Test fun boronSteelWith12_9_isBlocked() {
        assertTrue(calc(pc = PropertyClass.CLASS_12_9).isReleaseBlocked)
    }

    @Test fun electroplated12_9_isBlocked() {
        val r = calc(material = "4140", pc = PropertyClass.CLASS_12_9, coating = CoatingType.ZN_NI)
        assertTrue(r.criticalTitles().any { it.contains("Electroplating") })
    }

    @Test fun electroplated10_9_requiresBake_butNot8_8() {
        val r109 = calc(coating = CoatingType.ZINC_BLUE)
        assertTrue(r109.hasHydrogenEmbrittlementRisk)
        assertTrue(r109.pokaYokeIssues.any { it.severity == PokaYokeSeverity.MANDATORY && it.title.contains("bake") })
        assertFalse(calc(coating = CoatingType.ZINC_BLUE, pc = PropertyClass.CLASS_8_8).hasHydrogenEmbrittlementRisk)
    }

    @Test fun passivationOnCarbonSteel_isBlocked() {
        assertTrue(calc(coating = CoatingType.PASSIVATION).criticalTitles().any { it.contains("Passivation") })
    }

    @Test fun nutWithBoltStandard_isBlocked() {
        val r = calc(type = FastenerType.HEX_NUT, standard = ProductStandard.ISO_4014, threadClass = "6H")
        assertTrue(r.criticalTitles().any { it.contains("nut") })
    }

    @Test fun nonStandardPitch_isBlocked() {
        assertTrue(calc(pitch = 2.0).criticalTitles().any { it.contains("pitch") })
    }

    @Test fun externalClassOnNut_isBlocked() {
        val r = calc(type = FastenerType.HEX_NUT, standard = ProductStandard.ISO_4032, threadClass = "6g")
        assertTrue(r.criticalTitles().any { it.contains("tolerance class") })
    }

    @Test fun hotDipGalvanizedOn6g_isBlocked() {
        val r = calc(length = 80.0, coating = CoatingType.HOT_DIP_GALV, pc = PropertyClass.CLASS_8_8)
        assertTrue(r.criticalTitles().any { it.contains("HDG") })
    }

    @Test fun thickPlatingOn6g_requiresUndersizePrePlate() {
        val r = calc(coating = CoatingType.ZN_NI) // 15 µm max → 60 µm growth vs 32 µm allowance
        assertTrue(r.pokaYokeIssues.any { it.title.contains("undersize") || it.title.contains("whole thread tolerance") })
    }
}
