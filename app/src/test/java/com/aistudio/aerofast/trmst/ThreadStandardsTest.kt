package com.aistudio.aerofast.trmst

import com.aistudio.aerofast.trmst.data.CalculationEngine
import com.aistudio.aerofast.trmst.data.CoatingType
import com.aistudio.aerofast.trmst.data.FastenerCatalog
import com.aistudio.aerofast.trmst.data.FastenerType
import com.aistudio.aerofast.trmst.data.IsoThreadClass
import com.aistudio.aerofast.trmst.data.PokaYokeEngine
import com.aistudio.aerofast.trmst.data.PokaYokeSeverity
import com.aistudio.aerofast.trmst.data.ProductStandard
import com.aistudio.aerofast.trmst.data.PropertyClass
import com.aistudio.aerofast.trmst.data.ThreadClassCatalog
import com.aistudio.aerofast.trmst.data.UnitSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreadStandardsTest {
    private fun metric(d: Double) = FastenerCatalog.METRIC_SIZES.indexOfFirst { it.d == d }

    private fun calc(cls: String, nut: Boolean = false, d: Double = 10.0, p: Double = 1.5,
                     pc: PropertyClass? = PropertyClass.CLASS_10_9, coating: CoatingType = CoatingType.GEOMET,
                     mat: String = "10B21") = CalculationEngine.calculate(
        fastenerType = if (nut) FastenerType.HEX_NUT else FastenerType.HEX_BOLT, unitSystem = UnitSystem.METRIC,
        standard = if (nut) ProductStandard.ISO_4032 else ProductStandard.ISO_4014, nominalSizeIndex = metric(d),
        pitchValue = p, lengthMm = 60.0, threadClass = cls, material = FastenerCatalog.MATERIALS.getValue(mat),
        coating = coating, batchQty = 1000, propertyClass = pc
    )

    @Test fun parser_readsPitchAndCrestTolerances() {
        assertEquals(IsoThreadClass(4, 6, "g", false), IsoThreadClass.parse("4g6g"))
        assertEquals(IsoThreadClass(6, 6, "H", false), IsoThreadClass.parse("6H"))
        assertEquals(IsoThreadClass(4, 5, "H", true), IsoThreadClass.parse("4H5H-MJ"))
        assertEquals("AZ", IsoThreadClass.parse("6AZ").position)
    }

    @Test fun m10x1_5_6g_matchesIso965Table() {
        val r = calc("6g")
        assertEquals(8.994, r.pitchDiaMax, 0.001)      // ISO 965-2: d2 max 8.994
        assertEquals(9.968, r.majorDiaMax, 0.001)      // es = −32 µm
    }

    @Test fun deviationsOrderByPosition_andGradesScaleTolerance() {
        val es = listOf("6d", "6e", "6f", "6g", "6h").map { calc(it).fundamentalDeviationMm }
        assertEquals(es.sorted(), es)
        val td6 = calc("6g").let { it.pitchDiaMax - it.pitchDiaMin }
        val td9 = calc("9g8g").let { it.pitchDiaMax - it.pitchDiaMin }
        assertEquals(2.0, td9 / td6, 0.01)
    }

    @Test fun everyCatalogueClass_computesAValidThread() {
        (ThreadClassCatalog.METRIC_EXTERNAL.map { it to false } + ThreadClassCatalog.METRIC_INTERNAL.map { it to true })
            .forEach { (c, nut) ->
                val r = calc(c, nut)
                assertTrue("$c d2", r.pitchDiaMin < r.pitchDiaMax)
                assertTrue("$c minor", r.minorDiaMin < r.minorDiaMax)
                assertTrue("$c finite", r.blankTargetDia.isFinite() && r.blankTargetDia > 0)
            }
    }

    @Test fun galvanizedNutClasses_useIso9655Deviation() {
        assertEquals(0.330, calc("6AZ", nut = true).fundamentalDeviationMm, 1e-6)
        assertEquals(0.310, calc("6AX", nut = true).fundamentalDeviationMm, 1e-6)
    }

    @Test fun mjThreads_haveTheControlledRoot() {
        val r = calc("4h6h-MJ")
        assertEquals(0.15011 * 1.5, r.rootRadiusMinMm!!, 1e-4)
    }

    @Test fun sizes_areOrdered_andIncludeIso261AndAsmeB11() {
        val m = FastenerCatalog.METRIC_SIZES.map { it.d }
        assertEquals(m.sorted(), m)
        listOf(1.2, 1.4, 3.5, 18.0, 22.0, 27.0, 33.0, 39.0, 42.0, 45.0, 48.0, 52.0, 56.0, 60.0, 64.0).forEach { assertTrue("M$it", it in m) }
        FastenerCatalog.METRIC_SIZES.forEach { assertEquals("${it.label} coarse first", it.pitches.max(), it.pitches.first(), 0.0) }
        val i = FastenerCatalog.INCH_SIZES.map { it.dInch }
        assertEquals(i.sorted(), i)
        assertTrue(!FastenerCatalog.INCH_SIZES.first().hasUnc)
        FastenerCatalog.INCH_SIZES.forEach { s -> s.tpis.forEach { assertTrue("${s.label} TPI must be >= 5 for the pitch conversion", it >= 5) } }
    }

    @Test fun defaultsAndLegacySavesStillPointAtTheSameSize() {
        assertEquals(10.0, FastenerCatalog.diameterMm(UnitSystem.METRIC, FastenerCatalog.defaultSizeIndex(UnitSystem.METRIC)), 0.0)
        assertEquals(0.375 * 25.4, FastenerCatalog.diameterMm(UnitSystem.INCH, FastenerCatalog.defaultSizeIndex(UnitSystem.INCH)), 1e-9)
        // old list: index 11 = M10, 18 = M36; inch index 8 = 3/8"
        assertEquals(10.0, FastenerCatalog.diameterMm(UnitSystem.METRIC, FastenerCatalog.migrateLegacySizeIndex(UnitSystem.METRIC, 11)), 0.0)
        assertEquals(36.0, FastenerCatalog.diameterMm(UnitSystem.METRIC, FastenerCatalog.migrateLegacySizeIndex(UnitSystem.METRIC, 18)), 0.0)
        assertEquals(9.525, FastenerCatalog.diameterMm(UnitSystem.INCH, FastenerCatalog.migrateLegacySizeIndex(UnitSystem.INCH, 8)), 1e-9)
    }

    @Test fun everyPropertyClass_hasStrengthAndANutMatch() {
        val mat = FastenerCatalog.MATERIALS.getValue("4140")
        PropertyClass.values().forEach { pc ->
            val cs = PokaYokeEngine.classStrength(pc, 10.0, mat)
            assertTrue("$pc", cs.rmMinMpa > 0 && cs.yieldMinMpa <= cs.rmMinMpa && cs.proofStressMpa <= cs.rmMinMpa)
            assertTrue("$pc nut", PokaYokeEngine.nutClassFor(pc).isNotBlank())
        }
        assertEquals(724.0, PokaYokeEngine.classStrength(PropertyClass.SAE_GRADE_5, 30.0, mat).rmMinMpa, 0.0)
        assertEquals("ASTM A194 Grade 2H", PokaYokeEngine.nutClassFor(PropertyClass.ASTM_A193_B7))
    }

    @Test fun class9_8_isBlockedAboveM16() {
        assertTrue(calc("6g", d = 20.0, p = 2.5, pc = PropertyClass.CLASS_9_8).pokaYokeIssues
            .any { it.severity == PokaYokeSeverity.CRITICAL && it.title == "Class 9.8 is limited to M16" })
    }

    @Test fun anodizeOnSteel_isBlocked_onAluminium_isNot() {
        fun blocked(mat: String) = calc("6g", coating = CoatingType.ANODIZE_II, pc = null, mat = mat).pokaYokeIssues
            .any { it.title.contains("is for aluminium only") }
        assertTrue(blocked("10B21"))
        assertTrue(!blocked("AL7075"))
    }
}
