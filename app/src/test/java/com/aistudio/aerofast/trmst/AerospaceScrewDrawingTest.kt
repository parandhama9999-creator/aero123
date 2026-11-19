package com.aistudio.aerofast.trmst

import com.aistudio.aerofast.trmst.data.*
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sqrt

/**
 * A real aerospace drawing: #8-32 UNJC-3A hexalobular pan head screw, AISI 4340, Rm 1000–1240 MPa,
 * cadmium QQ-P-416 Type II Class 2, nylon patch, QTY 13000 (head Ø8.17, edge 1.12, crown R6.20, L 6.35).
 */
class AerospaceScrewDrawingTest {

    private val json = """{"nominalDiameterMm":4.166,"pitch":0.79375,"lengthMm":6.35,"fastenerType":"PAN_HEXALOBULAR","unitSystem":"INCH",
          "materialKey":"AISI 4340","propertyClass":"","threadClass":"3A-UNJ","coating":"Cadmium plate AMS-QQ-P-416 Type II Class 2",
          "headDiaMm":8.17,"headHeightMm":2.92,"socketSizeMm":3.94,"headDomeRadiusMm":6.20,"headEdgeHeightMm":1.12,"radius":0.25,"quantity":13000,
          "detectedDimensions":[{"feature":"Note 1","nominal":"TENSILE STRENGTH = 1000-1240MPa","tolerance":""},{"feature":"Thread patch","nominal":"3-5 threads","tolerance":""}],
          "summary":"Pan head Torx screw #8-32 UNJC-3A"}"""
    private val spec = GeminiAiService.parseSpecFromJson(JSONObject(json), "test")
    private val r = DrawingScan.calculate(spec)
    private val titles = r.pokaYokeIssues.map { it.title }

    @Test fun scannerReadsEveryRequirement() {
        assertEquals(FastenerType.PAN_HEXALOBULAR, spec.fastenerType)
        assertEquals("4340", spec.materialKey)
        assertEquals(CoatingType.AERO_CAD_CL2, spec.coating)
        assertEquals(TensileRange(1000.0, 1240.0), spec.tensileRange)
        assertTrue(spec.threadLockPatch)
        assertEquals(13000, spec.batchQty)
        assertTrue(spec.assumed.toString(), spec.assumed.isEmpty())
    }

    @Test fun domedHeadAndRecessGiveTheRealWeight() {
        val a = 8.17 / 2; val cap = 6.20 - sqrt(6.20 * 6.20 - a * a)
        val head = PI * a * a * 1.12 + PI * cap * cap * (3 * 6.20 - cap) / 3
        val part = r.finishedPart!!
        assertEquals(head, part.items.filter { it.feature.startsWith("Pan head") }.sumOf { it.volumeMm3 }, 1e-6)
        assertTrue(part.items.any { it.feature == "Hexalobular recess" && it.volumeMm3 < 0 })
        assertEquals(1.23, r.netWeightGrams, 0.03)                 // not 1.72 g as a solid cylinder
        assertTrue(part.cautions.any { "head height 2.66" in it })  // R6.20 + edge 1.12 cannot make k 2.92
    }

    @Test fun fullThreadScrewIsHeadedFromBlankSizeWire() {
        assertEquals(kotlin.math.ceil(r.blankTargetDia * 100 - 1e-6) / 100, r.wireDiaMm, 1e-9)
        assertEquals(15.1, r.cutOffLengthMm, 0.2)
        val plan = r.headingPlan!!
        // No extrusion and a 2.4 L/D head: one die, cone blow + finishing blow
        assertEquals(HeaderRoute.TWO_BLOW, plan.route)
        assertEquals("1st blow: cone preform", plan.stations[1].title)
        assertEquals(3, plan.stations.size)
        assertTrue(plan.stations.last().operation.contains("hexalobular recess"))
        assertEquals("#8-32 3A-UNJ", r.threadLabel)
        assertFalse(r.recommendedMachine.contains("warm"))          // 4340 at #8 is cold headed
    }

    @Test fun tensileRangeDrivesLoadsAndHardness() {
        assertEquals(1000.0, r.postForgingTargetUtsMpa, 1e-9)
        assertEquals(r.stressAreaAsMm2 * 1000.0 / 1000.0, r.tensileBreakingLoadKn, 0.1)
        assertTrue(r.heatTreatmentSpec.targetHardnessHrc, r.heatTreatmentSpec.targetHardnessHrc.contains("aim 33–36 HRC"))
        assertTrue(r.heatTreatmentSpec.temperingOrAgingTempC, r.heatTreatmentSpec.temperingOrAgingTempC.startsWith("Trial temper 5"))
    }

