package com.aistudio.aerofast.trmst

import com.aistudio.aerofast.trmst.data.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Flange bolts (DIN 6921) and flange nuts (DIN 6923) are built with their flange from the tables. */
class FlangeProductTest {
    private fun calc(t: FastenerType, std: ProductStandard, d: Double, len: Double, tc: String) = CalculationEngine.calculate(
        t, UnitSystem.METRIC, std, FastenerCatalog.sizeIndexFor(UnitSystem.METRIC, d), if (d == 10.0) 1.5 else 1.25, len, tc,
        FastenerCatalog.MATERIALS.getValue("10B21"), CoatingType.ZN_NI, 10000, propertyClass = PropertyClass.CLASS_8_8)

    @Test fun flangeBoltHasItsFlangeWithoutADrawing() {
        val r = calc(FastenerType.FLANGE_BOLT, ProductStandard.ISO_1665, 10.0, 40.0, "6g")
        val flange = r.finishedPart!!.segments.single { it.shape == SectionShape.ROUND && it.dBottomMm == 22.3 }
        assertEquals(1.5, flange.heightMm, 1e-9)              // DIN 6921 M10: dc 22.3, c 1.5
        val hex = r.finishedPart!!.segments.last()
        assertEquals(SectionShape.HEX, hex.shape); assertEquals(15.0, hex.dBottomMm, 1e-9); assertEquals(9.2 - 1.5, hex.heightMm, 1e-9)
        assertTrue(r.headingPlan!!.stations.last().operation.contains("Coin flange Ø22.30"))
        assertTrue(r.finishedPart!!.cautions.none { it.contains("not in the volume.") && it.startsWith("Flange Ø dc and thickness c are not both") })
    }

    @Test fun flangeNutUsesDin6923NotThePlainHexNut() {
        val r = calc(FastenerType.FLANGE_NUT, ProductStandard.DIN_6923, 10.0, 10.0, "6H")
        assertEquals("DIN 6923 (supplier tables)", r.headDimensions!!.source)
        val segs = r.finishedPart!!.segments
        assertEquals(21.8, segs.first().dBottomMm, 1e-9); assertEquals(1.5, segs.first().heightMm, 1e-9)
        assertEquals(15.0, segs.last().dBottomMm, 1e-9)        // s 15, not the ISO 4032 16
        assertEquals(10.0, segs.sumOf { it.heightMm }, 1e-9)   // m 10
        assertTrue(r.headingPlan!!.stations.any { it.operation.contains("flange Ø21.80") })
    }

    @Test fun sizesOutsideTheTablesSayTheyAreApproximated() {
        assertTrue(DimensionTables.forPart(FastenerType.FLANGE_NUT, UnitSystem.METRIC, 14.0).source.startsWith("Approximated"))
        assertEquals(null, BoltForging.din6921(20.0)!!.c)      // M20 bolt c not confirmed by a source
    }
}
