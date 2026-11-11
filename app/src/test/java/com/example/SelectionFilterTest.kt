package com.example

import com.example.data.*
import org.junit.Assert.*
import org.junit.Test

/** The Sizing selectors list every option, so the release gate is what must catch a wrong combination. */
class SelectionFilterTest {
    private val cat = FastenerCatalog.MATERIALS

    private fun gate(
        type: FastenerType = FastenerType.HEX_BOLT, unit: UnitSystem = UnitSystem.METRIC,
        std: ProductStandard = PokaYokeEngine.defaultStandard(type, unit), d: Double = 10.0, length: Double = 60.0,
        pc: PropertyClass? = PropertyClass.CLASS_10_9, material: String = "10B21", coating: CoatingType = CoatingType.GEOMET,
        threadClass: String = SelectionFilter.threadClasses(unit, type, std).first()
    ): List<PokaYokeIssue> {
        val idx = FastenerCatalog.sizeIndexFor(unit, d)
        val pitch = if (unit == UnitSystem.METRIC) FastenerCatalog.METRIC_SIZES[idx].pitches.first() else FastenerCatalog.INCH_SIZES[idx].tpis.first().toDouble()
        return PokaYokeEngine.check(type, unit, std, idx, pitch, FastenerCatalog.diameterMm(unit, idx), length, threadClass, pc, cat[material]!!, coating, 100,
            0.0, 0.0, 100.0, false, 50.0, null)
    }
    private fun List<PokaYokeIssue>.critical() = filter { it.severity == PokaYokeSeverity.CRITICAL }.map { it.title }
    private fun List<PokaYokeIssue>.titles() = map { it.title }

    @Test fun everyClassDefaultMaterialSatisfiesThatClass() {
        for (pc in PropertyClass.values()) {
            val unit = if (pc.name.startsWith("SAE") || pc.name.startsWith("ASTM")) UnitSystem.INCH else UnitSystem.METRIC
            val type = if (pc.name.startsWith("AERO") || pc == PropertyClass.MATERIAL_SPEC) FastenerType.NAS_CLOSE_TOL_BOLT else FastenerType.HEX_BOLT
            val issues = gate(type = type, unit = unit, d = if (unit == UnitSystem.METRIC) 10.0 else 9.525, pc = pc, material = pc.defaultMatKey, coating = CoatingType.AERO_IVD)
            assertTrue("$pc default ${pc.defaultMatKey}: ${issues.critical()}", issues.critical().none { it.contains("Material") || it.contains("needs") || it.contains("grade") })
        }
    }

    @Test fun aerospaceRangesAcceptEveryAlloyInTheLabelledRange() {
        for ((pc, alloys) in mapOf(
            PropertyClass.AERO_AL to listOf("AL2024", "AL2219", "AL7075"),
            PropertyClass.AERO_1100 to listOf("TI_GR5", "TI_6242"),
            PropertyClass.AERO_1750 to listOf("300M", "MP35N", "H11_H13", "CUSTOM_455")
        )) for (m in alloys)
            assertFalse("$pc $m", "Material cannot reach the class strength" in gate(FastenerType.NAS_CLOSE_TOL_BOLT, UnitSystem.INCH, d = 6.35, pc = pc, material = m, coating = CoatingType.AERO_IVD).critical())
        assertTrue("Material cannot reach the class strength" in gate(FastenerType.NAS_CLOSE_TOL_BOLT, UnitSystem.INCH, d = 6.35, pc = PropertyClass.AERO_AL, material = "AL6061", coating = CoatingType.ANODIZE_II).critical())
    }

    @Test fun inchGradeUsesTheStrengthOfItsSizeBand() {
        // SAE J429 Grade 2 is 74 ksi up to 3/4" and 60 ksi (414 MPa) above it
        val small = gate(unit = UnitSystem.INCH, d = 12.7, pc = PropertyClass.SAE_GRADE_2, material = "1008").critical()
        val large = gate(unit = UnitSystem.INCH, d = 25.4, pc = PropertyClass.SAE_GRADE_2, material = "1008").critical()
        assertTrue("Material cannot reach the class strength" in small)
        assertFalse("Material cannot reach the class strength" in large)
    }

    @Test fun wrongMaterialForTheClassIsBlocked() {
        assertTrue(gate(pc = PropertyClass.CLASS_12_9, material = "1008").critical().isNotEmpty())
        assertTrue(gate(pc = PropertyClass.A2_70, material = "SS410", coating = CoatingType.PASSIVATION).critical().isNotEmpty())
        assertTrue(gate(pc = PropertyClass.CLASS_9_8, d = 20.0).critical().contains("Class 9.8 is limited to M16"))
    }

    @Test fun phosphateAndBlackOxideNeedTheRightSubstrate() {
        for ((m, pc) in listOf("SS304" to PropertyClass.A2_70, "TI_GR5" to PropertyClass.AERO_1100, "AL7075" to PropertyClass.AERO_AL, "INCONEL718" to PropertyClass.AERO_1380))
            for (c in listOf(CoatingType.PHOSPHATE, CoatingType.PHOSPHATE_MN))
                assertTrue("$m $c", "Phosphating needs carbon or alloy steel" in gate(pc = pc, material = m, coating = c).critical())
        assertTrue("Black oxide needs a ferrous or copper alloy" in gate(pc = PropertyClass.AERO_AL, material = "AL7075", coating = CoatingType.BLACK_OXIDE).critical())
        assertTrue("Black oxide needs a ferrous or copper alloy" in gate(pc = PropertyClass.AERO_1100, material = "TI_GR5", coating = CoatingType.BLACK_OXIDE).critical())
        assertTrue(gate(coating = CoatingType.PHOSPHATE).critical().isEmpty())
        assertTrue(gate(coating = CoatingType.BLACK_OXIDE).critical().isEmpty())
    }

