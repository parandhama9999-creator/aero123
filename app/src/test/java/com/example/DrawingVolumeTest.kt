package com.example

import com.example.data.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.PI

/** One finished shape per fastener type: volume, breakdown, die progression and the scanned-drawing volume. */
class DrawingVolumeTest {
    private val cat = FastenerCatalog.MATERIALS

    private fun calc(type: FastenerType, unit: UnitSystem = UnitSystem.METRIC, d: Double = 10.0, len: Double = 50.0, g: DrawingGeometry = DrawingGeometry()): FastenerCalculationResult {
        val idx = FastenerCatalog.sizeIndexFor(unit, d)
        return CalculationEngine.calculate(
            fastenerType = type, unitSystem = unit, standard = PokaYokeEngine.defaultStandard(type, unit), nominalSizeIndex = idx,
            pitchValue = if (unit == UnitSystem.METRIC) FastenerCatalog.METRIC_SIZES[idx].pitches.first() else FastenerCatalog.INCH_SIZES[idx].tpis.first().toDouble(),
            lengthMm = len, threadClass = SelectionFilter.threadClasses(unit, type, PokaYokeEngine.defaultStandard(type, unit)).first(),
            material = cat["10B21"]!!, coating = CoatingType.GEOMET, batchQty = 100, propertyClass = PropertyClass.CLASS_10_9, drawing = g
        )
    }

    @Test fun everyType_breakdownAddsUpAndProgressionConservesVolume() {
        for (type in FastenerType.values()) for (unit in UnitSystem.values()) for (d in if (unit == UnitSystem.METRIC) listOf(5.0, 10.0, 20.0) else listOf(6.35, 12.7)) {
            val r = calc(type, unit, d, len = 6 * d)
            val part = r.finishedPart!!
            assertTrue("$type $unit d=$d volume ${part.volumeMm3}", part.volumeMm3 > 0.0)
            assertEquals("$type $unit d=$d breakdown", part.volumeMm3, part.items.sumOf { it.volumeMm3 }, 1e-6)
            assertEquals("$type $unit d=$d weight", part.volumeMm3 / 1000.0 * 7.85, r.netWeightGrams, 0.011)
            r.boltForging?.let { assertEquals("$type $unit d=$d forging", it.finishedVolumeMm3!!, part.volumeMm3, 1e-6) }
            val plan = r.headingPlan
            if (type.isWasher) assertNull(plan) else {
                assertTrue("$type $unit d=$d volume error ${plan!!.maxVolumeError}", plan.maxVolumeError < 1e-9)
                assertEquals("$type $unit d=$d last station is the finished part", part.heightMm, plan.stations.last().heightMm, 1e-9)
            }
        }
    }

    @Test fun progressionFollowsTheFastenerType() {
        fun last(type: FastenerType) = calc(type).headingPlan!!.stations.last().title
        assertEquals("Trim to hex", last(FastenerType.HEX_BOLT))
        assertEquals("Socket back-extrude", last(FastenerType.SHCS))
        assertEquals("Countersunk head & socket", last(FastenerType.SOCKET_CSK))
        assertEquals("Button head & socket", last(FastenerType.SOCKET_BUTTON))
        assertEquals("Mushroom head & square neck", last(FastenerType.CARRIAGE_BOLT))
        assertEquals("12-point form", last(FastenerType.TWELVE_POINT))
        assertEquals("T-head form", last(FastenerType.T_BOLT))
        assertEquals("Head upset (Hi-Lok head)", last(FastenerType.HI_LOK_PIN))
        assertEquals("Hex & flange form", calc(FastenerType.FLANGE_NUT).headingPlan!!.stations[3].title)
        assertEquals("Hex form", calc(FastenerType.HEX_NUT).headingPlan!!.stations[3].title)
        // off-header work differs by type
        fun off(type: FastenerType) = calc(type).headingPlan!!.offHeaderOps.map { it.title }
        assertTrue("Bending" in off(FastenerType.U_BOLT))
        assertTrue("Drilling" in off(FastenerType.BANJO_BOLT))
        assertTrue("Slot milling" in off(FastenerType.CASTLE_NUT))
        assertTrue("Insert assembly" in off(FastenerType.NYLOC_NUT))
        assertTrue("Fillet rolling" in off(FastenerType.NAS_CLOSE_TOL_BOLT))
        assertEquals(listOf("Pointing", "Thread rolling"), off(FastenerType.HEX_BOLT))
    }

