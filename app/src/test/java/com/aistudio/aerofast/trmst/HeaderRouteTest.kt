package com.aistudio.aerofast.trmst

import com.aistudio.aerofast.trmst.data.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Headed parts run on a progressive former, or on a 1-die 2-blow header when no extrusion and ≤ 2 blows are needed; a nut runs on a nut
 * former and a washer is not headed at all. The head volume is also reported on its own.
 */
class HeaderRouteTest {
    private fun calc(type: FastenerType, d: Double, len: Double, mat: String = "10B21"): FastenerCalculationResult {
        val idx = FastenerCatalog.sizeIndexFor(UnitSystem.METRIC, d)
        return CalculationEngine.calculate(
            fastenerType = type, unitSystem = UnitSystem.METRIC, standard = PokaYokeEngine.defaultStandard(type, UnitSystem.METRIC),
            nominalSizeIndex = idx, pitchValue = FastenerCatalog.METRIC_SIZES[idx].pitches.first(), lengthMm = len,
            threadClass = if (type.isNut) "6H" else "6g", material = FastenerCatalog.MATERIALS[mat]!!, coating = CoatingType.GEOMET,
            batchQty = 100, propertyClass = PropertyClass.CLASS_10_9
        )
    }

    @Test fun oneDieTwoBlowOnlyWhereItIsEnough() {
        assertEquals(
            listOf("MULTI_STATION", "NUT_FORMER", "PRESS", "TWO_BLOW"),
            HeaderRoute.values().map { it.name }
        )
        for (type in FastenerType.values()) {
            if (type.isWasher) continue
            val r = calc(type, 10.0, 50.0)
            val plan = r.headingPlan!!
            if (plan.route == HeaderRoute.TWO_BLOW) {
                // No extrusion, no trimming, at most two blows on one die
                assertEquals("$type", 3, plan.stations.size)
                assertTrue("$type", plan.upsetBlows <= 2)
                assertTrue("$type", plan.stations.none { it.title.contains("extrude", ignoreCase = true) || it.title.contains("Trim") })
                assertTrue("$type", type.category == "Machine Screws" || r.finishedPart!!.style == HeadStyle.HEX_FORMED)
                assertTrue("$type", plan.maxVolumeError < 1e-9)
            } else {
                assertTrue("$type ran on ${plan.route}", plan.route == HeaderRoute.MULTI_STATION || plan.route == HeaderRoute.NUT_FORMER)
            }
        }
        // A full-thread pan head screw qualifies; a partial-thread bolt that must be extruded does not
        assertEquals(HeaderRoute.TWO_BLOW, calc(FastenerType.PAN_HEXALOBULAR, 4.0, 10.0).headingPlan!!.route)
        assertEquals(HeaderRoute.MULTI_STATION, calc(FastenerType.HEX_BOLT, 10.0, 60.0).headingPlan!!.route)
    }

    @Test fun aFullyThreadedBoltIsHeadedOnAProgressiveFormer() {
        val plan = calc(FastenerType.HEX_BOLT_FULL, 10.0, 40.0).headingPlan!!
        assertEquals(HeaderRoute.MULTI_STATION, plan.route)
        assertTrue(plan.stations.any { it.title.contains("Trim") })
        assertTrue(plan.maxVolumeError < 1e-9)
    }

    @Test fun aPartialThreadBoltIsExtrudedThenTrimmed() {
        val r = calc(FastenerType.HEX_BOLT, 10.0, 60.0)
        assertEquals(HeaderRoute.MULTI_STATION, r.headingPlan!!.route)
        assertTrue(r.recommendedMachine, r.recommendedMachine.contains("die progressive former"))
        assertTrue(r.headingPlan!!.stations.any { it.operation.contains("extrude") })
    }

    @Test fun nutsRunOnANutFormerAndWashersAreNotHeaded() {
        assertEquals(HeaderRoute.NUT_FORMER, calc(FastenerType.HEX_NUT, 10.0, 10.0).headingPlan!!.route)
        assertNull(calc(FastenerType.FLAT_WASHER, 10.0, 2.0).headingPlan)
    }

    @Test fun headAndShankVolumesAddUpToTheTotal() {
        for (type in FastenerType.values()) for (d in listOf(6.0, 10.0, 20.0)) {
            val r = calc(type, d, 5 * d)
            assertEquals("$type d=$d", r.partVolumeMm3, r.headVolumeMm3 + r.shankVolumeMm3, 1e-9)
            assertTrue("$type d=$d negative head volume ${r.headVolumeMm3}", r.headVolumeMm3 >= -1e-9)
            assertEquals("$type d=$d weights", r.partVolumeMm3 / 1000.0 * r.material.density,
                r.headWeightGrams + r.shankWeightGrams, 1e-9)
            // a nut or a washer is formed entirely above the die face
            if (type.isNut || type.isWasher) assertEquals("$type d=$d", r.partVolumeMm3, r.headVolumeMm3, 1e-9)
        }
    }

    @Test fun theScannedDrawingVolumeAlsoSplitsHeadFromShank() {
        val idx = FastenerCatalog.sizeIndexFor(UnitSystem.METRIC, 10.0)
        val spec = ExtractedFastenerSpec(
            fastenerType = FastenerType.HEX_BOLT, unitSystem = UnitSystem.METRIC, nominalSizeIndex = idx,
            nominalDiaMm = 10.0, pitchValue = 1.5, lengthMm = 60.0, propertyClass = PropertyClass.CLASS_10_9,
            materialKey = "10B21", threadClass = "6g", coating = CoatingType.GEOMET,
            technicalSummary = "test", detectedDimensions = emptyList(), isAiVisionExtracted = false
        )
        val vol = DrawingScan.volume(spec)
        assertTrue(vol.totalMm3 > 0.0)
        assertEquals(vol.totalMm3, vol.headMm3 + vol.shankMm3, 1e-9)
        assertTrue(vol.headMm3 > 0.0)
    }
}
