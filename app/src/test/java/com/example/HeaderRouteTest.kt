package com.example

import com.example.data.*
import org.junit.Assert.*
import org.junit.Test

/** A 1-die header and a progressive former are different machines, so they must give different progressions. */
class HeaderRouteTest {
    private fun calc(type: FastenerType, d: Double, len: Double, route: HeaderRoute? = null, mat: String = "10B21"): FastenerCalculationResult {
        val idx = FastenerCatalog.sizeIndexFor(UnitSystem.METRIC, d)
        return CalculationEngine.calculate(
            fastenerType = type, unitSystem = UnitSystem.METRIC, standard = PokaYokeEngine.defaultStandard(type, UnitSystem.METRIC),
            nominalSizeIndex = idx, pitchValue = FastenerCatalog.METRIC_SIZES[idx].pitches.first(), lengthMm = len,
            threadClass = if (type.isNut) "6H" else "6g", material = FastenerCatalog.MATERIALS[mat]!!, coating = CoatingType.GEOMET,
            batchQty = 100, propertyClass = PropertyClass.CLASS_10_9, headerRoute = route
        )
    }

    @Test fun aFullyThreadedBoltRunsInOneDie_andItsHexIsTrimmedOffTheMachine() {
        val r = calc(FastenerType.HEX_BOLT_FULL, 10.0, 40.0)
        val plan = r.headingPlan!!
        assertEquals(HeaderRoute.ONE_DIE, plan.route)
        // one die: cut-off plus the blows, and no extrusion station
        assertEquals(listOf("Shear cut-off", "Cone pre-upset", "Head upset (round, for trimming)"), plan.stations.map { it.title })
        assertEquals(2, plan.upsetBlows)
        assertTrue(plan.stations.none { it.operation.contains("extrude") })
        // the wire is already the rolling blank, so the slug is that diameter
        assertEquals(r.blankTargetDia, plan.slugDiaMm, 0.001)
        // the trim is a separate press, not a die station
        assertTrue(plan.stations.none { it.title.contains("Trim") })
        assertEquals("Trim to hex", plan.offHeaderOps.first().title)
        assertTrue(plan.offHeaderOps.first().operation.contains("separate trimming press"))
        assertTrue("1-die 2-blow header" in r.recommendedMachine)
        assertTrue(plan.maxVolumeError < 1e-9)
    }

    @Test fun aPartialThreadBoltCannotRunInOneDie_becauseTheShankIsExtruded() {
        val r = calc(FastenerType.HEX_BOLT, 10.0, 60.0)
        assertEquals(HeaderRoute.MULTI_STATION, r.headingPlan!!.route)
        assertTrue(r.headingPlan!!.routeReason, r.headingPlan!!.routeReason.contains("extrusion station"))
        assertTrue(r.recommendedMachine, r.recommendedMachine.contains("die progressive former"))
        // asking for one die is refused, with the reason
        val forced = calc(FastenerType.HEX_BOLT, 10.0, 60.0, HeaderRoute.ONE_DIE)
        assertEquals(HeaderRoute.MULTI_STATION, forced.headingPlan!!.route)
        assertTrue(forced.headingPlan!!.routeReason.startsWith("A 1-die header cannot make this part"))
    }

    @Test fun eachBlockerSendsThePartToAProgressiveFormer() {
        // a socket has to be back-extruded in its own station
        assertTrue(calc(FastenerType.SHCS, 10.0, 40.0, HeaderRoute.ONE_DIE).headingPlan!!.routeReason.contains("back-extruded"))
        // a nut is pierced on a nut former
        assertEquals(HeaderRoute.NUT_FORMER, calc(FastenerType.HEX_NUT, 10.0, 10.0).headingPlan!!.route)
        // a washer is not headed at all
        assertNull(calc(FastenerType.FLAT_WASHER, 10.0, 2.0).headingPlan)
        // too much head stock for two blows
        val big = calc(FastenerType.HEAVY_HEX_BOLT, 20.0, 40.0, HeaderRoute.ONE_DIE).headingPlan!!
        assertTrue(big.routeReason, big.route == HeaderRoute.ONE_DIE || big.routeReason.contains("L/D"))
    }

    @Test fun aRoundHeadNeedsNoTrim_soTheSecondBlowIsTheFinishedHead() {
        val idx = FastenerCatalog.sizeIndexFor(UnitSystem.METRIC, 5.0)
        val r = CalculationEngine.calculate(
            fastenerType = FastenerType.SOLID_RIVET, unitSystem = UnitSystem.METRIC, standard = ProductStandard.MS20470,
            nominalSizeIndex = idx, pitchValue = 0.8, lengthMm = 12.0, threadClass = "6g",
            material = FastenerCatalog.MATERIALS["AL2117"]!!, coating = CoatingType.ANODIZE_II, batchQty = 100, propertyClass = null
        )
        val plan = r.headingPlan!!
        assertEquals(HeaderRoute.ONE_DIE, plan.route)
        assertTrue(plan.stations.last().title, plan.stations.last().title.startsWith("Head upset (universal head)"))
        assertEquals(SectionShape.DOME, plan.stations.last().segments.last().shape)
        assertTrue(plan.offHeaderOps.none { it.title.contains("Trim") })
        // nothing is trimmed, so the slug is exactly the finished part
        assertEquals(r.partVolumeMm3, plan.slugVolumeMm3, 1e-9)
    }

    @Test fun forcingAProgressiveFormerOnAOneDiePartIsAllowed_andSaysSo() {
        val forced = calc(FastenerType.HEX_BOLT_FULL, 10.0, 40.0, HeaderRoute.MULTI_STATION).headingPlan!!
        assertEquals(HeaderRoute.MULTI_STATION, forced.route)
        assertTrue(forced.routeReason, forced.routeReason.contains("would also run in one die"))
        assertTrue(forced.stations.any { it.title.contains("Trim") })
        assertTrue(forced.maxVolumeError < 1e-9)
    }
}