    @Test fun headShapesAndTablesAreTypeSpecific() {
        // ISO 10642 M10: dk 22.4, k 6.2, key 6; the length includes the head; the head is a cone
        val csk = calc(FastenerType.SOCKET_CSK, len = 40.0).finishedPart!!
        val cone = csk.segments.last()
        assertEquals(22.4, cone.dTopMm, 1e-9); assertEquals(6.2, cone.heightMm, 1e-9); assertEquals(40.0, csk.heightMm, 1e-9)
        assertEquals(6.0, csk.recesses.single().sizeMm, 1e-9)
        // ISO 7380 M10 button: dome Ø17.5 × 5.5 = π·h/6·(3a² + h²)
        val dome = calc(FastenerType.SOCKET_BUTTON).finishedPart!!.segments.last()
        assertEquals(SectionShape.DOME, dome.shape)
        assertEquals(PI * 5.5 / 6 * (3 * 8.75 * 8.75 + 5.5 * 5.5), dome.volumeMm3, 1e-6)
        // DIN 603 M10: dome Ø24.65 × 5.38 (table) on a 10 × 10 square neck whose height is an estimate until the drawing gives it
        val carriage = calc(FastenerType.CARRIAGE_BOLT).finishedPart!!
        assertEquals(10.0 * 10.0 * 4.5, carriage.segments.first { it.shape == SectionShape.SQUARE }.volumeMm3, 1e-6)
        assertEquals(24.65, carriage.segments.last().dBottomMm, 1e-9)
        assertTrue(carriage.notOnDrawing.any { it.contains("square neck height f") && it.contains("estimated") })
        // ASME B18.3 3/8": head Ø 0.562, height = D, key 5/16
        val inch = calc(FastenerType.SHCS, UnitSystem.INCH, 9.525, 50.8).finishedPart!!
        assertEquals(0.562 * 25.4, inch.segments.last().dBottomMm, 1e-6); assertEquals(9.525, inch.segments.last().heightMm, 1e-6)
        assertEquals(0.3125 * 25.4, inch.recesses.single().sizeMm, 1e-6)
        // 12-point: bi-hex area (√3 − 3·tan15°)·s²
        assertEquals(0.92820 * 16 * 16 * 6.4, calc(FastenerType.TWELVE_POINT).finishedPart!!.segments.last().volumeMm3, 0.05)
        // types on stand-in dimensions say so
        for (t in listOf(FastenerType.TWELVE_POINT, FastenerType.T_BOLT, FastenerType.FLANGE_NUT, FastenerType.HEAVY_HEX_BOLT))
            assertFalse("$t", DimensionTables.forPart(t, UnitSystem.METRIC, 10.0).isTable)
        assertTrue(DimensionTables.forPart(FastenerType.SOCKET_CSK, UnitSystem.METRIC, 10.0).isTable)
    }

    @Test fun washerIsARing_notABolt() {
        // ISO 7089 M10: 10.5 × 20 × 2
        val r = calc(FastenerType.FLAT_WASHER)
        assertEquals(PI / 4 * (20.0 * 20.0 - 10.5 * 10.5) * 2.0, r.partVolumeMm3, 1e-6)
        assertEquals(3.57, r.netWeightGrams, 0.02)
        val drawn = calc(FastenerType.FLAT_WASHER, g = DrawingGeometry(washerOuterDiaMm = 30.0, washerInnerDiaMm = 11.0, washerThicknessMm = 3.0)).finishedPart!!
        assertEquals(PI / 4 * (900.0 - 121.0) * 3.0, drawn.volumeMm3, 1e-6)
        assertTrue(drawn.isComplete)
    }

    @Test fun drawingDimensionsDriveTheVolume_andAreReported() {
        val standard = calc(FastenerType.SHCS).finishedPart!!
        assertFalse(standard.isComplete)
        assertTrue(standard.notOnDrawing.any { it.startsWith("head Ø dk 16.00 mm — ISO 4762") })
        val g = DrawingGeometry(threadLengthMm = 30.0, shankDiaMm = 9.9, headDiaMm = 15.0, headHeightMm = 9.0, socketSizeMm = 8.0, socketDepthMm = 4.5)
        val drawn = calc(FastenerType.SHCS, g = g)
        val part = drawn.finishedPart!!
        assertTrue(part.notOnDrawing.toString(), part.isComplete)
        val blank = part.blankDiaMm
        val expected = PI / 4 * blank * blank * 30.0 + PI / 4 * 9.9 * 9.9 * 20.0 + PI / 4 * 15.0 * 15.0 * 9.0 - Math.sqrt(3.0) / 2 * 64.0 * 4.5
        assertEquals(expected, part.volumeMm3, 1e-6)
        // the die progression ends at the drawn head, not the table one
        assertEquals(15.0, drawn.headingPlan!!.stations.last().segments.last().dBottomMm, 1e-9)
        assertTrue(drawn.headingPlan!!.maxVolumeError < 1e-9)
    }

    private fun scan(json: String) = GeminiAiService.parseSpecFromJson(JSONObject(json), "test")