    @Test fun wrongStandardForThePartIsBlocked() {
        assertTrue(gate(std = ProductStandard.ISO_4032).critical().contains("Standard does not fit a bolt/screw"))
        assertTrue(gate(type = FastenerType.HEX_NUT, std = ProductStandard.ISO_4014, threadClass = "6H").critical().contains("Standard does not fit a nut"))
        assertTrue(gate(unit = UnitSystem.INCH, d = 9.525, std = ProductStandard.ISO_4014, pc = PropertyClass.SAE_GRADE_8, material = "4140").critical().contains("Metric standard with inch thread"))
        // ASTM A490 on a metric thread is allowed, with a sign-off to confirm the metric edition (A490M)
        val a490 = gate(type = FastenerType.HEAVY_HEX_BOLT, std = ProductStandard.ASTM_A490, d = 20.0, length = 100.0)
        assertTrue(a490.critical().isEmpty())
        assertTrue("Inch standard with metric thread" in a490.titles())
    }

    @Test fun lengthRulesAreFlaggedByTheGate() {
        assertTrue("Very short length for diameter" in gate(d = 64.0, length = 6.0).titles())
        assertTrue("Too short for a partially threaded bolt" in gate(d = 10.0, length = 30.0).titles())
        assertTrue("Too short for a partially threaded bolt" in gate(unit = UnitSystem.INCH, d = 9.525, length = 25.4, pc = PropertyClass.SAE_GRADE_5).titles())
        assertFalse("Too short for a partially threaded bolt" in gate(d = 10.0, length = 60.0).titles())
        // rivets are sized by grip, set screws may be shorter than 2d
        assertFalse("Very short length for diameter" in gate(FastenerType.SOLID_RIVET, UnitSystem.INCH, d = 4.826, length = 6.35, pc = PropertyClass.AERO_AL, material = "AL2117", coating = CoatingType.ANODIZE_II).titles())
        assertFalse("Very short length for diameter" in gate(FastenerType.SOCKET_SET, length = 10.0).titles())
    }

    @Test fun standardsOwnThreadClassIsTheDefault() {
        assertEquals("3A", SelectionFilter.threadClasses(UnitSystem.INCH, FastenerType.SHCS, ProductStandard.ASME_B18_3).first())
        assertEquals("2A", SelectionFilter.threadClasses(UnitSystem.INCH, FastenerType.HEX_BOLT, ProductStandard.ASME_B18_2_1).first())
        assertEquals("6g", SelectionFilter.threadClasses(UnitSystem.METRIC, FastenerType.HEX_BOLT, ProductStandard.ISO_4014).first())
        assertEquals("6H", SelectionFilter.threadClasses(UnitSystem.METRIC, FastenerType.HEX_NUT, ProductStandard.ISO_4032).first())
    }

    @Test fun aerospaceStandardsLeadWithTheUnjOrMjThread() {
        assertEquals("3A-UNJ", SelectionFilter.threadClasses(UnitSystem.INCH, FastenerType.NAS_CLOSE_TOL_BOLT, ProductStandard.NAS6203).first())
        assertEquals("3B-UNJ", SelectionFilter.threadClasses(UnitSystem.INCH, FastenerType.AERO_LOCK_NUT, ProductStandard.MS21042).first())
        assertTrue(SelectionFilter.threadClasses(UnitSystem.METRIC, FastenerType.NAS_CLOSE_TOL_BOLT, ProductStandard.EN2882).first().endsWith("-MJ"))
        assertEquals("6g", SelectionFilter.threadClasses(UnitSystem.METRIC, FastenerType.THREAD_FORMING_SCREW, ProductStandard.DIN_7500).first())
    }

    @Test fun metricAerospaceBoltsDefaultToTheMetricStandard() {
        assertEquals(ProductStandard.EN2882, PokaYokeEngine.defaultStandard(FastenerType.TWELVE_POINT, UnitSystem.METRIC))
        assertEquals(ProductStandard.EN2882, PokaYokeEngine.defaultStandard(FastenerType.NAS_CLOSE_TOL_BOLT, UnitSystem.METRIC))
        assertEquals(ProductStandard.NAS624, PokaYokeEngine.defaultStandard(FastenerType.TWELVE_POINT, UnitSystem.INCH))
        assertEquals(ProductStandard.NAS6203, PokaYokeEngine.defaultStandard(FastenerType.NAS_CLOSE_TOL_BOLT, UnitSystem.INCH))
    }

    @Test fun lengthCataloguesAreTheStandardSeries() {
        assertEquals(2.0, FastenerCatalog.METRIC_LENGTHS.first(), 0.0)
        assertEquals(500.0, FastenerCatalog.METRIC_LENGTHS.last(), 0.0)
        assertFalse(75.0 in FastenerCatalog.METRIC_LENGTHS)
        assertEquals(3.175, FastenerCatalog.INCH_LENGTHS.first(), 1e-9)
        assertEquals(203.2, FastenerCatalog.INCH_LENGTHS.last(), 1e-9)
    }
}