    @Test fun pokaYokeGivesTheRightControlsAndNoFalseAlarms() {
        assertTrue(titles.toString(), r.criticalIssues.isEmpty())
        listOf("High-strength steel cadmium plated: hydrogen controls", "Bake before the chromate / passivate",
            "Locking patch after the bake", "Gauge the thread before the patch", "Control coating thickness on the thread")
            .forEach { assertTrue("$it missing from $titles", it in titles) }
        listOf("Electroplating ≥ 1200 MPa fasteners", "No dimension table for this type", "Very short length for diameter", "Aerospace alloy on a commercial class")
            .forEach { assertFalse("$it should not fire", it in titles) }
        assertTrue(titles.none { it.startsWith("Thread class not per") })
        val coat = r.pokaYokeIssues.first { it.title == "Control coating thickness on the thread" }
        assertTrue(coat.fix, coat.fix.contains("8–10 µm"))
    }

    @Test fun reportListsTheProcessInOrder() {
        val seq = ProductionReport.processSequence(r)
        val plate = seq.indexOfFirst { it.startsWith("Plate") }
        val bake = seq.indexOfFirst { it.startsWith("Hydrogen") }
        val chromate = seq.indexOfFirst { it.startsWith("Chromate") }
        val patch = seq.indexOfFirst { it.contains("locking patch") }
        assertTrue(seq.toString(), plate in 0 until bake && bake < chromate && chromate < patch)
        val text = ProductionReport.build(r, spec)
        assertTrue("Rm 1000–1240 MPa (drawing)" in text && "13000 pcs" in text)
    }

    @Test fun conversionsAndParsing() {
        assertEquals(32.0, HardnessConversion.hrcFor(1040.0), 1e-9)
        assertEquals(39.0, HardnessConversion.hrcFor(1220.0), 1e-9)
        assertEquals(TensileRange(1000.0, 1240.0), TensileRange.parse("TENSILE STRENGTH = 1000-1240MPa"))
        assertEquals(160 * 6.894757, TensileRange.parse("160-180 KSI")!!.minMpa, 1e-6)
        assertNull(TensileRange.parse("HEAD Ø8.17"))
        assertEquals(13.0, CoatingType.AERO_CAD.thicknessMinUm, 0.0)
        assertEquals(8.0, CoatingType.AERO_CAD_CL2.thicknessMinUm, 0.0)
        assertEquals(5.0, CoatingType.AERO_CAD_CL3.thicknessMinUm, 0.0)
    }

    @Test fun warmHeadingOnlyWhereItBelongs() {
        val m = FastenerCatalog.MATERIALS
        assertFalse(CalculationEngine.isWarmHeaded(m.getValue("4340"), 4.2))
        assertTrue(CalculationEngine.isWarmHeaded(m.getValue("4340"), 20.0))
        assertTrue(CalculationEngine.isWarmHeaded(m.getValue("TI_GR5"), 6.0))
        assertFalse(CalculationEngine.isWarmHeaded(m.getValue("10B21"), 20.0))
    }

    @Test fun commercialHighStrengthZincIsStillBlocked() {
        val idx = FastenerCatalog.sizeIndexFor(UnitSystem.METRIC, 10.0)
        val c = CalculationEngine.calculate(FastenerType.HEX_BOLT, UnitSystem.METRIC, ProductStandard.ISO_4014, idx, 1.5, 60.0, "6g",
            m("4140"), CoatingType.ZINC_BLUE, 100, propertyClass = PropertyClass.CLASS_12_9)
        assertTrue(c.criticalIssues.any { it.title == "Electroplating ≥ 1200 MPa fasteners" })
    }

    private fun m(k: String) = FastenerCatalog.MATERIALS.getValue(k)

    @Test fun fullCoatingOnA3AThreadAsksForThicknessControlNotRejection() {
        // Zn-Ni 8–15 µm on #8-32 3A-UNJ: at 15 µm no band is left, but 8–10 µm on the thread is holdable
        val idx = FastenerCatalog.INCH_SIZES.indexOfFirst { it.dInch == 0.1640 }
        val r = CalculationEngine.calculate(FastenerType.PAN_HEXALOBULAR, UnitSystem.INCH, ProductStandard.ASME_B18_6_3, idx, 32.0, 6.35, "3A-UNJ",
            m("4340"), CoatingType.ZN_NI, 100, propertyClass = PropertyClass.CLASS_10_9)
        assertTrue(r.pokaYokeIssues.none { it.title == "Coating consumes the whole thread tolerance" })
        assertTrue(r.pokaYokeIssues.any { it.title == "Control coating thickness on the thread" && it.fix.contains("8–10 µm") })
    }
}