    @Test fun scan_reportsWhatTheDrawingDidNotShow() {
        val spec = scan("""{"fastenerType":"SHCS","unitSystem":"METRIC","nominalDiameterMm":6,"lengthMm":0,"pitch":0,"materialKey":"","coating":""}""")
        assertEquals(1.0, spec.pitchValue, 1e-9) // coarse pitch of M6, not 1.5 for every size
        assertTrue(spec.assumed.any { it.startsWith("length") })
        assertTrue(spec.assumed.any { it.startsWith("pitch") })
        assertTrue(spec.assumed.any { it.startsWith("material") })
        assertTrue(spec.assumed.none { it.startsWith("nominal diameter") })
        val vol = DrawingScan.volume(spec)
        assertFalse(vol.isComplete)
        assertTrue(vol.notOnDrawing.any { it.startsWith("length") })
    }

    @Test fun scan_solidsGiveTheWholeVolume_includingFeaturesTheStandardShapeLacks() {
        // M10 × 40 hex bolt with a Ø14 × 3 collar under the head and a Ø7 × 5 dog point
        val spec = scan("""{"fastenerType":"HEX_BOLT","unitSystem":"METRIC","nominalDiameterMm":10,"pitch":1.5,"lengthMm":40,"materialKey":"10B21",
            "propertyClass":"10.9","threadClass":"6g","coating":"GEOMET","headAcrossFlatsMm":16,"headHeightMm":6.4,"threadLengthMm":20,"shankDiaMm":10,
            "solids":[{"feature":"hex head","shape":"HEX","d1":16,"lengthMm":6.4},{"feature":"collar","shape":"CYLINDER","d1":14,"lengthMm":3},
              {"feature":"plain shank","shape":"CYLINDER","d1":10,"lengthMm":12},{"feature":"thread","shape":"CYLINDER","d1":10,"lengthMm":20,"threaded":true},
              {"feature":"dog point","shape":"CYLINDER","d1":7,"lengthMm":5}],
            "cutouts":[{"feature":"cross hole","shape":"CYLINDER","d1":2,"lengthMm":10,"count":1,"from":"CROSS"}]}""")
        assertEquals(5, spec.solids.size)
        val result = DrawingScan.calculate(spec)
        val vol = DrawingScan.volume(spec, result)
        assertEquals(VolumeBasis.SOLIDS, vol.basis)
        val blank = result.blankTargetDia
        val expected = Math.sqrt(3.0) / 2 * 256 * 6.4 + PI / 4 * 196 * 3 + PI / 4 * 100 * 12 + PI / 4 * blank * blank * 20 + PI / 4 * 49 * 5 - PI / 4 * 4 * 10
        assertEquals(expected, vol.totalMm3, 1e-6)
        assertEquals(vol.totalMm3, vol.items.sumOf { it.volumeMm3 }, 1e-9)
        assertEquals(46.4, vol.outline!!.heightMm, 1e-9)
        assertTrue(vol.cutOffLengthMm > vol.totalMm3 / (PI / 4 * result.wireDiaMm * result.wireDiaMm)) // carries the trim cheese
    }

    @Test fun scan_solidsThatDoNotAddUpAreNotUsed() {
        val spec = scan("""{"fastenerType":"HEX_BOLT","unitSystem":"METRIC","nominalDiameterMm":10,"pitch":1.5,"lengthMm":40,"headAcrossFlatsMm":16,"headHeightMm":6.4,
            "solids":[{"feature":"hex head","shape":"HEX","d1":16,"lengthMm":6.4},{"feature":"thread","shape":"CYLINDER","d1":10,"lengthMm":20,"threaded":true}]}""")
        val vol = DrawingScan.volume(spec)
        assertEquals(VolumeBasis.DIMENSIONS, vol.basis)
        assertTrue(vol.cautions.any { it.contains("add up to 26.40 mm") })
        // a section with no size drops the whole list
        assertTrue(scan("""{"fastenerType":"HEX_BOLT","solids":[{"shape":"HEX","d1":16,"lengthMm":0}]}""").solids.isEmpty())
    }

    @Test fun coPilot_takesTheDrawingFirst() {
        val spec = scan("""{"fastenerType":"SHCS","unitSystem":"METRIC","nominalDiameterMm":10,"pitch":1.5,"lengthMm":50,"materialKey":"4140","propertyClass":"12.9",
            "coating":"GEOMET","detectedDimensions":[{"feature":"Head dia","nominal":"16 mm","tolerance":"h13"}],"summary":"M10 socket screw"}""")
        val result = DrawingScan.calculate(spec)
        val ctx = GeminiAiService.drawingContext(spec, result)
        assertTrue(ctx, ctx.contains("TOTAL VOLUME AS PER DRAWING") && ctx.contains("Head dia: 16 mm h13"))
        val local = GeminiAiService.tryAnswerLocally("what is the volume and weight?", result, spec)!!
        assertTrue(local.contains("From the scanned drawing") && local.contains("mm³"))
        // without a drawing the volume question is not answered from one
        assertFalse(GeminiAiService.tryAnswerLocally("what is the volume?", result, null)?.contains("scanned drawing") ?: false)
        val prompt = GeminiAiService.buildExtractionPrompt()
        assertTrue(prompt.contains("\"solids\"") && prompt.contains("socketDepthMm") && prompt.contains("washerThicknessMm"))
    }
}
