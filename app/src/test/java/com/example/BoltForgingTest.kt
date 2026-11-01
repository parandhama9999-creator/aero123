package com.example

import com.example.data.*
import org.junit.Assert.assertEquals
import org.junit.Test
class BoltForgingTest {
    @Test fun m10HexMatchesHandCalc() {
        val head = HeadDimensions(16.0, 6.4, 14.63, 8.4, "ISO 4014", true)
        val f = BoltForging.compute(FastenerType.HEX_BOLT, UnitSystem.METRIC, 10.0, 1.5, 60.0, 8.962, 9.85, head, 7.85,
            null, null, null, null, null, null)
        assertEquals(26.0, f.threadLengthMm, 1e-9)
        assertEquals(1.66, f.coneLengthMm, 0.01)
        assertEquals(23.04, f.stockToExtrudeMm, 0.01)
        assertEquals(55.38, f.lengthUnderHeadBeforeMm, 0.01)
        assertEquals(1052.0, f.cheeseVolumeMm3!!, 1.0)
        assertEquals(32.42, f.headStockMm!!, 0.01)
        assertEquals(87.80, f.cutOffLengthMm!!, 0.02)
        // slug volume = finished + cheese = cut-off × shank area
        assertEquals(f.cutOffLengthMm!! * Math.PI / 4 * 9.85 * 9.85, f.finishedVolumeMm3!! + f.cheeseVolumeMm3!!, 0.5)
    }
}

class ProgressionMatchesCardTest {
    @Test fun dieProgressionUsesSameSlugAsCard() {
        val m = FastenerCatalog.MATERIALS["10B21"]!!
        for (d in listOf(6.0, 10.0, 16.0)) for (len in listOf(40.0, 60.0, 150.0)) for (scan in listOf(false, true)) {
            val idx = FastenerCatalog.sizeIndexFor(UnitSystem.METRIC, d)
            val r = CalculationEngine.calculate(FastenerType.HEX_BOLT, UnitSystem.METRIC, ProductStandard.ISO_4014, idx,
                FastenerCatalog.METRIC_SIZES[idx].pitches.first(), len, "6g", m, CoatingType.ZN_NI, 100,
                propertyClass = PropertyClass.CLASS_8_8,
                drawingThreadLengthMm = if (scan) 2 * d + 10 else null, drawingShankDiaMm = if (scan) d - 0.1 else null,
                drawingAcrossFlatsMm = if (scan) d * 1.5 else null, drawingHeadHeightMm = if (scan) d * 0.65 else null)
            val plan = r.headingPlan!!
            val card = BoltForging.compute(FastenerType.HEX_BOLT, UnitSystem.METRIC, d, FastenerCatalog.METRIC_SIZES[idx].pitches.first(), len,
                r.blankTargetDia, r.wireDiaMm, r.headDimensions,  m.density,
                if (scan) 2 * d + 10 else PokaYokeEngine.threadLengthB(FastenerType.HEX_BOLT, UnitSystem.METRIC, d, len),
                if (scan) d - 0.1 else null, if (scan) d * 1.5 else null, if (scan) d * 0.65 else null, null, null)
            assertEquals("d=$d L=$len scan=$scan", card.cutOffLengthMm!!, plan.slugLengthMm, 0.01)
            assertEquals("Trim to hex", plan.stations.last().title)
            org.junit.Assert.assertTrue("vol err ${plan.maxVolumeError}", plan.maxVolumeError < 0.005)
        }
    }
}

class FlangeProgressionTest {
    @Test fun flangeBoltWithThicknessGetsTrimmedCollar() {
        val m = FastenerCatalog.MATERIALS["10B21"]!!
        val idx = FastenerCatalog.sizeIndexFor(UnitSystem.METRIC, 10.0)
        val r = CalculationEngine.calculate(FastenerType.FLANGE_BOLT, UnitSystem.METRIC, ProductStandard.ISO_1665, idx, 1.5, 60.0, "6g", m,
            CoatingType.ZN_NI, 100, propertyClass = PropertyClass.CLASS_8_8, drawingFlangeThicknessMm = 2.0)
        val plan = r.headingPlan!!
        assertEquals("Trim flange collar", plan.stations.last().title)
        val f = r.boltForging!!
        assertEquals(f.cutOffLengthMm!!, plan.slugLengthMm, 0.01)
        org.junit.Assert.assertTrue("vol err ${plan.maxVolumeError}", plan.maxVolumeError < 0.005)
        // DIN 6921 M10: dc 22.3 → collar 26.3 × 2.0; cheese = π/4 (26.3² − 22.3²) × 2
        assertEquals(Math.PI / 4 * (26.3 * 26.3 - 22.3 * 22.3) * 2.0, f.cheeseVolumeMm3!!, 0.01)
        // Without c the old progression is kept and no forging figures are claimed
        val r2 = CalculationEngine.calculate(FastenerType.FLANGE_BOLT, UnitSystem.METRIC, ProductStandard.ISO_1665, idx, 1.5, 60.0, "6g", m,
            CoatingType.ZN_NI, 100, propertyClass = PropertyClass.CLASS_8_8)
        assertEquals(null, r2.boltForging)
    }
}
